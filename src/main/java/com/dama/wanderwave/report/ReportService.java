package com.dama.wanderwave.report;

import com.dama.wanderwave.comment.CommentRepository;
import com.dama.wanderwave.handler.comment.CommentNotFoundException;
import com.dama.wanderwave.handler.post.PostNotFoundException;
import com.dama.wanderwave.handler.report.DuplicateReportException;
import com.dama.wanderwave.handler.report.ReportNotFoundException;
import com.dama.wanderwave.handler.report.ReportStatusNotFoundException;
import com.dama.wanderwave.handler.report.ReportTypeNotFoundException;
import com.dama.wanderwave.post.PostRepository;
import com.dama.wanderwave.report.comment.CommentReport;
import com.dama.wanderwave.report.general.*;
import com.dama.wanderwave.report.post.PostReport;
import com.dama.wanderwave.report.request.FilteredReportPageRequest;
import com.dama.wanderwave.report.request.ReviewReportRequest;
import com.dama.wanderwave.report.request.SendReportRequest;
import com.dama.wanderwave.report.response.ReportResponse;
import com.dama.wanderwave.user.BlackList;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import com.dama.wanderwave.user.UserService;
import com.dama.wanderwave.user.response.UserResponse;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
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
    private final ModelMapper modelMapper;

    @Transactional
    public String sendReport(SendReportRequest request) {
        User sender = userService.getAuthenticatedUser();

        reportRepository.findUserReportByObjectAndSender(request.getObjectId(), sender.getId())
                .ifPresent(
                        existingReport -> {
                            log.warn("User '{}' sent duplicate report on object ID '{}'", sender.getId(), request.getObjectId());
                            throw new DuplicateReportException("Report on object from that user already existing in system");
                        }
                );

        UserReport report = createReportByType(request);

        report.setType(typeRepository.findByName(request.getReportType()).orElseThrow(
                () -> new ReportTypeNotFoundException("Report type not found with name: " + request.getReportType()))
        );
        report.setDescription(request.getDescription() == null ? "" : request.getDescription());
        report.setStatus(getDefaultStatus());
        report.setSender(sender);

        if (request.getUserReportedId() != null) {
            User reported = userService.findUserByNicknameOrThrow(request.getUserReportedId());
            if (sender.getBlackList() == null) {
                sender.setBlackList(new BlackList(new HashSet<>()));
            }
            sender.getBlackList().userIds().add(reported.getId());
            userRepository.save(sender);
        }

        reportRepository.save(report);
        log.info("Report created successfully for user: {}", sender.getId());
        return "Report created successfully";
    }

    public Page<ReportResponse> getUserReports(Pageable pageRequest, String userId) {
        User authenticatedUser = userService.getAuthenticatedUser();
        userService.checkUserAccessRights(authenticatedUser, userId);

        Page<UserReport> reports = reportRepository.findAllBySender(PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize()), authenticatedUser);
        isReportsEmpty(reports, "No reports found for user ID: " + userId);

        log.info("Successfully fetched {} reports for user ID '{}'.", reports.getNumberOfElements(), userId);
        return reports.map(this::userReportToReportResponse);
    }

    public ReportResponse userReportToReportResponse(UserReport report) {
        if (report == null) {
            return null;
        }

        ReportResponse.ReportResponseBuilder responseBuilder = ReportResponse.builder()
                .id(report.getId())
                .description(report.getDescription())
                .sender(report.getSender() != null ? modelMapper.map(report.getSender(), UserResponse.class) : null)
                .reported(report.getReported() != null ? modelMapper.map(report.getReported(), UserResponse.class) : null)
                .reportType(report.getType() != null ? report.getType().getName() : null)
                .reportStatus(report.getStatus() != null ? report.getStatus().getName() : null)
                .createdAt(report.getCreatedAt())
                .reviewedAt(report.getReviewedAt())
                .reviewedBy(report.getReviewedBy() != null ? modelMapper.map(report.getReviewedBy(), UserResponse.class) : null)
                .reportComment(report.getReportComment());

        if (report instanceof PostReport postReport) {
            responseBuilder.objectId(postReport.getPost().getId())
                    .objectType("POST");
        } else if (report instanceof CommentReport commentReport) {
            responseBuilder.objectId(commentReport.getComment().getId())
                    .objectType("COMMENT");
        } else {
            responseBuilder.objectId(report.getReported().getId());
        }

        return responseBuilder.build();
    }

    public ReportResponse getReportById(String reportId) {
        UserReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found with ID: " + reportId));

        userService.checkUserAccessRights(userService.getAuthenticatedUser(), report.getSender().getId());

        log.info("Successfully fetched report for report ID '{}'.", reportId);
        return userReportToReportResponse(report);
    }

    public Page<ReportResponse> getAllReports(Pageable page, FilteredReportPageRequest filter) {
        log.info("Entering getAllReports method. Page: {}, Filter: {}", page, filter);

        boolean isEmptyFilter = isFilterEmpty(filter);
        log.info("Filter is empty: {}", isEmptyFilter);

        Page<UserReport> reports = isEmptyFilter
                ? reportRepository.findAll(page)
                : filterReports(page, filter);

        log.info("Retrieved {} reports.", reports.getTotalElements());

        if (reports.isEmpty()) {
            String errorMessage = isEmptyFilter
                    ? "No reports found"
                    : "No reports found with the provided filters: " + filter;
            log.warn(errorMessage);
            throw new ReportNotFoundException(errorMessage);
        }

        Page<ReportResponse> reportResponses = reports.map(this::userReportToReportResponse);
        log.info("Successfully mapped {} reports to ReportResponse.", reportResponses.getTotalElements());
        return reportResponses;
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

    private UserReport createReportByType(SendReportRequest request) {
        return switch (request.getObjectType()) {
            case COMMENT -> {
                User reportedUser = commentRepository.findById(request.getObjectId())
                        .orElseThrow(
                                () -> new CommentNotFoundException("Comment not found with id: " + request.getObjectId())
                        )
                        .getUser();

                yield buildReport(commentRepository.findById(request.getObjectId()),
                        CommentNotFoundException.class,
                        comment -> CommentReport.builder().comment(comment).reported(reportedUser).build());
            }
            case POST -> {
                User reportedUser = postRepository.findById(request.getObjectId())
                        .orElseThrow(
                                () -> new PostNotFoundException("Post not found with id: " + request.getObjectId())
                        )
                        .getUser();

                yield buildReport(postRepository.findById(request.getObjectId()),
                        PostNotFoundException.class,
                        post -> PostReport.builder().post(post).reported(reportedUser).build());
            }
            case USER -> {
                User user = userService.findUserByIdOrThrow(request.getObjectId());
                yield UserReport.builder().reported(user).build();
            }
        };

    }

    private <T, E extends UserReport> E buildReport(Optional<T> entity, Class<? extends
            RuntimeException> exceptionClass, Function<T, E> reportBuilder) {
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


    private void addFilterPredicates(FilteredReportPageRequest
                                             filter, List<Predicate> predicates, CriteriaBuilder cb, Root<UserReport> root) {
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
