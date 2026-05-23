package com.madara.security.response.DTO.admin;

import com.madara.security.model.Plan;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePlanRequest {

    @NotNull(message = "Plan must not be null")
    private Plan plan;

    // Required only when plan = SUBSCRIBER. Number of days until expiry.
    @Min(value = 1, message = "Subscription days must be at least 1")
    private Integer subscriptionDays;
}
