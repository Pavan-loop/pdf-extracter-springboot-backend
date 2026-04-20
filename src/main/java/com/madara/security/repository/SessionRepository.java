package com.madara.security.repository;

import com.madara.security.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    @Query(
            value = "SELECT COUNT(*) FROM session WHERE user_id = :userId",
            nativeQuery = true
    )
    Long countByUserID(@Param("userId") Long userId);

    List<Session> getSessionByUserId(Long userId);
    Session getSessionById(Long sessionId);
}
