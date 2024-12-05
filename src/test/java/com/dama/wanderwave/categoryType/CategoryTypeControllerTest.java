package com.dama.wanderwave.categoryType;

import com.dama.wanderwave.handler.GlobalExceptionHandler;
import com.dama.wanderwave.handler.category_type.CategoryTypeNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryTypeController Tests")
class CategoryTypeControllerTest {

	@InjectMocks
	private CategoryTypeController categoryTypeController;

	@Mock
	private CategoryTypeService categoryTypeService;

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;

	private static final String CONTENT_TYPE = MediaType.APPLICATION_JSON_VALUE;
	private static final MediaType ACCEPT_TYPE = MediaType.APPLICATION_JSON;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders
				          .standaloneSetup(categoryTypeController)
				          .setControllerAdvice(new GlobalExceptionHandler())
				          .build();
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
	}

	@Nested
	@DisplayName("getCategoryTypeById Method")
	class GetCategoryTypeByIdTests {

		@Test
		@DisplayName("Should fetch category type by ID successfully")
		void getCategoryTypeByIdShouldBeOk() throws Exception {
			String categoryId = "1";
			CategoryTypeResponse mockResponse = new CategoryTypeResponse(
					"1", "Sample Category", "http://example.com/image.jpg"
			);
			when(categoryTypeService.getCategoryTypeById(categoryId)).thenReturn(mockResponse);

			mockMvc.perform(get("/api/category-types/{id}", categoryId)
					                .contentType(CONTENT_TYPE)
					                .accept(ACCEPT_TYPE))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value(200))
					.andExpect(jsonPath("$.message.id").value("1"))
					.andExpect(jsonPath("$.message.name").value("Sample Category"))
					.andExpect(jsonPath("$.message.imageUrl").value("http://example.com/image.jpg"));

			verify(categoryTypeService, times(1)).getCategoryTypeById(categoryId);
		}

		@Test
		@DisplayName("Should fail with 404 when category type not found")
		void getCategoryTypeByIdNotFound() throws Exception {
			String categoryId = "1";
			when(categoryTypeService.getCategoryTypeById(categoryId))
					.thenThrow(new CategoryTypeNotFoundException("CategoryType not found"));

			mockMvc.perform(get("/api/category-types/{id}", categoryId)
					                .contentType(CONTENT_TYPE)
					                .accept(ACCEPT_TYPE))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(404))
					.andExpect(jsonPath("$.message").value("CategoryType not found"));

			verify(categoryTypeService, times(1)).getCategoryTypeById(categoryId);
		}
	}

	@Nested
	@DisplayName("getAllCategoryTypes Method")
	class GetAllCategoryTypesTests {

		@Test
		@DisplayName("Should fetch all category types successfully")
		void getAllCategoryTypesShouldBeOk() throws Exception {
			List<CategoryTypeResponse> mockResponseList = List.of(
					new CategoryTypeResponse("1", "Category 1", "http://example.com/image1.jpg"),
					new CategoryTypeResponse("2", "Category 2", "http://example.com/image2.jpg")
			);
			when(categoryTypeService.getAllCategoryTypes()).thenReturn(mockResponseList);

			mockMvc.perform(get("/api/category-types")
					                .contentType(CONTENT_TYPE)
					                .accept(ACCEPT_TYPE))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value(200))
					.andExpect(jsonPath("$.message[0].id").value("1"))
					.andExpect(jsonPath("$.message[0].name").value("Category 1"))
					.andExpect(jsonPath("$.message[0].imageUrl").value("http://example.com/image1.jpg"))
					.andExpect(jsonPath("$.message[1].id").value("2"))
					.andExpect(jsonPath("$.message[1].name").value("Category 2"))
					.andExpect(jsonPath("$.message[1].imageUrl").value("http://example.com/image2.jpg"));

			verify(categoryTypeService, times(1)).getAllCategoryTypes();
		}
	}

	@Nested
	@DisplayName("Error Handling Tests")
	class ErrorHandlingTests {

		@Test
		@DisplayName("Should return 500 if unexpected error occurs")
		void unexpectedErrorShouldReturn500() throws Exception {
			when(categoryTypeService.getAllCategoryTypes()).thenThrow(new RuntimeException("Unexpected error"));

			mockMvc.perform(get("/api/category-types")
					                .contentType(CONTENT_TYPE)
					                .accept(ACCEPT_TYPE))
					.andExpect(status().isInternalServerError())
					.andExpect(jsonPath("$.errorCode").value(500))
					.andExpect(jsonPath("$.message").value("Unexpected error"));

			verify(categoryTypeService, times(1)).getAllCategoryTypes();
		}

		@Test
		@DisplayName("Should handle EntityNotFoundException and return 404")
		void entityNotFoundExceptionShouldReturn404() throws Exception {
			String categoryId = "1";
			when(categoryTypeService.getCategoryTypeById(categoryId))
					.thenThrow(new CategoryTypeNotFoundException("CategoryType not found"));

			mockMvc.perform(get("/api/category-types/{id}", categoryId)
					                .contentType(CONTENT_TYPE)
					                .accept(ACCEPT_TYPE))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.errorCode").value(404))
					.andExpect(jsonPath("$.message").value("CategoryType not found"));

			verify(categoryTypeService, times(1)).getCategoryTypeById(categoryId);
		}
	}
}
