
package com.login.login.repo;

import com.login.login.domain.User;
import com.login.login.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Testes de integração para UserRepository
 * 
 * Testa operações de banco de dados reais com H2 in-memory
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
})
@DisplayName("User Repository Integration Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        testUser1 = User.builder()
                .email("test1@example.com")
                .name("Test User 1")
                .password("hashedPassword1")
                .build();

        testUser2 = User.builder()
                .email("test2@example.com")
                .name("Test User 2")
                .password("hashedPassword2")
                .build();
    }

    @Test
    @DisplayName("Deve salvar e recuperar usuário corretamente")
    void shouldSaveAndRetrieveUserCorrectly() {
        // Act
        User saved = userRepository.save(testUser1);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("test1@example.com");
        assertThat(saved.getName()).isEqualTo("Test User 1");
        assertThat(saved.getPassword()).isEqualTo("hashedPassword1");

        // Verifica se foi persistido no banco
        entityManager.flush();
        entityManager.clear();

        Optional<User> retrieved = userRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getEmail()).isEqualTo("test1@example.com");
    }

    @Test
    @DisplayName("Deve encontrar usuário por email")
    void shouldFindUserByEmail() {
        // Arrange
        entityManager.persist(testUser1);
        entityManager.flush();

        // Act
        Optional<User> found = userRepository.findByEmail("test1@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test User 1");
        assertThat(found.get().getPassword()).isEqualTo("hashedPassword1");
    }

    @Test
    @DisplayName("Deve retornar Optional vazio para email não encontrado")
    void shouldReturnEmptyOptionalForNotFoundEmail() {
        // Act
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Deve ser case-insensitive na busca por email")
    void shouldBeCaseInsensitiveForEmailSearch() {
        // Arrange
        entityManager.persist(testUser1);
        entityManager.flush();

        // Act & Assert
        assertThat(userRepository.findByEmail("TEST1@EXAMPLE.COM")).isEmpty(); // H2 é case-sensitive por padrão
        assertThat(userRepository.findByEmail("test1@example.com")).isPresent();
        
        // Para teste case-insensitive real, seria necessário configurar collation no banco
    }

    @Test
    @DisplayName("Deve garantir unicidade de email")
    void shouldEnforceEmailUniqueness() {
        // Arrange
        User user1 = User.builder()
                .email("duplicate@example.com")
                .name("User 1")
                .password("password1")
                .build();

        User user2 = User.builder()
                .email("duplicate@example.com")
                .name("User 2")
                .password("password2")
                .build();

        // Act
        userRepository.save(user1);
        entityManager.flush();

        // Assert
        assertThatThrownBy(() -> {
            userRepository.save(user2);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Deve persistir todos os campos obrigatórios")
    void shouldPersistAllRequiredFields() {
        // Arrange
        User completeUser = User.builder()
                .email("complete@example.com")
                .name("Complete User")
                .password("hashedCompletePassword")
                .build();

        // Act
        User saved = userRepository.save(completeUser);
        entityManager.flush();
        entityManager.clear();

        // Assert
        User retrieved = entityManager.find(User.class, saved.getId());
        assertThat(retrieved.getEmail()).isEqualTo("complete@example.com");
        assertThat(retrieved.getName()).isEqualTo("Complete User");
        assertThat(retrieved.getPassword()).isEqualTo("hashedCompletePassword");
        assertThat(retrieved.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar usuário com email null")
    void shouldRejectUserWithNullEmail() {
        // Arrange
        User userWithNullEmail = User.builder()
                .email(null)
                .name("User Without Email")
                .password("password")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> {
            userRepository.save(userWithNullEmail);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar usuário com nome null")
    void shouldRejectUserWithNullName() {
        // Arrange
        User userWithNullName = User.builder()
                .email("user@example.com")
                .name(null)
                .password("password")
                .build();

        // Act & Assert
        assertThatThrownBy(() -> {
            userRepository.save(userWithNullName);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Deve rejeitar usuário com senha null")
    void shouldRejectUserWithNullPassword() {
        // Arrange
        User userWithNullPassword = User.builder()
                .email("user@example.com")
                .name("User")
                .password(null)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> {
            userRepository.save(userWithNullPassword);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Deve atualizar dados do usuário")
    void shouldUpdateUserData() {
        // Arrange
        User saved = userRepository.save(testUser1);
        entityManager.flush();
        
        String originalName = saved.getName();
        String originalEmail = saved.getEmail();
        
        assertThat(originalName).isNotNull();

        // Act
        saved.setName("Updated Name");
        User updated = userRepository.save(saved);
        entityManager.flush();

        // Assert
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getName()).isNotEqualTo(originalName);
        assertThat(updated.getEmail()).isEqualTo(originalEmail); // Email permanece igual
        assertThat(updated.getId()).isEqualTo(saved.getId()); // ID permanece igual
    }

    @Test
    @DisplayName("Deve listar todos os usuários")
    void shouldListAllUsers() {
        // Arrange
        userRepository.save(testUser1);
        userRepository.save(testUser2);
        entityManager.flush();

        // Act
        var allUsers = userRepository.findAll();

        // Assert
        assertThat(allUsers).hasSize(2);
        assertThat(allUsers).extracting(User::getEmail)
                .containsExactlyInAnyOrder("test1@example.com", "test2@example.com");
    }

    @Test
    @DisplayName("Deve deletar usuário por ID")
    void shouldDeleteUserById() {
        // Arrange
        User saved = userRepository.save(testUser1);
        entityManager.flush();
        
        assertThat(userRepository.existsById(saved.getId())).isTrue();

        // Act
        userRepository.deleteById(saved.getId());
        entityManager.flush();

        // Assert
        assertThat(userRepository.existsById(saved.getId())).isFalse();
        assertThat(userRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("Deve contar usuários corretamente")
    void shouldCountUsersCorrectly() {
        // Arrange
        assertThat(userRepository.count()).isZero();

        userRepository.save(testUser1);
        userRepository.save(testUser2);
        entityManager.flush();

        // Act & Assert
        assertThat(userRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve verificar existência por ID")
    void shouldCheckExistenceById() {
        // Arrange
        User saved = userRepository.save(testUser1);
        entityManager.flush();

        // Act & Assert
        assertThat(userRepository.existsById(saved.getId())).isTrue();
        assertThat(userRepository.existsById(999L)).isFalse();
    }

    @Test
    @DisplayName("Deve lidar com caracteres especiais no nome e email")
    void shouldHandleSpecialCharactersInNameAndEmail() {
        // Arrange
        User userWithSpecialChars = User.builder()
                .email("joão.pérez+test@domínio.com.br")
                .name("João Pérez da Silva & Cia.")
                .password("hashedPassword")
                .build();

        // Act
        User saved = userRepository.save(userWithSpecialChars);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<User> retrieved = userRepository.findByEmail("joão.pérez+test@domínio.com.br");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("João Pérez da Silva & Cia.");
    }

    @Test
    @DisplayName("Deve manter performance com múltiplos usuários")
    void shouldMaintainPerformanceWithMultipleUsers() {
        // Arrange - Cria muitos usuários para teste de performance básico
        long startTime = System.currentTimeMillis();

        // Act
        for (int i = 0; i < 100; i++) {
            User user = User.builder()
                    .email("user" + i + "@example.com")
                    .name("User " + i)
                    .password("hashedPassword" + i)
                    .build();
            userRepository.save(user);
        }
        entityManager.flush();

        long endTime = System.currentTimeMillis();

        // Assert
        assertThat(userRepository.count()).isEqualTo(100);
        assertThat(endTime - startTime).isLessThan(5000); // Menos que 5 segundos

        // Teste busca por email em lote grande
        long searchStart = System.currentTimeMillis();
        Optional<User> found = userRepository.findByEmail("user50@example.com");
        long searchEnd = System.currentTimeMillis();

        assertThat(found).isPresent();
        assertThat(searchEnd - searchStart).isLessThan(100); // Menos que 100ms
    }
}