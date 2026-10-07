package ua.horuktaras.osbb.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.horuktaras.osbb.bot.model.entity.Request;

public interface RequestRepository extends JpaRepository<Request, Long> {
}
