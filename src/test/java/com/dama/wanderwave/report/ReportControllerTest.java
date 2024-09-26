package com.dama.wanderwave.report;

import com.dama.wanderwave.handler.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static com.dama.wanderwave.report.ApiUrls.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Getter
@RequiredArgsConstructor
enum ApiUrls {
    SEND_REPORT("/api/reports/send"),
    REPORT_TYPES("/api/reports/types"),
    REPORTS_BY_DATES("/api/reports/find-by-dates"),
    USER_REPORTS("/api/reports/get/{userId}"),
    REVIEW_REPORT("/api/reports/review");


    private final String url;
}


@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @InjectMocks
    private ReportController reportController;
    @Mock
    private ReportService reportService;

    public record ErrorResponse(int errorCode, String message) {
    }

    public record ResponseRecord(int code, String message) {
    }

    private static final String CONTENT_TYPE = MediaType.APPLICATION_JSON_VALUE;
    private static final MediaType ACCEPT_TYPE = MediaType.APPLICATION_JSON;


    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reportController).setControllerAdvice(new GlobalExceptionHandler()).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // send report

    @Test
    void sendReportShouldBeOk() throws Exception {
        String mockRequestJson = mapToJson(getReportRequest());
        ResponseRecord response = new ResponseRecord(200, "User reported successfully");

        when(reportService.sendReport(any(SendReportRequest.class))).thenReturn(response.message);

        mockMvc.perform(post(SEND_REPORT.getUrl())
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE)
                        .content(mockRequestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(response.code))
                .andExpect(jsonPath("$.message").value(response.message));
        verify(reportService, times(1)).sendReport(any(SendReportRequest.class));
    }

    @Test
    void sendReportShouldThrowWhenForbidden() throws Exception {
        String mockRequestJson = mapToJson(getReportRequest());
        ErrorResponse response = new ErrorResponse(403, "Forbidden: Authenticated user is not authorized to perform this action");

        when(reportService.sendReport(any(SendReportRequest.class))).thenThrow(new UnauthorizedActionException(response.message));

        mockMvc.perform(post(SEND_REPORT.getUrl())
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE)
                        .content(mockRequestJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                .andExpect(jsonPath("$.message").value(response.message));
        verify(reportService, times(1)).sendReport(any(SendReportRequest.class));
    }

    @Test
    void sendReportShouldThrowWhenNotFound() throws Exception {
        String mockRequestJson = mapToJson(getReportRequest());
        ErrorResponse response = new ErrorResponse(404, "Request parameters not found");

        when(reportService.sendReport(any(SendReportRequest.class))).thenThrow(new UserNotFoundException(response.message));

        mockMvc.perform(post(SEND_REPORT.getUrl())
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE)
                        .content(mockRequestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                .andExpect(jsonPath("$.message").value(response.message));
        verify(reportService, times(1)).sendReport(any(SendReportRequest.class));
    }

    @Test
    void sendReportShouldThrowWhenInternalServerError() throws Exception {
        String mockRequestJson = mapToJson(getReportRequest());
        ErrorResponse response = new ErrorResponse(500, "Internal server error");

        when(reportService.sendReport(any(SendReportRequest.class))).thenThrow(new RuntimeException(response.message));

        mockMvc.perform(post(SEND_REPORT.getUrl())
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE)
                        .content(mockRequestJson))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                .andExpect(jsonPath("$.message").value(response.message));
        verify(reportService, times(1)).sendReport(any(SendReportRequest.class));
    }

    // get report types


    @Test
    void getReportTypesShouldBeOk() throws Exception {
        ResponseRecord response = new ResponseRecord(200, "All report types retrieved successfully");

        List<ReportType> mockReportTypes = List.of(
                ReportType.builder().name("test").build()
        );

        when(reportService.getReportTypes()).thenReturn(mockReportTypes);

        mockMvc.perform(get(REPORT_TYPES.getUrl())
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(response.code))
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message[0].name").value("test"));

        verify(reportService, times(1)).getReportTypes();
    }

    @Test
    void getReportTypesShouldThrowWhenNotFound() throws Exception {
        ErrorResponse response = new ErrorResponse(404, "Report types not found");
        when(reportService.getReportTypes()).thenThrow(new ReportTypeNotFoundException(response.message));

        mockMvc.perform(get(REPORT_TYPES.getUrl())
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(reportService, times(1)).getReportTypes();
    }

    @Test
    void getReportTypesShouldThrowWhenInternalServerError() throws Exception {
        ErrorResponse response = new ErrorResponse(500, "Internal server error");
        when(reportService.getReportTypes()).thenThrow(new RuntimeException(response.message));

        mockMvc.perform(get(REPORT_TYPES.getUrl())
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(reportService, times(1)).getReportTypes();
    }

    // find reports by dates

    @Test
    void getReportsByDatesShouldBeOk() throws Exception {
        ResponseRecord response = new ResponseRecord(200, "Reports retrieved successfully");

        var request = mapToJson(getReportsByDatesRequest());

        when(reportService.getReportsByDate(any(ReportsByDatesRequest.class))).thenReturn(
                List.of(Report.builder().id("mockId").build())
        );

        mockMvc.perform(get(REPORTS_BY_DATES.getUrl())
                        .contentType(CONTENT_TYPE)
                        .content(request)
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(response.code))
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message[0].id").value("mockId"));

        verify(reportService, times(1)).getReportsByDate(any(ReportsByDatesRequest.class));
    }

    @Test
    void getReportsByDatesShouldThrowNotFoundWithWrongDates() throws Exception {
        var request = mapToJson(getReportsByDatesRequest());

        ErrorResponse response = new ErrorResponse(404, "Reports not found for the specified date range");
        when(reportService.getReportsByDate(any(ReportsByDatesRequest.class))).thenThrow(new ReportNotFoundException(response.message));

        mockMvc.perform(get(REPORTS_BY_DATES.getUrl())
                        .contentType(CONTENT_TYPE)
                        .content(request)
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(reportService, times(1)).getReportsByDate(any(ReportsByDatesRequest.class));
    }

    @Test
    void getReportsByDatesShouldThrowWhenInternalServerError() throws Exception {
        var request = mapToJson(getReportsByDatesRequest());

        ErrorResponse response = new ErrorResponse(500, "Internal server error");
        when(reportService.getReportsByDate(any(ReportsByDatesRequest.class))).thenThrow(new RuntimeException(response.message));

        mockMvc.perform(get(REPORTS_BY_DATES.getUrl())
                        .contentType(CONTENT_TYPE)
                        .content(request)
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(reportService, times(1)).getReportsByDate(any(ReportsByDatesRequest.class));
    }

    // get user reports

    @Test
    void getUserReportsShouldBeOk() throws Exception {
        ResponseRecord response = new ResponseRecord(200, "All reports retrieved successfully");

        when(reportService.getUserReports(any(String.class))).thenReturn(
                List.of(Report.builder().id("mockId").build())
        );

        mockMvc.perform(get(USER_REPORTS.getUrl(), "mockUserId")
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(response.code))
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message[0].id").value("mockId"));

        verify(reportService, times(1)).getUserReports(any(String.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "report"})
    void getUserReportsShouldThrowWhenNotFound(String ex) throws Exception {
        ErrorResponse response = new ErrorResponse(404, "User not found or no reports available");

        Exception exception = ex.equals("user") ? new UserNotFoundException(response.message) : ex.equals("report") ?
                new ReportNotFoundException(response.message) : null;

        when(reportService.getUserReports(any(String.class))).thenThrow(exception);

        mockMvc.perform(get(USER_REPORTS.getUrl(), "mockUserId")
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(reportService, times(1)).getUserReports(any(String.class));
    }

    @Test
    void getUserReportsShouldThrowWhenInternalServerError() throws Exception {
        ErrorResponse response = new ErrorResponse(500, "Internal server error");

        when(reportService.getUserReports(any(String.class))).thenThrow(new RuntimeException(response.message));

        mockMvc.perform(get(USER_REPORTS.getUrl(), "mockUserId")
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(reportService, times(1)).getUserReports(any(String.class));
    }

    @Test
    void getUserReportsShouldThrowNotAuthed() throws Exception {
        ErrorResponse response = new ErrorResponse(403, "Forbidden: Authenticated user is not authorized to perform this action");

        when(reportService.getUserReports(any(String.class))).thenThrow(new UnauthorizedActionException(response.message));

        mockMvc.perform(get(USER_REPORTS.getUrl(), "mockUserId")
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(reportService, times(1)).getUserReports(any(String.class));
    }

    /*
    @PostMapping("/review")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Review a report", description = "Review a specific report by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report reviewed successfully"),
            @ApiResponse(responseCode = "404", description = "Report not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResponseRecord> reviewReport(@RequestBody @Valid ReviewReportRequest request) {
        var message = service.reviewReport(request);
        return ResponseEntity.ok().body(new ResponseRecord(200, message));
    }
     */

    // review report

    @Test
    void reviewReportShouldBeOk() throws Exception {
        ResponseRecord response = new ResponseRecord(200, "Report reviewed");

        String mockJson = mapToJson(getReviewReportRequest());
        when(reportService.reviewReport(any(ReviewReportRequest.class))).thenReturn(response.message);

        mockMvc.perform(post(REVIEW_REPORT.getUrl())
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE)
                        .content(mockJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(response.code))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(reportService, times(1)).reviewReport(any(ReviewReportRequest.class));
    }

    @Test
    void reviewReportShouldThrowWhenNotFound() throws Exception {
        ErrorResponse response = new ErrorResponse(404, "Report not found");

        String mockJson = mapToJson(getReviewReportRequest());
        when(reportService.reviewReport(any(ReviewReportRequest.class))).thenThrow(new ReportTypeNotFoundException(response.message));

        mockMvc.perform(post(REVIEW_REPORT.getUrl())
                        .accept(ACCEPT_TYPE)
                        .contentType(CONTENT_TYPE)
                        .content(mockJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(reportService, times(1)).reviewReport(any(ReviewReportRequest.class));
    }

    @Test
    void reviewReportShouldThrowWhenInternalServerError() throws Exception {
        ErrorResponse response = new ErrorResponse(500, "Internal server error");

        String mockJson = mapToJson(getReviewReportRequest());
        when(reportService.reviewReport(any(ReviewReportRequest.class))).thenThrow(new RuntimeException(response.message));

        mockMvc.perform(post(REVIEW_REPORT.getUrl())
                        .accept(ACCEPT_TYPE)
                        .contentType(CONTENT_TYPE)
                        .content(mockJson))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(reportService, times(1)).reviewReport(any(ReviewReportRequest.class));
    }

    private SendReportRequest getReportRequest() {
        return SendReportRequest.builder()
                .description("test")
                .objectId("objectId")
                .userSenderId("sender")
                .userReportedId("reported")
                .reportType("type")
                .build();
    }

    private ReviewReportRequest getReviewReportRequest() {
        return ReviewReportRequest.builder()
                .reportId("reportId")
                .comment("comment")
                .build();
    }

    private ReportsByDatesRequest getReportsByDatesRequest() {
        return ReportsByDatesRequest.builder()
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .build();
    }

    private String mapToJson(Object request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }
}
