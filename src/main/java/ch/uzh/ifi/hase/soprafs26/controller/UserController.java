package ch.uzh.ifi.hase.soprafs26.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PasswordChangeDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserLoginResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import java.util.UUID;

import java.util.ArrayList;
import java.util.List;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

	private final UserService userService;
	private final AuthService authService;

	UserController(UserService userService, AuthService authService) {
		this.userService = userService;
		this.authService = authService;
	}

	@GetMapping("/users")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserGetDTO> getAllUsers(@RequestHeader(value = "Authorization", required = false) String token) {
		// Check if user is authenticated
		authService.checkAuthToken(token);

		// fetch all users in the internal representation
		List<User> users = userService.getUsers();
		List<UserGetDTO> userGetDTOs = new ArrayList<>();

		// convert each user to the API representation
		for (User user : users) {
			userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
		}
		return userGetDTOs;
	}

	@GetMapping("/users/{userId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO getUserProfile(@PathVariable UUID userId,
		@RequestHeader(value = "Authorization", required = false) String token) {
		// Check if user is authenticated
		authService.checkAuthToken(token);

		// fetch user
		User user = userService.getUserById(userId);

		// convert to DTO
		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
	}

	@PostMapping("/users")
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
		if (userPostDTO.getUsername() == null || userPostDTO.getUsername().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be empty!");
		}
		if (userPostDTO.getPassword() == null || userPostDTO.getPassword().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password cannot be empty!");
		}

		// convert API user to internal representation
		User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		// create user
		User createdUser = userService.createUser(userInput);
		// convert internal representation of user back to API
		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
	}

	@PostMapping("/users/login")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserLoginResponseDTO login(@RequestBody UserPostDTO userPostDTO) {
		// Find user by username and password
		User user = userService.loginUser(userPostDTO.getUsername(), userPostDTO.getPassword());

		// Convert to login response DTO with token
		return DTOMapper.INSTANCE.convertEntityToUserLoginResponseDTO(user);
	}

	@PostMapping("/users/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@ResponseBody
	public void logout(@RequestHeader(value = "Authorization", required = false) String token) {
		// Check if user is authenticated
		authService.checkAuthToken(token);

		// Extract token from Bearer format
		String actualToken = authService.extractTokenFromBearer(token);

		// Logout user
		userService.logoutUser(actualToken);
	}

	@PutMapping("/users/{userId}/password")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGetDTO changePassword(@PathVariable UUID userId,
		@RequestBody PasswordChangeDTO passwordChangeDTO,
		@RequestHeader(value = "Authorization", required = false) String token) {
		// Check if user is authenticated
		authService.checkAuthToken(token);

		// Verify user is changing their own password
		verifyUserAuthorization(userId, token);

		// Change password
		User updatedUser = userService.changePassword(userId,
			passwordChangeDTO.getOldPassword(),
			passwordChangeDTO.getNewPassword());

		// Convert to DTO and return
		return DTOMapper.INSTANCE.convertEntityToUserGetDTO(updatedUser); // Returning update profile, but also could return just a simple message, since already pushes for logout. 
	}

	@PutMapping("/users/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@ResponseBody
	public void updateUserProfile(@PathVariable UUID userId,
		@RequestBody UserPostDTO userPostDTO,
		@RequestHeader(value = "Authorization", required = false) String token) {
		// Check if user is authenticated
		authService.checkAuthToken(token);

		// Verify user is updating their own profile
		verifyUserAuthorization(userId, token);

		// Convert DTO to User entity for update
		User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		// Update user profile
		userService.updateUser(userId, userInput);
	}

	private void verifyUserAuthorization(UUID userId, String token) {
		String actualToken = authService.extractTokenFromBearer(token);
		User user = userService.getUserByToken(actualToken);

		if (!user.getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only modify your own profile!");
		}
	}
}
