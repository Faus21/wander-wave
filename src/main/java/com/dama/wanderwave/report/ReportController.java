package com.dama.wanderwave.report;

import com.dama.wanderwave.auth.*;
import com.dama.wanderwave.report.request.FilteredReportPageRequest;
import com.dama.wanderwave.report.request.ReviewReportRequest;
import com.dama.wanderwave.report.request.SendReportRequest;
import com.dama.wanderwave.utils.ResponseRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Tag(name = "Report", description = "Endpoints for reports")
@Validated
public class ReportController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReportService service;

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Send a report", description = "Sends a report regarding a user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User reported successfully", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Forbidden: Authenticated user is not authorized to perform this action", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Request parameters not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> sendReport( @RequestBody @Valid SendReportRequest request) {
        String message = service.sendReport(request);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), message));
    }

    @GetMapping("/get")
    @Operation(summary = "Get reports", description = "Retrieve reports.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reports retrieved successfully", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Reports not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getAllReports(
            @RequestParam int pageNumber,
            @RequestParam @Max(MAX_PAGE_SIZE) Integer pageSize,
            @RequestBody(required = false) @Valid FilteredReportPageRequest request) {
        Pageable page = PageRequest.of(pageNumber, pageSize);
        var reports = service.getAllReports(page, request);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), reports));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user reports", description = "Get all reports for a specific user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All reports retrieved successfully", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Forbidden: Authenticated user is not authorized to perform this action", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found or no reports available", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getUserReports(@RequestParam int pageNumber,
                                                         @RequestParam @Max(MAX_PAGE_SIZE) int pageSize,
                                                         @PathVariable String userId) {
        Pageable page = PageRequest.of(pageNumber, pageSize);
        var reports = service.getUserReports(page, userId);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), reports));
    }

    @PostMapping("/review")
    @Operation(summary = "Review a report", description = "Review a specific report by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report reviewed successfully", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Report or admin not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> reviewReport(@RequestBody @Valid ReviewReportRequest request) {
        var message = service.reviewReport(request);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), message));
    }

    @GetMapping("/get/{reportId}")
    @Operation(summary = "Returns report by id", description = "Get a specific report by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report retrieved successfully", content = @Content()),
            @ApiResponse(responseCode = "403", description = "Forbidden: Authenticated user is not authorized to perform this action", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Report not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> getReportById(@PathVariable String reportId) {
        var message = service.getReportById(reportId);
        return ResponseEntity.ok().body(new ResponseRecord(HttpStatus.OK.value(), message));
    }


}