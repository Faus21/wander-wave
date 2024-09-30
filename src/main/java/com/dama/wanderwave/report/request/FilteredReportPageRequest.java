package com.dama.wanderwave.report.request;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilteredReportPageRequest {

    @Size(max = 255)
    private String from;
    @Size(max = 255)
    private String on;
    @Size(max = 255)
    private String admin;
    @Size(max = 255)
    private String category;
    Boolean isReviewed;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
