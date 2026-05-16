package ch.uzh.ifi.hase.soprafs26.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.entity.ChatMessage;

// We can get rid of the whole Repository part
// Do we need any repository or persistance?

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByGameIdOrderBySentAtAsc(UUID gameId);
}