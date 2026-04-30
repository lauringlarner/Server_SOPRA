package ch.uzh.ifi.hase.soprafs26.rest.mapper;

import java.time.Instant;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.entity.LobbyPlayer;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyAccessInfoDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyPlayerDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserLoginResponseDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ImageAnalysisGetDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.ImageAnalysisResult;



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

	// User mapper
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
	
	@Mapping(source = "id", target = "id")
	@Mapping(source = "username", target = "username")
	@Mapping(source = "token", target = "token")
	@Mapping(source = "status", target = "status")
	@Mapping(source = "createdAt", target = "createdAt")
	UserLoginResponseDTO convertEntityToUserLoginResponseDTO(User user);
	
	//image mappper
	@Mapping(source = "found", target = "found")
	ImageAnalysisGetDTO convertImageAnalysisResultToGetDTO(ImageAnalysisResult result);

	// game mapper
	@Mapping(source = "id", target = "id")
	@Mapping(source = "status", target = "status")
	@Mapping(source = "wordList", target = "wordList")
	@Mapping(source = "wordListScore", target = "wordListScore")
	@Mapping(source = "score_1", target = "score_1")
	@Mapping(source = "score_2", target = "score_2")
	@Mapping(source = "gameDuration", target = "gameDuration")
	@Mapping(source = "startedAt", target = "startedAt")
	@Mapping(source = "lobbyId", target = "lobbyId")
	@Mapping(source = "tileGrid", target = "tileGrid")
	GameDTO convertEntityToGameDTO(Game game);

	// lobby mapper
	@Mapping(source = "id", target = "id")
	@Mapping(source = "joinCode", target = "joinCode")
	LobbyAccessInfoDTO convertEntityToLobbyAccessInfoDTO(Lobby lobby);

	@Mapping (source = "id", target = "id")
	@Mapping(source = "joinedAt", target = "joinedAt")
	@Mapping(source = "teamType", target = "teamType")
	@Mapping(source = "isHost", target = "isHost")
	@Mapping(source = "isReady", target = "isReady")
	@Mapping(source = "user", target = "userGetDTO")
	LobbyPlayerDTO convertEntityToLobbyPlayerDTO(LobbyPlayer lobbyPlayer);

	@Mapping(source = "id", target = "id")
	@Mapping(source = "joinCode", target = "joinCode")
	@Mapping(source = "gameDuration", target = "gameDuration")
	@Mapping(source = "gameId", target = "gameId")
	@Mapping(source = "lobbyPlayers", target = "lobbyPlayers")
	@Mapping(source = "listType", target = "listType")
	LobbyDTO convertEntityToLobbyDTO(Lobby lobby);

	default String map(Instant value) {
		return value == null ? null : value.toString();
	}

}
