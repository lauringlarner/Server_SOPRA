package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;

	public UserService(@Qualifier("userRepository") UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public List<User> getUsers() {
		return this.userRepository.findAll();
	}

	public User createUser(User newUser) {
		// Validate username
		if (newUser.getUsername() == null || newUser.getUsername().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty!");
		}

		// If name is not provided, set it to username
		if (newUser.getName() == null || newUser.getName().isEmpty()) {
			newUser.setName(newUser.getUsername());
		}

		if (newUser.getPassword() == null || newUser.getPassword().isEmpty()) {
			newUser.setPassword(UUID.randomUUID().toString());
		}

		if (newUser.getBio() == null) {
			newUser.setBio("");
		}

		newUser.setToken(UUID.randomUUID().toString());
		newUser.setStatus(UserStatus.OFFLINE);
		newUser.setCreatedAt(LocalDateTime.now());
		checkIfUserExists(newUser);
		// saves the given entity but data is only persisted in the database once
		// flush() is called
		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}

	/**
	 * This is a helper method that will check the uniqueness criteria of the
	 * username defined in the User entity. The method will do nothing if the input
	 * is unique and throw an error otherwise.
	 *
	 * @param userToBeCreated
	 * @throws org.springframework.web.server.ResponseStatusException
	 * @see User
	 */
	private void checkIfUserExists(User userToBeCreated) {
		User userByName = userRepository.findByName(userToBeCreated.getName());
		User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());

		if (userByName != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
				"The username is not unique. Therefore, the user could not be created!");
		}

		if (userByUsername != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
				"The username is not unique. Therefore, the user could not be created!");
		}
	}

	public User getUserById(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!"));
	}

	public User changePassword(Long userId, String oldPassword, String newPassword) {
		// Validate new password
		if (newPassword == null || newPassword.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be empty!");
		}

		User user = getUserById(userId);

		// Verify old password
		if (!user.getPassword().equals(oldPassword)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Old password is incorrect!");
		}

		// Update password
		user.setPassword(newPassword);

		// Enforce logout: generate new token and set status to OFFLINE
		user.setToken(UUID.randomUUID().toString());
		user.setStatus(UserStatus.OFFLINE);

		user = userRepository.save(user);
		userRepository.flush();

		log.debug("Password changed for User: {}", user);
		return user;
	}

	public User loginUser(String username, String password) {
		User user = userRepository.findByUsername(username);

		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password!");
		}

		if (!user.getPassword().equals(password)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password!");
		}

		// Set user status to ONLINE on successful login
		user.setStatus(UserStatus.ONLINE);
		user = userRepository.save(user);
		userRepository.flush();

		log.debug("User logged in: {}", user);
		return user;
	}

	public User getUserByToken(String token) {
		if (token == null || token.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token!");
		}

		User user = userRepository.findByToken(token);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token!");
		}

		return user;
	}

	public User updateUser(Long userId, User updatedUser) {
		User user = getUserById(userId);

		// Check if username is already taken by another user
		if (updatedUser.getUsername() != null && !updatedUser.getUsername().equals(user.getUsername())) {
			User userByUsername = userRepository.findByUsername(updatedUser.getUsername());
			if (userByUsername != null) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
					"The username is not unique. Therefore, the user could not be updated!");
			}
			user.setUsername(updatedUser.getUsername());
		}

		// Update name if provided
		if (updatedUser.getName() != null) {
			user.setName(updatedUser.getName());
		}

		// Update bio if provided
		if (updatedUser.getBio() != null) {
			user.setBio(updatedUser.getBio());
		}

		user = userRepository.save(user);
		userRepository.flush();

		log.debug("Updated Information for User: {}", user);
		return user;
	}

	public User logoutUser(String token) {
		// Find user by token
		User user = getUserByToken(token);

		// Set status to OFFLINE and generate new token (invalidate old token)
		user.setStatus(UserStatus.OFFLINE);
		user.setToken(UUID.randomUUID().toString());

		user = userRepository.save(user);
		userRepository.flush();

		log.debug("User logged out: {}", user);
		return user;
	}
}
