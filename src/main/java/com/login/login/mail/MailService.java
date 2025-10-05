package com.login.login.mail;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;

import org.springframework.beans.factory.annotation.Value;

@Service
public class MailService {
    private final JavaMailSender sender;

    @Value("${app.security.base-url}") 
    String baseUrl;

    public MailService(JavaMailSender sender) {
        this.sender = sender;
    }

    public void sendResetEmail(String to, String token){
        var link = baseUrl + "/auth/reset" + token;
        var helper = new MimeMessageHelper(sender.createMimeMessage());
        try{
            helper.setTo(to);
            helper.setSubject("Redefinição de senha");
            helper.setText(String.format("""
                Olá,
                para redefinir sua senha, clique no link abaixo:
                %s
                Se você não solicitou a redefinição de senha, ignore este e-mail.
                (válido por 30 minutos)
                    """, link), false);
            sender.send(helper.getMimeMessage());
        } catch(MessagingException e){
        throw new IllegalStateException(e);
     }
    }
    
}
