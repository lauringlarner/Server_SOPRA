package ch.uzh.ifi.hase.soprafs26.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.TimerStatus;
import ch.uzh.ifi.hase.soprafs26.entity.GameTimer;
import ch.uzh.ifi.hase.soprafs26.repository.GameTimerRepository;

@Service
@Transactional
public class GameTimerService {

    private final GameTimerRepository gameTimerRepository;

    public GameTimerService(@Qualifier("gameTimerRepository") GameTimerRepository gameTimerRepository) {
        this.gameTimerRepository = gameTimerRepository;
    }

    public GameTimer createTimer(UUID gameId, Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes < 5 || durationMinutes > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game duration must be between 5 and 120 minutes!");
        }
        if (gameTimerRepository.findByGameId(gameId) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A timer already exists for this game!");
        }

        GameTimer timer = new GameTimer();
        timer.setGameId(gameId);
        timer.setDurationMinutes(durationMinutes);
        timer.setStatus(TimerStatus.NOT_STARTED);

        return gameTimerRepository.save(timer);
    }

    public GameTimer startTimer(UUID gameId) {
        GameTimer timer = getTimerByGameId(gameId);

        if (timer.getStatus() != TimerStatus.NOT_STARTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Timer is already started!");
        }

        timer.setStartedAt(Instant.now());
        timer.setStatus(TimerStatus.RUNNING);

        return gameTimerRepository.save(timer);
    }

    public GameTimer getTimerByGameId(UUID gameId) {
        GameTimer timer = gameTimerRepository.findByGameId(gameId);
        if (timer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No timer found for this game!");
        }
        return timer;
    }

    public Integer getRemainingMinutes(GameTimer timer) {
        if (timer.getStatus() == TimerStatus.NOT_STARTED) {
            return timer.getDurationMinutes();
        }
        if (timer.getStatus() == TimerStatus.FINISHED) {
            return 0;
        }

        long elapsedMinutes = (Instant.now().getEpochSecond() - timer.getStartedAt().getEpochSecond()) / 60;
        int remaining = (int) (timer.getDurationMinutes() - elapsedMinutes);
        return Math.max(remaining, 0);
    }

    @Scheduled(fixedRate = 60000)
    public void checkExpiredTimers() {
        List<GameTimer> runningTimers = gameTimerRepository.findAllByStatus(TimerStatus.RUNNING);

        for (GameTimer timer : runningTimers) {
            if (getRemainingMinutes(timer) == 0) {
                timer.setStatus(TimerStatus.FINISHED);
                gameTimerRepository.save(timer);
            }
        }
    }
}
