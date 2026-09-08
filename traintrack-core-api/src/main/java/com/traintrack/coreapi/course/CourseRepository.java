package com.traintrack.coreapi.course;

import com.traintrack.coreapi.domain.Course;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    /** See UserRepository.findByIdScoped for why this goes through JPQL rather than findById. */
    @Query("select c from Course c where c.id = :id")
    Optional<Course> findByIdScoped(@Param("id") UUID id);
}
