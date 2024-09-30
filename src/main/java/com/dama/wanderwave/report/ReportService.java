package com.dama.wanderwave.report;

import com.dama.wanderwave.comment.CommentRepository;
import com.dama.wanderwave.handler.*;
import com.dama.wanderwave.post.PostRepository;
import com.dama.wanderwave.report.entity.*;
import com.dama.wanderwave.report.repository.*;
import com.dama.wanderwave.report.request.FilteredReportPageRequest;
import com.dama.wanderwave.report.request.ReviewReportRequest;
import com.dama.wanderwave.report.request.SendReportRequest;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
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

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReportService {

    private final ReportRepository reportRepository;
    private final CommentReportRepository commentReportRepository;
    private final PostReportRepository postReportRepository;
    private final UserReportRepository userReportRepository;

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ReportTypeRepository typeRepository;

    @Transactional
    public String sendReport(SendReportRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String authenticatedEmail = authentication.getName();
        User sender = userRepository.findById(request.getUserSenderId())
                .orElseThrow(() -> {
                    log.error("Sender user not found with ID: {}", request.getUserSenderId());
                    return new UserNotFoundException("Sender user not found with ID: " + request.getUserSenderId());
                });

        log.info("Sender user found with ID: {}", sender.getId());

        if (!authenticatedEmail.equals(sender.getEmail())) {
            log.warn("Unauthorized attempt by user {} to send a report on behalf of user {}", authenticatedEmail, sender.getEmail());
            throw new UnauthorizedActionException("You are not authorized to send reports on behalf of this user.");
        }

        ReportType reportType = typeRepository.findByName(request.getReportType())
                .orElseThrow(() -> {
                    log.error("Report type not found with name: {}", request.getReportType());
                    return new ReportTypeNotFoundException("Report type not found with name: " + request.getReportType());
                });

        log.info("Report type found: {}", reportType.getName());

        Report report;

        switch (request.getObjectType()) {
            case "comment" -> {
                var comment = commentRepository.findById(request.getObjectId())
                        .orElseThrow(() -> {
                            log.error("Comment not found with ID: {}", request.getObjectId());
                            return new CommentNotFoundException("Comment not found with ID: " + request.getObjectId());
                        });
                log.info("Reported comment found with ID: {}", comment.getId());
                report = CommentReport.builder()
                        .comment(comment)
                        .build();
            }
            case "user" -> {
                var reportedUser = userRepository.findById(request.getUserReportedId())
                        .orElseThrow(() -> {
                            log.error("Reported user not found with ID: {}", request.getUserReportedId());
                            return new UserNotFoundException("Reported user not found with ID: " + request.getUserReportedId());
                        });
                log.info("Reported user found with ID: {}", reportedUser.getId());
                report = UserReport.builder()
                        .sender(reportedUser)
                        .build();
            }
            case "post" -> {
                var post = postRepository.findById(request.getObjectId())
                        .orElseThrow(() -> {
                            log.error("Post not found with ID: {}", request.getObjectId());
                            return new PostNotFoundException("Post not found with ID: " + request.getObjectId());
                        });
                log.info("Reported post found with ID: {}", post.getId());
                report = PostReport.builder()
                        .post(post)
                        .build();
            }
            default -> throw new WrongReportObjectException("Wrong report object! Should be comment/user/post");
        }


        log.debug("Creating new report with description: {}", request.getDescription());

        report.setSender(sender);
        report.setType(reportType);
        report.setDescription(request.getDescription());

        reportRepository.save(report);
        log.info("Report created successfully for user: {}", sender.getId());
        return "Report created successfully";
    }


    public Page<Report> getUserReports(Pageable pageRequest, String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("User '{}' is attempting to fetch reports for user ID '{}'.", authentication.getName(), userId);

        var userOptional = userRepository.findById(userId);

        if (userOptional.isEmpty()) {
            log.error("User not found with ID: {}", userId);
            throw new UserNotFoundException("User not found with ID: " + userId);
        }

        var user = userOptional.get();

        if (!user.getEmail().equals(authentication.getName())) {
            log.warn("Unauthorized access attempt by user '{}' to fetch reports of user ID '{}'.", authentication.getName(), userId);
            throw new UnauthorizedActionException("You are not authorized to get reports of that user.");
        }

        var reports = reportRepository
                .findAllBySender(PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize()), user);

        if (reports.isEmpty()) {
            log.warn("No reports found for user ID: {}", user.getId());
            throw new ReportNotFoundException("Reports not found for user with ID: " + user.getId());
        }

        log.info("Successfully fetched {} reports for user ID '{}'.", reports.getNumberOfElements(), user.getId());
        return reports;
    }

    public Report getReportById(String reportId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("User '{}' is attempting to fetch report for report ID '{}'.", authentication.getName(), reportId);

        var report = reportRepository.findById(reportId);

        if (report.isEmpty()) {
            log.warn("No report found for report ID: {}", reportId);
            throw new ReportNotFoundException("Report not found for report ID: " + reportId);
        }

        var userOptional = userRepository.findById(report.get().getSender().getId());

        if (userOptional.isEmpty()) {
            log.error("Sender not found with ID: {}", report.get().getSender().getId());
            throw new UserNotFoundException("Sender not found with ID: " + report.get().getSender().getId());
        }

        var user = userOptional.get();

        if (!user.getEmail().equals(authentication.getName())) {
            log.warn("Unauthorized access attempt by user '{}' to fetch report with ID '{}'.", authentication.getName(), reportId);
            throw new UnauthorizedActionException("You are not authorized to get reports of that user.");
        }


        log.info("Successfully fetched report for report ID '{}'.", report);
        return report.get();
    }

    public Page<Report> getAllReports(Pageable page, FilteredReportPageRequest filter) {
        log.info("Fetching reports: page = {}, size = {}", page.getPageNumber(), page.getPageSize());

        if (filter == null || (filter.getFrom() == null && filter.getOn() == null && filter.getAdmin() == null &&
                filter.getCategory() == null && filter.getIsReviewed() == null && filter.getStartDate() == null &&
                filter.getEndDate() == null)) {
            var reports = reportRepository.findAll(page);

            if (reports.isEmpty()) {
                log.warn("No reports found: page = {}, size = {}", page.getPageNumber(), page.getPageSize());
                throw new ReportNotFoundException("Reports not found");
            }

            log.info("Found {} reports: page = {}, size = {}", reports.getNumberOfElements(), page.getPageNumber(), page.getPageSize());
            return reports;
        }

        var reports = filterReports(page, filter);

        if (reports.isEmpty()) {
            log.warn("No reports found with the provided filters: {}", filter);
            throw new ReportNotFoundException("Reports not found with the provided filters");
        }

        log.info("Found {} reports with the provided filters: {}", reports.getNumberOfElements(), filter);
        return reports;
    }


    public List<ReportType> getReportTypes() {
        log.info("Fetching all report types");

        var types = typeRepository.findAll();

        if (types.isEmpty()) {
            log.warn("No report types found");
            throw new ReportTypeNotFoundException("Report types not found");
        }

        log.info("Found {} report types", types.size());
        return types;
    }

    @Transactional
    public String reviewReport(ReviewReportRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("User '{}' is attempting to review report with ID '{}'.", authentication.getName(), request.getReportId());

        var admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException("Admin not found"));

        var report = reportRepository.findById(request.getReportId())
                .orElseThrow(() -> {
                    log.error("Report not found with ID: {}", request.getReportId());
                    return new ReportNotFoundException("Report not found with ID: " + request.getReportId());
                });

        report.setReviewedAt(LocalDateTime.now());
        report.setReviewedBy(admin);
        report.setReportComment(request.getComment());

        List<? extends Report> reports = findRelatedReports(report);

        if (reports == null) {
            log.error("No reports found for report ID: {}", request.getReportId());
            throw new ReportNotFoundException("No reports found for report ID: " + request.getReportId());
        }

        reports.forEach(rep -> {
            rep.setReviewedBy(admin);
            rep.setReviewedAt(LocalDateTime.now());
            rep.setReportComment(request.getComment());
        });

        reportRepository.saveAll(reports);
        log.info("Report with ID '{}' reviewed successfully by admin '{}'.", request.getReportId(), admin.getId());

        return "Report reviewed";
    }

    private List<? extends Report> findRelatedReports(Report report) {
        switch (report) {
            case CommentReport commentReport -> {
                var comment = commentReport.getComment();
                return commentReportRepository.findAllByComment(comment);
            }
            case PostReport postReport -> {
                var post = postReport.getPost();
                return postReportRepository.findAllByPost(post);
            }
            case UserReport userReport -> {
                var user = userReport.getUser();
                return userReportRepository.findAllByUser(user);
            }
            default -> {
                return null;
            }
        }
    }

    private Page<Report> filterReports(Pageable pageable, FilteredReportPageRequest filter) {
        return reportRepository.findAll((root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();

            if (filter.getFrom() != null) {
                predicates.add(criteriaBuilder.equal(root.get("sender").get("id"), filter.getFrom()));
            }
            if (filter.getOn() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type").get("id"), filter.getOn()));
            }
            if (filter.getAdmin() != null) {
                predicates.add(criteriaBuilder.equal(root.get("reviewedBy").get("id"), filter.getAdmin()));
            }
            if (filter.getCategory() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), filter.getCategory()));
            }
            if (filter.getIsReviewed() != null) {
                if (filter.getIsReviewed()) {
                    predicates.add(criteriaBuilder.isNotNull(root.get("reviewedAt")));
                } else {
                    predicates.add(criteriaBuilder.isNull(root.get("reviewedAt")));
                }
            }
            if (filter.getStartDate() != null && filter.getEndDate() != null) {
                predicates.add(criteriaBuilder.between(root.get("createdAt"), filter.getStartDate(), filter.getEndDate()));
            } else if (filter.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate()));
            } else if (filter.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

}
