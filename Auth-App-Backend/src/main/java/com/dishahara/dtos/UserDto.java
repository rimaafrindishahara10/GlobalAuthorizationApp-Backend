package com.dishahara.dtos;

import com.dishahara.entities.Provider;
import com.dishahara.entities.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private UUID id;
    private String name;
    private String email;
    private String password;
    private String imageUrl;
    private boolean enable = true;
    private LocalDateTime createdAt ;
    private LocalDateTime updatedAt ;
    private Provider provider=Provider.LOCAL;
    private Set<RoleDto> roles= new HashSet<>();
}
