package ua.horuktaras.osbb.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ua.horuktaras.osbb.bot.model.entity.ProcessedUpdate;

import java.time.Instant;

@Repository
public interface ProcessedUpdateRepository extends JpaRepository<ProcessedUpdate, Long> {

    boolean existsByUpdateId(Integer updateId);

    @Modifying
    @Query("DELETE FROM ProcessedUpdate pu WHERE pu.processedAt < :cutoff")
    int deleteByProcessedAtBefore(@Param("cutoff") Instant cutoff);
}
