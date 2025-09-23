package com.hanrry.workshopmongo.resources;

import com.hanrry.workshopmongo.domain.Post;
import com.hanrry.workshopmongo.domain.User;
import com.hanrry.workshopmongo.dto.UserDTO;
import com.hanrry.workshopmongo.resources.util.URL;
import com.hanrry.workshopmongo.services.PostService;
import com.hanrry.workshopmongo.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value= "/posts")
public class PostResource {

    @Autowired
    private PostService service;

    @GetMapping(value="/{id}")
    public ResponseEntity<Post> findById(@PathVariable String id){
        Post obj = service.findById(id);
        return ResponseEntity.ok().body(obj);
    }

    @GetMapping(value="/titlesearch")
    public ResponseEntity<List<Post>> findByTitle(@RequestParam(value="text", defaultValue = "") String text){
        text= URL.decodeParam(text);
        List<Post> obj = service.findByTitle(text);
        return ResponseEntity.ok().body(obj);
    }
}
