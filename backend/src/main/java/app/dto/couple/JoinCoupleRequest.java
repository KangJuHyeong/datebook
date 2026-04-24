package app.dto.couple;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinCoupleRequest(
        @NotBlank(message = "초대 코드를 입력해 주세요.")
        @Size(max = 32, message = "초대 코드를 다시 확인해 주세요.")
        String inviteCode
) {
}
