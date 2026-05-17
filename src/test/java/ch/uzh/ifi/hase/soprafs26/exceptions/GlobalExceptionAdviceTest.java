package ch.uzh.ifi.hase.soprafs26.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.rest.dto.ErrorDTO;

class GlobalExceptionAdviceTest {

    private final GlobalExceptionAdvice advice = new GlobalExceptionAdvice();

    @Test
    void handleResponseStatusException_notFound_returnsCorrectStatusAndBody() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");

        ResponseEntity<ErrorDTO> response = advice.handleResponseStatusException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Game not found", response.getBody().getReason());
    }

    @Test
    void handleResponseStatusException_badRequest_returnsCorrectStatus() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input");

        ResponseEntity<ErrorDTO> response = advice.handleResponseStatusException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid input", response.getBody().getReason());
    }

    @Test
    void handleTransactionSystemException_returnsConflict() {
        TransactionSystemException ex = new TransactionSystemException("Constraint violation");
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseStatusException result = advice.handleTransactionSystemException(ex, request);

        assertEquals(HttpStatus.CONFLICT, result.getStatusCode());
    }

    @Test
    void handleException_returnsInternalServerError() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseStatusException result = advice.handleException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
    }
}
