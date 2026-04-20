package ch.uzh.ifi.hase.soprafs26.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseService {

    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private final Logger log = LoggerFactory.getLogger(SseService.class);

    public void register(UUID id, SseEmitter emitter) {
        emitters.computeIfAbsent(id, k -> new CopyOnWriteArrayList<>()).add(emitter);
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
                log.error("SSE send failed for {}", id, e);
                emitter.completeWithError(e);
                remove(id, emitter);
            }
        }
    }

    @Scheduled(fixedRate = 15000) public void sendHeartbeat() {
         for (Map.Entry<UUID, CopyOnWriteArrayList<SseEmitter>> entry : emitters.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                try { 
                    emitter.send(SseEmitter.event()
                        .comment("heartbeat"));
                } catch (Exception e) {
                    log.error("SSE heartbeat send failed for lobby {}", entry.getKey(), e);
                    emitter.completeWithError(e);
                    remove(entry.getKey(), emitter);
                }
            }
        }
    }
}

