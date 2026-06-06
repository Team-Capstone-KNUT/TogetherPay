package com.devcrew.togetherpay.domain.schedule.dto.request;

import com.devcrew.togetherpay.domain.schedule.controller.command.UpdateScheduleItemCommand;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateScheduleItemRequest(
        @Size(max = 200, message = "일정 제목은 200자 이하여야 합니다.")
        String title,
        @Size(max = 10000, message = "일정 설명은 10000자 이하여야 합니다.")
        String description
) {

    public UpdateScheduleItemCommand toCommand() {
        return UpdateScheduleItemCommand.builder()
                .title(title)
                .description(description)
                .build();
    }
}
