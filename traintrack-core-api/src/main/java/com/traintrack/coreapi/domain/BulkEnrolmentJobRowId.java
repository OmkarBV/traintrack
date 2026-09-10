package com.traintrack.coreapi.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
public record BulkEnrolmentJobRowId(UUID jobId, Integer rowNumber) implements Serializable {
}
