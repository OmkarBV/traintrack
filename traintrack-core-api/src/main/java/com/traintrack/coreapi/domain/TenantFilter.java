package com.traintrack.coreapi.domain;

/**
 * Names the Hibernate filter (declared on the {@code domain} package, see
 * package-info.java) that scopes every tenant-owned entity to the current
 * organisation. Enabling it is not the entities' job — see
 * {@code com.traintrack.coreapi.security.TenantAwareJpaTransactionManager},
 * which turns it on for every transaction based on the authenticated
 * principal's org id, in exactly one place.
 */
public final class TenantFilter {

    public static final String NAME = "orgFilter";
    public static final String PARAM_ORG_ID = "orgId";

    private TenantFilter() {
    }
}
