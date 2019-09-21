package io.iamkyu;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import javax.persistence.*;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class PersistenceTestContext {
    private static final String PERSISTENCE_UNIT_NAME = "test";

    protected static EntityManagerFactory emf;
    protected static EntityManager em;
    protected static EntityTransaction tx;
    protected static PersistenceUnitUtil persistenceUnitUtil;

    @BeforeAll
    static void beforeAll() {
        emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        em = emf.createEntityManager();
        tx = em.getTransaction();

        assertThat(emf).isNotNull();
        assertThat(em).isNotNull();
        assertThat(tx).isNotNull();

        persistenceUnitUtil = emf.getPersistenceUnitUtil();
    }

    @AfterAll
    static void afterAll() {
        em.clear();
        em.close();
        emf.close();
    }
}
