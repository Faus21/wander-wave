package com.dama.wanderwave.report;

import com.dama.wanderwave.comment.CommentRepository;
import com.dama.wanderwave.handler.*;
import com.dama.wanderwave.post.PostRepository;
import com.dama.wanderwave.report.general.*;
import com.dama.wanderwave.report.post.PostReport;
import com.dama.wanderwave.report.request.FilteredReportPageRequest;
import com.dama.wanderwave.report.request.ReportObjectType;
import com.dama.wanderwave.report.request.ReviewReportRequest;
import com.dama.wanderwave.report.request.SendReportRequest;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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

            when(authentication.getName()).thenReturn(mockUser.getName());
            when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));

            when(reportRepository.findUserReportByObjectAndSender(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(reportStatusRepository.findByName(anyString())).thenReturn(Optional.of(new ReportStatus()));

            when(typeRepository.findByName(anyString())).thenReturn(Optional.of(getMockReportType()));

            when(reportRepository.save(any(UserReport.class))).thenReturn(getMockReport());

            var response = reportService.sendReport(getSendReportRequest());
            assertEquals("Report created successfully", response);

            verify(userRepository, times(2)).findById(anyString());

            verify(reportRepository).findUserReportByObjectAndSender(anyString(), anyString());
            verify(reportRepository).save(any(UserReport.class));
            verify(reportStatusRepository).findByName(anyString());
            verify(typeRepository).findByName(anyString());

            verifyNoInteractions(commentRepository, postRepository);
        }

        @Test
        @DisplayName("Send report should throw UnauthorizedActionException")
        void sendReport_UnauthorizedAction() {
            assertThrows(UnauthorizedActionException.class, () -> reportService.sendReport(getSendReportRequest()));

            verify(userRepository).findById(anyString());

            verifyNoInteractions(reportRepository, reportStatusRepository, commentRepository, typeRepository, postRepository);
        }

        @Test
        @DisplayName("Send report should throw DuplicateReportException")
        void sendReport_DuplicateReport() {
            var mockUser = getMockUser();

            when(authentication.getName()).thenReturn(mockUser.getName());
            when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));

            when(reportRepository.findUserReportByObjectAndSender(anyString(), anyString()))
                    .thenReturn(Optional.of(getMockReport()));

            assertThrows(DuplicateReportException.class, () -> reportService.sendReport(getSendReportRequest()));

            verify(userRepository).findById(anyString());
            verify(reportRepository).findUserReportByObjectAndSender(anyString(), anyString());

            verifyNoInteractions(reportStatusRepository, commentRepository, typeRepository, postRepository);
        }

        @Test
        @DisplayName("Send report should throw UserNotFoundException")
        void sendReport_UserNotFound() {
            var mockUser = getMockUser();

            when(authentication.getName()).thenReturn(mockUser.getName());
            when(userRepository.findById(anyString()))
                    .thenReturn(Optional.of(mockUser))
                    .thenReturn(Optional.empty());

            when(reportRepository.findUserReportByObjectAndSender(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> reportService.sendReport(getSendReportRequest()));

            verify(userRepository, times(2)).findById(anyString());
            verify(reportRepository).findUserReportByObjectAndSender(anyString(), anyString());

            verifyNoInteractions(reportStatusRepository, commentRepository, typeRepository, postRepository);
        }

        @Test
        @DisplayName("Send report should throw CommentNotFoundException")
        void sendReport_CommentNotFound() {
            var mockUser = getMockUser();

            when(authentication.getName()).thenReturn(mockUser.getName());
            when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));
            when(reportRepository.findUserReportByObjectAndSender(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(commentRepository.findById(anyString())).thenReturn(Optional.empty());

            var mockReq = getSendReportRequest();
            mockReq.setObjectType(ReportObjectType.COMMENT);

            assertThrows(CommentNotFoundException.class,
                    () -> reportService.sendReport(mockReq));

            verify(userRepository, times(2)).findById(anyString());
            verify(commentRepository).findById(anyString());
            verify(reportRepository).findUserReportByObjectAndSender(anyString(), anyString());

            verifyNoInteractions(reportStatusRepository, typeRepository, postRepository);
        }

        @Test
        @DisplayName("Send report should throw PostNotFoundException")
        void sendReport_PostNotFound() {
            var mockUser = getMockUser();

            when(authentication.getName()).thenReturn(mockUser.getName());
            when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));
            when(reportRepository.findUserReportByObjectAndSender(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(postRepository.findById(anyString())).thenReturn(Optional.empty());

            var mockReq = getSendReportRequest();
            mockReq.setObjectType(ReportObjectType.POST);

            assertThrows(PostNotFoundException.class,
                    () -> reportService.sendReport(mockReq));

            verify(userRepository, times(2)).findById(anyString());
            verify(reportRepository).findUserReportByObjectAndSender(anyString(), anyString());
            verifyNoInteractions(reportStatusRepository, commentRepository, typeRepository);
        }

        @Test
        @DisplayName("Send report should throw ReportTypeNotFoundException")
        void sendReport_ReportTypeNotFound() {
            var mockUser = getMockUser();

            when(authentication.getName()).thenReturn(mockUser.getName());
            when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));
            when(reportRepository.findUserReportByObjectAndSender(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(typeRepository.findByName(anyString())).thenReturn(Optional.empty());

            assertThrows(ReportTypeNotFoundException.class,
                    () -> reportService.sendReport(getSendReportRequest()));

            verify(userRepository, times(2)).findById(anyString());
            verify(reportRepository).findUserReportByObjectAndSender(anyString(), anyString());
            verify(typeRepository).findByName(anyString());

            verifyNoInteractions(reportStatusRepository, commentRepository, postRepository);
        }

        @Test
        @DisplayName("Send report should throw ReportStatusNotFound")
        void sendReport_ReportStatusNotFound() {
            var mockUser = getMockUser();

            when(authentication.getName()).thenReturn(mockUser.getName());
            when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));
            when(reportRepository.findUserReportByObjectAndSender(anyString(), anyString()))
                    .thenReturn(Optional.empty());
            when(typeRepository.findByName(anyString())).thenReturn(Optional.of(getMockReportType()));
            when(reportStatusRepository.findByName(anyString())).thenReturn(Optional.empty());

            assertThrows(ReportStatusNotFoundException.class,
                    () -> reportService.sendReport(getSendReportRequest()));

            verify(userRepository, times(2)).findById(anyString());
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

            when(authentication.getName()).thenReturn(mockUser.getName());
            when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

            var mockPage = getMockPage();

            when(reportRepository
                    .findAllBySender(PageRequest.of(0, 2), mockUser)).thenReturn(mockPage);

            var result = reportService.getUserReports(PageRequest.of(0, 2), mockUser.getId());

            verify(userRepository).findById(anyString());
            verify(reportRepository).findAllBySender(PageRequest.of(0, 2), mockUser);
            assertEquals(mockPage, result);
        }

        @Test
        @DisplayName("Get user reports should throw ReportNotFoundException")
        void getUserReports_ReportNotFoundException() {
            var mockUser = getMockUser();

            when(authentication.getName()).thenReturn(mockUser.getName());
            when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
            when(reportRepository.findAllBySender(any(PageRequest.class), any(User.class))).thenReturn(
                    Page.empty()
            );

            assertThrows(ReportNotFoundException.class,
                    () -> reportService.getUserReports(PageRequest.of(0, 2), mockUser.getId()));

            verify(userRepository).findById(anyString());
        }

        @Test
        @DisplayName("Get user reports should throw UnauthorizedActionException")
        void getUserReports_UnauthorizedActionException() {
            var mockUser = getMockUser();

            when(authentication.getName()).thenReturn(mockUser.getName());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

            assertThrows(UnauthorizedActionException.class,
                    () -> reportService.getUserReports(PageRequest.of(0, 2), mockUser.getId()));

            verify(userRepository).findById(anyString());
            verifyNoInteractions(reportRepository);
        }

        @Test
        @DisplayName("Get user reports should throw UserNotFoundException")
        void getUserReports_UserNotFoundException() {
            var mockUser = getMockUser();

            when(authentication.getName()).thenReturn(mockUser.getName());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> reportService.getUserReports(PageRequest.of(0, 2), mockUser.getId()));

            verify(userRepository).findByEmail(anyString());
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

            when(authentication.getName()).thenReturn(mockUser.getName());
            when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
            when(reportRepository.findById(anyString())).thenReturn(Optional.of(mockReport));

            var result = reportService.getReportById("reportId");

            assertEquals(mockReport, result);
            verify(reportRepository).findById(anyString());
            verify(userRepository).findById(mockUser.getId());
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

            when(authentication.getName()).thenReturn(mockReport.getSender().getEmail());
            when(reportRepository.findById(anyString())).thenReturn(Optional.of(mockReport));
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> reportService.getReportById("reportId"));

            verify(reportRepository).findById(anyString());
            verify(userRepository).findByEmail(anyString());
        }

        @Test
        @DisplayName("Get report by id should throw UnauthorizedActionException")
        void getReportById_UnauthorizedActionException() {
            UserReport mockReport = getMockReport();

            when(authentication.getName()).thenReturn("wrong@mail.com");
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));
            when(reportRepository.findById(anyString())).thenReturn(Optional.of(mockReport));
            when(userRepository.findById(mockReport.getSender().getId())).thenReturn(Optional.of(mockReport.getSender()));

            assertThrows(UnauthorizedActionException.class, () -> reportService.getReportById("reportId"));

            verify(reportRepository).findById(anyString());
            verify(userRepository).findById(mockReport.getSender().getId());
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

            when(authentication.getName()).thenReturn(mockAdmin.getName());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockAdmin));

            when(reportRepository.findAllByReported(any(User.class))).thenReturn(getMockPage().stream().toList());

            when(reportRepository.findById(anyString())).thenReturn(Optional.of(mockReport));
            when(reportStatusRepository.findByName(anyString())).thenReturn(Optional.of(mockReport.getStatus()));

            var result = reportService.reviewReport(request);

            assertEquals("Report reviewed", result);

            verify(userRepository).findByEmail(anyString());
            verify(reportRepository).findById(anyString());
            verify(reportStatusRepository).findByName(anyString());
            verify(reportRepository).saveAll(any());
        }

        @Test
        @DisplayName("Review report by id should throw UserNotFoundException")
        void reviewReport_UserNotFound() {
            User mockAdmin = getMockUser();
            ReviewReportRequest request = getMockReviewRequest();

            when(authentication.getName()).thenReturn(mockAdmin.getName());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> reportService.reviewReport(request));

            verify(userRepository).findByEmail(anyString());
            verifyNoInteractions(reportStatusRepository, reportRepository);
        }


        @Test
        @DisplayName("Review report by id should throw ReportNotFoundException")
        void reviewReport_ReportNotFound() {
            User mockAdmin = getMockUser();

            when(authentication.getName()).thenReturn(mockAdmin.getEmail());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockAdmin));

            when(reportRepository.findById(anyString())).thenReturn(Optional.empty());

            ReviewReportRequest request = getMockReviewRequest();

            assertThrows(ReportNotFoundException.class, () -> reportService.reviewReport(request));

            verify(userRepository).findByEmail(anyString());
            verify(reportRepository).findById(anyString());
            verifyNoInteractions(reportStatusRepository);
        }

        @Test
        @DisplayName("Review report by id should throw ReportStatusNotFoundException")
        void reviewReport_ReportStatusNotFound() {
            User mockAdmin = getMockUser();
            ReviewReportRequest request = getMockReviewRequest();

            when(authentication.getName()).thenReturn(mockAdmin.getName());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockAdmin));

            when(reportRepository.findById(anyString())).thenReturn(Optional.of(getMockReport()));

            assertThrows(ReportStatusNotFoundException.class, () -> reportService.reviewReport(request));

            verify(userRepository).findByEmail(anyString());
            verify(reportRepository).findById(anyString());
            verify(reportStatusRepository).findByName(anyString());
        }
    }

    private UserReport getMockReport() {
        User sender = getMockUser();

        return PostReport.builder()
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
                .userSenderId("mockId")
                .userReportedId("user321")
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
