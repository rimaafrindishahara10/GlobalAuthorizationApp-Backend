package com.dishahara.services;

import com.dishahara.dtos.UserDto;

public interface UserService {

    //Create-> User
    UserDto createUser(UserDto userDto);
    //Update-> User
    UserDto updateUser(UserDto userDto, String userId);
    //Delete -> User
    void deleteUser(String userId);
    //Get -> All users
    Iterable<UserDto> findAllUsers();
    //Get -> User by ID
    UserDto findUserById(String userId);
    //Get -> User By Email
    UserDto findUserByEmail(String email);


}
