package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import org.mockito.Mockito;
import static org.mockito.Mockito.doThrow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.service.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
//@WebMvcTest(UserController.class)
public class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private AuthService authService;

	@Test
	public void createUser_validInput_201() throws Exception {
		// given
		UUID userId = UUID.randomUUID();
		User user = new User();
		user.setId(userId);
		user.setUsername("testuser");
		user.setPassword("password123");
		user.setStatus(UserStatus.ONLINE);
		user.setToken("token123");

		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setUsername("testuser");
		userPostDTO.setPassword("password123");

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
		userPostDTO.setPassword("password123");

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

	@Test
	public void getUserById_validId_200() throws Exception {
		// given
		UUID userId = UUID.randomUUID();
		User user = new User();
		user.setId(userId);
		user.setUsername("testuser");
		user.setStatus(UserStatus.ONLINE);
		user.setToken("token123");

		given(userService.getUserByToken("token123")).willReturn(user);
		given(userService.getUserById(userId)).willReturn(user);

		// when/then
		MockHttpServletRequestBuilder getRequest = get("/users/" + userId)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "token123");

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

		given(userService.getUserByToken("token123")).willReturn(authUser);
		given(userService.getUserById(unknownId))
				.willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		// when/then
		MockHttpServletRequestBuilder getRequest = get("/users/" + unknownId)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "token123");

		mockMvc.perform(getRequest)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.reason", is("User not found")));
	}

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

		given(authService.extractTokenFromBearer("token123")).willReturn("token123");
		given(userService.getUserByToken("token123")).willReturn(user);
		given(userService.updateUser(Mockito.eq(userId), Mockito.any())).willReturn(user);

		// when/then
		MockHttpServletRequestBuilder putRequest = put("/users/" + userId)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "token123")
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

		given(authService.authenticateToken("token123")).willReturn(authUser);
		doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, 
        	"You can only modify your own profile!"))
        	.when(userService)
        	.validateUserMatchesUserId(targetUserId, authUser);
			
		// when/then
		MockHttpServletRequestBuilder putRequest = put("/users/" + targetUserId)
				.contentType(MediaType.APPLICATION_JSON)
				.header("Authorization", "token123")
				.content(asJsonString(updateDTO));

		mockMvc.perform(putRequest)
				.andExpect(status().isForbidden());
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
