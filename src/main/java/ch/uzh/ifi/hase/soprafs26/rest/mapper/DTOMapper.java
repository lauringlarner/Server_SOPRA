package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import java.time.LocalDateTime;
import java.util.UUID;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.constant.TeamColor;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyPlayerDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserLoginResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;

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

	@Mapping(source = "name", target = "name")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "password", target = "password")
	@Mapping(source = "bio", target = "bio")
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "token", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "name", target = "name")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "token", target = "token")
	@Mapping(source = "status", target = "status")
	@Mapping(source = "bio", target = "bio")
	@Mapping(source = "createdAt", target = "createdAt")
	UserGetDTO convertEntityToUserGetDTO(User user);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "token", target = "token")
	@Mapping(source = "status", target = "status")
	@Mapping(source = "bio", target = "bio")
	@Mapping(source = "createdAt", target = "createdAt")
	UserLoginResponseDTO convertEntityToUserLoginResponseDTO(User user);



	@Mapping(source = "id", target = "id")
	@Mapping(source = "joinCode", target = "joinCode")
	@Mapping(source = "gameDuration", target = "gameDuration")
	@Mapping(source = "bingoBoardSize", target = "bingoBoardSize")
	@Mapping(source = "lobbyPlayers", target = "lobbyPlayers")
	LobbyDTO convertEntityToLobbyDTO(Lobby lobby);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "joinedAt", target = "joinedAt")
	@Mapping(source = "team", target = "team")
	@Mapping(source = "isHost", target = "isHost")
	@Mapping(source = "isReady", target = "isReady")
	@Mapping(source = "userId", target = "userId")
	LobbyPlayerDTO lobbyPlayerToLobbyPlayerDTO(LobbyPlayer lobbyPlayer);

}
