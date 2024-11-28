package com.mican.zara.model.enums;

import java.util.Locale;

public enum Availability {
    IN_STOCK,
    OUT_OF_STOCK,
    COMING_SOON,
    UNKNOWN;

    public static Availability fromString(String value) {
        try {
            return Availability.valueOf(value.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}