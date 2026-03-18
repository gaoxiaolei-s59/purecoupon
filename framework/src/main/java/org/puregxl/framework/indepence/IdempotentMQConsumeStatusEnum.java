package org.puregxl.framework.indepence;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
public enum IdempotentMQConsumeStatusEnum {

    /**
     * 消费中
     */
    CONSUMING("0"),

    /**
     * 消费结束
     */
    CONSUMED("1");

    @Getter
    private final String code;

    public static boolean isError(String consumeStatus) {
        return Objects.equals(CONSUMING.code, consumeStatus);
    }
}
