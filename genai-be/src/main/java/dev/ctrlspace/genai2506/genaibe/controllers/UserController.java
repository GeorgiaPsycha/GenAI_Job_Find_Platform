package dev.ctrlspace.genai2506.genaibe.controllers;

import dev.ctrlspace.genai2506.genaibe.models.entities.User;
import dev.ctrlspace.genai2506.genaibe.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class UserController {


    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/{id}")
    public User getUserById(@PathVariable UUID id) throws Exception {
        return userService.getUserById(id);
    }

}
