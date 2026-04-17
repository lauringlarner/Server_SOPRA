package ch.uzh.ifi.hase.soprafs26.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.entity.Game;

@Repository("gameRepository")
public interface GameRepository extends JpaRepository<Game, UUID> {
	Optional<Game> findById(UUID id);

}

