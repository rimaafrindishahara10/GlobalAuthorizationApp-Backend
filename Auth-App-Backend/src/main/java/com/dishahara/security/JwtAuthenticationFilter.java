package com.dishahara.security;

import com.dishahara.dtos.UserDto;
import com.dishahara.helpers.UserHelper;
import com.dishahara.repositories.UserRepository;
import com.dishahara.services.UserService;
import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.security.SignatureException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        //Get header
        String header = request.getHeader("Authorization");
        log.info("Authorization header: {}", header);
        if ((header !=null) && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try{
                if (!jwtService.isAccessToken(token)) {
                    filterChain.doFilter(request, response);
                    return;
                }
                Jws<Claims> claimsJws = jwtService.parseToken(token);
                String userId = claimsJws.getPayload().getSubject();
                UUID uuid = UserHelper.parseUUID(userId);
                userRepository.findById(uuid).ifPresent(user->{
                   //If user is enabled
                    if(user.isEnable()){
                        //We have user
                        List<GrantedAuthority> authorities = user.getRoles()==null? List.of(): user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());
                        //Let Authenticate the user
                        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        //Set the authenticate user at Security context holder
                        if (SecurityContextHolder.getContext().getAuthentication() == null){
                            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                        }
                    }

                });


            }catch (ExpiredJwtException e){
                request.setAttribute("error", "Token is expired");
               // e.printStackTrace();
            }
            catch (Exception e){
                request.setAttribute("error", "Token is invalid");
              //  e.printStackTrace();
            }



        }
        filterChain.doFilter(request, response);


    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return request.getRequestURI().startsWith("/api/v1/auth");
    }
}
