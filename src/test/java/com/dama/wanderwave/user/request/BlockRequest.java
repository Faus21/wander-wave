package com.dama.wanderwave.user.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockRequest {

    @NotEmpty(message = "Blocker id is mandatory")
    private String blockerId;
    @NotEmpty(message = "Blocked id is mandatory")
    private String blockedId;

}
