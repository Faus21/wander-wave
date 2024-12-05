package com.dama.wanderwave.categoryType;

import com.dama.wanderwave.handler.category_type.CategoryTypeNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryTypeService {

	private final CategoryTypeRepository categoryTypeRepository;
	private final ModelMapper modelMapper;

	public CategoryTypeResponse getCategoryTypeById(String id) {
		log.info("Fetching CategoryType with ID: {}", id);

		CategoryType categoryType = categoryTypeRepository.findById(id)
				                            .orElseThrow(() -> {
					                            log.error("CategoryType with ID: {} not found", id);
					                            return new CategoryTypeNotFoundException("CategoryType not found with ID: " + id);
				                            });


		CategoryTypeResponse response = modelMapper.map(categoryType, CategoryTypeResponse.class);
		log.info("Successfully fetched and mapped CategoryType with ID: {}", id);
		return response;
	}

	public List<CategoryTypeResponse> getAllCategoryTypes() {
		log.info("Fetching all CategoryTypes");

		List<CategoryType> categoryTypes = categoryTypeRepository.findAll();

		List<CategoryTypeResponse> responses = categoryTypes.stream()
				                                       .map(categoryType -> modelMapper.map(categoryType, CategoryTypeResponse.class))
				                                       .toList();

		log.info("Successfully fetched and mapped {} CategoryTypes", responses.size());
		return responses;
	}

}
