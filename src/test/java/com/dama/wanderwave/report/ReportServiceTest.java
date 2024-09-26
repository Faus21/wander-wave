package com.dama.wanderwave.report;

import com.dama.wanderwave.handler.ReportNotFoundException;
import com.dama.wanderwave.handler.ReportTypeNotFoundException;
import com.dama.wanderwave.handler.UnauthorizedActionException;
import com.dama.wanderwave.handler.UserNotFoundException;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import com.dama.wanderwave.util.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
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
    private ReportService service;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReportTypeRepository typeRepository;
    @Mock
    private Utils utils;

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

        var types = service.getReportTypes();

        verify(typeRepository, times(1)).findAll();
        assertEquals(types, List.of(mockType1, mockType2));
        assertEquals(2, types.size());
        assertEquals("test-type1", types.get(0).getName());
        assertEquals("test-type2", types.get(1).getName());
    }

    @Test
    void getReportTypesShouldThrowWhenEmpty() {
        when(typeRepository.findAll()).thenReturn(new ArrayList<>());

        assertThrows(ReportTypeNotFoundException.class, () -> service.getReportTypes());

        verify(typeRepository, times(1)).findAll();
    }

    @ParameterizedTest
    @MethodSource("userProvider")
    void testSendReport(String authEmail, String userSenderId, String userReportedId, String reportType, Exception expectedException) {
        SendReportRequest mockRequest = SendReportRequest.builder()
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
            String result = service.sendReport(mockRequest);
            assertEquals("Report created successfully", result);
            verify(reportRepository, times(1)).save(any(Report.class));
        } else {
            Exception exception = assertThrows(expectedException.getClass(), () -> service.sendReport(mockRequest));
            verify(reportRepository, never()).save(any(Report.class));
            assertEquals(expectedException.getMessage(), exception.getMessage());
        }
    }

    @Test
    void getUserReportsShouldBeOk() {
        when(authentication.getName()).thenReturn("mock@mail.com");
        User mockUser = User.builder()
                .email("mock@mail.com")
                .build();

        List<Report> mockReports = List.of(
                Report.builder().id("abc").build(),
                Report.builder().id("qwe").build()
        );

        when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));
        when(reportRepository.findAllBySender(mockUser)).thenReturn(mockReports);


        var result = service.getUserReports("mock@mail.com");

        verify(reportRepository, times(1)).findAllBySender(mockUser);
        verify(userRepository, times(1)).findById(anyString());
        assertEquals(mockReports, result);
    }

    @Test
    void getUserReportsShouldThrowWhenUserNotFound() {
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.getUserReports("mock@mail.com"));

        verify(userRepository, times(1)).findById(anyString());
        verify(reportRepository, never()).findAllBySender(any(User.class));
    }

    @Test
    void getUserReportsShouldThrowWhenReportNotFound() {
        when(authentication.getName()).thenReturn("mock@mail.com");

        User mockUser = User.builder()
                .email("mock@mail.com")
                .build();

        when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));
        when(reportRepository.findAllBySender(any(User.class))).thenReturn(List.of());

        assertThrows(ReportNotFoundException.class, () -> service.getUserReports("mock@mail.com"));

        verify(userRepository, times(1)).findById(anyString());
        verify(reportRepository, times(1)).findAllBySender(any(User.class));
    }

    @Test
    void getUserReportsShouldThrowWhenUserTryingToFetchNotOwnReports() {
        when(authentication.getName()).thenReturn("mock@mail.com");

        User mockUser = User.builder()
                .email("diff@mail.com")
                .build();

        when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));

        assertThrows(UnauthorizedActionException.class, () -> service.getUserReports("diff@mail.com"));

        verify(userRepository, times(1)).findById(anyString());
        verify(reportRepository, never()).findAllBySender(any(User.class));
    }

    @Test
    void getReportsByDateShouldBeOk() {
        var startDate = LocalDateTime.now().minusDays(1);
        var endDate = LocalDateTime.now().plusDays(1);

        var mockReports = List.of(
                Report.builder().id("abc").build(),
                Report.builder().id("qwe").build()
        );

        when(reportRepository.findByDates(startDate, endDate)).thenReturn(mockReports);

        var result = reportRepository.findByDates(startDate, endDate);

        verify(reportRepository, times(1)).findByDates(startDate, endDate);
        assertEquals(result, mockReports);
    }

    @Test
    void getReportsByDateShouldThrowWhenReportNotFound() {
        var startDate = LocalDateTime.now().minusDays(1);
        var endDate = LocalDateTime.now().plusDays(1);
        var request = ReportsByDatesRequest.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        when(reportRepository.findByDates(startDate, endDate)).thenReturn(List.of());

        assertThrows(ReportNotFoundException.class, () -> service.getReportsByDate(request));
        verify(reportRepository, times(1)).findByDates(startDate, endDate);
    }

}
