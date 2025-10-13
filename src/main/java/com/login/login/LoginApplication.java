// Declaração do pacote - organiza as classes em estrutura hierárquica
package com.login.login;

// Importação da classe SpringApplication - responsável por iniciar a aplicação Spring Boot
import org.springframework.boot.SpringApplication;
// Importação da anotação @SpringBootApplication - configura automaticamente a aplicação
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principal da aplicação Spring Boot
 * Esta é a classe que contém o método main() e inicia toda a aplicação
 */
@SpringBootApplication  // Anotação que combina 3 anotações importantes:
                       // @Configuration: Permite definir beans de configuração
                       // @EnableAutoConfiguration: Ativa a configuração automática do Spring Boot
                       // @ComponentScan: Escaneia automaticamente por componentes (@Service, @Controller, etc.)
public class LoginApplication {

	/**
	 * Método main - ponto de entrada da aplicação Java
	 * @param args - argumentos da linha de comando (não usados neste projeto)
	 */
	public static void main(String[] args) {
		// SpringApplication.run() faz toda a mágica:
		// 1. Cria o contexto do Spring
		// 2. Configura o servidor web embutido (Tomcat)
		// 3. Escaneia e registra todos os componentes (@Service, @Controller, etc.)
		// 4. Inicia a aplicação na porta 8080 por padrão
		SpringApplication.run(LoginApplication.class, args);
	}

}
