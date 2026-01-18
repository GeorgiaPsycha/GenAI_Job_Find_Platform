package dev.ctrlspace.genai2506.genaibe;

import dev.ctrlspace.genai2506.genaibe.controllers.UserController;
import dev.ctrlspace.genai2506.genaibe.exceptions.GlobalExceptionHandler;
import dev.ctrlspace.genai2506.genaibe.models.entities.User;
import dev.ctrlspace.genai2506.genaibe.repositories.UserRepository;
import dev.ctrlspace.genai2506.genaibe.services.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@ComponentScan(basePackageClasses = {
        UserController.class,
        GenAiBeApplication.class,
        UserService.class,
        UserRepository.class,
        GlobalExceptionHandler.class
})
@EnableJpaRepositories(basePackageClasses = {UserRepository.class})
@EntityScan(basePackageClasses = {
        User.class
})
@Configuration
public class GenAiBeApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(GenAiBeApplication.class, args);
    }


    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // IMPORTANT (see note below)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
