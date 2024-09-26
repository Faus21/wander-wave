package com.dama.wanderwave.report;

import com.dama.wanderwave.auth.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Tag(name = "Report", description = "Endpoints for reports")
public class ReportController {

    private final ReportService service;

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Send a report", description = "Sends a report regarding a user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User reported successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Authenticated user is not authorized to perform this action"),
            @ApiResponse(responseCode = "404", description = "Request parameters not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResponseRecord> sendReport(@RequestBody @Valid SendReportRequest request) {
        String message = service.sendReport(request);
        return ResponseEntity.ok().body(new ResponseRecord(200, message));
    }

    @GetMapping("/types")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get report types", description = "Get all report types.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All report types retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Report types not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResponseRecord> getReportTypes() {
        var types = service.getReportTypes();
        return ResponseEntity.ok().body(new ResponseRecord(200, types));
    }

    @GetMapping("/find-by-dates")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get reports by date range", description = "Retrieve reports based on a date range.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reports retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Reports not found for the specified date range"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResponseRecord> getReportsByDates(@RequestBody @Valid ReportsByDatesRequest request) {
        var reports = service.getReportsByDate(request);
        return ResponseEntity.ok().body(new ResponseRecord(200, reports));
    }

    @GetMapping("/get/{userId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get user reports", description = "Get all reports for a specific user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All reports retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Authenticated user is not authorized to perform this action"),
            @ApiResponse(responseCode = "404", description = "User not found or no reports available"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResponseRecord> getUserReports(@PathVariable String userId) {
        var reports = service.getUserReports(userId);
        return ResponseEntity.ok().body(new ResponseRecord(200, reports));
    }

    @PostMapping("/review")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Review a report", description = "Review a specific report by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report reviewed successfully"),
            @ApiResponse(responseCode = "404", description = "Report not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResponseRecord> reviewReport(@RequestBody @Valid ReviewReportRequest request) {
        var message = service.reviewReport(request);
        return ResponseEntity.ok().body(new ResponseRecord(200, message));
    }


}