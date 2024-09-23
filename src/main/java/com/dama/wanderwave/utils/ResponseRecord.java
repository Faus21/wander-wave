package com.dama.wanderwave.utils;

import lombok.Builder;

@Builder
public record ResponseRecord (int code, Object message) {

}
