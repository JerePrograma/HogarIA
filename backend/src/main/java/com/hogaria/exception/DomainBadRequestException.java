package com.hogaria.exception;

import java.util.List;

public class DomainBadRequestException extends BadRequestException {

    private final String code;
    private final List<ErrorResponse.Detail> details;

    public DomainBadRequestException(String message, String code) {
        this(message, code, List.of());
    }

    public DomainBadRequestException(
            String message,
            String code,
            List<ErrorResponse.Detail> details
    ) {
        super(message);
        this.code = code;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public String getCode() {
        return code;
    }

    public List<ErrorResponse.Detail> getDetails() {
        return details;
    }
}
