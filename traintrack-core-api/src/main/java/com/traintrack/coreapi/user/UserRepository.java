package com.traintrack.coreapi.user;

import com.traintrack.coreapi.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Deliberately global (not org-scoped): login has no org selector, so a
     * user must be resolvable by email alone. Safe regardless of whether the
     * org filter happens to be enabled, since email is now unique platform-wide
     * (see V3 migration).
     */
    Optional<User> findByEmail(String email);

    /**
     * Explicit JPQL rather than the inherited {@code findById}: query
     * execution reliably applies the Hibernate {@code @Filter}, whereas
     * relying on {@code EntityManager.find()} semantics for that guarantee
     * is the kind of thing worth not leaving to chance on a tenant-isolation
     * boundary.
     */
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdScoped(@Param("id") UUID id);
}
