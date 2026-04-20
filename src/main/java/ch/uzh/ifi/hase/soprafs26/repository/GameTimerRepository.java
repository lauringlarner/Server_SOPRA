package ch.uzh.ifi.hase.soprafs26.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.constant.TimerStatus;
import ch.uzh.ifi.hase.soprafs26.entity.GameTimer;

@Repository("gameTimerRepository")
public interface GameTimerRepository extends JpaRepository<GameTimer, UUID> {

    GameTimer findByGameId(UUID gameId);

    List<GameTimer> findAllByStatus(TimerStatus status);
}
