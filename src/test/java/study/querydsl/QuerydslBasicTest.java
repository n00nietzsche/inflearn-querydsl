package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.UserDto;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback(value = false)
public class QuerydslBasicTest {
    private static QMember qMember = QMember.member;
    private static QTeam qTeam = QTeam.team;
    QMember qMemberSub = new QMember("memberSub");

    @Autowired EntityManager em;
    JPAQueryFactory queryFactory;
    @PersistenceUnit EntityManagerFactory emf;

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

    @Test
    @DisplayName("페치 조인 없을 때")
    public void withoutFetchJoin() {
        // 영속성 컨텍스트 캐시에 데이터가 남아있으면 select 문을 제대로 볼 수 없음
        em.flush();
        em.clear();

        // `LAZY` 로 세팅되어 있기 때문에, `team`을 안가져오면 자동으로 `member`만 조회한다.
        Member member = queryFactory
                .selectFrom(qMember)
                .join(qMember.team, qTeam)
                .where(qMember.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    @DisplayName("페치 조인 적용했을 때")
    public void withFetchJoin() {
        // 영속성 컨텍스트 캐시에 데이터가 남아있으면 select 문을 제대로 볼 수 없음
        em.flush();
        em.clear();

        // `LAZY` 로 세팅되어 있기 때문에, `team`을 안가져오면 자동으로 `member`만 조회한다.
        Member member = queryFactory
                .selectFrom(qMember)
                .join(qMember.team, qTeam).fetchJoin() // 끝에 `.fetchJoin()`만 추가해주면 된다.
                .where(qMember.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member.getTeam());
        // `fetch join` 사용 안하고,
        // 그냥 `join`으로 하면 `false`가 나온다.
        // 단, `team` 조회시에도 쿼리가 또 나간다.
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    @Test
    @DisplayName("나이가 가장 많은 회원을 조회")
    public void subQueryMaxAge() {

        // alias 가 중복되면 안되기 때문에,
        // 서브쿼리의 경우 `QMember`를 새로 만들어주어야 한다.
        QMember memberSub = new QMember("memberSub");

        List<Member> members = queryFactory
                .selectFrom(qMember)
                .where(qMember.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                )).fetch();

        // 결과가 1개고, 나이가 40이다.
        assertThat(members)
                .hasSize(1)
                .extracting("age")
                .containsExactly(40);
    }

    @Test
    @DisplayName("나이가 평균 이상인 회원을 조회")
    public void subQueryGOEAverageAge() {

        // alias 가 중복되면 안되기 때문에,
        // 서브쿼리의 경우 `QMember`를 새로 만들어주어야 한다.
        QMember memberSub = new QMember("memberSub");

        List<Member> members = queryFactory
                .selectFrom(qMember)
                .where(qMember.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                )).fetch();

        // 결과가 2개고, 나이가 30, 40이다.
        assertThat(members)
                .hasSize(2)
                .extracting("age")
                .containsExactly(30, 40);
    }

    @Test
    @DisplayName("In")
    public void subQueryIn() {

        // alias 가 중복되면 안되기 때문에,
        // 서브쿼리의 경우 `QMember`를 새로 만들어주어야 한다.
        List<Member> members = queryFactory
                .selectFrom(qMember)
                .where(qMember.age.in(
                        select(qMemberSub.age)
                                .from(qMemberSub)
                                .where(qMemberSub.age.gt(10))
                )).fetch();

        // 결과가 2개고, 나이가 30, 40이다.
        assertThat(members)
                .hasSize(3)
                .extracting("age")
                .containsExactly(20, 30, 40);
    }


    @Test
    public void selectSubQuery() {
        List<Tuple> tuples = queryFactory
                .select(qMember.username,
                        /*JPAExpressions*/select(qMemberSub.age.avg()).from(qMemberSub))
                .from(qMember)
                .fetch();

        for (Tuple tuple : tuples) {
            System.out.println("tuple = " + tuple);
        }
    }

    // from 절에 사용하는 서브 쿼리는 좋지 않은 경우가 많다.
    // from 절 내부에 from 절이 들어가고 그 내부에 from 절이 들어가는 경우도 있다.
    // 위와 같은 쿼리는 지양하고, DB는 순수 데이터를 가져오는 책임만 가지는 것이 좋다.
    // 예쁘게 formatting 하고 이런저런 연산을 하는 것은 DB 로직에서 하지 않는 것이 좋다.
    // `Layer` 를 잘 구분하여 설계하자.

    @Test
    @DisplayName("Select에 간단한 조건의 Case문 넣기")
    public void basicCase() {
        List<String> list = queryFactory
                .select(qMember.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(qMember)
                .fetch();

        System.out.println("list = " + list);
    }

    // DB에서 CASE문을 제공하긴 하지만, 어쩔 수 없는 경우가 아니면 사용하지 않는 것이 좋다.
    // 처리에 관련된 레이어가 꼬여버린다.
    // 추후에 깔끔하지 않은 앱이 완성될 확률이 높아진다.
    // 데이터를 가공하는 곳은 되도록이면 자바 애플리케이션 내부가 좋을 것이다.
    @Test
    @DisplayName("복잡한 조건의 Case문")
    public void complexCase() {
        List<String> list = queryFactory
                .select(new CaseBuilder()
                        .when(qMember.age.between(0, 20)).then("0~20살")
                        .when(qMember.age.between(21, 30)).then("21살~30살")
                        .otherwise("기타")) // otherwise가 있어야함
                .from(qMember)
                .fetch();

        System.out.println("list = " + list);
    }

    @Test
    @DisplayName("orderBy와 Case 같이 사용하기")
    public void orderByCase() {
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(qMember.age.between(0, 20)).then(2)
                .when(qMember.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> list = queryFactory
                .select(qMember.username,
                        qMember.age,
                        rankPath)
                .from(qMember)
                .orderBy(rankPath.desc())
                .fetch();

        System.out.println("list = " + list);
    }

    @Test
    @DisplayName("상수 더하기")
    public void constant() {
        List<Tuple> tuples = queryFactory
                .select(qMember.username, Expressions.constant("A"))
                .from(qMember)
                .fetch();

        // 결과를 보면 JPQL 쿼리는
        // select member1.username from Member member1
        // 위와 같이 상수를 넣어주는 부분이 없는데, 결과에서만 상수가 나온다.
        System.out.println("tuples = " + tuples);
    }

    @Test
    @DisplayName("문자 더하기")
    public void concat() {
        List<String> strings = queryFactory
                .select(qMember.username
                        .concat("_")
                        .concat(qMember.age.stringValue())) // 타입이 다르다. age는 숫자라서 `.stringValue()`가 필요
                .from(qMember)
                .fetch();

        System.out.println("strings = " + strings);
    }

    @Test
    @DisplayName("프로젝션 대상이 하나")
    public void projection1() {
        List<String> usernames = queryFactory
                .select(qMember.username)
                .from(qMember)
                .fetch();

        System.out.println("usernames = " + usernames);

        List<Integer> ages = queryFactory
                .select(qMember.age)
                .from(qMember)
                .fetch();

        System.out.println("ages = " + ages);
    }

    @Test
    @DisplayName("프로젝션 대상이 둘 이상")
    public void projectionMoreThan2() {
        // `Tuple`은 `com.querydsl.core` 패키지에 속해있다.
        // `Tuple`을 비즈니스 로직 등에 사용하는 것은 좋지 않다.
        // `QueryDSL`에 강하게 결합되어 있는 라이브러리를 사용하는 것은 설계상 좋지 않다.
        // `Repository`나 `DAO`와 같은 곳에서만 사용하는 것이 좋다.

        List<Tuple> tuples = queryFactory
                .select(qMember.username, qMember.age)
                .from(qMember)
                .fetch();

        for (Tuple tuple : tuples) {
            String username = tuple.get(qMember.username);
            Integer age = tuple.get(qMember.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    @DisplayName("JPQL을 이용해 DTO로 데이터를 가져오기")
    public void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery("select " +
                        "new study.querydsl.dto.MemberDto(m.username, m.age) " +
                        "from Member m", MemberDto.class)
                .getResultList();

        System.out.println("resultList = " + resultList);
    }

    @Test
    @DisplayName("QueryDSL로 DTO 반환하기 - Setter 이용")
    public void findDtoBySetter() {
        // 스프링에서 XML로 빈 만들 때처럼
        // 빈 생성자가 있어야 함
        List<MemberDto> memberDtos = queryFactory
                .select(Projections.bean
                        (MemberDto.class, qMember.username, qMember.age))
                .from(qMember)
                .fetch();

        System.out.println("memberDtos = " + memberDtos);
    }

    @Test
    @DisplayName("QueryDSL로 DTO 반환하기 - Field 이용")
    public void findDtoByField() {
        List<MemberDto> memberDtos = queryFactory
                .select(Projections.fields(MemberDto.class, qMember.username, qMember.age))
                .from(qMember)
                .fetch();

        System.out.println("memberDtos = " + memberDtos);
    }

    @Test
    @DisplayName("QueryDSL로 DTO 반환하기 - Constructor 이용")
    public void findDtoByConstructor() {
        List<MemberDto> memberDtos = queryFactory
                .select(Projections.constructor(MemberDto.class, qMember.username, qMember.age))
                .from(qMember)
                .fetch();

        System.out.println("memberDtos = " + memberDtos);
    }

    @Test
    @DisplayName("QueryDSL로 DTO 반환하기 - Field 이용 / UserDto")
    public void findDtoByFieldUserDto() {
        // 필드명이 안맞아도 `.fields()`를 사용한 뒤에
        // `.as()`를 추가하여 값을 넣어줄 수 있다.

        List<UserDto> userDtos = queryFactory
                .select(Projections.fields(
                        UserDto.class,
                        //qMember.username.as("name"),
                        ExpressionUtils.as(qMember.username, "name"),
                        ExpressionUtils.as(JPAExpressions.select(qMemberSub.age.max()).from(qMemberSub), "age")))
                .from(qMember)
                .fetch();

        System.out.println("userDtos = " + userDtos);
    }

    @Test
    @DisplayName("QueryDSL로 DTO 반환하기 - 생성자 이용 / UserDto")
    public void findDtoByConstructorUserDto() {
        List<UserDto> userDtos = queryFactory
                .select(Projections.constructor(
                        UserDto.class,
                        ExpressionUtils.as(qMember.username, "name"),
                        ExpressionUtils.as(JPAExpressions.select(qMemberSub.age.max()).from(qMemberSub), "age")))
                .from(qMember)
                .fetch();

        System.out.println("userDtos = " + userDtos);
    }


}