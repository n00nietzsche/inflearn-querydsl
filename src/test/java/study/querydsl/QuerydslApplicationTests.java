package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {
	// @PersistenceContext를 쓰면 프레임워크 무관
	@Autowired EntityManager em;

	@Test
	void contextLoads() {
		Hello hello = new Hello();
		em.persist(hello);

		JPAQueryFactory queryFactory = new JPAQueryFactory(em);
		// QHello qHello = new QHello("h");
		QHello qHello = QHello.hello;

		Hello result = queryFactory
				.selectFrom(qHello)
				.fetchOne();

		Long id = result.getId();
		System.out.println("id = " + id);

		// 아래 2줄은 같은 내용이다.
		// JPA는 기본으로 ID 값으로 동등성 비교
		assertThat(result).isEqualTo(hello);
		assertThat(result.getId()).isEqualTo(hello.getId());
	}
}
