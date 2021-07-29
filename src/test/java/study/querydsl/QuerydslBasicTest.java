package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.domain.Member;
import study.querydsl.domain.QMember;
import study.querydsl.domain.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.domain.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @Autowired EntityManager em;
    JPAQueryFactory queryFactory;

    Team teamA;
    Team teamB;

    Member member1;
    Member member2;
    Member member3;
    Member member4;

    @BeforeEach
    public void makeFixture() {
        // 여러 스레드에서 JPAQueryFactory 를 만들어도,
        // 멀티스레딩 문제가 일어나지 않는다.
        // `EntityManager` 트랜잭션에서 알아서 잘 처리해주기 때문에.
        queryFactory = new JPAQueryFactory(em);

        teamA = new Team("teamA");
        teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        member1 = new Member("member1", 10, teamA);
        member2 = new Member("member2", 20, teamA);
        member3 = new Member("member3", 30, teamB);
        member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("member1 찾기 - JPQL")
    public void findMember1WithJpql() {
        Member member = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        System.out.println("member = " + member);
        assertThat(member1.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("member1 찾기 - QueryDSL")
    public void findMember1WithQuerydsl() {
        // 항상 `JPAQueryFactory`로 시작한다.
        // 생성자에 매개변수로 `EntityManager`를 넘겨주어야 한다.
        // JPAQueryFactory queryFactory = new JPAQueryFactory(em); // 필드로 이동

        // 도메인에 엔티티 만든 후에는 항상 `compileQuerydsl` 태스크를 돌려주자.

        // "m"은 어떤 QMember 인지 구분하는 이름을 주는 것이다
        // 이번에는 예제라 정석으로 생성해보고 나중에는 `QMember.member`를 쓸 것이다.
        // QMember m = new QMember("m");

        // JPQL 에서는 오타가 나면, 컴파일까지 정상적으로 실행되고 런타임에 에러가 나는 반면,
        // QueryDSL 에서는 오타가 나면, 컴파일 단계에서 벌써 에러가 난다.
        // 코드 어시스턴트도 어마어마해서 SQL 문법이 정확히 기억이 안 나도 생성할 수 있다.
        Member member1 = queryFactory
                .select(member)
                .from(member)
                // 파라미터 바인딩을 안해도, 파라미터 바인딩을 자동으로 한다.
                // 자동으로 `PreparedStatement`의 파라미터 바인딩 방식을 사용한다.
                // 그럼으로 인해 성능상에서도 이득이 있다.
                .where(member.username.eq("member1"))
                .fetchOne();

        System.out.println("member = " + member1);
        assertThat(this.member1.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("And Or 검색")
    public void search() {
        Member member1 = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        assertThat(member1.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("검색조건 활용 테스트")
    public void searchCondition() {
        // 쿼리에서의 조건을 표현 가능
        member.username.eq("member1"); // =
        member.username.ne("member1"); // !=
        member.username.eq("member1").not(); // !=

        member.username.isNotNull(); // isNotNull

        member.age.in(10, 20); // in
        member.age.notIn(10, 20); // not in
        member.age.between(10, 30); // between 10 to 30

        member.age.goe(30); // greater or equal (age >= 30)
        member.age.gt(30); // greater (age > 30)
        member.age.loe(30); // less or equal (age <= 30)
        member.age.lt(30); // less than (age < 30)

        member.username.like("member%"); // like
        member.username.contains("member"); // like '%member%'
        member.username.startsWith("member"); // like 'member%'
    }

    @Test
    @DisplayName("like 테스트")
    public void like() {
        List<Member> members = queryFactory.selectFrom(member)
                .where(
                        member.username.like("%ember%"),
                        member.age.goe(30)
                )
                .fetch();

        for (Member foundMember : members) {
            System.out.println("foundMember = " + foundMember);
        }
    }
}
