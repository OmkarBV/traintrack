package com.traintrack.coreapi.security;

import com.traintrack.coreapi.domain.TenantFilter;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Enables the org-scoping Hibernate filter for every transaction, based on
 * the authenticated principal, in exactly one place — rather than a
 * repository base layer where every query method has to remember to add
 * {@code WHERE org_id = ?} itself. A Hibernate {@code @Filter} is applied at
 * the ORM session level, so it's structurally impossible to forget on a new
 * query the way a hand-written WHERE clause is.
 *
 * <p>Unauthenticated transactions (e.g. login, which must look a user up by
 * email across all orgs before any org context exists) simply don't get the
 * filter enabled — {@link CurrentUser#currentOrgId()} returns empty and this
 * is a no-op.
 */
public class TenantAwareJpaTransactionManager extends JpaTransactionManager {

    public TenantAwareJpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        super.doBegin(transaction, definition);
        CurrentUser.currentOrgId().ifPresent(orgId -> {
            EntityManagerHolder holder =
                    (EntityManagerHolder) TransactionSynchronizationManager.getResource(getEntityManagerFactory());
            if (holder != null) {
                Session session = holder.getEntityManager().unwrap(Session.class);
                session.enableFilter(TenantFilter.NAME).setParameter(TenantFilter.PARAM_ORG_ID, orgId);
            }
        });
    }
}
