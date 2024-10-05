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

    private List<String> from;
    private List<String> on;
    private List<String> admins;

    @Size(max = 255)
    private String category;

    private String status;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
