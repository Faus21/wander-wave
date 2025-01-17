package com.dama.wanderwave.report;

import com.dama.wanderwave.comment.CommentRepository;
import com.dama.wanderwave.handler.comment.CommentNotFoundException;
import com.dama.wanderwave.handler.post.PostNotFoundException;
import com.dama.wanderwave.handler.report.DuplicateReportException;
import com.dama.wanderwave.handler.report.ReportNotFoundException;
import com.dama.wanderwave.handler.report.ReportStatusNotFoundException;
import com.dama.wanderwave.handler.report.ReportTypeNotFoundException;
import com.dama.wanderwave.handler.user.UnauthorizedActionException;
import com.dama.wanderwave.handler.user.UserNotFoundException;
import com.dama.wanderwave.post.PostRepository;
import com.dama.wanderwave.report.general.*;
import com.dama.wanderwave.report.post.PostReport;
import com.dama.wanderwave.report.request.FilteredReportPageRequest;
import com.dama.wanderwave.report.request.ReportObjectType;
import com.dama.wanderwave.report.request.ReviewReportRequest;
import com.dama.wanderwave.report.request.SendReportRequest;
import com.dama.wanderwave.user.BlackList;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import com.dama.wanderwave.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;
    @Mock
    private UserReportRepository reportRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private ReportStatusRepository reportStatusRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReportTypeRepository typeRepository;
    @Mock
    private UserService userService;
    @Mock
    private ModelMapper modelMapper;

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        authentication = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Nested
    class SendReportTest {
        @Test
        @DisplayName("Send report should be ok")
        void sendReport_Success() {
            var mockUser = getMockUser();
            when(userService.getAuthenticatedUser()).thenReturn(getMockUser());

            when(userService.findUserByIdOrThrow(anyString())).thenReturn(mockUser);

            when(reportRepository.findUserReportByObjectAndSender(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(reportStatusRepository.findByName(anyString())).thenReturn(Optional.of(new ReportStatus()));

            when(typeRepository.findByName(anyString())).thenReturn(Optional.of(getMockReportType()));

            when(reportRepository.save(any(UserReport.class))).thenReturn(getMockReport());

            var response = reportService.sendReport(getSendReportRequest());
            assertEquals("Report created successfully", response);

            verify(reportRepository).findUserReportByObjectAndSender(anyString(), anyString());
            verify(reportRepository).save(any(UserReport.class));
            verify(reportStatusRepository).findByName(anyString());
            verify(typeRepository).findByName(anyString());

            verifyNoInteractions(commentRepository, postRepository);
        }

        @Test
        @DisplayName("Send report should throw DuplicateReportException")
        void sendReport_DuplicateReport() {
            when(userService.getAuthenticatedUser()).thenReturn(getMockUser());
            when(reportRepository.findUserReportByObjectAndSender(anyString(), anyString()))
                    .thenReturn(Optional.of(getMockReport()));

            assertThrows(DuplicateReportException.class, () -> reportService.sendReport(getSendReportRequest()));

            verify(reportRepository).findUserReportByObjectAndSender(anyString(), anyString());

            verifyNoInteractions(reportStatusRepository, commentRepository, typeRepository, postRepository);
        }

        @Test
        @DisplayName("Send report should throw UserNotFoundException")
        void sendReport_UserNotFound() {
            when(userService.getAuthenticatedUser()).thenReturn(getMockUser());
            when(reportRepository.findUserReportByObjectAndSender(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            when(userService.findUserByIdOrThrow(anyString()))
                    .thenThrow(UserNotFoundException.class);

            assertThrows(UserNotFoundException.class, () -> reportService.sendReport(getSendReportRequest()));

            verify(reportRepository).findUserReportByObjectAndSender(anyString(), anyString());

            verifyNoInteractions(reportStatusRepository, commentRepository, typeRepository, postRepository);
        }

        @Test
        @DisplayName("Send report should throw CommentNotFoundException")
        void sendReport_CommentNotFound() {
            when(userService.getAuthenticatedUser()).thenReturn(getMockUser());
            when(reportRepository.findUserReportByObjectAndSender(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(commentRepository.findById(anyString())).thenReturn(Optional.empty());

            var mockReq = getSendReportRequest();
            mockReq.setObjectType(ReportObjectType.COMMENT);

            assertThrows(CommentNotFoundException.class,
                    () -> reportService.sendReport(mockReq));

            verify(commentRepository).findById(anyString());
            verify(reportRepository).findUserReportByObjectAndSender(anyString(), anyString());

            verifyNoInteractions(reportStatusRepository, typeRepository, postRepository);
        }

        @Test
        @DisplayName("Send report should throw PostNotFoundException")
        void sendReport_PostNotFound() {
            when(userService.getAuthenticatedUser()).thenReturn(getMockUser());

            when(reportRepository.findUserReportByObjectAndSender(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(postRepository.findById(anyString())).thenReturn(Optional.empty());

            var mockReq = getSendReportRequest();
            mockReq.setObjectType(ReportObjectType.POST);

            assertThrows(PostNotFoundException.class,
                    () -> reportService.sendReport(mockReq));

            verify(reportRepository).findUserReportByObjectAndSender(anyString(), anyString());
            verifyNoInteractions(reportStatusRepository, commentRepository, typeRepository);
        }

        @Test
        @DisplayName("Send report should throw ReportTypeNotFoundException")
        void sendReport_ReportTypeNotFound() {
            when(userService.getAuthenticatedUser()).thenReturn(getMockUser());

            when(reportRepository.findUserReportByObjectAndSender(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(typeRepository.findByName(anyString())).thenReturn(Optional.empty());

            assertThrows(ReportTypeNotFoundException.class,
                    () -> reportService.sendReport(getSendReportRequest()));

            verify(reportRepository).findUserReportByObjectAndSender(anyString(), anyString());
            verify(typeRepository).findByName(anyString());

            verifyNoInteractions(reportStatusRepository, commentRepository, postRepository);
        }

        @Test
        @DisplayName("Send report should throw ReportStatusNotFound")
        void sendReport_ReportStatusNotFound() {
            when(userService.getAuthenticatedUser()).thenReturn(getMockUser());
            when(reportRepository.findUserReportByObjectAndSender(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(typeRepository.findByName(anyString())).thenReturn(Optional.of(getMockReportType()));
            when(reportStatusRepository.findByName(anyString())).thenReturn(Optional.empty());

            assertThrows(ReportStatusNotFoundException.class,
                    () -> reportService.sendReport(getSendReportRequest()));

            verify(reportRepository).findUserReportByObjectAndSender(anyString(), anyString());
            verify(typeRepository).findByName(anyString());

            verifyNoInteractions(postRepository, commentRepository);
        }
    }

    @Nested
    class GetUserReportsTest {
        @Test
        @DisplayName("Get user reports should be ok")
        void getUserReports_Success() {
            var mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);

            var mockPage = getMockPage();

            when(reportRepository
                    .findAllBySender(PageRequest.of(0, 2), mockUser)).thenReturn(mockPage);

            var result = reportService.getUserReports(PageRequest.of(0, 2), mockUser.getId());

            verify(reportRepository).findAllBySender(PageRequest.of(0, 2), mockUser);
            assertEquals(mockPage.getTotalElements(), result.getTotalElements());
        }

        @Test
        @DisplayName("Get user reports should throw ReportNotFoundException")
        void getUserReports_ReportNotFoundException() {
            var mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);

            when(reportRepository.findAllBySender(any(PageRequest.class), any(User.class))).thenReturn(
                    Page.empty()
            );

            assertThrows(ReportNotFoundException.class,
                    () -> reportService.getUserReports(PageRequest.of(0, 2), mockUser.getId()));
        }

        @Test
        @DisplayName("Get user reports should throw UnauthorizedActionException")
        void getUserReports_UnauthorizedActionException() {
            var mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            doThrow(UnauthorizedActionException.class).when(userService).checkUserAccessRights(any(User.class), anyString());

            assertThrows(UnauthorizedActionException.class,
                    () -> reportService.getUserReports(PageRequest.of(0, 2), mockUser.getId()));

            verifyNoInteractions(reportRepository);
        }

        @Test
        @DisplayName("Get user reports should throw UserNotFoundException")
        void getUserReports_UserNotFoundException() {
            var mockUser = getMockUser();

            when(userService.getAuthenticatedUser()).thenThrow(UserNotFoundException.class);

            assertThrows(UserNotFoundException.class,
                    () -> reportService.getUserReports(PageRequest.of(0, 2), mockUser.getId()));

            verifyNoInteractions(reportRepository);
        }
    }

    @Nested
    class GetAllReportsTest {
        @Test
        @DisplayName("Get all reports should be ok")
        void getAllReports_Success() {
            PageRequest request = PageRequest.of(0, 2);

            Page<UserReport> mockPage = getMockPage();
            when(reportRepository.findAll(PageRequest.of(request.getPageNumber(), request.getPageSize()))).thenReturn(mockPage);

            var result = reportService.getAllReports(request, new FilteredReportPageRequest());

            verify(reportRepository).findAll(PageRequest.of(request.getPageNumber(), request.getPageSize()));
            assertEquals(2, result.getNumberOfElements());
        }


        @Test
        @DisplayName("Get all reports should be throw ReportNotFoundException")
        void getAllReports_ReportNotFoundException() {
            PageRequest request = PageRequest.of(0, 2);

            Page<UserReport> emptyPage = new PageImpl<>(List.of());
            when(reportRepository.findAll(PageRequest.of(request.getPageNumber(), request.getPageSize()))).thenReturn(emptyPage);

            assertThrows(ReportNotFoundException.class, () -> reportService.getAllReports(request, new FilteredReportPageRequest()));

            verify(reportRepository).findAll(PageRequest.of(request.getPageNumber(), request.getPageSize()));
        }
    }

    @Nested
    class GetReportByIdTest {
        @Test
        @DisplayName("Get report by id should be ok")
        void getReportById_Success() {
            UserReport mockReport = getMockReport();
            User mockUser = mockReport.getSender();

            when(userService.getAuthenticatedUser()).thenReturn(mockUser);
            when(reportRepository.findById(anyString())).thenReturn(Optional.of(mockReport));

            var result = reportService.getReportById("reportId");

            assertEquals(mockReport.getId(), result.getId());
            verify(reportRepository).findById(anyString());
        }


        @Test
        @DisplayName("Get report by id should throw ReportNotFoundException")
        void getReportById_ReportNotFoundException() {
            when(reportRepository.findById(anyString())).thenReturn(Optional.empty());

            assertThrows(ReportNotFoundException.class, () -> reportService.getReportById("nonExistentReportId"));

            verify(reportRepository).findById(anyString());
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("Get report by id should throw UserNotFoundException")
        void getReportById_UserNotFoundException() {
            UserReport mockReport = getMockReport();

            when(userService.getAuthenticatedUser()).thenThrow(UserNotFoundException.class);
            when(reportRepository.findById(anyString())).thenReturn(Optional.of(mockReport));

            assertThrows(UserNotFoundException.class, () -> reportService.getReportById("reportId"));
            verify(reportRepository).findById(anyString());
        }

        @Test
        @DisplayName("Get report by id should throw UnauthorizedActionException")
        void getReportById_UnauthorizedActionException() {
            UserReport mockReport = getMockReport();

            when(userService.getAuthenticatedUser()).thenReturn(new User());
            when(reportRepository.findById(anyString())).thenReturn(Optional.of(mockReport));

            doThrow(UnauthorizedActionException.class).when(userService).checkUserAccessRights(any(User.class), anyString());

            assertThrows(UnauthorizedActionException.class, () -> reportService.getReportById("reportId"));
            verify(reportRepository).findById(anyString());
        }
    }

    @Nested
    class ReviewReportTest {
        @Test
        @DisplayName("Review report by id should be ok")
        void reviewReport_Success() {
            User mockAdmin = getMockUser();
            UserReport mockReport = getMockReport();
            ReviewReportRequest request = getMockReviewRequest();

            when(userService.getAuthenticatedUser()).thenReturn(mockAdmin);

            when(reportRepository.findAllByReported(any(User.class))).thenReturn(getMockPage().stream().toList());

            when(reportRepository.findById(anyString())).thenReturn(Optional.of(mockReport));
            when(reportStatusRepository.findByName(anyString())).thenReturn(Optional.of(mockReport.getStatus()));

            var result = reportService.reviewReport(request);

            assertEquals("Report reviewed", result);

            verify(reportRepository).findById(anyString());
            verify(reportStatusRepository).findByName(anyString());
            verify(reportRepository).saveAll(any());
        }

        @Test
        @DisplayName("Review report by id should throw UserNotFoundException")
        void reviewReport_UserNotFound() {
            ReviewReportRequest request = getMockReviewRequest();

            when(userService.getAuthenticatedUser()).thenThrow(UserNotFoundException.class);

            assertThrows(UserNotFoundException.class, () -> reportService.reviewReport(request));

            verifyNoInteractions(reportStatusRepository, reportRepository);
        }


        @Test
        @DisplayName("Review report by id should throw ReportNotFoundException")
        void reviewReport_ReportNotFound() {
            User mockAdmin = getMockUser();

            when(userService.getAuthenticatedUser()).thenReturn(mockAdmin);

            when(reportRepository.findById(anyString())).thenReturn(Optional.empty());

            ReviewReportRequest request = getMockReviewRequest();

            assertThrows(ReportNotFoundException.class, () -> reportService.reviewReport(request));

            verify(reportRepository).findById(anyString());
            verifyNoInteractions(reportStatusRepository);
        }

        @Test
        @DisplayName("Review report by id should throw ReportStatusNotFoundException")
        void reviewReport_ReportStatusNotFound() {
            User mockAdmin = getMockUser();
            ReviewReportRequest request = getMockReviewRequest();

            when(userService.getAuthenticatedUser()).thenReturn(mockAdmin);

            when(reportRepository.findById(anyString())).thenReturn(Optional.of(getMockReport()));

            assertThrows(ReportStatusNotFoundException.class, () -> reportService.reviewReport(request));

            verify(reportRepository).findById(anyString());
            verify(reportStatusRepository).findByName(anyString());
        }
    }

    private UserReport getMockReport() {
        User sender = getMockUser();

        return UserReport.builder()
                .id("reportId")
                .sender(sender)
                .reported(sender)
                .status(new ReportStatus())
                .build();
    }

    private User getMockUser() {
        return User.builder()
                .id("mockId")
                .email("mock@mail.com")
                .blackList(new BlackList(new HashSet<>()))
                .build();
    }

    private ReportType getMockReportType() {
        return ReportType.builder()
                .name("mock")
                .build();
    }

    private SendReportRequest getSendReportRequest() {
        return SendReportRequest
                .builder()
                .objectId("object123")
                .objectType(ReportObjectType.USER)
                .reportType("reportType")
                .description("description")
                .build();
    }

    private Page<UserReport> getMockPage() {
        List<UserReport> mockReports = List.of(
                UserReport.builder().id("abc").reported(getMockUser()).build(),
                UserReport.builder().id("qwe").reported(getMockUser()).build()
        );

        return new PageImpl<>(mockReports);
    }

    private ReviewReportRequest getMockReviewRequest() {
        return ReviewReportRequest.builder()
                .reportId("reportId")
                .status("status")
                .comment("Test comment")
                .build();
    }
}
