package ch.uzh.ifi.hase.soprafs26.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;

@Repository("lobbyRepository")
public interface LobbyRepository extends JpaRepository<Lobby, UUID> {
    Lobby findByJoinCode(String joinCode);
}
