package com.madara.security.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Plan {

    FREE(10, "Free Tier — 10 pages per month"),
    SUBSCRIBER(-1, "Subscriber — unlimited pages"),
    DEV(-1, "Developer — unlimited pages, testing only");

    private final int monthlyPageLimit;
    private final String description;

    public boolean isUnlimited() {
        return monthlyPageLimit == -1;
    }
}
