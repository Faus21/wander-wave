package com.dama.wanderwave.categoryType;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryTypeResponse {
	private String id;
	private String name;
	private String imageUrl;
}
