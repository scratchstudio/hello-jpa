package io.iamkyu.proxy;

import io.iamkyu.PersistenceTestContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.LazyInitializationException;
import org.hibernate.proxy.HibernateProxy;
import org.junit.jupiter.api.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

public class ProxyTest extends PersistenceTestContext {

    private Member savedMember;

    @BeforeEach
    void setUp() {
        tx.begin();
        savedMember = new Member("member");
        em.persist(savedMember);
        em.flush();
        em.clear();
    }

    @AfterEach
    void tearDown() {
        tx.rollback();
    }

    @DisplayName("find 를 할때는 DB 에 쿼리한다")
    @Test
    void shouldQueryWhenFind() {
        //given when
        Member found = em.find(Member.class, savedMember.getId());

        //then
        assertThat(persistenceUnitUtil.isLoaded(found)).isTrue();
        assertThat(found).isInstanceOf(Member.class);
        assertThat(found).isNotInstanceOf(HibernateProxy.class);
    }

    @DisplayName("레퍼런스를 가져올때는 DB 에 쿼리하지 않고 프록시 객체를 일단 반환한다")
    @Test
    void shouldNoQueryWhenGetReference() {
        // given when
        Member reference = em.getReference(Member.class, savedMember.getId()); // 이 시점에 select 하지 않음.

        // then
        assertThat(persistenceUnitUtil.isLoaded(reference)).isFalse();
        assertThat(reference).isInstanceOf(HibernateProxy.class);
        assertThat(reference).isInstanceOf(Member.class); // Member 를 상속한 프록시 객체
        assertThat(reference).isNotExactlyInstanceOf(Member.class);
    }

    @DisplayName("레퍼런스를 가져올때는 DB 에 쿼리하지 않고 실제 사용할때 쿼리한다")
    @Test
    void shouldQueryWhenActualUsage() {
        // given when
        Member reference = em.getReference(Member.class, savedMember.getId()); // 이 시점에 select 하지 않음.

        // then
        assertThat(persistenceUnitUtil.isLoaded(reference)).isFalse();
        assertThat(reference).isInstanceOf(HibernateProxy.class);
        assertThat(reference).isNotExactlyInstanceOf(Member.class);

        // and when
        // reference.getId(); // 이때 쿼리 안함. GenerationType.IDENTITY 생성된 값이라 컨텍스트에서 이미 알고 있는 듯
        reference.getName(); // 이때 실제로 쿼리

        // then
        assertThat(persistenceUnitUtil.isLoaded(reference)).isTrue();

        // 실제 쿼리를 실행했다고 해서 reference 객체 자체가 Member 로 바뀌는건 아님.
        // 그대로 프록시 객체가 초기화 된 상태.
        assertThat(reference).isInstanceOf(HibernateProxy.class);
        assertThat(reference).isInstanceOf(Member.class); // Member 를 상속한 프록시 객체
        assertThat(reference).isNotExactlyInstanceOf(Member.class);
    }

    @DisplayName("JPA 는 같은 트랜잭션 안에서 find 또는 reference 로 가져온 같은 entity 의 == 연산을 보장한다")
    @Test
    void shouldJpaAlwaysEnsureSameEntitiesInTransaction1() {
        // given when
        Member found = em.find(Member.class, savedMember.getId());
        Member reference = em.getReference(Member.class, savedMember.getId());

        // then
        assertThat(persistenceUnitUtil.isLoaded(found)).isTrue();
        // 이전 테스트에서는 reference 의 값을 사용안했기 때문에 proxy 가 초기화 되지 않은 상태였음.
        assertThat(persistenceUnitUtil.isLoaded(reference)).isTrue();

        assertThat(found).isSameAs(reference);
        // 이전 테스트에서는 getReference() 호출시 proxy 객체 반환
        assertThat(reference).isNotInstanceOf(HibernateProxy.class);
    }

    @DisplayName("JPA 는 같은 트랜잭션 안에서 find 또는 reference 로 가져온 같은 entity 의 == 연산을 보장한다")
    @Test
    void shouldJpaAlwaysEnsureSameEntitiesInTransaction2() {
        // given when
        Member reference = em.getReference(Member.class, savedMember.getId());
        Member found = em.find(Member.class, savedMember.getId());

        // then
        assertThat(persistenceUnitUtil.isLoaded(found)).isTrue();
        assertThat(persistenceUnitUtil.isLoaded(reference)).isTrue();
        assertThat(reference).isSameAs(found);

        // 이전 테스트에서는 find() 호출시 proxy 가 아닌 실제 객체 반환
        assertThat(found).isInstanceOf(HibernateProxy.class);
    }

    @Test
    @DisplayName("영속성 컨텍스트 clear 후 프록시 객체를 참조하면 예외가 발생한다")
    void shouldThrownWhenReferenceProxyObjectAfterPersistenceContextClear() {
        //given
        Member reference = em.getReference(Member.class, savedMember.getId());
        em.clear();

        //when
        Throwable thrown = catchThrowable(reference::getName);

        //then
        assertThat(thrown).isInstanceOf(LazyInitializationException.class)
                .hasMessageContaining("no Session");
    }

    @DisplayName("영속성 컨텍스트 detach 된 프록시 객체를 참조하면 예외가 발생한다")
    @Test
    void shouldThrownWhenReferenceProxyObjectAfterPersistenceContextDetach() {
        //given
        Member reference = em.getReference(Member.class, savedMember.getId());
        em.detach(reference);

        //when
        Throwable thrown = catchThrowable(reference::getName);

        //then
        assertThat(thrown).isInstanceOf(LazyInitializationException.class)
                .hasMessageContaining("no Session");
    }

    @Disabled("이 테스트에서 em.close() 를 실행하면 다른 테스트가 영향 받음")
    @DisplayName("영속성 컨텍스트 close 후 프록시 객체를 참조하면 예외가 발생한다")
    @Test
    void shouldThrownWhenReferenceProxyObjectAfterPersistenceContextClose() {
        //given
        Member reference = em.getReference(Member.class, savedMember.getId());
        em.close();

        //when
        Throwable thrown = catchThrowable(reference::getName);

        //then
        assertThat(thrown).isInstanceOf(LazyInitializationException.class)
                .hasMessageContaining("Session was closed");
    }

    @Entity
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    private static class Member {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String name;

        public Member(String name) {
            this.name = name;
        }
    }
}
