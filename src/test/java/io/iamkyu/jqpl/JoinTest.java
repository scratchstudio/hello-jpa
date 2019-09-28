package io.iamkyu.jqpl;

import io.iamkyu.PersistenceTestContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JoinTest extends PersistenceTestContext {
    private Team savedTeam;
    private Member savedMember1;
    private Member savedMember2;

    @BeforeEach
    void setUp() {
        tx.begin();
        savedTeam = new Team("team");
        em.persist(savedTeam);

        savedMember1 = new Member("member1", savedTeam);
        savedMember2 = new Member("member2", savedTeam);
        em.persist(savedMember1);
        em.persist(savedMember2);
        em.flush();
        em.clear();
    }

    @AfterEach
    void tearDown() {
        tx.rollback();
    }

    @DisplayName("조인쿼리를 실행하면 조인 대상 객체는 지연로딩한다")
    @Test
    void shouldLazyLoadingWhenJoinQuery() {
        //given when
        List<Member> members = em.createQuery("select m from JoinTest$Member m", Member.class)
                .getResultList();

        //then
        assertThat(members.size()).isEqualTo(2);
        assertThat(persistenceUnitUtil.isLoaded(members.get(0))).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(members.get(0).getTeam())).isFalse();

        for (Member member : members) {
            // 회원1 => 팀1. 쿼리 실행
            // 회원2 => 팀1. 1차캐시
            // 만약 팀이 서로 달랐다면 팀 숫자만큼 쿼리 실행 됨. N+1
            member.getTeam().getName();
        }
    }

    @DisplayName("일대다 조인을 실행하면서 데이터가 중복된다")
    @Test
    void shouldDuplicatedResultWhenJoin() {
        //given when
        List<Team> teams = em.createQuery("select t from JoinTest$Team t", Team.class)
                .getResultList();

        // 팀1에 회원1, 회원2 가 있는 상황. 이때 조인을 하면 팀1-회원1, 팀1-회원2 => 2개의 row 가 반환 됨.
        List<Team> teamsWithJoin = em.createQuery("select t from JoinTest$Team t join fetch t.members", Team.class)
                .getResultList();

        //then
        assertThat(teams.size()).isEqualTo(1);
        assertThat(teamsWithJoin.size()).isEqualTo(2);
    }

    @Entity
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    private static class Member {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String name;
        @ManyToOne(fetch = FetchType.LAZY)
        private Team team;

        public Member(String name, Team team) {
            this.name = name;
            this.team = team;
            team.addMember(this);
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
        @OneToMany
        private List<Member> members = new ArrayList<>();

        public Team(String name) {
            this.name = name;
        }

        public void addMember(Member member) {
            members.add(member);
        }
    }
}
