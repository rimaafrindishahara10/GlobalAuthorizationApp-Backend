package com.dishahara.services;

import com.dishahara.dtos.UserDto;

public interface AuthService {

    //Register->User
    UserDto registerUser(UserDto userDto);

    //Login -> User
}
