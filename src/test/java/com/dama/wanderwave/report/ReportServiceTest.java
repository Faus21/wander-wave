package com.dama.wanderwave.report;

import com.dama.wanderwave.handler.ReportNotFoundException;
import com.dama.wanderwave.handler.ReportTypeNotFoundException;
import com.dama.wanderwave.handler.UnauthorizedActionException;
import com.dama.wanderwave.handler.UserNotFoundException;
import com.dama.wanderwave.report.entity.PostReport;
import com.dama.wanderwave.report.entity.Report;
import com.dama.wanderwave.report.entity.ReportType;
import com.dama.wanderwave.report.repository.ReportRepository;
import com.dama.wanderwave.report.repository.ReportTypeRepository;
import com.dama.wanderwave.report.request.ReportPageRequest;
import com.dama.wanderwave.report.request.ReviewReportRequest;
import com.dama.wanderwave.report.request.SendReportRequest;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReportTypeRepository typeRepository;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        authentication = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static Stream<Object[]> userProvider() {
        return Stream.of(
                new Object[]{"test@example.com", "senderId", "reportedId", "typeName", null},
                new Object[]{"test@example.com", "wrongId", "reportedId", "typeName", new UserNotFoundException("Sender user not found with ID: wrongId")},
                new Object[]{"different@example.com", "senderId", "reportedId", "typeName", new UnauthorizedActionException("You are not authorized to send reports on behalf of this user.")},
                new Object[]{"test@example.com", "senderId", "wrongId", "typeName", new UserNotFoundException("Reported user not found with ID: wrongId")},
                new Object[]{"test@example.com", "senderId", "reportedId", "unknownType", new ReportTypeNotFoundException("Report type not found with name: unknownType")}
        );
    }

    @Test
    void getReportTypesShouldReturnTypesIfExist() {
        var mockType1 = new ReportType("test-id1", "test-type1");
        var mockType2 = new ReportType("test-id2", "test-type2");

        when(typeRepository.findAll()).thenReturn(List.of(mockType1, mockType2));

        var types = reportService.getReportTypes();

        verify(typeRepository, times(1)).findAll();
        assertEquals(types, List.of(mockType1, mockType2));
        assertEquals(2, types.size());
        assertEquals("test-type1", types.get(0).getName());
        assertEquals("test-type2", types.get(1).getName());
    }

    @Test
    void getReportTypesShouldThrowWhenEmpty() {
        when(typeRepository.findAll()).thenReturn(new ArrayList<>());

        assertThrows(ReportTypeNotFoundException.class, () -> reportService.getReportTypes());

        verify(typeRepository, times(1)).findAll();
    }

    @ParameterizedTest
    @MethodSource("userProvider")
    void testSendReport(String authEmail, String userSenderId, String userReportedId, String reportType, Exception expectedException) {
        com.dama.wanderwave.report.request.SendReportRequest mockRequest = SendReportRequest.builder()
                .userSenderId(userSenderId)
                .userReportedId(userReportedId)
                .reportType(reportType)
                .build();

        when(authentication.getName()).thenReturn(authEmail);

        User sender = new User();
        sender.setEmail(authEmail);
        User reported = new User();

        if (userSenderId.equals("wrongId")) {
            when(userRepository.findById(userSenderId)).thenReturn(Optional.empty());
        } else if (userReportedId.equals("wrongId")) {
            when(userRepository.findById(userSenderId)).thenReturn(Optional.of(sender));
            when(userRepository.findById(userReportedId)).thenReturn(Optional.empty());
        } else if (!(expectedException instanceof UnauthorizedActionException)) {
            when(userRepository.findById(userSenderId)).thenReturn(Optional.of(sender));
            when(userRepository.findById(userReportedId)).thenReturn(Optional.of(reported));
        } else {
            when(userRepository.findById(userSenderId)).thenReturn(Optional.of(sender));
            sender.setEmail("wrong@mail.com");
        }

        if (expectedException instanceof ReportTypeNotFoundException) {
            when(typeRepository.findByName(reportType)).thenReturn(Optional.empty());
        }

        if (expectedException == null) {
            when(typeRepository.findByName(reportType)).thenReturn(Optional.of(new ReportType()));
            String result = reportService.sendReport(mockRequest);
            assertEquals("Report created successfully", result);
            verify(reportRepository, times(1)).save(any(Report.class));
        } else {
            Exception exception = assertThrows(expectedException.getClass(), () -> reportService.sendReport(mockRequest));
            verify(reportRepository, never()).save(any(Report.class));
            assertEquals(expectedException.getMessage(), exception.getMessage());
        }
    }

    @Test
    void getUserReportsShouldBeOk() {
        when(authentication.getName()).thenReturn("mock@mail.com");
        var mockUser = getMockUser();

        var mockPage = getMockPage();

        when(userRepository.findById("userId")).thenReturn(Optional.of(mockUser));
        when(reportRepository.findAllBySender(PageRequest.of(0, 2), mockUser)).thenReturn(mockPage);

        var result = reportService.getUserReports(PageRequest.of(0, 2), "userId");

        verify(userRepository, times(1)).findById("userId");
        verify(reportRepository, times(1)).findAllBySender(PageRequest.of(0, 2), mockUser);
        assertEquals(mockPage, result);
    }

    @Test
    void getUserReportsShouldThrowUserNotFoundException() {
        when(authentication.getName()).thenReturn("mock@mail.com");

        when(userRepository.findById("invalidUserId")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> reportService.getUserReports(PageRequest.of(0, 2), "invalidUserId"));

        verify(userRepository, times(1)).findById("invalidUserId");
        verify(reportRepository, times(0)).findAllBySender(any(), any());
    }

    @Test
    void getUserReportsShouldThrowUnauthorizedActionException() {
        when(authentication.getName()).thenReturn("mock@mail.com");

        User mockUser = getMockUser();
        mockUser.setEmail("wrong@mail.com");

        when(userRepository.findById("userId")).thenReturn(Optional.of(mockUser));

        assertThrows(UnauthorizedActionException.class, () -> reportService.getUserReports(PageRequest.of(0, 2), "userId"));

        verify(userRepository, times(1)).findById("userId");
        verify(reportRepository, times(0)).findAllBySender(any(), any());
    }

    @Test
    void getUserReportsShouldThrowReportNotFoundException() {
        when(authentication.getName()).thenReturn("mock@mail.com");

        User mockUser = getMockUser();

        Page<Report> emptyPage = new PageImpl<>(List.of());

        when(userRepository.findById("userId")).thenReturn(Optional.of(mockUser));
        when(reportRepository.findAllBySender(PageRequest.of(0, 2), mockUser)).thenReturn(emptyPage);

        assertThrows(ReportNotFoundException.class, () -> reportService.getUserReports(PageRequest.of(0, 2), "userId"));

        verify(userRepository, times(1)).findById("userId");
        verify(reportRepository, times(1)).findAllBySender(PageRequest.of(0, 2), mockUser);
    }

    @Test
    void getAllReportsShouldReturnPageOfReports() {
        com.dama.wanderwave.report.request.ReportPageRequest request = new com.dama.wanderwave.report.request.ReportPageRequest(0, 2);

        Page<Report> mockPage = getMockPage();
        when(reportRepository.findAll(PageRequest.of(request.getPageNumber(), request.getPageSize()))).thenReturn(mockPage);

        var result = reportService.getAllReports(request);

        verify(reportRepository, times(1)).findAll(PageRequest.of(request.getPageNumber(), request.getPageSize()));
        assertEquals(2, result.getNumberOfElements());
    }

    @Test
    void getAllReportsShouldThrowReportNotFoundExceptionWhenEmpty() {
        com.dama.wanderwave.report.request.ReportPageRequest request = new ReportPageRequest(0, 2);

        Page<Report> emptyPage = new PageImpl<>(List.of());
        when(reportRepository.findAll(PageRequest.of(request.getPageNumber(), request.getPageSize()))).thenReturn(emptyPage);

        assertThrows(ReportNotFoundException.class, () -> reportService.getAllReports(request));

        verify(reportRepository, times(1)).findAll(PageRequest.of(request.getPageNumber(), request.getPageSize()));
    }

    @Test
    void getReportByIdShouldReturnReportWhenAuthorized() {
        Report mockReport = getMockReport();
        User mockUser = mockReport.getSender();

        when(authentication.getName()).thenReturn("mock@mail.com");
        when(userRepository.findById("userId")).thenReturn(Optional.of(mockUser));
        when(reportRepository.findById("reportId")).thenReturn(Optional.of(mockReport));

        var result = reportService.getReportById("reportId");

        assertEquals(mockReport, result);
        verify(reportRepository, times(1)).findById("reportId");
        verify(userRepository, times(1)).findById(mockUser.getId());
    }

    @Test
    void getReportByIdShouldThrowReportNotFoundException() {
        when(reportRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(ReportNotFoundException.class, () -> reportService.getReportById("nonExistentReportId"));

        verify(reportRepository, times(1)).findById("nonExistentReportId");
        verify(userRepository, times(0)).findById(anyString());
    }

    @Test
    void getReportByIdShouldThrowUserNotFoundException() {
        Report mockReport = getMockReport();

        when(authentication.getName()).thenReturn("mock@mail.com");
        when(reportRepository.findById(anyString())).thenReturn(Optional.of(mockReport));
        when(userRepository.findById(mockReport.getSender().getId())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> reportService.getReportById("reportId"));

        verify(reportRepository, times(1)).findById("reportId");
        verify(userRepository, times(1)).findById(mockReport.getSender().getId());
    }

    @Test
    void getReportByIdShouldThrowUnauthorizedActionException() {
        Report mockReport = getMockReport();

        when(authentication.getName()).thenReturn("wrong@mail.com");
        when(reportRepository.findById(anyString())).thenReturn(Optional.of(mockReport));
        when(userRepository.findById(mockReport.getSender().getId())).thenReturn(Optional.of(mockReport.getSender()));

        assertThrows(UnauthorizedActionException.class, () -> reportService.getReportById("reportId"));

        verify(reportRepository, times(1)).findById("reportId");
        verify(userRepository, times(1)).findById(mockReport.getSender().getId());
    }

    @Test
    void reviewReportShouldBeSuccessful() {
        User mockAdmin = getMockUser();
        Report mockReport = getMockReport();
        com.dama.wanderwave.report.request.ReviewReportRequest request = getMockReviewRequest();

        when(authentication.getName()).thenReturn(mockAdmin.getEmail());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockAdmin));
        when(reportRepository.findById(anyString())).thenReturn(Optional.of(mockReport));

        var result = reportService.reviewReport(request);

        assertEquals("Report reviewed", result);
        verify(userRepository, times(1)).findByEmail(mockAdmin.getEmail());
        verify(reportRepository, times(1)).findById("reportId");
        verify(reportRepository, times(1)).save(mockReport);
        assertNotNull(mockReport.getReviewedAt());
        assertEquals(mockAdmin, mockReport.getReviewedBy());
        assertEquals("Test comment", mockReport.getReportComment());
    }

    @Test
    void reviewReportShouldThrowUserNotFoundException() {
        when(authentication.getName()).thenReturn("admin@mail.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        com.dama.wanderwave.report.request.ReviewReportRequest request = getMockReviewRequest();

        assertThrows(UserNotFoundException.class, () -> reportService.reviewReport(request));

        verify(userRepository, times(1)).findByEmail("admin@mail.com");
        verify(reportRepository, times(0)).findById(anyString());
        verify(reportRepository, times(0)).save(any(Report.class));
    }

    @Test
    void reviewReportShouldThrowReportNotFoundException() {
        User mockAdmin = getMockUser();

        // Mocking SecurityContext and repository behavior
        when(authentication.getName()).thenReturn(mockAdmin.getEmail());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockAdmin));
        when(reportRepository.findById(anyString())).thenReturn(Optional.empty());

        com.dama.wanderwave.report.request.ReviewReportRequest request = getMockReviewRequest();

        assertThrows(ReportNotFoundException.class, () -> reportService.reviewReport(request));

        verify(userRepository, times(1)).findByEmail(mockAdmin.getEmail());
        verify(reportRepository, times(1)).findById("reportId");
        verify(reportRepository, times(0)).save(any(Report.class));
    }

    private Report getMockReport() {
        User sender = getMockUser();

        return PostReport.builder()
                .id("reportId")
                .sender(sender)
                .build();
    }

    private User getMockUser () {
        return User.builder()
                .id("userId")
                .email("mock@mail.com")
                .build();
    }

    private Page<Report> getMockPage() {
        List<Report> mockReports = List.of(
                PostReport.builder().id("abc").build(),
                PostReport.builder().id("qwe").build()
        );

        return new PageImpl<>(mockReports);
    }

    private com.dama.wanderwave.report.request.ReviewReportRequest getMockReviewRequest() {
        return ReviewReportRequest.builder()
                .reportId("reportId")
                .comment("Test comment")
                .build();
    }

}
