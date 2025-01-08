package com.dama.wanderwave.report.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendReportRequest {

    @Size(max = 1024, message = "Description length must be less than or equal to 1024 characters")
    @NotEmpty(message = "Description is mandatory")
    private String description;

    @Size(max = 255, message = "Reported user ID length must be less than or equal to 255 characters")
    private String userReportedId;

    @Size(max = 255, message = "Report type length must be less than or equal to 255 characters")
    @NotEmpty(message = "Report type is mandatory")
    private String reportType;

    @NotNull(message = "Object type is mandatory")
    private ReportObjectType objectType;

    @Size(max = 8, message = "Object ID length must be less than or equal to 8 characters")
    @NotEmpty(message = "Object ID is mandatory")
    private String objectId;

}
