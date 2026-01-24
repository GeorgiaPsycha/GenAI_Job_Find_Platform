package dev.genai.genaibe.services;


import dev.genai.genaibe.exceptions.GenAiException;
import dev.genai.genaibe.models.entities.User;
import dev.genai.genaibe.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {


    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserById(UUID id) throws Exception {

        return userRepository.findById(id)
                .orElseThrow(() -> new GenAiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }




}
