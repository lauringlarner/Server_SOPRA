package ch.uzh.ifi.hase.soprafs26.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseService {

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void register(UUID id, SseEmitter emitter) {
        emitters.computeIfAbsent(id, k -> new ArrayList<>()).add(emitter);
    }

    public void remove(UUID id, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(id);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(id);
            }
        }
    }
    
    public <T> void push(UUID id, String eventName, T data) {
        List<SseEmitter> list = emitters.get(id);
        if (list == null) return;

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }
    }
}
