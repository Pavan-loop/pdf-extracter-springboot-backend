package com.madara.security.response.DTO;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCardsDTO {
    private Long session;
    private Long records;
    private Long completed;
}
