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
import com.dama.wanderwave.report.comment.CommentReport;
import com.dama.wanderwave.report.general.*;
import com.dama.wanderwave.report.post.PostReport;
import com.dama.wanderwave.report.request.FilteredReportPageRequest;
import com.dama.wanderwave.report.request.ReviewReportRequest;
import com.dama.wanderwave.report.request.SendReportRequest;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import com.dama.wanderwave.user.UserService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
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
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("SameParameterValue")
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReportService {

    private static final String DEFAULT_REPORT_STATUS = "IN_PROGRESS";

    private final UserReportRepository reportRepository;
    private final ReportStatusRepository statusRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReportTypeRepository typeRepository;
    private final UserService userService;

    @Transactional
    public String sendReport(SendReportRequest request) {
        User sender = getAuthenticatedSender(request.getUserSenderId());

        var existing = reportRepository.findUserReportByObjectAndSender(request.getObjectId(),
                request.getUserSenderId());

        existing.ifPresent(existingReport -> {
            log.warn("User '{}' sent duplicate report on object ID '{}'", sender.getId(), request.getObjectId());
            throw new DuplicateReportException("Report on object from that user already existing in system");
        });

        UserReport report = createReportByType(request);

        report.setType(typeRepository.findByName(request.getReportType()).orElseThrow(
                () -> new ReportTypeNotFoundException("Report type not found with name: " + request.getReportType()))
        );
        report.setDescription(request.getDescription());
        report.setStatus(getDefaultStatus());

        reportRepository.save(report);
        log.info("Report created successfully for user: {}", sender.getId());
        return "Report created successfully";
    }

    public Page<UserReport> getUserReports(Pageable pageRequest, String userId) {
        User authenticatedUser = userService.getAuthenticatedUser();
        User user = userService.verifyUserAccess(authenticatedUser, userId);

        Page<UserReport> reports = reportRepository.findAllBySender(PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize()), user);
        isReportsEmpty(reports, "No reports found for user ID: " + user.getId());

        log.info("Successfully fetched {} reports for user ID '{}'.", reports.getNumberOfElements(), user.getId());
        return reports;
    }

    public UserReport getReportById(String reportId) {
        UserReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found with ID: " + reportId));

        userService.verifyUserAccess(userService.getAuthenticatedUser(), report.getSender().getId());

        log.info("Successfully fetched report for report ID '{}'.", reportId);
        return report;
    }

    public Page<UserReport> getAllReports(Pageable page, FilteredReportPageRequest filter) {
        if (isFilterEmpty(filter)) {
            var reports = reportRepository.findAll(page);
            isReportsEmpty(reports, "No reports found");
            return reports;
        }
        Page<UserReport> reports = filterReports(page, filter);
        isReportsEmpty(reports, "No reports found with the provided filters: " + filter);
        return reports;
    }

    @Transactional
    public String reviewReport(ReviewReportRequest request) {
        User admin = userService.getAuthenticatedUser();
        UserReport report = reportRepository.findById(request.getReportId())
                .orElseThrow(() -> new ReportNotFoundException("Report not found with ID: " + request.getReportId()));

        ReportStatus status = statusRepository.findByName(request.getStatus()).orElseThrow(
                () -> new ReportStatusNotFoundException("Report status not found with name: " + request.getStatus()));

        List<? extends UserReport> relatedReports = findRelatedReports(report);
        relatedReports.forEach(rep -> {
            rep.setReviewedBy(admin);
            rep.setReviewedAt(LocalDateTime.now());
            rep.setReportComment(request.getComment());
            rep.setStatus(status);
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

    private UserReport createReportByType(SendReportRequest request) {
        var reportedUser = userRepository.findById(request.getUserReportedId()).orElseThrow(
                () -> {
                    log.warn("User with ID {} not found", request.getObjectId());
                    return new UserNotFoundException("User not found");
                });

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
        addDatePredicate(filter.getStartDate(), "createdAt", cb::greaterThanOrEqualTo, predicates, root);
        addDatePredicate(filter.getEndDate(), "createdAt", cb::lessThanOrEqualTo, predicates, root);

        addEqualPredicate(filter.getCategory(), "type", "name", predicates, root, cb);
        addEqualPredicate(filter.getStatus(), "status", "name", predicates, root, cb);

        addInPredicate(filter.getAdmins(), "reviewedBy", "id", predicates, root);
        addInPredicate(filter.getFrom(), "sender", "id", predicates, root);
        addInPredicate(filter.getOn(), "reported", "id", predicates, root);
    }

    private void addDatePredicate(LocalDateTime date, String field,
                                  BiFunction<Path<LocalDateTime>, LocalDateTime, Predicate> predicateFunction,
                                  List<Predicate> predicates, Root<UserReport> root) {
        if (date != null) {
            predicates.add(predicateFunction.apply(root.get(field), date));
        }
    }

    private void addEqualPredicate(String value, String entity, String field,
                                   List<Predicate> predicates, Root<UserReport> root, CriteriaBuilder cb) {
        if (value != null) {
            predicates.add(cb.equal(root.get(entity).get(field), value));
        }
    }

    private void addInPredicate(List<String> values, String entity, String field,
                                List<Predicate> predicates, Root<UserReport> root) {
        if (values != null && !values.isEmpty()) {
            predicates.add(root.get(entity).get(field).in(values));
        }
    }

    private ReportStatus getDefaultStatus() {
        return statusRepository.findByName(DEFAULT_REPORT_STATUS).orElseThrow(
                () -> new ReportStatusNotFoundException("Default status not found with name: " + DEFAULT_REPORT_STATUS));
    }
}
