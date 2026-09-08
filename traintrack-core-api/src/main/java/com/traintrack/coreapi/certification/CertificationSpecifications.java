package com.traintrack.coreapi.certification;

import com.traintrack.coreapi.domain.Certification;
import com.traintrack.coreapi.domain.CertificationStatus;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds each filter as its own Specification and returns null (Spring Data's
 * convention for "no predicate") when the value is absent — rather than a
 * single JPQL {@code :param is null or column = :param} clause, which hits a
 * real Postgres/pgjdbc limitation: the driver can't always infer a bind
 * parameter's type when it's reused across an IS NULL check and an equality
 * comparison ("could not determine data type of parameter"). This also
 * produces simpler SQL when a filter isn't supplied — no dead clause at all.
 */
final class CertificationSpecifications {

    private CertificationSpecifications() {
    }

    static Specification<Certification> hasStatus(CertificationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    static Specification<Certification> expiresBefore(Instant expiringBefore) {
        return (root, query, cb) -> expiringBefore == null ? null : cb.lessThan(root.get("expiresAt"), expiringBefore);
    }
}
