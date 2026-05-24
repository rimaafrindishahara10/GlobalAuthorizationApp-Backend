package com.dishahara.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
@Getter
@Slf4j
public class CookieService {

    private final String refreshTokenCookieName;
    private final boolean cookieSecure;
    private final boolean cookieHttpOnly;
    private final String cookieSameSite;
    private final String cookieDomain;

    public CookieService(
            @Value("${logging.security.jwt.refresh-token-cookie-name}") String refreshTokenCookieName,
            @Value("${logging.security.jwt.cookie-secure}") boolean cookieSecure,
            @Value("${logging.security.jwt.cookie-http-only}") boolean cookieHttpOnly,
            @Value("${logging.security.jwt.cookie-same-site}") String cookieSameSite,
            @Value("${logging.security.jwt.cookie-domain}") String cookieDomain) {
        this.refreshTokenCookieName = refreshTokenCookieName;
        this.cookieSecure = cookieSecure;
        this.cookieHttpOnly = cookieHttpOnly;
        this.cookieSameSite = cookieSameSite;
        this.cookieDomain = cookieDomain;
    }

    //Now I have to create a method to attach a refresh token at cookie
    public void attachRefreshTokenAtCookie( HttpServletResponse response, String value ,int maxAgeSeconds) {
      log.info("Attaching refresh token cookie at {}", value);
       var responseCookieBuilder = ResponseCookie.from(refreshTokenCookieName,value)
                .maxAge(maxAgeSeconds)
                .httpOnly(cookieHttpOnly)
                .sameSite(cookieSameSite)
                .path("/")
                .secure(cookieSecure);

                if(cookieDomain != null && !cookieDomain.isBlank()) {
                    responseCookieBuilder.domain(cookieDomain);
                }
                ResponseCookie cookie = responseCookieBuilder.build();
                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());


    }

    //Now we are going to create a method to clear refreshToken
    public void clearRefreshTokenAtCookie( HttpServletResponse response, String value, int maxAgeSeconds) {

        var responseCookieBuilder = ResponseCookie.from(refreshTokenCookieName,"")
                .maxAge(0)
                .httpOnly(cookieHttpOnly)
                .sameSite(cookieSameSite)
                .path("/")
                .secure(cookieSecure);
        if(cookieDomain != null && !cookieDomain.isBlank()) {
            responseCookieBuilder.domain(cookieDomain);
        }
        ResponseCookie cookie = responseCookieBuilder.build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
    //For anty caches clear in headers
    public void addNoCacheHeaders( HttpServletResponse response) {
        response.addHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
        response.addHeader(HttpHeaders.PRAGMA, "no-cache");
    }

}
