package com.dama.wanderwave.report;

import com.dama.wanderwave.handler.*;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import com.dama.wanderwave.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ReportTypeRepository typeRepository;
    private final Utils utils;

    @Transactional
    public String sendReport(SendReportRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        utils.checkUserBan(authentication);

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

        User reported = userRepository.findById(request.getUserReportedId())
                .orElseThrow(() -> {
                    log.error("Reported user not found with ID: {}", request.getUserReportedId());
                    return new UserNotFoundException("Reported user not found with ID: " + request.getUserReportedId());
                });

        log.info("Reported user found with ID: {}", reported.getId());

        ReportType reportType = typeRepository.findByName(request.getReportType())
                .orElseThrow(() -> {
                    log.error("Report type not found with name: {}", request.getReportType());
                    return new ReportTypeNotFoundException("Report type not found with name: " + request.getReportType());
                });

        log.info("Report type found: {}", reportType.getName());

        // TODO: Add objectId check
        log.debug("Creating new report with description: {}", request.getDescription());

        Report report = Report.builder()
                .sender(sender)
                .reportedUser(reported)
                .type(reportType)
                .description(request.getDescription())
                .objectId(request.getObjectId())
                .build();

        reportRepository.save(report);
        log.info("Report created successfully for user: {}", sender.getId());
        return "Report created successfully";
    }


    public List<Report> getUserReports(String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        utils.checkUserBan(authentication);

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

        var reports = reportRepository.findAllBySender(user);

        if (reports.isEmpty()) {
            log.warn("No reports found for user ID: {}", user.getId());
            throw new ReportNotFoundException("Reports not found for user with ID: " + user.getId());
        }

        log.info("Successfully fetched {} reports for user ID '{}'.", reports.size(), user.getId());
        return reports;
    }

    public List<Report> getReportsByDate(ReportsByDatesRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        utils.checkUserBan(authentication);

        log.info("Fetching reports between dates: start = {}, end = {}", request.getStartDate(), request.getEndDate());

        var reports = reportRepository.findByDates(request.getStartDate(), request.getEndDate());

        if (reports.isEmpty()) {
            log.warn("No reports found between dates: start = {}, end = {}", request.getStartDate(), request.getEndDate());
            throw new ReportNotFoundException("Reports by that dates not found");
        }

        log.info("Found {} reports between dates: start = {}, end = {}", reports.size(), request.getStartDate(), request.getEndDate());
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

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Transactional
    public String reviewReport(ReviewReportRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        utils.checkUserBan(authentication);

        log.info("User '{}' is attempting to review report with ID '{}'.", authentication.getName(), request.getReportId());

        var admin = userRepository.findByEmail(authentication.getName()).get();

        var reportOptional = reportRepository.findById(request.getReportId());

        if (reportOptional.isEmpty()) {
            log.error("Report not found with ID: {}", request.getReportId());
            throw new ReportNotFoundException("Report not found with ID: " + request.getReportId());
        }

        var report = reportOptional.get();
        report.setReviewedAt(LocalDateTime.now());
        report.setReviewedBy(admin);
        report.setComment(request.getComment());

        reportRepository.save(report);
        log.info("Report with ID '{}' reviewed successfully by admin '{}'.", request.getReportId(), admin.getId());

        return "Report reviewed";
    }


}
