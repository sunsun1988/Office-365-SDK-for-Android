package com.microsoft.office.integration.test;

public enum AuthType {
    BASIC("basic"), 
    AAD("aad"), 
    UNDEFINED(""), 
    ;

    private String mValue;

    private AuthType(String pValue) {
        mValue = pValue;
    }

    private String getValue() {
        return mValue;
    }

    public static AuthType fromString(String authType) {
        for (AuthType type : values()) {
            if (authType.equalsIgnoreCase(type.getValue())) {
                return type;
            }
        }
        return UNDEFINED;
    }
}
