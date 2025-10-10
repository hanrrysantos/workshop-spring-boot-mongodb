# Workshop MongoDB com Spring Boot - Análise Detalhada

Este projeto é uma API RESTful construída com **Spring Boot** para gerenciar usuários e posts, utilizando o **MongoDB** como banco de dados NoSQL. O projeto adota uma arquitetura em camadas clara (Resource, Service, Repository, Domain, DTO) e implementa funcionalidades avançadas como consultas customizadas com `@Query` e tratamento de exceções global.

## 🚀 Tecnologias e Configurações

| Recurso | Detalhe | Arquivo Fonte |
| :--- | :--- | :--- |
| **Linguagem** | Java 21 | `pom.xml` |
| **Framework** | Spring Boot 3.5.6 | `pom.xml` |
| **Persistência** | Spring Data MongoDB | `pom.xml` |
| **Banco URI** | `mongodb://localhost:27017/workshop_mongo` | `application.properties` |

## 📐 Arquitetura do Código

O projeto está dividido em pacotes que representam as camadas da aplicação:

### 1. Pacote `domain`

Contém as entidades (coleções) mapeadas para o MongoDB.

* **`User.java`**:
    * Entidade principal, marcada com `@Document`.
    * Possui os campos `id`, `name`, e `email`.
    * Contém uma lista de `Post`s, mapeada com `@DBRef(lazy = true)` para armazenar apenas a referência (ID) do post no documento do usuário, garantindo o carregamento lento.
* **`Post.java`**:
    * Entidade para as publicações.
    * Campos: `id`, `date`, `title`, `body`.
    * Referência para o autor: Utiliza o DTO `AuthorDTO` para armazenar o ID e o nome do autor *dentro* do documento Post (denormalização).
    * Comentários: Utiliza uma lista de DTOs `CommentDTO` para armazenar os comentários *embutidos* no documento Post (denormalização).

### 2. Pacote `dto` (Data Transfer Object)

Usado para transferir dados entre camadas e expor apenas os dados necessários na API, evitando que as entidades de domínio sejam expostas diretamente.

* **`UserDTO.java`**: Versão simplificada do `User` com `id`, `name`, e `email`, usada para listar e inserir usuários.
* **`AuthorDTO.java`**: Representa a informação mínima do autor (apenas `id` e `name`) para ser embutida dentro do `Post` ou `CommentDTO`.
* **`CommentDTO.java`**: Representa um comentário, contendo `text`, `date`, e um `AuthorDTO`.

### 3. Pacote `repositories`

Interfaces de acesso a dados que estendem `MongoRepository`.

* **`UserRepository.java`**: Interface básica para operações CRUD de `User`.
* **`PostRepository.java`**:
    * **`findByTitleContainingIgnoreCase(String text)`**: Método de busca implícito do Spring Data, que traduz o nome para uma query MongoDB de busca por título insensível a maiúsculas/minúsculas.
    * **`searchTitle(String text)`**: Equivalente ao método acima, mas com uma consulta MongoDB explícita (`@Query`) usando regex: `{ 'title': { $regex: ?0, $options: 'i' } }`.
    * **`fullSearch(String text, Date minDate, Date maxDate)`**: Query complexa com operador `$and` e `$or` para buscar o `text` no título, corpo ou nos comentários, e aplicar filtros de data (`$gte` e `$lte`).

### 4. Pacote `services`

Camada de lógica de negócio (Business Logic).

* **`UserService.java`**:
    * **`findAll()`**: Retorna todos os usuários.
    * **`findById(String id)`**: Busca um usuário, lançando `ObjectNotFoundException` se não for encontrado (utiliza `orElseThrow`).
    * **`insert(User obj)`**: Insere um novo usuário.
    * **`update(User obj)`**: Atualiza dados do usuário (somente `name` e `email`).
    * **`fromDTO(UserDTO objDTO)`**: Método auxiliar para converter um `UserDTO` em uma entidade `User`.
* **`PostService.java`**:
    * Implementa `findById`, `findByTitle` (usando `searchTitle` do repository) e `fullSearch`.
    * Ajusta a data máxima (`maxDate`) no `fullSearch` para incluir o dia inteiro: `maxDate.getTime() + 24 * 60 * 60 * 1000`.

### 5. Pacote `resources`

Camada de controladores REST (Controllers).

* **`UserResource.java`**:
    * Mapeado para `/users`.
    * **`findAll()`**: Converte a lista de entidades `User` retornada pelo serviço em uma lista de `UserDTO`s usando `stream().map().collect(Collectors.toList())`.
    * **`insert()`**: Cria a URI do novo recurso inserido no cabeçalho HTTP de resposta (status `201 Created`).
    * **`findPosts(@PathVariable String id)`**: Rota `/users/{id}/posts` que retorna os posts do usuário, aproveitando o `@DBRef` carregado pelo serviço.
* **`PostResource.java`**:
    * Mapeado para `/posts`.
    * Contém os endpoints de busca `titlesearch` e `fullsearch`, utilizando a classe de utilidade `URL` para decodificar parâmetros e converter datas.

### 6. Pacotes Auxiliares (`config`, `exception`, `util`)

#### `config/Instantiation.java`
* Classe de configuração que implementa `CommandLineRunner`.
* É responsável por limpar as coleções (`deleteAll()`) e popular o banco de dados com dados de teste (`Maria Brown`, `Alex Green`, `Bob Grey` e posts relacionados) na inicialização da aplicação.

#### `services/exception/ObjectNotFoundException.java`
* Exceção customizada que estende `RuntimeException` para ser lançada quando um ID não for encontrado.

#### `resources/exception/StandardError.java` e `resources/exception/ResourceExceptionHandler.java`
* **`StandardError.java`**: Classe de modelo para estruturar a resposta de erro HTTP (timestamp, status, error, message, path).
* **`ResourceExceptionHandler.java`**: Implementa `@ControllerAdvice` para interceptar a `ObjectNotFoundException` em todo o projeto e retornar uma resposta HTTP padronizada com status `404 NOT FOUND`.

#### `resources/util/URL.java`
* Classe utilitária com métodos estáticos para:
    * **`decodeParam(String text)`**: Decodificar parâmetros de URL (ex: `+` ou `%20` para espaço).
    * **`convertDate(String textDate, Date defaultValue)`**: Converter uma string de data (`yyyy-MM-dd`) para um objeto `java.util.Date`, tratando a TimeZone como GMT.
