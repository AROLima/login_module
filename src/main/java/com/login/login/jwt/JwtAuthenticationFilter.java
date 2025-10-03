package com.login.login.jwt;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.login.login.repo.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

public class JwtAuthenticationFilter  extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthenticationFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException{
            try{
                var cookies = req.getCookies();
                if(cookies != null){
                    var c = Arrays.stream(cookies)
                    .filter(cookie -> "ACCESS_TOKEN".equals(cookie.getName()))
                    .findFirst();
                    if (c.isPresent()){
                        var userID = jwt.subjectToUserId(c.get().getValue());
                        var user = users.findById(userID).orElse(null);
                        if (user != null){
                            var auth = new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                }
                    
            }catch (Exception ignored){}
            chain.doFilter(req, res);
        }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req){
        var p = req.getServletPath();
        //não filtrar essas rotas
        return p.startsWith("/auth")  || p.startsWith("/css") || p.startsWith("/js") || p.startsWith("/images") || p.startsWith("/");

    }

    
}
