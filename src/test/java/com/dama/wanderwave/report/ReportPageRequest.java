package com.dama.wanderwave.report;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportPageRequest {

    @NotNull
    private Integer pageNumber;

    @Max(30)
    @Builder.Default
    private Integer pageSize = 10;

}
