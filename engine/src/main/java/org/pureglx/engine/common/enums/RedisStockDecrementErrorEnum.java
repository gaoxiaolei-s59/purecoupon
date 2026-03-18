package org.pureglx.engine.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RedisStockDecrementErrorEnum {
    /**
     * 成功
     */
    SUCCESS(0, "成功"),

    /**
     * 库存不足
     */
    STOCK_INSUFFICIENT(1, "优惠券已被领取完啦"),

    /**
     * 用户已经达到领取上限
     */
    LIMIT_REACHED(2, "用户已经达到领取上限");


    @Getter
    private final int code;

    @Getter
    private final String errorMessage;

    public static boolean isFail(int code) {
        return code == 1 || code == 2;
    }

    /**
     * 根据传入的 错误类型 返回对应的错误信息
     * @param code
     * @return
     */
    public static String formFailMessage(int code) {
        for (RedisStockDecrementErrorEnum value : RedisStockDecrementErrorEnum.values()) {
            if (value.getCode() == code) {
                return value.errorMessage;
            }
        }
        throw new IllegalArgumentException("Invalid code: " + code);
    }

}
