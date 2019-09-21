package io.iamkyu.proxy;

import io.iamkyu.PersistenceTestContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.persistence.*;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LazyLoadingTest extends PersistenceTestContext {
    private LazyMember savedLazyMember;
    private EagerMember savedEagerMember;
    private Team savedTeam;

    @BeforeEach
    void setUp() {
        tx.begin();

        savedTeam = new Team("my team");
        em.persist(savedTeam);

        savedLazyMember = new LazyMember("my member", savedTeam);
        em.persist(savedLazyMember);

        savedEagerMember = new EagerMember("my member", savedTeam);
        em.persist(savedEagerMember);

        em.flush();
        em.clear();
    }

    @AfterEach
    void tearDown() {
        tx.commit();
    }

    @DisplayName("연관관계가 Lazy Loading 인 경우 실제 사용전까지는 Load 되지 않는다")
    @Test
    void shouldNoQueryWhenGetReference() {
        //given when
        LazyMember found = em.find(LazyMember.class, savedLazyMember.getId());
        Team foundTeam = found.getTeam();

        //then
        assertThat(persistenceUnitUtil.isLoaded(found)).isTrue();

        // Lazy 로딩인 Team 은 실제 사용 시점에 쿼리
        assertThat(persistenceUnitUtil.isLoaded(foundTeam)).isFalse();
    }

    @DisplayName("연관관계가 Eager Loading 인 경우 실제 사용전에 Load 된다")
    @Test
    void shouldQueryWhenGetReference() {
        //given when
        EagerMember found = em.find(EagerMember.class, savedEagerMember.getId());
        Team foundTeam = found.getTeam();

        //then
        assertThat(persistenceUnitUtil.isLoaded(found)).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(foundTeam)).isTrue();
    }

    @DisplayName("Eager Loading 이면서 N+1 문제가 발생하는 경우")
    @Test
    void shouldOccurNPlusOneProblem() {
        List<EagerMember> eagerMembers = em.createQuery("select m from LazyLoadingTest$EagerMember m", EagerMember.class)
                .getResultList();

        // Member 쿼리
        // Team 쿼리
        // 각각 실행됨
    }

    @Entity
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    private static class LazyMember {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String name;

        @ManyToOne(fetch = FetchType.LAZY)
        private Team team;

        public LazyMember(String name, Team team) {
            this.name = name;
            this.team = team;
        }
    }

    @Entity
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    private static class EagerMember {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String name;

        // ManyToOne 은 기본이 Eager 이긴 함.
        @ManyToOne(fetch = FetchType.EAGER)
        private Team team;

        public EagerMember(String name, Team team) {
            this.name = name;
            this.team = team;
        }
    }

    @Entity
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    private static class Team {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String name;

        public Team(String name) {
            this.name = name;
        }
    }
}
