package com.traintrack.coreapi.organisation;

import com.traintrack.coreapi.domain.Organisation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganisationRepository extends JpaRepository<Organisation, UUID> {
}
