package ch.uzh.ifi.hase.soprafs26.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.TimerStatus;
import ch.uzh.ifi.hase.soprafs26.entity.GameTimer;
import ch.uzh.ifi.hase.soprafs26.repository.GameTimerRepository;

@ExtendWith(MockitoExtension.class)
class GameTimerServiceTest {

    @Mock
    private GameTimerRepository gameTimerRepository;

    @InjectMocks
    private GameTimerService gameTimerService;

    private UUID gameId;

    @BeforeEach
    void setup() {
        gameId = UUID.randomUUID();
    }

    // ---------------- CREATE TIMER ----------------

    @Test
    void createTimer_valid_success() {
        when(gameTimerRepository.findByGameId(gameId)).thenReturn(null);

        GameTimer saved = new GameTimer();
        when(gameTimerRepository.save(any(GameTimer.class))).thenReturn(saved);

        GameTimer result = gameTimerService.createTimer(gameId, 30);

        assertNotNull(result);
        verify(gameTimerRepository).save(any(GameTimer.class));
    }

    @Test
    void createTimer_invalidDuration_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> gameTimerService.createTimer(gameId, 2)
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void createTimer_alreadyExists_throwsConflict() {
        GameTimer existing = new GameTimer();
        when(gameTimerRepository.findByGameId(gameId)).thenReturn(existing);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> gameTimerService.createTimer(gameId, 10)
        );

        assertEquals(409, ex.getStatusCode().value());
    }

    // ---------------- START TIMER ----------------

    @Test
    void startTimer_valid_success() {
        GameTimer timer = new GameTimer();
        timer.setStatus(TimerStatus.NOT_STARTED);
        timer.setDurationMinutes(30);

        when(gameTimerRepository.findByGameId(gameId)).thenReturn(timer);
        when(gameTimerRepository.save(any(GameTimer.class))).thenReturn(timer);

        GameTimer result = gameTimerService.startTimer(gameId);

        assertEquals(TimerStatus.RUNNING, result.getStatus());
        assertNotNull(result.getStartedAt());
    }

    @Test
    void startTimer_alreadyStarted_throwsConflict() {
        GameTimer timer = new GameTimer();
        timer.setStatus(TimerStatus.RUNNING);

        when(gameTimerRepository.findByGameId(gameId)).thenReturn(timer);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> gameTimerService.startTimer(gameId)
        );

        assertEquals(409, ex.getStatusCode().value());
    }

    // ---------------- GET TIMER ----------------

    @Test
    void getTimerByGameId_notFound_throws404() {
        when(gameTimerRepository.findByGameId(gameId)).thenReturn(null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> gameTimerService.getTimerByGameId(gameId)
        );

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void getTimerByGameId_found_success() {
        GameTimer timer = new GameTimer();
        when(gameTimerRepository.findByGameId(gameId)).thenReturn(timer);

        GameTimer result = gameTimerService.getTimerByGameId(gameId);

        assertNotNull(result);
    }

    // ---------------- REMAINING MINUTES ----------------

    @Test
    void getRemainingMinutes_notStarted_returnsDuration() {
        GameTimer timer = new GameTimer();
        timer.setStatus(TimerStatus.NOT_STARTED);
        timer.setDurationMinutes(25);

        int result = gameTimerService.getRemainingMinutes(timer);

        assertEquals(25, result);
    }

    @Test
    void getRemainingMinutes_finished_returnsZero() {
        GameTimer timer = new GameTimer();
        timer.setStatus(TimerStatus.FINISHED);

        int result = gameTimerService.getRemainingMinutes(timer);

        assertEquals(0, result);
    }

    @Test
    void getRemainingMinutes_running_returnsPositiveOrZero() {
        GameTimer timer = new GameTimer();
        timer.setStatus(TimerStatus.RUNNING);
        timer.setDurationMinutes(10);
        timer.setStartedAt(Instant.now().minusSeconds(120)); // 2 minutes ago

        int result = gameTimerService.getRemainingMinutes(timer);

        assertTrue(result >= 0);
    }

    // ---------------- SCHEDULED TASK ----------------

    @Test
    void checkExpiredTimers_setsFinished_whenExpired() {
        GameTimer timer = new GameTimer();
        timer.setStatus(TimerStatus.RUNNING);
        timer.setDurationMinutes(1);
        timer.setStartedAt(Instant.now().minusSeconds(3600)); // expired

        when(gameTimerRepository.findAllByStatus(TimerStatus.RUNNING))
                .thenReturn(List.of(timer));

        gameTimerService.checkExpiredTimers();

        verify(gameTimerRepository).save(timer);
        assertEquals(TimerStatus.FINISHED, timer.getStatus());
    }
}