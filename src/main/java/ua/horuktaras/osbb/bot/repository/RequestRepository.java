package ua.horuktaras.osbb.bot.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.horuktaras.osbb.bot.model.entity.Request;
import ua.horuktaras.osbb.bot.model.enums.RequestStatus;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByStatus(RequestStatus status, Sort sort);
    long countByStatus(RequestStatus status);
}
