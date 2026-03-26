package ch.uzh.ifi.hase.soprafs26.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs26.entity.User;

@Repository("userRepository")
public interface UserRepository extends JpaRepository<User, UUID> {


	User findByUsername(String username);

	User findByToken(String token);
}
