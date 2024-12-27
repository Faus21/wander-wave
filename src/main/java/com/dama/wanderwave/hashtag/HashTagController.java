package com.dama.wanderwave.hashtag;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hashtags")
@RequiredArgsConstructor
@Tag(name = "Hashtags", description = "Endpoints for managing and retrieving hashtags")
public class HashTagController {

    private final static int MAX_PAGE_SIZE = 50;
    private final HashTagService hashTagService;

    @GetMapping
    @Operation(summary = "Retrieve all hashtags", description = "Retrieves all hashtags in a paginated format.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hashtags retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<HashTag>> getAllHashTags(
            @RequestParam @Max(MAX_PAGE_SIZE) Integer page,
            @RequestParam int size) {
        Page<HashTag> hashTags = hashTagService.getAllHashTags(PageRequest.of(page, size));
        return new ResponseEntity<>(hashTags, HttpStatus.OK);
    }

    @GetMapping("/search")
    @Operation(summary = "Retrieve hashtags by prefix", description = "Retrieves hashtags that start with a specific prefix.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hashtags retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<HashTag>> getHashTagsByPrefix(
            @RequestParam String prefix,
            @RequestParam @Max(MAX_PAGE_SIZE) Integer page,
            @RequestParam int size) {
        Page<HashTag> hashTags = hashTagService.getHashTagsByPrefix(prefix, PageRequest.of(page, size));
        return new ResponseEntity<>(hashTags, HttpStatus.OK);
    }
}