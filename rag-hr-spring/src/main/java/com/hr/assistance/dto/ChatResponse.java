package com.hr.assistance.dto;

import java.util.List;

public record ChatResponse(
        String answer,
        List<SourceResponse> sources
) {
}
