package com.sse.service.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallbackRequest {

    @NotBlank
    private UUID id;

    @NotBlank
    private String status;
}
