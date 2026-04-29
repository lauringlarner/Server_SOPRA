package ch.uzh.ifi.hase.soprafs26.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.pusher.rest.Pusher;

public class PusherServiceTest {
    
    
    @Test
    void constructor_shouldInitializePusherInstance() throws Exception {
        PusherService service = new PusherService(
                "appId",
                "key",
                "secret",
                "eu"
        );

        Field field = PusherService.class.getDeclaredField("pusher");
        field.setAccessible(true);
        Object value = field.get(service);

        assertNotNull(value);
        assertTrue(value instanceof Pusher);
    }

    @Test
    void trigger_shouldNotThrowException() {
        PusherService service = new PusherService(
                "appId",
                "key",
                "secret",
                "eu"
        );

        assertDoesNotThrow(() -> service.trigger("testChannel", "testEvent", "payload"));
    }

}