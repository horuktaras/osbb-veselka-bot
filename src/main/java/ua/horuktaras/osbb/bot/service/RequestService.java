package ua.horuktaras.osbb.bot.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.horuktaras.osbb.bot.model.entity.Request;
import ua.horuktaras.osbb.bot.model.enums.RequestStatus;
import ua.horuktaras.osbb.bot.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RequestService {

    private final RequestRepository repository;

    public RequestService(RequestRepository repository) {
        this.repository = repository;
    }

    public Request save(Request request) {
        return repository.save(request);
    }

    public Optional<Request> findById(Long id) {
        return repository.findById(id);
    }

    public List<Request> findOpenSince(LocalDateTime since) {
        return repository.findOpenSince(since);
    }

    public Request updateStatus(Long id, RequestStatus newStatus) {
        Request request = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + id));
        request.setStatus(newStatus);
        return repository.save(request);
    }
}
