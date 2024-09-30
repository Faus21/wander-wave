package com.dama.wanderwave.report;

import com.dama.wanderwave.handler.*;
import com.dama.wanderwave.report.entity.PostReport;
import com.dama.wanderwave.report.entity.Report;
import com.dama.wanderwave.report.entity.ReportType;
import com.dama.wanderwave.report.request.ReportPageRequest;
import com.dama.wanderwave.report.request.ReviewReportRequest;
import com.dama.wanderwave.report.request.SendReportRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
    ALL_REPORTS("/api/reports/get"),
    USER_REPORTS("/api/reports/user/{userId}"),
    REVIEW_REPORT("/api/reports/review");


    private final String url;
}


@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @InjectMocks
    private ReportController reportController;
    @Mock
    private ReportService reportService;

    public record ErrorResponse(HttpStatus errorCode, String message) {
    }

    public record ResponseRecord(HttpStatus code, String message) {
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
        String mockRequestJson = mapToJson(getSendReportRequest());
        ResponseRecord response = new ResponseRecord(HttpStatus.OK, "User reported successfully");

        when(reportService.sendReport(any(com.dama.wanderwave.report.request.SendReportRequest.class))).thenReturn(response.message);

        mockMvc.perform(post(SEND_REPORT.getUrl())
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE)
                        .content(mockRequestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(response.message));
        verify(reportService, times(1)).sendReport(any(com.dama.wanderwave.report.request.SendReportRequest.class));
    }

    @Test
    void sendReportShouldThrowWhenForbidden() throws Exception {
        String mockRequestJson = mapToJson(getSendReportRequest());
        ErrorResponse response = new ErrorResponse(HttpStatus.FORBIDDEN, "Forbidden: Authenticated user is not authorized to perform this action");

        when(reportService.sendReport(any(com.dama.wanderwave.report.request.SendReportRequest.class))).thenThrow(new UnauthorizedActionException(response.message));

        mockMvc.perform(post(SEND_REPORT.getUrl())
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE)
                        .content(mockRequestJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(response.message));
        verify(reportService, times(1)).sendReport(any(com.dama.wanderwave.report.request.SendReportRequest.class));
    }

    @Test
    void sendReportShouldThrowWhenNotFound() throws Exception {
        String mockRequestJson = mapToJson(getSendReportRequest());
        ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND, "Request parameters not found");

        when(reportService.sendReport(any(com.dama.wanderwave.report.request.SendReportRequest.class))).thenThrow(new UserNotFoundException(response.message));

        mockMvc.perform(post(SEND_REPORT.getUrl())
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE)
                        .content(mockRequestJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(response.message));
        verify(reportService, times(1)).sendReport(any(com.dama.wanderwave.report.request.SendReportRequest.class));
    }

    @Test
    void sendReportShouldThrowWhenInternalServerError() throws Exception {
        String mockRequestJson = mapToJson(getSendReportRequest());
        ErrorResponse response = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

        when(reportService.sendReport(any(com.dama.wanderwave.report.request.SendReportRequest.class))).thenThrow(new RuntimeException(response.message));

        mockMvc.perform(post(SEND_REPORT.getUrl())
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE)
                        .content(mockRequestJson))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(response.message));
        verify(reportService, times(1)).sendReport(any(com.dama.wanderwave.report.request.SendReportRequest.class));
    }

    // get report types


    @Test
    void getReportTypesShouldBeOk() throws Exception {
        List<ReportType> mockReportTypes = List.of(
                ReportType.builder().name("test").build()
        );

        when(reportService.getReportTypes()).thenReturn(mockReportTypes);

        mockMvc.perform(get(REPORT_TYPES.getUrl())
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isArray())
                .andExpect(jsonPath("$.message[0].name").value("test"));

        verify(reportService, times(1)).getReportTypes();
    }

    @Test
    void getReportTypesShouldThrowWhenNotFound() throws Exception {
        ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND, "Report types not found");
        when(reportService.getReportTypes()).thenThrow(new ReportTypeNotFoundException(response.message));

        mockMvc.perform(get(REPORT_TYPES.getUrl())
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode.value()))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(reportService, times(1)).getReportTypes();
    }

    @Test
    void getReportTypesShouldThrowWhenInternalServerError() throws Exception {
        ErrorResponse response = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        when(reportService.getReportTypes()).thenThrow(new RuntimeException(response.message));

        mockMvc.perform(get(REPORT_TYPES.getUrl())
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode.value()))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(reportService, times(1)).getReportTypes();
    }

    // get all reports

    @Test
    void getAllReportsShouldBeOk() throws Exception {
        var request = mapToJson(getReportPageRequest());

        when(reportService.getAllReports(any(com.dama.wanderwave.report.request.ReportPageRequest.class))).thenReturn(
                getMockPage()
        );

        mockMvc.perform(get(ALL_REPORTS.getUrl())
                        .contentType(CONTENT_TYPE)
                        .content(request)
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message.content").isArray())
                .andExpect(jsonPath("$.message.content[0].id").value("abc"));

        verify(reportService, times(1)).getAllReports(any(com.dama.wanderwave.report.request.ReportPageRequest.class));
    }

    @Test
    void getReportsShouldThrowWhenNotFound() throws Exception {
        var request = mapToJson(getReportPageRequest());

        ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND, "Reports not found");

        when(reportService.getAllReports(any(com.dama.wanderwave.report.request.ReportPageRequest.class)))
                .thenThrow(new ReportNotFoundException(response.message));

        mockMvc.perform(get(ALL_REPORTS.getUrl())
                        .contentType(CONTENT_TYPE)
                        .content(request)
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode.value()))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(reportService, times(1)).getAllReports(any(com.dama.wanderwave.report.request.ReportPageRequest.class));
    }

    @Test
    void getReportsShouldThrowWhenInternalServerError() throws Exception {
        var request = mapToJson(getReportPageRequest());

        ErrorResponse response = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");


        when(reportService.getAllReports(any(com.dama.wanderwave.report.request.ReportPageRequest.class)))
                .thenThrow(new RuntimeException(response.message));

        mockMvc.perform(get(ALL_REPORTS.getUrl())
                        .contentType(CONTENT_TYPE)
                        .content(request)
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode.value()))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(reportService, times(1)).getAllReports(any(com.dama.wanderwave.report.request.ReportPageRequest.class));
    }


    // get user reports

    @Test
    void getUserReportsShouldBeOk() throws Exception {
        when(reportService.getUserReports(any(Pageable.class), any(String.class))).thenReturn(
                getMockPage()
        );

        mockMvc.perform(get(USER_REPORTS.getUrl(), "mockUserId")
                        .param("pageNumber", "0")
                        .param("pageSize", "2")
                        .contentType(CONTENT_TYPE)
                        .accept(ACCEPT_TYPE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message.content").isArray())
                .andExpect(jsonPath("$.message.content[0].id").value("abc"));

        verify(reportService, times(1)).getUserReports(any(Pageable.class), any(String.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "report"})
    void getUserReportsShouldThrowWhenNotFound(String ex) throws Exception {
        ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND, "User not found or no reports available");

        Exception exception = ex.equals("user") ? new UserNotFoundException(response.message) : ex.equals("report") ?
                new ReportNotFoundException(response.message) : null;

        when(reportService.getUserReports(any(Pageable.class), any(String.class))).thenThrow(exception);

        mockMvc.perform(get(USER_REPORTS.getUrl(), "mockUserId")
                        .accept(ACCEPT_TYPE)
                        .param("pageNumber", "0")
                        .param("pageSize", "2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode.value()))
                .andExpect(jsonPath("$.message").value(response.message));


        verify(reportService, times(1)).getUserReports(any(Pageable.class), any(String.class));
    }

    @Test
    void getUserReportsShouldThrowWhenInternalServerError() throws Exception {
        ErrorResponse response = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

        when(reportService.getUserReports(any(Pageable.class), any(String.class))).thenThrow(new RuntimeException(response.message));

        mockMvc.perform(get(USER_REPORTS.getUrl(), "mockUserId")
                        .accept(ACCEPT_TYPE)
                        .param("pageNumber", "0")
                        .param("pageSize", "2"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(response.message));

        verify(reportService, times(1)).getUserReports(any(Pageable.class), any(String.class));
    }

    @Test
    void getUserReportsShouldThrowNotAuthed() throws Exception {
        ErrorResponse response = new ErrorResponse(HttpStatus.FORBIDDEN, "Forbidden: Authenticated user is not authorized to perform this action");

        when(reportService.getUserReports(any(Pageable.class), any(String.class))).thenThrow(new UnauthorizedActionException(response.message));

        mockMvc.perform(get(USER_REPORTS.getUrl(), "mockUserId")
                        .accept(ACCEPT_TYPE).param("pageNumber", "0")
                        .param("pageSize", "2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode.value()))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(reportService, times(1)).getUserReports(any(Pageable.class), any(String.class));
    }
//
//    // review report
//
//    @Test
//    void reviewReportShouldBeOk() throws Exception {
//        ResponseRecord response = new ResponseRecord(200, "Report reviewed");
//
//        String mockJson = mapToJson(getReviewReportRequest());
//        when(reportService.reviewReport(any(ReviewReportRequest.class))).thenReturn(response.message);
//
//        mockMvc.perform(post(REVIEW_REPORT.getUrl())
//                        .contentType(CONTENT_TYPE)
//                        .accept(ACCEPT_TYPE)
//                        .content(mockJson))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(response.code))
//                .andExpect(jsonPath("$.message").value(response.message));
//
//        verify(reportService, times(1)).reviewReport(any(ReviewReportRequest.class));
//    }
//
//    @Test
//    void reviewReportShouldThrowWhenNotFound() throws Exception {
//        ErrorResponse response = new ErrorResponse(404, "Report not found");
//
//        String mockJson = mapToJson(getReviewReportRequest());
//        when(reportService.reviewReport(any(ReviewReportRequest.class))).thenThrow(new ReportTypeNotFoundException(response.message));
//
//        mockMvc.perform(post(REVIEW_REPORT.getUrl())
//                        .accept(ACCEPT_TYPE)
//                        .contentType(CONTENT_TYPE)
//                        .content(mockJson))
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
//                .andExpect(jsonPath("$.message").value(response.message));
//
//        verify(reportService, times(1)).reviewReport(any(ReviewReportRequest.class));
//    }
//
//    @Test
//    void reviewReportShouldThrowWhenInternalServerError() throws Exception {
//        ErrorResponse response = new ErrorResponse(500, "Internal server error");
//
//        String mockJson = mapToJson(getReviewReportRequest());
//        when(reportService.reviewReport(any(ReviewReportRequest.class))).thenThrow(new RuntimeException(response.message));
//
//        mockMvc.perform(post(REVIEW_REPORT.getUrl())
//                        .accept(ACCEPT_TYPE)
//                        .contentType(CONTENT_TYPE)
//                        .content(mockJson))
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
//                .andExpect(jsonPath("$.message").value(response.message));
//
//        verify(reportService, times(1)).reviewReport(any(ReviewReportRequest.class));
//    }

    private com.dama.wanderwave.report.request.SendReportRequest getSendReportRequest() {
        return SendReportRequest.builder()
                .description("test")
                .objectId("objectId")
                .userSenderId("sender")
                .userReportedId("reported")
                .reportType("type")
                .build();
    }

    private com.dama.wanderwave.report.request.ReviewReportRequest getReviewReportRequest() {
        return ReviewReportRequest.builder()
                .reportId("reportId")
                .comment("comment")
                .build();
    }

    private com.dama.wanderwave.report.request.ReportPageRequest getReportPageRequest() {
        return ReportPageRequest.builder()
                .pageSize(2)
                .pageNumber(0)
                .build();
    }

    private Page<Report> getMockPage() {
        List<Report> mockReports = List.of(
                PostReport.builder().id("abc").build(),
                PostReport.builder().id("qwe").build()
        );

        return new PageImpl<>(mockReports);
    }

    private String mapToJson(Object request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }
}
