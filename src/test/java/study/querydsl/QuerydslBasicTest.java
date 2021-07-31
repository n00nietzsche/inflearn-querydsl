package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.domain.Member;
import study.querydsl.domain.QMember;
import study.querydsl.domain.QTeam;
import study.querydsl.domain.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback(value = false)
public class QuerydslBasicTest {
    private static QMember qMember = QMember.member;
    private static QTeam qTeam = QTeam.team;

    @Autowired
    EntityManager em;
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
                .select(qMember)
                .from(qMember)
                // 파라미터 바인딩을 안해도, 파라미터 바인딩을 자동으로 한다.
                // 자동으로 `PreparedStatement`의 파라미터 바인딩 방식을 사용한다.
                // 그럼으로 인해 성능상에서도 이득이 있다.
                .where(qMember.username.eq("member1"))
                .fetchOne();

        System.out.println("member = " + member1);
        assertThat(this.member1.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("And Or 검색")
    public void search() {
        Member member1 = queryFactory
                .selectFrom(qMember)
                .where(qMember.username.eq("member1").and(qMember.age.eq(10)))
                .fetchOne();

        assertThat(member1.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("검색조건 활용 테스트")
    public void searchCondition() {
        // 쿼리에서의 조건을 표현 가능
       qMember.username.eq("member1"); // =
       qMember.username.ne("member1"); // !=
       qMember.username.eq("member1").not(); // !=

       qMember.username.isNotNull(); // isNotNull

       qMember.age.in(10, 20); // in
       qMember.age.notIn(10, 20); // not in
       qMember.age.between(10, 30); // between 10 to 30

       qMember.age.goe(30); // greater or equal (age >= 30)
       qMember.age.gt(30); // greater (age > 30)
       qMember.age.loe(30); // less or equal (age <= 30)
       qMember.age.lt(30); // less than (age < 30)

       qMember.username.like("member%"); // like
       qMember.username.contains("member"); // like '%member%'
       qMember.username.startsWith("member"); // like 'member%'
    }

    @Test
    @DisplayName("like 테스트")
    public void like() {
        List<Member> members = queryFactory.selectFrom(qMember)
                .where(
                        qMember.username.like("%ember%"),
                        qMember.age.goe(30)
                )
                .fetch();

        for (Member foundMember : members) {
            System.out.println("foundMember = " + foundMember);
        }
    }

    @Test
    @DisplayName("결과를 가져오는 종류 ")
    public void resultFetchTest() {
        // 일반 리스트
        List<Member> fetch = queryFactory.selectFrom(qMember).fetch();

        // 결과가 1개일 때 -> 없으면 null, 1개 이상이면 `com.querydsl.core.NonUniqueResultException
        Member fetchOne = queryFactory.selectFrom(qMember).fetchOne();

        // `limit(1).fetchOne()`과 동일
        Member fetchFirst = queryFactory.selectFrom(qMember).fetchFirst();

        // 페이징 정보 포함한 결과 반환
        // count 를 알기 위해 쿼리가 1번 나가고, 전체 데이터를 조회하기 위해 1번 나감 -> 총 2번의 쿼리 발생
        // 성능 때문에 간혹 `total count`를 다른 방식으로 가져와야 할 때가 있는데, 그 때는 쿼리 두번을 따로 날려야 한다.
        QueryResults<Member> fetchResults = queryFactory.selectFrom(qMember).fetchResults();

        // 카운트만 조회
        long fetchCount = queryFactory.selectFrom(qMember).fetchCount();
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순 (desc)
     * 2. 회원 나이 오름차순 (asc)
     * 단, 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @Test
    @DisplayName("정렬 순서 테스트")
    public void orderBy() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> members = queryFactory.selectFrom(qMember)
                .where(qMember.age.eq(100))
                // null이 먼저 오도록 `nullsFirst`도 있다.
                .orderBy(qMember.age.desc(), qMember.username.asc().nullsLast())
                .fetch();

        Member member5 = members.get(0);
        Member member6 = members.get(1);
        Member memberNull = members.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    @DisplayName("페이징 테스트 1")
    public void paging1() {
        List<Member> members = queryFactory
                .selectFrom(qMember)
                .orderBy(qMember.username.desc())
                .offset(1) // 0부터 시작이어서 1이면 1개를 스킵한다는 뜻
                .limit(2)
                .fetch();

        assertThat(members.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("페이징 테스트 2")
    public void paging2() {
        // 단, `count` 쿼리는 성능 개선의 여지가 많아서 따로 작성하는 경우가 많다.

        QueryResults<Member> memberQueryResults = queryFactory
                .selectFrom(qMember)
                .orderBy(qMember.username.desc())
                .offset(1) // 0부터 시작이어서 1이면 1개를 스킵한다는 뜻
                .limit(2)
                .fetchResults();

        assertThat(memberQueryResults.getTotal()).isEqualTo(4);
        assertThat(memberQueryResults.getLimit()).isEqualTo(2);
        assertThat(memberQueryResults.getOffset()).isEqualTo(1);
        assertThat(memberQueryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation() {
        List<Tuple> tuples = queryFactory
                .select(
                        qMember.count(),
                        qMember.age.sum(),
                        qMember.age.avg(),
                        qMember.age.max(),
                        qMember.age.min()
                )
                .from(qMember)
                .fetch();

        Tuple tuple = tuples.get(0);
        assertThat(tuple.get(qMember.count())).isEqualTo(4);
        assertThat(tuple.get(qMember.age.sum())).isEqualTo(100);
        assertThat(tuple.get(qMember.age.avg())).isEqualTo(25);
        assertThat(tuple.get(qMember.age.max())).isEqualTo(40);
        assertThat(tuple.get(qMember.age.min())).isEqualTo(10);

        // 실무에서는 튜플을 사용하기보다는 DTO로 직접 뽑아오는 방식을 많이 쓴다.
    }

    @Test
    @DisplayName("팀 이름과 각팀의 평균 연령 구하기")
    public void group() throws Exception {
        List<Tuple> tuples = queryFactory
                .select(
                        qTeam.name,
                        qMember.age.avg()
                )
                .from(qMember)
                .join(qMember.team, qTeam)
                .groupBy(qTeam.name)
                .fetch();

        Tuple teamA = tuples.get(0);
        Tuple teamB = tuples.get(1);

        assertThat(teamA.get(qTeam.name)).isEqualTo("teamA");
        assertThat(teamA.get(qMember.age.avg())).isEqualTo(15);

        assertThat(teamB.get(qTeam.name)).isEqualTo("teamB");
        assertThat(teamB.get(qMember.age.avg())).isEqualTo(35);
    }

    @Test
    @DisplayName("(멤버 관점) 팀 멤버 평균연령이 20살 이상인 팀 이름 구하기")
    public void having() throws Exception {
        List<Team> teams = queryFactory
                .select(qTeam)
                .from(qMember)
                .join(qMember.team, qTeam)
                .groupBy(qTeam.name)
                .having(qMember.age.avg().goe(20))
                .fetch();

        for (Team team : teams) {
            System.out.println("team = " + team);
        }
    }

    @Test
    @DisplayName("(팀 관점) 팀 멤버 평균연령이 20살 이상인 팀 이름 구하기")
    public void having2() throws Exception {
        List<Team> teams = queryFactory
                .select(qTeam)
                .from(qTeam)
                .join(qTeam.members, qMember)
                .groupBy(qTeam.name)
                .having(qMember.age.avg().goe(20))
                .fetch();

        for (Team team : teams) {
            System.out.println("team = " + team);
        }
    }

    @Test
    @DisplayName("팀 A에 소속된 모든 회원을 찾아라")
    public void join() {
        List<Member> teamAMembers = queryFactory
                .selectFrom(qMember)
                .join(qMember.team, qTeam) // left, right join 전부 가능하다.
                .where(qTeam.name.eq("teamA"))
                .fetch();

        assertThat(teamAMembers)
                .extracting("username")
                .containsExactly("member1", "member2");

        // 실행시켜보면 결국 객체의 참조가 나오는 JPQL 쿼리가 나간다.
        // QueryDSL은 결국 JPQL 쿼리 빌더의 역할이다.
    }

    @Test
    @DisplayName("세타 조인 (연관관계가 없는 조인), 회원 이름이 팀 이름과 같은 회원을 조회해보자.")
    public void thetaJoin() {
        // 세타 조인은 `from`에 두개의 테이블 박아놓고,
        // `where`에서 조인시키는 것
        em.persist(new Member("teamA", 10));
        em.persist(new Member("teamB", 20));
        em.persist(new Member("teamC", 30));

        List<Member> members = queryFactory
                .select(qMember)
                .from(qMember, qTeam)
                .where(qMember.username.eq(qTeam.name))
                .fetch();

        assertThat(members)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    @Test
    @DisplayName("회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회")
    public void joinOnFiltering() throws Exception {
        /*
        * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
        * */

        List<Tuple> tuples = queryFactory.select(qMember, qTeam)
                .from(qMember)
                .leftJoin(qMember.team, qTeam)
                // `inner join`의 경우, 굳이 `on`만 쓸 필요 없이 `where`로 해도 깔끔하다.
                .on(qTeam.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : tuples) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    @DisplayName("연관관계가 없는 엔티티를 외부 조인, 회원의 이름이 팀 이름과 같은 대상 외부 조인")
    public void joinOnNoRelation() {
        // 세타 조인은 `from`에 두개의 테이블 박아놓고,
        // `where`에서 조인시키는 것
        em.persist(new Member("teamA", 10));
        em.persist(new Member("teamB", 20));
        em.persist(new Member("teamC", 30));

        List<Tuple> tuples = queryFactory
                .select(qMember, qTeam)
                .from(qMember)
                // 세타조인에서는 문법이 다름.
                // 엔티티의 어떤 필드를 조인할 것이냐가 아니라, 그냥 조인할 엔티티 이름을 바로 박아버림.
                // 그냥 엔티티 연관관계 필드 대신 조인할 엔티티 이름을 박으면, 외래키 id로 조인 안함. on에 있는 조건만 사용.
                // `.on` 은 `join` 할 데이터를 필터링할 때 쓰임.
                .leftJoin(qTeam)
                .on(qMember.username.eq(qTeam.name))
                .fetch();

        for (Tuple tuple : tuples) {
            System.out.println("tuple = " + tuple);
        }
    }
}