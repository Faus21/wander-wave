package com.dama.wanderwave.auth;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryRequest {

	private String token;


	private String password;

}
