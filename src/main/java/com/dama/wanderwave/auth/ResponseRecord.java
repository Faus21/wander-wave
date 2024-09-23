package com.dama.wanderwave.auth;

import lombok.Builder;

@Builder
public record ResponseRecord (int code, Object message) {

}
