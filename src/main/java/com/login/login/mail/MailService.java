// Pacote mail - serviços relacionados ao envio de emails
package com.login.login.mail;

// Importações Spring Mail
import org.springframework.mail.javamail.JavaMailSender;    // Interface principal para envio de emails
import org.springframework.mail.javamail.MimeMessageHelper; // Helper para construir emails MIME
import org.springframework.stereotype.Service;              // Marca como componente de serviço

// Importações Jakarta Mail (novo nome do javax.mail)
import jakarta.mail.MessagingException;  // Exceção para problemas de email

// Importação Spring para configuração
import org.springframework.beans.factory.annotation.Value;  // Injeta valores de configuração

/**
 * SERVIÇO DE ENVIO DE EMAILS
 * 
 * Responsável por enviar todos os tipos de emails da aplicação.
 * 
 * EMAILS IMPLEMENTADOS:
 * - Reset de senha (link para redefinir senha)
 * 
 * EMAILS QUE PODERIAM SER IMPLEMENTADOS:
 * - Confirmação de cadastro
 * - Notificação de login suspeito  
 * - Alteração de senha
 * - Newsletter/comunicados
 * 
 * TECNOLOGIA USADA:
 * - Spring Boot Mail Starter
 * - JavaMailSender (abstração do Spring)
 * - MIME messages (suporte a HTML, anexos, etc.)
 * 
 * CONFIGURAÇÃO NECESSÁRIA (application.yml):
 * spring:
 *   mail:
 *     host: smtp.gmail.com
 *     port: 587
 *     username: ${EMAIL_USERNAME}
 *     password: ${EMAIL_PASSWORD}
 *     properties:
 *       mail.smtp.auth: true
 *       mail.smtp.starttls.enable: true
 */
@Service  // Componente Spring gerenciado pelo container IoC
public class MailService {
    
    /**
     * DEPENDÊNCIA PRINCIPAL PARA ENVIO DE EMAIL
     * 
     * JavaMailSender - interface do Spring que abstrai JavaMail API
     * - Configuração automática via Spring Boot
     * - Suporte a SMTP, autenticação, TLS/SSL
     * - Pool de conexões automático
     * 
     * Implementações disponíveis:
     * - JavaMailSenderImpl (padrão do Spring)
     * - MockMailSender (para testes)
     */
    private final JavaMailSender sender;

    /**
     * CONFIGURAÇÃO DE URL BASE
     * 
     * @Value injeta valor do arquivo de configuração
     * 
     * Exemplo no application.yml:
     * app:
     *   security:
     *     base-url: "https://meuapp.com"
     * 
     * AMBIENTES DIFERENTES:
     * - Desenvolvimento: "http://localhost:8080"
     * - Homologação: "https://staging.meuapp.com"  
     * - Produção: "https://meuapp.com"
     */
    @Value("${app.security.base-url}") 
    String baseUrl;

    /**
     * CONSTRUTOR COM INJEÇÃO DE DEPENDÊNCIA
     * 
     * Spring automaticamente injeta o JavaMailSender configurado.
     * Configuração vem do spring.mail.* no application.yml
     * 
     * @param sender JavaMailSender configurado pelo Spring Boot
     */
    public MailService(JavaMailSender sender) {
        this.sender = sender;
    }

    /**
     * ENVIAR EMAIL DE RESET DE SENHA
     * 
     * Constrói e envia email com link para redefinir senha.
     * 
     * FLUXO:
     * 1. Monta URL completa com token
     * 2. Cria mensagem MIME  
     * 3. Define destinatário, assunto e conteúdo
     * 4. Envia email via SMTP
     * 
     * @param to Email do destinatário
     * @param token Token único de reset gerado pelo sistema
     * @throws IllegalStateException se falhar ao enviar email
     */
    public void sendResetEmail(String to, String token){
        // CONSTRUIR URL COMPLETA DO LINK DE RESET
        var link = baseUrl + "/auth/reset/" + token;
        //    ↑         ↑                ↑
        // Config   Rota fixa      Token único
        //
        // Exemplo: "https://meuapp.com/auth/reset/a1b2c3d4-e5f6-7890"

        // CRIAR HELPER PARA CONSTRUIR EMAIL MIME  
        var helper = new MimeMessageHelper(sender.createMimeMessage());
        //            ↑                    ↑
        //       Helper p/ facilitar   Cria mensagem vazia
        //
        // MimeMessageHelper facilita:
        // - Definir remetente, destinatário, assunto
        // - Texto simples ou HTML
        // - Anexos e imagens inline
        // - Codificação de caracteres

        try{
            // CONFIGURAR DESTINATÁRIO
            helper.setTo(to);  // Email de quem vai receber
            
            // CONFIGURAR ASSUNTO
            helper.setSubject("Redefinição de senha");  // Assunto do email
            
            // CONFIGURAR CONTEÚDO DO EMAIL
            helper.setText(String.format("""
                Olá,
                para redefinir sua senha, clique no link abaixo:
                %s
                Se você não solicitou a redefinição de senha, ignore este e-mail.
                (válido por 30 minutos)
                    """, link), false);
            //              ↑      ↑
            //        Text block   false = texto simples (não HTML)
            //        (Java 15+)   true = HTML content
            
            // ENVIAR EMAIL
            sender.send(helper.getMimeMessage());
            //     ↑    ↑
            //   Envia  Pega mensagem montada pelo helper
            
        } catch(MessagingException e){
            // TRATAR ERROS DE ENVIO
            throw new IllegalStateException(e);
            //    ↑
            // Converte checked exception em runtime exception
            // Spring vai fazer rollback automático se estiver em @Transactional
        }
    }
    
    /*
     * MÉTODOS ADICIONAIS QUE PODERÍAMOS IMPLEMENTAR:
     * 
     * public void sendWelcomeEmail(String to, String name) {
     *     var helper = new MimeMessageHelper(sender.createMimeMessage());
     *     try {
     *         helper.setTo(to);
     *         helper.setSubject("Bem-vindo ao Sistema!");
     *         helper.setText(String.format("""
     *             Olá %s,
     *             
     *             Bem-vindo ao nosso sistema!
     *             Sua conta foi criada com sucesso.
     *             
     *             Acesse: %s/login
     *             """, name, baseUrl), false);
     *         sender.send(helper.getMimeMessage());
     *     } catch(MessagingException e) {
     *         throw new IllegalStateException(e);
     *     }
     * }
     * 
     * public void sendPasswordChangedNotification(String to) {
     *     var helper = new MimeMessageHelper(sender.createMimeMessage());
     *     try {
     *         helper.setTo(to);
     *         helper.setSubject("Senha alterada com sucesso");
     *         helper.setText("""
     *             Sua senha foi alterada com sucesso.
     *             
     *             Se não foi você, entre em contato conosco imediatamente.
     *             """, false);
     *         sender.send(helper.getMimeMessage());
     *     } catch(MessagingException e) {
     *         throw new IllegalStateException(e);
     *     }
     * }
     * 
     * public void sendHtmlEmail(String to, String subject, String htmlContent) {
     *     var helper = new MimeMessageHelper(sender.createMimeMessage());
     *     try {
     *         helper.setTo(to);
     *         helper.setSubject(subject);
     *         helper.setText(htmlContent, true); // true = HTML
     *         sender.send(helper.getMimeMessage());
     *     } catch(MessagingException e) {
     *         throw new IllegalStateException(e);
     *     }
     * }
     * 
     * MELHORIAS POSSÍVEIS:
     * 
     * 1. Templates de email com Thymeleaf:
     * @Autowired
     * private TemplateEngine templateEngine;
     * 
     * public void sendTemplatedEmail(String to, String template, Map<String, Object> variables) {
     *     Context context = new Context();
     *     context.setVariables(variables);
     *     String htmlContent = templateEngine.process(template, context);
     *     sendHtmlEmail(to, "Assunto", htmlContent);
     * }
     * 
     * 2. Queue de emails para performance:
     * @Async
     * public CompletableFuture<Void> sendEmailAsync(String to, String subject, String content) {
     *     // enviar email em background
     * }
     * 
     * 3. Retry automático em caso de falha:
     * @Retryable(value = {MessagingException.class}, maxAttempts = 3)
     * public void sendEmailWithRetry(...) { ... }
     * 
     * 4. Métricas e logging:
     * @EventListener
     * public void handleEmailSent(EmailSentEvent event) {
     *     log.info("Email enviado para: {}", event.getTo());
     *     meterRegistry.counter("emails.sent").increment();
     * }
     */
}
