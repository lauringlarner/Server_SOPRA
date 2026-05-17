package ch.uzh.ifi.hase.soprafs26.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserService userService;

	private User testUser;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		// given
		testUser = new User();
		testUser.setId(UUID.randomUUID());
		testUser.setUsername("testUsername");
		testUser.setEmail("test@example.com");
		testUser.setPassword("Password123!");

		// when -> any object is being save in the userRepository -> return the dummy testUser
		Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
	}

	@Test
	public void createUser_validInputs_success() {
		// when -> any object is being save in the userRepository -> return the dummy testUser
		User createdUser = userService.createUser(testUser);

		// then
		Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

		assertEquals(testUser.getId(), createdUser.getId());
		assertEquals(testUser.getUsername(), createdUser.getUsername());
		assertEquals(testUser.getEmail(), createdUser.getEmail());
		assertNotNull(createdUser.getToken());
		assertEquals(UserStatus.OFFLINE, createdUser.getStatus());
	}

	@Test
	public void createUser_duplicateUsername_throwsException() {
		// given -> a first user has already been created
		userService.createUser(testUser);

		// when -> setup additional mocks for UserRepository
		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

		// then -> attempt to create second user with same username -> check that an error is thrown
		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}

	@Test
	public void createUser_duplicateInputs_throwsException() {
		// given -> a first user has already been created
		userService.createUser(testUser);

		// when -> setup additional mocks for UserRepository
		Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

		// then -> attempt to create second user with same user -> check that an error is thrown
		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}

	@Test
	public void createUser_emptyUsername_throwsException() {
		testUser.setUsername("");

		assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
	}

	@Test
	public void loginUser_validCredentials_success() {
		testUser.setPassword(new BCryptPasswordEncoder().encode("Password123!"));
		Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(testUser);

		User result = userService.loginUser("testUsername", "Password123!");

		assertEquals(UserStatus.ONLINE, result.getStatus());
	}

	@Test
	public void loginUser_wrongPassword_throwsException() {
		testUser.setPassword(new BCryptPasswordEncoder().encode("Password123!"));
		Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(testUser);

		assertThrows(ResponseStatusException.class, () -> userService.loginUser("testUsername", "WrongPassword1!"));
	}

	@Test
	public void changePassword_validInput_success() {
		testUser.setPassword(new BCryptPasswordEncoder().encode("Password123!"));
		testUser.setStatus(UserStatus.ONLINE);
		String oldToken = testUser.getToken();

		Mockito.when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

		userService.changePassword(testUser.getId(), "Password123!", "NewPassword123!");

		assertEquals(UserStatus.OFFLINE, testUser.getStatus());
		assertNotEquals(oldToken, testUser.getToken());
	}

	@Test
	public void logoutUser_success() {
		testUser.setStatus(UserStatus.ONLINE);
		String oldToken = testUser.getToken();

		userService.logoutUser(testUser);

		assertEquals(UserStatus.OFFLINE, testUser.getStatus());
		assertNotEquals(oldToken, testUser.getToken());
	}

	@Test
	public void loginUser_invalidUsername_throwsException() {
		Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(null);

		assertThrows(ResponseStatusException.class, () -> userService.loginUser("testUsername", "Password123!"));
	}

	@Test
	public void changePassword_newPasswordTooShort_throwsException() {
		assertThrows(ResponseStatusException.class,
			() -> userService.changePassword(testUser.getId(), "Password123!", "Short1!"));
	}

	@Test
	public void changePassword_wrongOldPassword_throwsException() {
		testUser.setPassword(new BCryptPasswordEncoder().encode("Password123!"));

		Mockito.when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

		assertThrows(ResponseStatusException.class,
			() -> userService.changePassword(testUser.getId(), "WrongPassword1!", "NewPassword123!"));
	}

	@Test
	public void updateUser_validUsername_success() {
		User updatedUser = new User();
		updatedUser.setUsername("newUsername");

		Mockito.when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
		Mockito.when(userRepository.findByUsername("newUsername")).thenReturn(null);

		User result = userService.updateUser(testUser.getId(), updatedUser);

		assertEquals("newUsername", result.getUsername());
	}

	@Test
	public void getUsers_returnsAllUsers() {
		List<User> users = List.of(testUser);
		Mockito.when(userRepository.findAll()).thenReturn(users);

		List<User> result = userService.getUsers();

		assertEquals(1, result.size());
		Mockito.verify(userRepository, Mockito.times(1)).findAll();
	}

	@Test
	public void getUserById_validId_returnsUser() {
		Mockito.when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

		User result = userService.getUserById(testUser.getId());

		assertEquals(testUser.getId(), result.getId());
	}

	@Test
	public void getUserById_invalidId_throwsNotFound() {
		UUID unknownId = UUID.randomUUID();
		Mockito.when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

		assertThrows(ResponseStatusException.class, () -> userService.getUserById(unknownId));
	}

	@Test
	public void getUserByToken_validToken_returnsUser() {
		testUser.setToken("valid-token");
		Mockito.when(userRepository.findByToken("valid-token")).thenReturn(testUser);

		User result = userService.getUserByToken("valid-token");

		assertEquals(testUser, result);
	}

	@Test
	public void getUserByToken_invalidToken_throwsUnauthorized() {
		Mockito.when(userRepository.findByToken("invalid-token")).thenReturn(null);

		assertThrows(ResponseStatusException.class, () -> userService.getUserByToken("invalid-token"));
	}

	@Test
	public void updateUserStats_gameWon_incrementsGamesPlayedAndWon() {
		testUser.setGamesPlayed(0);
		testUser.setGamesWon(0);

		userService.updateUserStats(testUser, true);

		assertEquals(1, testUser.getGamesPlayed());
		assertEquals(1, testUser.getGamesWon());
	}

	@Test
	public void updateUserStats_gameLost_incrementsGamesPlayedOnly() {
		testUser.setGamesPlayed(2);
		testUser.setGamesWon(1);

		userService.updateUserStats(testUser, false);

		assertEquals(3, testUser.getGamesPlayed());
		assertEquals(1, testUser.getGamesWon());
	}

	@Test
	public void validateUserMatchesUserId_sameId_noException() {
		assertDoesNotThrow(() -> userService.validateUserMatchesUserId(testUser.getId(), testUser));
	}

	@Test
	public void validateUserMatchesUserId_differentId_throwsForbidden() {
		UUID otherId = UUID.randomUUID();

		assertThrows(ResponseStatusException.class,
			() -> userService.validateUserMatchesUserId(otherId, testUser));
	}
}
