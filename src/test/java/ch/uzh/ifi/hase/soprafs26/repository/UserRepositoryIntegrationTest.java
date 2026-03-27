package ch.uzh.ifi.hase.soprafs26.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
public class UserRepositoryIntegrationTest {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private UserRepository userRepository;

	@Test
	public void findByUsername_success() {
		// given
		User user = new User();
		user.setUsername("testuser");
		user.setEmail("testuser@example.com");
		user.setPassword("password123");
		user.setStatus(UserStatus.OFFLINE);
		user.setToken("test-token-1");

		entityManager.persist(user);
		entityManager.flush();

		// when
		User found = userRepository.findByUsername(user.getUsername());

		// then
		assertNotNull(found.getId());
		assertEquals(found.getUsername(), user.getUsername());
		assertEquals(found.getEmail(), user.getEmail());
		assertEquals(found.getToken(), user.getToken());
		assertEquals(found.getStatus(), user.getStatus());
	}

	@Test
	public void findByToken_success() {
		// given
		User user = new User();
		user.setUsername("tokenuser");
		user.setEmail("tokenuser@example.com");
		user.setPassword("password123");
		user.setStatus(UserStatus.ONLINE);
		user.setToken("unique-token-abc");

		entityManager.persist(user);
		entityManager.flush();

		// when
		User found = userRepository.findByToken(user.getToken());

		// then
		assertNotNull(found.getId());
		assertEquals(found.getUsername(), user.getUsername());
		assertEquals(found.getToken(), user.getToken());
	}
}
