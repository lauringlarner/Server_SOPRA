package ch.uzh.ifi.hase.soprafs26.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;

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
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public UserService(@Qualifier("userRepository") UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	//////////////
	// Creation //
	//////////////



	public User createUser(User newUser) {
		// Validate username
		if (newUser.getUsername() == null || newUser.getUsername().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty!");
		}

		// Name is deleted

		// Password validation
		if (newUser.getPassword() == null || newUser.getPassword().isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty!");
		}
		if (newUser.getPassword().length() < 12) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 12 characters long!");
		}
		if (!newUser.getPassword().chars().anyMatch(Character::isDigit)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least one digit!");
		}
		if (!newUser.getPassword().chars().anyMatch(c -> !Character.isLetterOrDigit(c))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least one punctuation!");
		}


		// Bio is deleted

		newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
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

	///////////////
	// Retrieval //	
	///////////////
	
	public List<User> getUsers() {
		return this.userRepository.findAll();
	}
	
	public User getUserById(UUID userId) {
		return userRepository.findById(userId)
		.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!"));
	}
	
	public User getUserByToken(String token) {
		User user = userRepository.findByToken(token);
		if (user == null) {
			log.debug("User not found by Token!");
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token!");
		}
		return user;
	}
	
	public User getUserByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	/////////////
	// Updates //
	/////////////

	public User updateUser(UUID userId, User updatedUser) {
		User user = getUserById(userId);

		// Check if username is already taken by another user
		if (updatedUser.getUsername() != null && !updatedUser.getUsername().equals(user.getUsername())) {
			if (getUserByUsername(updatedUser.getUsername()) != null) {
				throw new ResponseStatusException(HttpStatus.CONFLICT,
					"The username is not unique. Therefore, the user could not be updated!");
			}
			user.setUsername(updatedUser.getUsername());
		}

		// name and bio are deleted

		user = userRepository.save(user);
		userRepository.flush();

		log.debug("Updated Information for User: {}", user);
		return user;
	}

	public User changePassword(UUID userId, String oldPassword, String newPassword) {
		// Validate new password
		if (newPassword == null || newPassword.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password cannot be empty!");
		}
		if (newPassword.length() < 12) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 12 characters long!");
		}
		if (!newPassword.chars().anyMatch(Character::isDigit)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least one digit!");
		}
		if (!newPassword.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must contain at least one punctuation!");
		}

		User user = getUserById(userId);

		// Verify old password
		if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Old password is incorrect!");
		}

		// Update password
		user.setPassword(passwordEncoder.encode(newPassword));

		// Enforce logout: generate new token and set status to OFFLINE
		user.setToken(UUID.randomUUID().toString());
		user.setStatus(UserStatus.OFFLINE);

		user = userRepository.save(user);
		userRepository.flush();

		log.debug("Password changed for User: {}", user);
		return user;
	}

	/////////////
	// Actions //
	/////////////
	
	public User loginUser(String username, String password) {
		User user = getUserByUsername(username);

		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password!");
		}

		if (!passwordEncoder.matches(password, user.getPassword())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password!");
		}

		// Set user status to ONLINE on successful login
		user.setStatus(UserStatus.ONLINE);
		user = userRepository.save(user);
		userRepository.flush();

		log.debug("User logged in: {}", user);
		return user;
	}

	public User logoutUser(User user) {

		// Set status to OFFLINE and generate new token (invalidate old token)
		user.setStatus(UserStatus.OFFLINE);
		user.setToken(UUID.randomUUID().toString());

		user = userRepository.save(user);
		userRepository.flush();

		log.debug("User logged out: {}", user);
		return user;
	}

	////////////////
	// Validation //
	////////////////
	
	private void checkIfUserExists(User userToBeCreated) {
		if (getUserByUsername(userToBeCreated.getUsername()) != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
				"The username is not unique. Therefore, the user could not be created!");
		}
	}

	public void validateUserMatchesUserId(UUID userId, User user) {
		if (!user.getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only modify your own profile!");
		}
	}

}
