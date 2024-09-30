package com.dama.wanderwave.report.request;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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

    private List<String> admins;

    @Size(max = 255)
    private String category;
    Boolean isReviewed;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
