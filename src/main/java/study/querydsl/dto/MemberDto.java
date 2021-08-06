package study.querydsl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto {
    /**
     * 무언가 최적화해서 가져오고 싶거나
     * 컨트롤러를 통해 엔티티 데이터를 내려야 할 때 매개체로 `DTO`를 쓴다.
     * 이 경우에는 `Member`에 있는 정보 중
     * `username`과 `age`만 가져오고 싶은 경우라고 가정해보자.
     */

    private String username;
    private int age;
}
