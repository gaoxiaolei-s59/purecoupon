package org.puregxl.framework.result;


import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    private static final String SUCCESS_CODE = "0";

    private String code;

    private String message;

    private T data;

    private String requestId;

    public boolean isSuccess() {
        return true;
    }
}
