package ch.uzh.ifi.hase.soprafs26.controller;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PasswordChangeDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private AuthService authService;

	// POST /users
	@Test
	public void createUser_validInput_201() throws Exception {
		// given
		UUID userId = UUID.randomUUID();
		User user = new User();
		user.setId(userId);
		user.setUsername("testuser");
		user.setPassword("Password123!");
		user.setStatus(UserStatus.ONLINE);
		user.setToken("token123");

		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setUsername("testuser");
		userPostDTO.setPassword("Password123!");

		given(userService.createUser(Mockito.any())).willReturn(user);

		// when/then
		MockHttpServletRequestBuilder postRequest = post("/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(userPostDTO));

		mockMvc.perform(postRequest)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", is(userId.toString())))
				.andExpect(jsonPath("$.username", is(user.getUsername())));
	}

	@Test
	public void createUser_usernameTaken_409() throws Exception {
		// given
		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setUsername("existinguser");
		userPostDTO.setPassword("Password123!");

		given(userService.createUser(Mockito.any()))
				.willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists"));

		// when/then
		MockHttpServletRequestBuilder postRequest = post("/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(userPostDTO));

		mockMvc.perform(postRequest)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.reason", is("Username already exists")));
	}

	// GET /users/{userId}
	@Test
	public void getUserById_validId_200() throws Exception {
		// given
		UUID userId = UUID.randomUUID();
		User user = new User();
		user.setId(userId);
		user.setUsername("testuser");
		user.setStatus(UserStatus.ONLINE);
		user.setToken("token123");

		given(authService.authenticateToken("Bearer token123")).willReturn(user);
		given(userService.getUserById(userId)).willReturn(user);

		// when/then
		MockHttpServletRequestBuilder getRequest = get("/users/" + userId)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer token123");

		mockMvc.perform(getRequest)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(userId.toString())))
				.andExpect(jsonPath("$.username", is(user.getUsername())));
	}

	@Test
	public void getUserById_invalidId_404() throws Exception {
		// given
		UUID unknownId = UUID.randomUUID();
		UUID authUserId = UUID.randomUUID();
		User authUser = new User();
		authUser.setId(authUserId);
		authUser.setToken("token123");

		given(authService.authenticateToken("Bearer token123")).willReturn(authUser);
		given(userService.getUserById(unknownId))
				.willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		// when/then
		MockHttpServletRequestBuilder getRequest = get("/users/" + unknownId)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer token123");

		mockMvc.perform(getRequest)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.reason", is("User not found")));
	}

	// PUT /users/{userId}
	@Test
	public void updateUser_validId_204() throws Exception {
		// given
		UUID userId = UUID.randomUUID();
		User user = new User();
		user.setId(userId);
		user.setUsername("updateduser");
		user.setToken("token123");

		UserPostDTO updateDTO = new UserPostDTO();
		updateDTO.setUsername("updateduser");

		given(authService.authenticateToken("Bearer token123")).willReturn(user);
		given(userService.updateUser(Mockito.eq(userId), Mockito.any())).willReturn(user);

		// when/then
		MockHttpServletRequestBuilder putRequest = put("/users/" + userId)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer token123")
				.content(asJsonString(updateDTO));

		mockMvc.perform(putRequest)
				.andExpect(status().isNoContent());
	}

	@Test
	public void updateUser_unauthorized_403() throws Exception {
		// given - authenticated user tries to update a different user's profile
		UUID targetUserId = UUID.randomUUID();
		UUID authUserId = UUID.randomUUID();

		User authUser = new User();
		authUser.setId(authUserId);
		authUser.setToken("token123");

		UserPostDTO updateDTO = new UserPostDTO();
		updateDTO.setUsername("updateduser");

		given(authService.authenticateToken("Bearer token123")).willReturn(authUser);
		doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
			"You can only modify your own profile!"))
			.when(userService)
			.validateUserMatchesUserId(targetUserId, authUser);

		// when/then
		MockHttpServletRequestBuilder putRequest = put("/users/" + targetUserId)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer token123")
				.content(asJsonString(updateDTO));

		mockMvc.perform(putRequest)
				.andExpect(status().isForbidden());
	}

	// POST /users/login
	@Test
	public void login_validCredentials_200() throws Exception {
		// given
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setUsername("testuser");
		user.setToken("token123");
		user.setStatus(UserStatus.ONLINE);

		UserPostDTO loginDTO = new UserPostDTO();
		loginDTO.setUsername("testuser");
		loginDTO.setPassword("Password123!");

		given(userService.loginUser("testuser", "Password123!")).willReturn(user);

		// when/then
		MockHttpServletRequestBuilder postRequest = post("/users/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(loginDTO));

		mockMvc.perform(postRequest)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username", is(user.getUsername())))
				.andExpect(jsonPath("$.token", is(user.getToken())));
	}

	// PUT /users/{userId}/password
	@Test
	public void changePassword_validInput_200() throws Exception {
		// given
		UUID userId = UUID.randomUUID();
		User user = new User();
		user.setId(userId);
		user.setUsername("testuser");
		user.setToken("token123");

		PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO();
		passwordChangeDTO.setOldPassword("Password123!");
		passwordChangeDTO.setNewPassword("NewPassword123!");

		given(authService.authenticateToken("Bearer token123")).willReturn(user);
		given(userService.changePassword(userId, "Password123!", "NewPassword123!")).willReturn(user);

		// when/then
		MockHttpServletRequestBuilder putRequest = put("/users/" + userId + "/password")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer token123")
				.content(asJsonString(passwordChangeDTO));

		mockMvc.perform(putRequest)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username", is(user.getUsername())));
	}

	// POST /users/logout
	@Test
	public void logout_validToken_204() throws Exception {
		// given
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setToken("token123");

		given(authService.authenticateToken("Bearer token123")).willReturn(user);

		// when/then
		MockHttpServletRequestBuilder postRequest = post("/users/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer token123");

		mockMvc.perform(postRequest)
				.andExpect(status().isNoContent());
	}

	// GET /users
	@Test
	public void getAllUsers_validToken_200() throws Exception {
		// given
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setUsername("testuser");
		user.setStatus(UserStatus.ONLINE);
		user.setToken("token123");

		given(authService.authenticateToken("Bearer token123")).willReturn(user);
		given(userService.getUsers()).willReturn(List.of(user));

		// when/then
		MockHttpServletRequestBuilder getRequest = get("/users")
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "Bearer token123");

		mockMvc.perform(getRequest)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].username", is(user.getUsername())));
	}

	/**
	 * Helper Method to convert userPostDTO into a JSON string such that the input
	 * can be processed
	 *
	 * @param object
	 * @return string
	 */
	private String asJsonString(final Object object) {
		try {
			return new ObjectMapper().writeValueAsString(object);
		} catch (JacksonException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("The request body could not be created.%s", e.toString()));
		}
	}
}
