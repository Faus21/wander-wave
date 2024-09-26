package com.dama.wanderwave.report;

import jakarta.validation.constraints.NotEmpty;
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

    @Size(max = 255, message = "Sender user ID length must be less than or equal to 255 characters")
    @NotEmpty(message = "Sender user ID is mandatory")
    private String userSenderId;

    @Size(max = 255, message = "Reported user ID length must be less than or equal to 255 characters")
    @NotEmpty(message = "Reported user ID is mandatory")
    private String userReportedId;

    @Size(max = 255, message = "Report type length must be less than or equal to 255 characters")
    @NotEmpty(message = "Report type is mandatory")
    private String reportType;

    @Size(max = 8, message = "Object ID length must be less than or equal to 8 characters")
    @NotEmpty(message = "Object ID is mandatory")
    private String objectId;

}
