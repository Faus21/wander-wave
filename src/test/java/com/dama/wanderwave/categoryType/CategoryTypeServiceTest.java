package com.dama.wanderwave.categoryType;

import com.dama.wanderwave.handler.category_type.CategoryTypeNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryTypeService Tests")
class CategoryTypeServiceTest {

	@InjectMocks
	private CategoryTypeService categoryTypeService;

	@Mock
	private CategoryTypeRepository categoryTypeRepository;

	@Mock
	private ModelMapper modelMapper;

	private CategoryType categoryType;
	private CategoryTypeResponse categoryTypeResponse;

	@BeforeEach
	void setUp() {
		categoryType = new CategoryType();
		categoryType.setId("1");
		categoryType.setName("Test Category");

		categoryTypeResponse = new CategoryTypeResponse();
		categoryTypeResponse.setId("1");
		categoryTypeResponse.setName("Test Category");
	}

	@Nested
	@DisplayName("getCategoryTypeById Method")
	class GetCategoryTypeByIdTests {

		@Test
		@DisplayName("Should return category type response when category type exists")
		void shouldReturnCategoryTypeResponse_WhenCategoryTypeExists() {
			when(categoryTypeRepository.findById("1")).thenReturn(Optional.of(categoryType));
			when(modelMapper.map(categoryType, CategoryTypeResponse.class)).thenReturn(categoryTypeResponse);

			CategoryTypeResponse actualResponse = categoryTypeService.getCategoryTypeById("1");

			assertThat(actualResponse).isEqualTo(categoryTypeResponse);
			verify(categoryTypeRepository).findById("1");
			verify(modelMapper).map(categoryType, CategoryTypeResponse.class);
		}

		@Test
		@DisplayName("Should throw EntityNotFoundException when category type does not exist")
		void shouldThrowEntityNotFoundException_WhenCategoryTypeDoesNotExist() {
			when(categoryTypeRepository.findById("1")).thenReturn(Optional.empty());

			assertThatThrownBy(() -> categoryTypeService.getCategoryTypeById("1"))
					.isInstanceOf(CategoryTypeNotFoundException.class)
					.hasMessageContaining("CategoryType not found with ID: 1");

			verify(categoryTypeRepository).findById("1");
			verifyNoInteractions(modelMapper);
		}
	}

	@Nested
	@DisplayName("getAllCategoryTypes Method")
	class GetAllCategoryTypesTests {

		@Test
		@DisplayName("Should return a list of category type responses")
		void shouldReturnListOfCategoryTypeResponses() {
			List<CategoryType> categoryTypeList = List.of(categoryType);
			List<CategoryTypeResponse> expectedResponses = List.of(categoryTypeResponse);

			when(categoryTypeRepository.findAll()).thenReturn(categoryTypeList);
			when(modelMapper.map(categoryType, CategoryTypeResponse.class)).thenReturn(categoryTypeResponse);

			List<CategoryTypeResponse> actualResponses = categoryTypeService.getAllCategoryTypes();

			assertThat(actualResponses).hasSize(1).containsExactlyElementsOf(expectedResponses);
			verify(categoryTypeRepository).findAll();
			verify(modelMapper, times(categoryTypeList.size())).map(categoryType, CategoryTypeResponse.class);
		}

		@Test
		@DisplayName("Should return an empty list when no category types exist")
		void shouldReturnEmptyList_WhenNoCategoryTypesExist() {
			when(categoryTypeRepository.findAll()).thenReturn(List.of());

			List<CategoryTypeResponse> actualResponses = categoryTypeService.getAllCategoryTypes();

			assertThat(actualResponses).isEmpty();
			verify(categoryTypeRepository).findAll();
			verifyNoInteractions(modelMapper);
		}
	}
}
