package com.dama.wanderwave.report.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReportRequest {

    @NotEmpty(message = "Report id is mandatory")
    private String reportId;

    @Size(max = 255, message = "Comment length should be less or equal 255")
    private String comment;

}
