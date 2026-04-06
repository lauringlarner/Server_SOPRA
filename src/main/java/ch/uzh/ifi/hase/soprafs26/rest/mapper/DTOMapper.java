package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyJoinCodeDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserLoginResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GamePostDTO;


/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

	DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

	@Mapping(source = "username", target = "username")
	@Mapping(source = "password", target = "password")
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "token", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "gamesPlayed", ignore = true)
	@Mapping(target = "gamesWon", ignore = true)
	@Mapping(target = "correctItemsFound", ignore = true)
	User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "token", target = "token")
	@Mapping(source = "status", target = "status")
	@Mapping(source = "createdAt", target = "createdAt")
	UserGetDTO convertEntityToUserGetDTO(User user);

	//game mapper
	@Mapping(source = "name", target = "name")
	Game convertGamePostDTOtoEntity(GamePostDTO gamePostDTO);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "name", target = "name")
	@Mapping(source = "status", target = "status")
	@Mapping(source = "wordList", target = "wordList")
	@Mapping(source = "token", target = "token")
	GameGetDTO convertEntityToGameGetDTO(Game game);
	@Mapping(source = "id", target = "id")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "token", target = "token")
	@Mapping(source = "status", target = "status")
	@Mapping(source = "createdAt", target = "createdAt")
	UserLoginResponseDTO convertEntityToUserLoginResponseDTO(User user);



	@Mapping(source = "joinCode", target = "joinCode")
	LobbyJoinCodeDTO convertEntityToLobbyJoinCodeDTO(Lobby lobby);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "joinCode", target = "joinCode")
	@Mapping(source = "gameDuration", target = "gameDuration")
	@Mapping(source = "bingoBoardSize", target = "bingoBoardSize")
	@Mapping(source = "lobbyPlayers", target = "lobbyPlayers")
	LobbyDTO convertEntityToLobbyDTO(Lobby lobby);

}
