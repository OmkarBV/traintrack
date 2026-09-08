package com.traintrack.coreapi.user;

import com.traintrack.coreapi.common.exception.NotFoundException;
import com.traintrack.coreapi.domain.User;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findByIdScoped(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
    }
}
