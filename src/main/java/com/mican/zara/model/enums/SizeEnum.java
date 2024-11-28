package com.mican.zara.model.enums;

public enum SizeEnum {
    XS,
    S,
    M,
    L,
    XL,
    XXL,
    XXXL,
    UNKNOWN;

    public static SizeEnum fromString(String value) {
        try {
            return SizeEnum.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
