package com.dishahara.controllers;

import com.dishahara.dtos.LoginRequest;
import com.dishahara.dtos.RefreshTokenRequest;
import com.dishahara.dtos.TokenResponse;
import com.dishahara.dtos.UserDto;
import com.dishahara.entities.RefreshToken;
import com.dishahara.entities.User;
import com.dishahara.repositories.RefreshTokenRepo;
import com.dishahara.repositories.UserRepository;
import com.dishahara.security.CookieService;
import com.dishahara.security.JwtService;
import com.dishahara.services.AuthService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final RefreshTokenRepo refreshTokenRepo;
    private final CookieService cookieService;

    //Register -> User
    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.registerUser(userDto));
    }

    //Access refresh token and renew refresh token:
    @PostMapping("/refreshToken")
    public ResponseEntity<TokenResponse> refreshToken(@RequestBody(required = false) RefreshTokenRequest body, HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = readRefreshTokenFromRequest(body,request).orElseThrow(() -> new BadCredentialsException("Refresh Token is missing"));
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Refresh Token is not refresh token type");
        }
        String jwtId = jwtService.getJwtId(refreshToken);
        UUID userId = jwtService.getUserId(refreshToken);
        RefreshToken storedRefreshToken = refreshTokenRepo.findByJwtId(jwtId).orElseThrow(() -> new BadCredentialsException("Refresh Token not found in database"));
        //Now checking refresh token is revoked or not:
        if (storedRefreshToken.isRevoked()) {
            throw new BadCredentialsException("Refresh Token is revoked");
        }
        //Now checking refresh token is expired or not:
        if (storedRefreshToken.getExpiredAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh Token is expired");
        }
        //Checking refresh token belongs to this user or not:
        if (!storedRefreshToken.getUser().getId().equals(userId)) {
            throw new BadCredentialsException("Refresh Token is not belongs to this user");
        }
        //Rotate refresh token:
        storedRefreshToken.setRevoked(true);
        String newJwtId = UUID.randomUUID().toString();
        storedRefreshToken.setReplacedByToken(newJwtId);
        refreshTokenRepo.save(storedRefreshToken);
         User user = storedRefreshToken.getUser();
        //Create a new refresh token:
        var newRefreshTokenOb= RefreshToken.builder()
                .jwtId(newJwtId)
                .user(user)
                .createdAt(Instant.now())
                .expiredAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();

        refreshTokenRepo.save(newRefreshTokenOb);
        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.refreshToken(user, newRefreshTokenOb.getJwtId());
        cookieService.attachRefreshTokenAtCookie(response, newRefreshToken, (int) jwtService.getRefreshTtlSeconds());
        cookieService.addNoCacheHeaders(response);
        return ResponseEntity.ok(TokenResponse.of(newAccessToken,newRefreshToken,(long) jwtService.getAccessTtlSeconds(),modelMapper.map(user , UserDto.class)));
    }

    private Optional<String> readRefreshTokenFromRequest(RefreshTokenRequest body, HttpServletRequest request) {
        //1. Prefer reading refreshToken from cookie:
        if (request.getCookies() != null) {
            Optional<String> readFromCookie = Arrays.stream(request.getCookies())
                    .filter(cookie -> cookieService.getRefreshTokenCookieName().equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .filter(c->!c.isBlank())
                    .findFirst();
            if (readFromCookie.isPresent()) {
                return readFromCookie;
            }
        }
        //2.If refresh token came from request body then:
        if (body!=null && body.refreshToken()!=null && !body.refreshToken().isBlank()) {
            return Optional.of(body.refreshToken());
        }
        //3.If refresh token came from custom headers then:
        String refreshTokenFromCustomHeader = request.getHeader("X-Refresh-Token");
        if (refreshTokenFromCustomHeader != null && !refreshTokenFromCustomHeader.isBlank()) {
            return Optional.of(refreshTokenFromCustomHeader.trim());
        }
        //4.We can also send a refresh token like Access token
        //Authorization = Bearer <refreshToken>
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.regionMatches(true,0,"Bearer", 0,7) ) {
            String candidate = authHeader.substring(7).trim();
            if ( !candidate.isEmpty()) {
                try {
                    jwtService.isRefreshToken(candidate);
                    return Optional.of(candidate);
                }catch (Exception ignored){

                }
            }
        }
        // If the above 4 ways do not work, then return empty.
        return Optional.empty();
    }
    //Logout -> User
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        readRefreshTokenFromRequest(null,request).ifPresent(token -> {
            try {
                if (jwtService.isRefreshToken(token)) {
                    String jwtId = jwtService.getJwtId(token);
                    refreshTokenRepo.findByJwtId(jwtId).ifPresent(refreshToken -> {
                        refreshToken.setRevoked(true);
                        refreshTokenRepo.save(refreshToken);
                    });
                }
            }catch (JwtException ignored){}
        });
        cookieService.clearRefreshTokenAtCookie(response,"",(int) jwtService.getAccessTtlSeconds());
        cookieService.addNoCacheHeaders(response);
        SecurityContextHolder.clearContext();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();



    }

    //Login -> User
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginUser(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        //Create a method to authenticated user
        Authentication authenticate = authenticate(loginRequest);
        User user = userRepository.findByEmail(loginRequest.email()).orElseThrow(()-> new BadCredentialsException("Username and Password are Invalid"));
        if (!user.isEnable()){
            throw new DisabledException("User is disabled");
        }
        //Let Create a Refresh-Token
        String jwtId = UUID.randomUUID().toString();
        var refreshTokenOb = RefreshToken.builder()
                .jwtId(jwtId)
                .user(user)
                .createdAt(Instant.now())
                .expiredAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .revoked(false)
                .build();
        //Now save the refresh token at database
        refreshTokenRepo.save(refreshTokenOb);
        //Generate Token
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.refreshToken(user, refreshTokenOb.getJwtId());
        //Now we are going attach refreshToken in cookie
        cookieService.attachRefreshTokenAtCookie(response,refreshToken, (int) jwtService.getRefreshTtlSeconds());
        cookieService.addNoCacheHeaders(response);

        TokenResponse tokenResponse = TokenResponse.of(accessToken, refreshToken, jwtService.getAccessTtlSeconds(), modelMapper.map(user, UserDto.class));
        return ResponseEntity.status(HttpStatus.OK).body(tokenResponse);
    }

    private Authentication authenticate(LoginRequest loginRequest) {
        try {
            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.email(),loginRequest.password()));
        }catch (Exception e){
            throw new BadCredentialsException("Invalid username or password");
        }

    }

}
