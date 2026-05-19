package org.tc.mtracker.category.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public enum IconIds {
    TREND_UP("trend_up"),
    AWARD ("award"),
    DOLLAR("dollar"),
    GIFT("gift"),
    DOCK("dock"),
    BRIEFCASE("briefcase"),
    COINS("coins"),
    WALLET("wallet"),
    PERCENT("percent"),
    TROPHY("trophy"),
    RECEIPT("receipt"),
    MONITOR("monitor"),
    GLOBE("globe"),
    DATABASE("database");

    private final String id;

    public static final Set<String> iconSet = EnumSet.allOf(IconIds.class)
            .stream()
                .map(iconIds -> iconIds.id)
                .collect(Collectors.toSet());

}