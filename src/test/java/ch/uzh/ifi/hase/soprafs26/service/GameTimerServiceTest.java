package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.TimerStatus;
import ch.uzh.ifi.hase.soprafs26.entity.GameTimer;
import ch.uzh.ifi.hase.soprafs26.repository.GameTimerRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
public class GameTimerServiceTest {

    @Mock
    private GameTimerRepository gameTimerRepository;

    @InjectMocks
    private GameTimerService gameTimerService;

    private UUID gameId;
    private GameTimer testTimer;

    @BeforeEach
    void setUp() {
        gameId = UUID.randomUUID();

        testTimer = new GameTimer();
        testTimer.setGameId(gameId);
        testTimer.setDurationMinutes(60);
        testTimer.setStatus(TimerStatus.NOT_STARTED);
    }

    // ─────────────────────────────────────────────
    // createTimer
    // ─────────────────────────────────────────────

    // A valid duration creates a timer with NOT_STARTED status and correct gameId
    @Test
    public void createTimer_validDuration_returnsNotStartedTimer() {
        given(gameTimerRepository.findByGameId(gameId)).willReturn(null);
        given(gameTimerRepository.save(any(GameTimer.class))).willAnswer(inv -> inv.getArgument(0));

        GameTimer result = gameTimerService.createTimer(gameId, 30);

        assertNotNull(result);
        assertEquals(gameId, result.getGameId());
        assertEquals(30, result.getDurationMinutes());
        assertEquals(TimerStatus.NOT_STARTED, result.getStatus());
        verify(gameTimerRepository).save(any(GameTimer.class));
    }

    // 5 minutes is the lower boundary; it must be accepted
    @Test
    public void createTimer_minimumDuration_returnsTimer() {
        given(gameTimerRepository.findByGameId(gameId)).willReturn(null);
        given(gameTimerRepository.save(any(GameTimer.class))).willAnswer(inv -> inv.getArgument(0));

        GameTimer result = gameTimerService.createTimer(gameId, 5);

        assertEquals(5, result.getDurationMinutes());
    }

    // 120 minutes is the upper boundary; it must be accepted
    @Test
    public void createTimer_maximumDuration_returnsTimer() {
        given(gameTimerRepository.findByGameId(gameId)).willReturn(null);
        given(gameTimerRepository.save(any(GameTimer.class))).willAnswer(inv -> inv.getArgument(0));

        GameTimer result = gameTimerService.createTimer(gameId, 120);

        assertEquals(120, result.getDurationMinutes());
    }

    // A null duration must be rejected before any repository call
    @Test
    public void createTimer_nullDuration_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameTimerService.createTimer(gameId, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // 4 minutes is one below the minimum; it must be rejected
    @Test
    public void createTimer_durationTooShort_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameTimerService.createTimer(gameId, 4));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // 121 minutes is one above the maximum; it must be rejected
    @Test
    public void createTimer_durationTooLong_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameTimerService.createTimer(gameId, 121));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    // Creating a second timer for the same game must return 409
    @Test
    public void createTimer_timerAlreadyExists_throwsConflict() {
        given(gameTimerRepository.findByGameId(gameId)).willReturn(testTimer);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameTimerService.createTimer(gameId, 30));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ─────────────────────────────────────────────
    // startTimer
    // ─────────────────────────────────────────────

    // Starting a NOT_STARTED timer must flip its status to RUNNING and record startedAt
    @Test
    public void startTimer_notStartedTimer_setsRunning() {
        given(gameTimerRepository.findByGameId(gameId)).willReturn(testTimer);
        given(gameTimerRepository.save(testTimer)).willReturn(testTimer);

        GameTimer result = gameTimerService.startTimer(gameId);

        assertEquals(TimerStatus.RUNNING, result.getStatus());
        assertNotNull(result.getStartedAt());
        verify(gameTimerRepository).save(testTimer);
    }

    // Starting an already running timer must return 409 without touching the repository
    @Test
    public void startTimer_alreadyRunning_throwsConflict() {
        testTimer.setStatus(TimerStatus.RUNNING);
        testTimer.setStartedAt(Instant.now().minus(10, ChronoUnit.MINUTES));
        given(gameTimerRepository.findByGameId(gameId)).willReturn(testTimer);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameTimerService.startTimer(gameId));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // Starting a finished timer must also return 409; a finished game cannot be restarted
    @Test
    public void startTimer_finishedTimer_throwsConflict() {
        testTimer.setStatus(TimerStatus.FINISHED);
        given(gameTimerRepository.findByGameId(gameId)).willReturn(testTimer);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameTimerService.startTimer(gameId));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // Starting a timer for a non-existent game must return 404
    @Test
    public void startTimer_timerNotFound_throwsNotFound() {
        given(gameTimerRepository.findByGameId(gameId)).willReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameTimerService.startTimer(gameId));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ─────────────────────────────────────────────
    // getTimerByGameId
    // ─────────────────────────────────────────────

    // A known gameId must return the correct timer object
    @Test
    public void getTimerByGameId_existingTimer_returnsTimer() {
        given(gameTimerRepository.findByGameId(gameId)).willReturn(testTimer);

        GameTimer result = gameTimerService.getTimerByGameId(gameId);

        assertNotNull(result);
        assertEquals(gameId, result.getGameId());
    }

    // An unknown gameId must throw 404 instead of returning null
    @Test
    public void getTimerByGameId_noTimer_throwsNotFound() {
        given(gameTimerRepository.findByGameId(gameId)).willReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameTimerService.getTimerByGameId(gameId));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ─────────────────────────────────────────────
    // getRemainingMinutes
    // ─────────────────────────────────────────────

    // A NOT_STARTED timer should report its full configured duration as remaining
    @Test
    public void getRemainingMinutes_notStarted_returnsFullDuration() {
        testTimer.setStatus(TimerStatus.NOT_STARTED);

        int remaining = gameTimerService.getRemainingMinutes(testTimer);

        assertEquals(60, remaining);
    }

    // A FINISHED timer should always report 0 regardless of duration
    @Test
    public void getRemainingMinutes_finished_returnsZero() {
        testTimer.setStatus(TimerStatus.FINISHED);

        int remaining = gameTimerService.getRemainingMinutes(testTimer);

        assertEquals(0, remaining);
    }

    // Elapsed time is subtracted correctly: 30 of 60 minutes gone → 30 remaining
    @Test
    public void getRemainingMinutes_halfElapsed_returnsHalf() {
        testTimer.setStatus(TimerStatus.RUNNING);
        testTimer.setDurationMinutes(60);
        testTimer.setStartedAt(Instant.now().minus(30, ChronoUnit.MINUTES));

        int remaining = gameTimerService.getRemainingMinutes(testTimer);

        assertEquals(30, remaining);
    }

    // With only 1 minute left the value must not round down to 0 prematurely
    @Test
    public void getRemainingMinutes_almostExpired_returnsOne() {
        testTimer.setStatus(TimerStatus.RUNNING);
        testTimer.setDurationMinutes(60);
        testTimer.setStartedAt(Instant.now().minus(59, ChronoUnit.MINUTES));

        int remaining = gameTimerService.getRemainingMinutes(testTimer);

        assertEquals(1, remaining);
    }

    // A user polling GET /games/{id}/timer sees 0 even if the scheduler hasn't ticked yet to mark
    // the timer FINISHED — the status is still RUNNING but the time window has passed
    @Test
    public void getRemainingMinutes_expiredButSchedulerNotYetRun_returnsZero() {
        testTimer.setStatus(TimerStatus.RUNNING);
        testTimer.setDurationMinutes(60);
        testTimer.setStartedAt(Instant.now().minus(61, ChronoUnit.MINUTES));

        int remaining = gameTimerService.getRemainingMinutes(testTimer);

        assertEquals(0, remaining);
    }

    // Integer division truncates seconds: 59 min 30 sec elapsed still shows 1 min remaining,
    // so the user sees 1 minute on screen but may have as little as 1 second left
    @Test
    public void getRemainingMinutes_subMinutePrecision_truncatesToWholeMinutes() {
        testTimer.setStatus(TimerStatus.RUNNING);
        testTimer.setDurationMinutes(60);
        testTimer.setStartedAt(Instant.now().minus(59, ChronoUnit.MINUTES).minus(30, ChronoUnit.SECONDS));

        int remaining = gameTimerService.getRemainingMinutes(testTimer);

        assertEquals(1, remaining);
    }

    // ─────────────────────────────────────────────
    // checkExpiredTimers
    // ─────────────────────────────────────────────

    // The scheduled job must flip an overdue RUNNING timer to FINISHED and persist it
    @Test
    public void checkExpiredTimers_expiredTimer_markedFinished() {
        GameTimer expired = new GameTimer();
        expired.setGameId(UUID.randomUUID());
        expired.setDurationMinutes(10);
        expired.setStartedAt(Instant.now().minus(11, ChronoUnit.MINUTES));
        expired.setStatus(TimerStatus.RUNNING);

        given(gameTimerRepository.findAllByStatus(TimerStatus.RUNNING)).willReturn(List.of(expired));

        gameTimerService.checkExpiredTimers();

        assertEquals(TimerStatus.FINISHED, expired.getStatus());
        verify(gameTimerRepository).save(expired);
    }

    // A timer still within its duration must not be touched by the scheduled job
    @Test
    public void checkExpiredTimers_activeTimer_notModified() {
        GameTimer active = new GameTimer();
        active.setGameId(UUID.randomUUID());
        active.setDurationMinutes(60);
        active.setStartedAt(Instant.now().minus(10, ChronoUnit.MINUTES));
        active.setStatus(TimerStatus.RUNNING);

        given(gameTimerRepository.findAllByStatus(TimerStatus.RUNNING)).willReturn(List.of(active));

        gameTimerService.checkExpiredTimers();

        assertEquals(TimerStatus.RUNNING, active.getStatus());
        verify(gameTimerRepository, never()).save(active);
    }

    // When no timers are running, the scheduled job must not call save at all
    @Test
    public void checkExpiredTimers_noRunningTimers_nothingHappens() {
        given(gameTimerRepository.findAllByStatus(TimerStatus.RUNNING)).willReturn(Collections.emptyList());

        gameTimerService.checkExpiredTimers();

        verify(gameTimerRepository, never()).save(any());
    }

    // Only the expired timer in a mixed list must be saved; the active one must be left alone
    @Test
    public void checkExpiredTimers_mixedTimers_onlyExpiredMarked() {
        GameTimer expired = new GameTimer();
        expired.setGameId(UUID.randomUUID());
        expired.setDurationMinutes(10);
        expired.setStartedAt(Instant.now().minus(15, ChronoUnit.MINUTES));
        expired.setStatus(TimerStatus.RUNNING);

        GameTimer active = new GameTimer();
        active.setGameId(UUID.randomUUID());
        active.setDurationMinutes(60);
        active.setStartedAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        active.setStatus(TimerStatus.RUNNING);

        given(gameTimerRepository.findAllByStatus(TimerStatus.RUNNING)).willReturn(List.of(expired, active));

        gameTimerService.checkExpiredTimers();

        assertEquals(TimerStatus.FINISHED, expired.getStatus());
        assertEquals(TimerStatus.RUNNING, active.getStatus());
        verify(gameTimerRepository, times(1)).save(expired);
        verify(gameTimerRepository, never()).save(active);
    }


}
