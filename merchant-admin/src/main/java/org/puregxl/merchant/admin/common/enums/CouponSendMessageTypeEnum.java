package org.puregxl.merchant.admin.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
@RequiredArgsConstructor
public enum CouponSendMessageTypeEnum {

    DELAY_MESSAGE(0),

    DELIVER_MESSAGE(1),

    COMMON_MESSAGE(2);

    @Getter
    private final int type;
}
