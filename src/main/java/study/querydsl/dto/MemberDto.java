package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {
    private String username;
    private int age;

    @QueryProjection
    // 애노테이션을 적기만 하면 된다.
    // 적은 후에 `gradle` `task`에서 `compile querydsl`을 실행하자.
    // 그러면 q파일로 만들어준다.
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
