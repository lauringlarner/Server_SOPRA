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
		userPostDTO.setUsername("username");
		userPostDTO.setPassword("password");
		userPostDTO.setEmail("email@example.com");

		// MAP -> Create user
		User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		// check content
		assertEquals(userPostDTO.getUsername(), user.getUsername());
		assertEquals(userPostDTO.getPassword(), user.getPassword());
		assertEquals(userPostDTO.getEmail(), user.getEmail());
	}

	@Test
	public void testGetUser_fromUser_toUserGetDTO_success() {
		// create User
		User user = new User();
		user.setUsername("firstname@lastname");
		user.setStatus(UserStatus.OFFLINE);
		user.setToken("1");

		// MAP -> Create UserGetDTO
		UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

		// check content
		assertEquals(user.getId(), userGetDTO.getId());
		assertEquals(user.getUsername(), userGetDTO.getUsername());
		assertEquals(user.getStatus(), userGetDTO.getStatus());
	}

	@Test
	public void testCreateUser_fromUserPostDTO_different_fields() {
		// create UserPostDTO with different values
		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setUsername("johndoe");
		userPostDTO.setPassword("securepassword");

		// MAP -> Create user
		User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		// check content
		assertEquals(userPostDTO.getUsername(), user.getUsername());
		assertEquals(userPostDTO.getPassword(), user.getPassword());
	}

	@Test
	public void testGetUser_fromUser_toUserGetDTO_with_complete_data() {
		// create User with all fields set
		User user = new User();
		user.setUsername("alice");
		user.setEmail("alice@example.com");
		user.setStatus(UserStatus.ONLINE);
		user.setToken("secure-token-123");
		user.setGamesPlayed(5);
		user.setGamesWon(3);
		user.setCorrectItemsFound(10);

		// MAP -> Create UserGetDTO
		UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

		// check all content is correctly mapped
		assertEquals(user.getId(), userGetDTO.getId());
		assertEquals(user.getUsername(), userGetDTO.getUsername());
		assertEquals(user.getEmail(), userGetDTO.getEmail());
		assertEquals(user.getStatus(), userGetDTO.getStatus());
		assertEquals(user.getToken(), userGetDTO.getToken());
		assertEquals(user.getGamesPlayed(), userGetDTO.getGamesPlayed());
		assertEquals(user.getGamesWon(), userGetDTO.getGamesWon());
		assertEquals(user.getCorrectItemsFound(), userGetDTO.getCorrectItemsFound());
	}

	@Test
	public void testUpdateUser_fromUserPostDTO_toUser_success() {
		// create UserPostDTO for update
		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setUsername("updated_username");
		userPostDTO.setPassword("newpassword");
		userPostDTO.setEmail("updated@example.com");

		// MAP -> Create/Update user
		User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

		// check content
		assertEquals(userPostDTO.getUsername(), user.getUsername());
		assertEquals(userPostDTO.getPassword(), user.getPassword());
		assertEquals(userPostDTO.getEmail(), user.getEmail());
	}
}
