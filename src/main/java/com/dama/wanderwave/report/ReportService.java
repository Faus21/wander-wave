package com.dama.wanderwave.report;

import com.dama.wanderwave.comment.CommentRepository;
import com.dama.wanderwave.handler.*;
import com.dama.wanderwave.post.PostRepository;
import com.dama.wanderwave.report.comment.CommentReport;
import com.dama.wanderwave.report.general.ReportType;
import com.dama.wanderwave.report.general.ReportTypeRepository;
import com.dama.wanderwave.report.general.UserReport;
import com.dama.wanderwave.report.general.UserReportRepository;
import com.dama.wanderwave.report.post.PostReport;
import com.dama.wanderwave.report.request.FilteredReportPageRequest;
import com.dama.wanderwave.report.request.ReviewReportRequest;
import com.dama.wanderwave.report.request.SendReportRequest;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReportService {

    private final UserReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReportTypeRepository typeRepository;

    @Transactional
    public String sendReport(SendReportRequest request) {
        User sender = getAuthenticatedSender(request.getUserSenderId());

        ReportType reportType = typeRepository.findByName(request.getReportType())
                .orElseThrow(() -> new ReportTypeNotFoundException("Report type not found with name: " + request.getReportType()));

        UserReport report = createReportByType(request);

        report.setType(reportType);
        report.setDescription(request.getDescription());

        reportRepository.save(report);
        log.info("Report created successfully for user: {}", sender.getId());
        return "Report created successfully";
    }

    public Page<UserReport> getUserReports(Pageable pageRequest, String userId) {
        User authenticatedUser = getAuthenticatedUser();
        User user = verifyUserAccess(authenticatedUser, userId);

        Page<UserReport> reports = reportRepository.findAllBySender(PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize()), user);
        isReportsEmpty(reports, "No reports found for user ID: " + user.getId());

        log.info("Successfully fetched {} reports for user ID '{}'.", reports.getNumberOfElements(), user.getId());
        return reports;
    }

    public UserReport getReportById(String reportId) {
        UserReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found with ID: " + reportId));

        verifyUserAccess(getAuthenticatedUser(), report.getSender().getId());

        log.info("Successfully fetched report for report ID '{}'.", reportId);
        return report;
    }

    public Page<UserReport> getAllReports(Pageable page, FilteredReportPageRequest filter) {
        if (isFilterEmpty(filter)) {
            return reportRepository.findAll(page);
        }
        Page<UserReport> reports = filterReports(page, filter);
        isReportsEmpty(reports, "No reports found with the provided filters: " + filter);
        return reports;
    }

    @Transactional
    public String reviewReport(ReviewReportRequest request) {
        User admin = getAuthenticatedUser();
        UserReport report = reportRepository.findById(request.getReportId())
                .orElseThrow(() -> new ReportNotFoundException("Report not found with ID: " + request.getReportId()));

        List<? extends UserReport> relatedReports = findRelatedReports(report);
        relatedReports.forEach(rep -> {
            rep.setReviewedBy(admin);
            rep.setReviewedAt(LocalDateTime.now());
            rep.setReportComment(request.getComment());
        });

        reportRepository.saveAll(relatedReports);
        log.info("Report with ID '{}' reviewed successfully by admin '{}'.", request.getReportId(), admin.getId());

        return "Report reviewed";
    }

    private User getAuthenticatedSender(String senderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedEmail = authentication.getName();

        return userRepository.findById(senderId)
                .filter(user -> user.getEmail().equals(authenticatedEmail))
                .orElseThrow(() -> {
                    log.warn("Unauthorized attempt by user {} to send a report on behalf of user {}", authenticatedEmail, senderId);
                    return new UnauthorizedActionException("Unauthorized action.");
                });
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private User verifyUserAccess(User authenticatedUser, String userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getEmail().equals(authenticatedUser.getEmail()))
                .orElseThrow(() -> new UnauthorizedActionException("Unauthorized access to user data"));
    }

    private UserReport createReportByType(SendReportRequest request) {
        var reportedUserOptional = userRepository.findById(request.getUserReportedId());

        if (reportedUserOptional.isEmpty()) {
            log.warn("User with ID {} not found", request.getObjectId());
            throw new UserNotFoundException("User not found");
        }

        var reportedUser = reportedUserOptional.get();

        return switch (request.getObjectType()) {
            case COMMENT -> buildReport(commentRepository.findById(request.getObjectId()),
                    CommentNotFoundException.class,
                    comment -> CommentReport.builder().comment(comment).reported(reportedUser).build());
            case POST -> buildReport(postRepository.findById(request.getObjectId()),
                    PostNotFoundException.class,
                    post -> PostReport.builder().post(post).reported(reportedUser).build());
            case USER -> UserReport.builder().reported(reportedUser).build();
        };

    }

    private <T, E extends UserReport> E buildReport(Optional<T> entity, Class<? extends RuntimeException> exceptionClass, Function<T, E> reportBuilder) {
        return entity.map(reportBuilder).orElseThrow(() -> createException(exceptionClass));
    }

    private RuntimeException createException(Class<? extends RuntimeException> exceptionClass) {
        try {
            return exceptionClass.getConstructor(String.class).newInstance("Entity not found");
        } catch (Exception e) {
            throw new RuntimeException("Error creating exception", e);
        }
    }

    private boolean isFilterEmpty(FilteredReportPageRequest filter) {
        return filter == null || (filter.getFrom() == null && filter.getOn() == null && filter.getAdmins() == null && filter.getCategory() == null && filter.getStartDate() == null && filter.getEndDate() == null);
    }

    private void isReportsEmpty(Page<UserReport> reports, String message) {
        if (reports.isEmpty()) {
            log.warn(message);
            throw new ReportNotFoundException(message);
        }
    }

    private List<? extends UserReport> findRelatedReports(UserReport report) {
        var reports = reportRepository.findAllByReported(report.getReported());
        if (reports.isEmpty()) {
            log.error("No reports found for report ID: {}", report.getId());
            throw new ReportNotFoundException("No reports found for report ID: " + report.getId());
        }
        return reports;
    }


    private Page<UserReport> filterReports(Pageable pageable, FilteredReportPageRequest filter) {
        return reportRepository.findAll((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            addFilterPredicates(filter, predicates, criteriaBuilder, root);
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    private void addFilterPredicates(FilteredReportPageRequest filter, List<Predicate> predicates, CriteriaBuilder cb, Root<UserReport> root) {
        if (filter.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate()));
        }
        if (filter.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate()));
        }

        if (filter.getCategory() != null) {
            predicates.add(cb.equal(root.get("type").get("name"), filter.getCategory()));
        }

        if (filter.getStatus() != null) {
            predicates.add(cb.equal(root.get("status").get("name"), filter.getCategory()));
        }

        if (filter.getAdmins() != null && !filter.getAdmins().isEmpty()) {
            predicates.add(root.get("reviewedBy").get("id").in(filter.getAdmins()));
        }

        if (filter.getFrom() != null) {
            predicates.add(root.get("sender").get("id").in(filter.getFrom()));
        }

        if (filter.getOn() != null) {
            predicates.add(root.get("reported").get("id").in(filter.getOn()));
        }
    }

}
