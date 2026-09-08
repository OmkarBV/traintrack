/**
 * Declares the org-scoping Hibernate filter once at the package level so
 * every tenant-owned entity only needs {@code @Filter(name = TenantFilter.NAME)}
 * rather than repeating the condition and parameter type.
 */
@FilterDef(
        name = TenantFilter.NAME,
        defaultCondition = "org_id = :" + TenantFilter.PARAM_ORG_ID,
        parameters = @ParamDef(name = TenantFilter.PARAM_ORG_ID, type = java.util.UUID.class))
package com.traintrack.coreapi.domain;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
