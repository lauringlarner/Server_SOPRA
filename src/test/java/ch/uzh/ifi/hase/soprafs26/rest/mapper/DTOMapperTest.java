package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DTOMapperTest
 * Tests if the mapping between the internal and the external/API representation
 * works.
 */
public class DTOMapperTest {
	@Test
	public void testCreateUser_fromUserPostDTO_toUser_success() {
		// create UserPostDTO
		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setName("name");
		userPostDTO.setUsername("username");
		userPostDTO.setPassword("password");
		userPostDTO.setBio("bio");

		// MAP -> Create user
		User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		// check content
		assertEquals(userPostDTO.getName(), user.getName());
		assertEquals(userPostDTO.getUsername(), user.getUsername());
		assertEquals(userPostDTO.getPassword(), user.getPassword());
		assertEquals(userPostDTO.getBio(), user.getBio());
	}

	@Test
	public void testGetUser_fromUser_toUserGetDTO_success() {
		// create User
		User user = new User();
		user.setName("Firstname Lastname");
		user.setUsername("firstname@lastname");
		user.setStatus(UserStatus.OFFLINE);
		user.setToken("1");
		user.setBio("test bio");

		// MAP -> Create UserGetDTO
		UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

		// check content
		assertEquals(user.getId(), userGetDTO.getId());
		assertEquals(user.getName(), userGetDTO.getName());
		assertEquals(user.getUsername(), userGetDTO.getUsername());
		assertEquals(user.getStatus(), userGetDTO.getStatus());
		assertEquals(user.getBio(), userGetDTO.getBio());
	}

	@Test
	public void testCreateUser_fromUserPostDTO_different_fields() {
		// create UserPostDTO with different values
		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setName("John Doe");
		userPostDTO.setUsername("johndoe");
		userPostDTO.setPassword("securepassword");
		userPostDTO.setBio("I am John");

		// MAP -> Create user
		User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		// check content
		assertEquals(userPostDTO.getName(), user.getName());
		assertEquals(userPostDTO.getUsername(), user.getUsername());
		assertEquals(userPostDTO.getPassword(), user.getPassword());
		assertEquals(userPostDTO.getBio(), user.getBio());
	}

	@Test
	public void testGetUser_fromUser_toUserGetDTO_with_complete_data() {
		// create User with all fields set
		User user = new User();
		user.setId(1L);
		user.setName("Alice Smith");
		user.setUsername("alice");
		user.setStatus(UserStatus.ONLINE);
		user.setToken("secure-token-123");
		user.setBio("I love coding");

		// MAP -> Create UserGetDTO
		UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

		// check all content is correctly mapped
		assertEquals(user.getId(), userGetDTO.getId());
		assertEquals(user.getName(), userGetDTO.getName());
		assertEquals(user.getUsername(), userGetDTO.getUsername());
		assertEquals(user.getStatus(), userGetDTO.getStatus());
		assertEquals(user.getToken(), userGetDTO.getToken());
		assertEquals(user.getBio(), userGetDTO.getBio());
	}

	@Test
	public void testUpdateUser_fromUserPostDTO_toUser_success() {
		// create UserPostDTO for update
		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setName("Updated Name");
		userPostDTO.setUsername("updated_username");
		userPostDTO.setPassword("newpassword");
		userPostDTO.setBio("Updated bio");

		// MAP -> Create/Update user
		User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		// check content
		assertEquals(userPostDTO.getName(), user.getName());
		assertEquals(userPostDTO.getUsername(), user.getUsername());
		assertEquals(userPostDTO.getPassword(), user.getPassword());
		assertEquals(userPostDTO.getBio(), user.getBio());
	}
}
