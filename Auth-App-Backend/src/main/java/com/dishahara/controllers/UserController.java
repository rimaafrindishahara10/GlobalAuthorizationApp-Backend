package com.dishahara.controllers;

import com.dishahara.dtos.UserDto;
import com.dishahara.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    //ADD->USER
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(userDto));
    }
    //GET -> ALL USERS
    @GetMapping
    public ResponseEntity<Iterable<UserDto>> findAllUsers(){
        return ResponseEntity.status(HttpStatus.OK).body(userService.findAllUsers());
    }
    //GET - USER BY EMAIL
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> findUserByEmail(@PathVariable String email){
        return ResponseEntity.status(HttpStatus.OK).body(userService.findUserByEmail(email));
    }
    //DELETE -> USER BY ID
    @GetMapping("/delete/{id}")
    public  ResponseEntity<String> deleteUserById(@PathVariable String id){
        userService.deleteUser(id);
        return ResponseEntity.status(HttpStatus.OK).body("User Deleted Successfully");
    }
    //GET -> USER BY ID
    @GetMapping("/{id}")
    public  ResponseEntity<UserDto> getUserById(@PathVariable String id){
        return ResponseEntity.status(HttpStatus.OK).body(userService.findUserById(id));
    }

    //UPDATE -> USER BY ID
    @PutMapping("/{id}")
    public  ResponseEntity<UserDto> updateUserById(@RequestBody UserDto userDto,@PathVariable String id){
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateUser(userDto,id));
    }


}
