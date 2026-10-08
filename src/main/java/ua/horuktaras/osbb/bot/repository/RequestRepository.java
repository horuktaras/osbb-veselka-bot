package ua.horuktaras.osbb.bot.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.horuktaras.osbb.bot.model.entity.Request;
import ua.horuktaras.osbb.bot.model.enums.RequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByStatus(RequestStatus status, Sort sort);
    long countByStatus(RequestStatus status);

    @Query("SELECT r FROM Request r WHERE r.status <> ua.horuktaras.osbb.bot.model.enums.RequestStatus.CLOSED AND r.createdAt >= :since")
    List<Request> findOpenSince(@Param("since") LocalDateTime since);
}
