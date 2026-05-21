package com.dishahara.services.impl;

import com.dishahara.dtos.UserDto;
import com.dishahara.entities.Provider;
import com.dishahara.entities.User;
import com.dishahara.exceptions.ResourceNotFoundException;
import com.dishahara.helpers.UserHelper;
import com.dishahara.repositories.UserRepository;
import com.dishahara.services.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import javax.management.InvalidAttributeValueException;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Transactional
    @Override
    public UserDto createUser(UserDto userDto) {
        //If email is null or blank
        if (userDto.getEmail()==null|| userDto.getEmail().isBlank()){
            throw new IllegalArgumentException("Email is required");
        }
        if(userRepository.existsByEmail(userDto.getEmail())){
            throw new IllegalArgumentException("Email already exists");
        }
        User user = modelMapper.map(userDto, User.class);
        user.setProvider(userDto.getProvider()!=null?userDto.getProvider(): Provider.LOCAL);
        //Assign user role from authorization
        //TODO:
        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserDto.class);
    }

    @Override
    public UserDto updateUser(UserDto userDto, String userId) {
        if (userDto.getEmail()!=null){
            throw new IllegalArgumentException("Email cannot be changed");
        }
        UUID uId = UserHelper.parseUUID(userId);
        User existUser = userRepository.findById(uId).orElseThrow(() -> new ResourceNotFoundException("User not found by given id"));
        if(userDto.getName()!=null) existUser.setName(userDto.getName());
        if(userDto.getImageUrl()!=null) existUser.setImageUrl(userDto.getImageUrl());
        if(userDto.getProvider()!=null) existUser.setProvider(userDto.getProvider());
        //TODO: change password updating logic....
        if (userDto.getPassword()!=null) existUser.setPassword(userDto.getPassword());
        existUser.setEnable(userDto.isEnable());
        User user = userRepository.save(existUser);
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public void deleteUser(String userId) {
        UUID uId = UserHelper.parseUUID(userId);
        User user = userRepository.findById(uId).orElseThrow(() -> new ResourceNotFoundException("User not found by given id"));
        userRepository.delete(user);

    }

    @Override
    @Transactional
    public Iterable<UserDto> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> modelMapper.map(user,UserDto.class))
                .toList();
    }

    @Override
    public UserDto findUserById(String userId) {
        UUID uId = UserHelper.parseUUID(userId);
        User user = userRepository.findById(uId).orElseThrow(() -> new ResourceNotFoundException("User not found by given id"));
        return modelMapper.map(user,UserDto.class);
    }

    @Override
    public UserDto findUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("User not found by given email"));
        return modelMapper.map(user,UserDto.class);
    }
}
