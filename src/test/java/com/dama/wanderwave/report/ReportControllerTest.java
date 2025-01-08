package com.dama.wanderwave.report;

import com.dama.wanderwave.handler.*;
import com.dama.wanderwave.handler.report.ReportNotFoundException;
import com.dama.wanderwave.handler.report.ReportTypeNotFoundException;
import com.dama.wanderwave.handler.user.UnauthorizedActionException;
import com.dama.wanderwave.handler.user.UserNotFoundException;
import com.dama.wanderwave.report.general.UserReport;
import com.dama.wanderwave.report.request.ReportObjectType;
import com.dama.wanderwave.report.request.ReviewReportRequest;
import com.dama.wanderwave.report.request.SendReportRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
    GET_REPORT("/api/reports/get/{reportId}"),
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

    public record ErrorResponse(int errorCode, String message) { }

    public record ResponseRecord(int code, String message) { }

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

    @Nested
    class SendReportTest {
        @Test
        @DisplayName("Send report should return OK (200)")
        void sendReport_Success() throws Exception {
            String mockRequestJson = mapToJson(getSendReportRequest());
            ResponseRecord response = new ResponseRecord(HttpStatus.OK.value(), "User reported successfully");

            when(reportService.sendReport(any(SendReportRequest.class))).thenReturn(response.message);

            mockMvc.perform(post(SEND_REPORT.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mockRequestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(response.message));
            verify(reportService).sendReport(any(SendReportRequest.class));
        }

        @Test
        @DisplayName("Send report should return Forbidden (403)")
        void sendReport_Forbidden() throws Exception {
            String mockRequestJson = mapToJson(getSendReportRequest());
            ErrorResponse response = new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Forbidden: Authenticated user is not authorized to perform this action");

            when(reportService.sendReport(any(SendReportRequest.class))).thenThrow(new UnauthorizedActionException(response.message));

            mockMvc.perform(post(SEND_REPORT.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mockRequestJson))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(response.message));
            verify(reportService).sendReport(any(SendReportRequest.class));
        }

        @Test
        @DisplayName("Send report should return Not Found (404)")
        void sendReport_NotFound() throws Exception {
            String mockRequestJson = mapToJson(getSendReportRequest());
            ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Request parameters not found");

            when(reportService.sendReport(any(SendReportRequest.class))).thenThrow(new UserNotFoundException(response.message));

            mockMvc.perform(post(SEND_REPORT.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mockRequestJson))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(response.message));
            verify(reportService).sendReport(any(SendReportRequest.class));
        }

        @Test
        @DisplayName("Send report should return Internal Server Error (500)")
        void sendReport_InternalServerError() throws Exception {
            String mockRequestJson = mapToJson(getSendReportRequest());
            ErrorResponse response = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");

            when(reportService.sendReport(any(SendReportRequest.class))).thenThrow(new RuntimeException(response.message));

            mockMvc.perform(post(SEND_REPORT.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mockRequestJson))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value(response.message));
            verify(reportService).sendReport(any(com.dama.wanderwave.report.request.SendReportRequest.class));
        }
    }

    @Nested
    class GetAllReportsTest {

        @Test
        @DisplayName("Get all reports should return OK (200)")
        void getAllReports_Success() throws Exception {
            when(reportService.getAllReports(any(Pageable.class), any())).thenReturn(
                    getMockPage()
            );

            mockMvc.perform(get(ALL_REPORTS.getUrl())
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message.content").isArray())
                    .andExpect(jsonPath("$.message.content[0].id").value("abc"));

            verify(reportService).getAllReports(any(Pageable.class), any());
        }

        @Test
        @DisplayName("Get all reports should return Not Found (404)")
        void getAllReports_NotFound() throws Exception {
            ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Reports not found");

            when(reportService.getAllReports(any(Pageable.class), any()))
                    .thenThrow(new ReportNotFoundException(response.message));

            mockMvc.perform(get(ALL_REPORTS.getUrl())
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(reportService).getAllReports(any(Pageable.class), any());
        }

        @Test
        @DisplayName("Get all reports should return Internal Server Error (500)")
        void getAllReports_InternalServerError() throws Exception {
            ErrorResponse response = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");

            when(reportService.getAllReports(any(Pageable.class), any()))
                    .thenThrow(new RuntimeException(response.message));

            mockMvc.perform(get(ALL_REPORTS.getUrl())
                            .param("pageNumber", "0")
                            .param("pageSize", "2")
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(reportService).getAllReports(any(Pageable.class), any());
        }

    }

    @Nested
    class GetUserReportsTest {

        @Test
        @DisplayName("Get user reports should return OK (200)")
        void getUserReports_Success() throws Exception {
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

            verify(reportService).getUserReports(any(Pageable.class), any(String.class));
        }


        @ParameterizedTest
        @DisplayName("Get user reports should return Not Found (404)")
        @ValueSource(strings = {"user", "report"})
        void getUserReports_NotFound(String ex) throws Exception {
            ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "User not found or no reports available");

            Exception exception = ex.equals("user") ? new UserNotFoundException(response.message) : ex.equals("report") ?
                    new ReportNotFoundException(response.message) : null;

            when(reportService.getUserReports(any(Pageable.class), any(String.class))).thenThrow(exception);

            mockMvc.perform(get(USER_REPORTS.getUrl(), "mockUserId")
                            .accept(ACCEPT_TYPE)
                            .param("pageNumber", "0")
                            .param("pageSize", "2"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(reportService).getUserReports(any(Pageable.class), any(String.class));
        }


        @Test
        @DisplayName("Get user reports should return Internal Server Error (500)")
        void getUserReports_InternalServerError() throws Exception {
            ErrorResponse response = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");

            when(reportService.getUserReports(any(Pageable.class), any(String.class))).thenThrow(new RuntimeException(response.message));

            mockMvc.perform(get(USER_REPORTS.getUrl(), "mockUserId")
                            .accept(ACCEPT_TYPE)
                            .param("pageNumber", "0")
                            .param("pageSize", "2"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(reportService).getUserReports(any(Pageable.class), any(String.class));
        }


        @Test
        @DisplayName("Get user reports should return Forbidden (403)")
        void getUserReports_Forbidden() throws Exception {
            ErrorResponse response = new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Forbidden: Authenticated user is not authorized to perform this action");

            when(reportService.getUserReports(any(Pageable.class), any(String.class))).thenThrow(new UnauthorizedActionException(response.message));

            mockMvc.perform(get(USER_REPORTS.getUrl(), "mockUserId")
                            .accept(ACCEPT_TYPE).param("pageNumber", "0")
                            .param("pageSize", "2"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(reportService).getUserReports(any(Pageable.class), any(String.class));
        }

    }

    @Nested
    class ReviewReportTest {

        @Test
        @DisplayName("Review report should return OK (200)")
        void reviewReport_Success() throws Exception {
            ResponseRecord response = new ResponseRecord(HttpStatus.OK.value(), "Report reviewed");

            String mockJson = mapToJson(getReviewReportRequest());
            when(reportService.reviewReport(any(ReviewReportRequest.class))).thenReturn(response.message);

            mockMvc.perform(post(REVIEW_REPORT.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mockJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(response.code))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(reportService).reviewReport(any(ReviewReportRequest.class));
        }

        @Test
        @DisplayName("Review report should return Not Found (404)")
        void reviewReport_NotFound() throws Exception {
            ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Report not found");

            String mockJson = mapToJson(getReviewReportRequest());
            when(reportService.reviewReport(any(ReviewReportRequest.class))).thenThrow(new ReportTypeNotFoundException(response.message));

            mockMvc.perform(post(REVIEW_REPORT.getUrl())
                            .accept(ACCEPT_TYPE)
                            .contentType(CONTENT_TYPE)
                            .content(mockJson))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(reportService).reviewReport(any(ReviewReportRequest.class));
        }

        @Test
        @DisplayName("Review report should return Internal Server Error (500)")
        void reviewReport_InternalServerError() throws Exception {
            ErrorResponse response = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");

            String mockJson = mapToJson(getReviewReportRequest());
            when(reportService.reviewReport(any(ReviewReportRequest.class))).thenThrow(new RuntimeException(response.message));

            mockMvc.perform(post(REVIEW_REPORT.getUrl())
                            .accept(ACCEPT_TYPE)
                            .contentType(CONTENT_TYPE)
                            .content(mockJson))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(reportService).reviewReport(any(ReviewReportRequest.class));
        }
    }

    @Nested
    class GetReportByIdTest {

        @Test
        @DisplayName("Get report by id return OK (200)")
        void getReportById_Success() throws Exception {
            when(reportService.getReportById(anyString())).thenReturn(getMockReport());

            mockMvc.perform(get(GET_REPORT.getUrl(), "mockReportId")
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message.id").value("abc"));

            verify(reportService).getReportById(any(String.class));
        }
        @Test
        @DisplayName("Get user reports should return Not Found (404)")
        void getReportById_NotFound() throws Exception {
            ErrorResponse response = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Report not found");

            when(reportService.getReportById(anyString())).thenThrow(new ReportNotFoundException(response.message));

            mockMvc.perform(get(GET_REPORT.getUrl(), "mockReportId")
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(reportService).getReportById(anyString());
        }

        @Test
        @DisplayName("Get user reports should return Internal Server Error (500)")
        void getReportById_InternalServerError() throws Exception {
            ErrorResponse response = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");

            when(reportService.getReportById(anyString())).thenThrow(new RuntimeException(response.message));

            mockMvc.perform(get(GET_REPORT.getUrl(), "mockUserId")
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(reportService).getReportById(anyString());
        }


        @Test
        @DisplayName("Get report by id should return Forbidden (403)")
        void getReportById_Forbidden() throws Exception {
            ErrorResponse response = new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Forbidden: Authenticated user is not authorized to perform this action");

            when(reportService.getReportById(anyString())).thenThrow(new UnauthorizedActionException(response.message));

            mockMvc.perform(get(GET_REPORT.getUrl(), "mockReportId")
                            .accept(ACCEPT_TYPE))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(reportService).getReportById(anyString());
        }
    }


    private SendReportRequest getSendReportRequest() {
        return SendReportRequest.builder()
                .description("This is a sample report description.")
                .userReportedId("user456")
                .reportType("Abuse")
                .objectType(ReportObjectType.COMMENT)
                .objectId("obj12345")
                .build();
    }


    private ReviewReportRequest getReviewReportRequest() {
        return ReviewReportRequest.builder()
                .reportId("reportId")
                .status("status")
                .comment("comment")
                .build();
    }

    private UserReport getMockReport() {
        return UserReport.builder().id("abc").build();
    }

    private Page<UserReport> getMockPage() {
        List<UserReport> mockReports = List.of(
                UserReport.builder().id("abc").build(),
                UserReport.builder().id("qwe").build()
        );

        return new PageImpl<>(mockReports);
    }

    private String mapToJson(Object request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }
}
