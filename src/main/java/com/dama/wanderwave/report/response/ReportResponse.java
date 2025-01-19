package com.dama.wanderwave.report.response;

import com.dama.wanderwave.user.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {

    private String id;
    private String description;
    private UserResponse sender;
    private UserResponse reported;
    private String reportType;
    private String reportStatus;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private UserResponse reviewedBy;
    private String reportComment;
    private String objectId;
    private String objectType;

}