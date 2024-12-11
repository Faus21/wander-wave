package com.dama.wanderwave.categoryType;

import com.dama.wanderwave.utils.ResponseRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category-types")
@RequiredArgsConstructor
public class CategoryTypeController {

    private final CategoryTypeService categoryTypeService;

    @Operation(summary = "Get CategoryType by ID", description = "Fetch a specific CategoryType by its unique ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "CategoryType found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryTypeResponse.class))), @ApiResponse(responseCode = "404", description = "CategoryType not found", content = @Content)})
    @GetMapping("/{id}")
    public ResponseEntity<ResponseRecord> getCategoryTypeById( @PathVariable String id ) {
        CategoryTypeResponse response = categoryTypeService.getCategoryTypeById(id);
        ResponseRecord responseRecord = new ResponseRecord(200, response);
        return ResponseEntity.ok(responseRecord);
    }

    @Operation(summary = "Get all CategoryTypes", description = "Fetch all available CategoryTypes")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "List of CategoryTypes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryTypeResponse.class)))})
    @GetMapping
    public ResponseEntity<ResponseRecord> getAllCategoryTypes() {
        List<CategoryTypeResponse> responses = categoryTypeService.getAllCategoryTypes();
        ResponseRecord responseRecord = new ResponseRecord(200, responses);
        return ResponseEntity.ok(responseRecord);
    }
}