package ch.uzh.ifi.hase.soprafs26.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

/**
 * Test class for the UserResource REST resource.
 *
 * @see UserService
 */
/*
@WebAppConfiguration
@SpringBootTest
public class UserServiceIntegrationTest {

	@Qualifier("userRepository")
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserService userService;

	@BeforeEach
	public void setup() {
		userRepository.deleteAll();
	}

	@Test
	public void createUser_validInputs_success() {
		// given
		assertNull(userRepository.findByUsername("testUsername"));

		User testUser = new User();
		testUser.setUsername("testUsername");
		testUser.setEmail("test@example.com");
		testUser.setPassword("Password123!");

		// when
		User createdUser = userService.createUser(testUser);

		// then
		assertEquals(testUser.getId(), createdUser.getId());
		assertEquals(testUser.getUsername(), createdUser.getUsername());
		assertEquals(testUser.getEmail(), createdUser.getEmail());
		assertNotNull(createdUser.getToken());
		assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
	}
	
	@Test
	public void createUser_duplicateUsername_throwsException() {
		assertNull(userRepository.findByUsername("testUsername"));

		User testUser = new User();
		testUser.setUsername("testUsername");
		testUser.setEmail("test@example.com");
		testUser.setPassword("Password123!");
		userService.createUser(testUser);

		// attempt to create second user with same username
		User testUser2 = new User();
		testUser2.setUsername("testUsername");
		testUser2.setEmail("other@example.com");
		testUser2.setPassword("Password123!");

		// check that an error is thrown
		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser2));
	}

	@Test
	public void createUser_nullUsername_throwsException() {
		User testUser = new User();
		testUser.setUsername("");
		testUser.setEmail("test@example.com");
		testUser.setPassword("Password123!");

		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}

	@Test
	public void createUser_nullPassword_throwsException() {
		User testUser = new User();
		testUser.setUsername("testUsername");
		testUser.setEmail("test@example.com");
		testUser.setPassword("");

		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}

	@Test
	public void createUser_shortPassword_throwsException() {
		User testUser = new User();
		testUser.setUsername("testUsername");
		testUser.setEmail("test@example.com");
		testUser.setPassword("Passwd1!");

		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}

	@Test
	public void createUser_noMark_throwsException() {
		User testUser = new User();
		testUser.setUsername("testUsername");
		testUser.setEmail("test@example.com");
		testUser.setPassword("Password123");

		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}
}

*/