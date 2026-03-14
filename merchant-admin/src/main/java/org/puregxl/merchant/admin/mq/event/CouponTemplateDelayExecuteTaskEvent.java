package org.puregxl.merchant.admin.mq.event;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponTemplateDelayExecuteTaskEvent {
    /**
     * 延时任务文件名
     */
    private String fileAddress;

    /**
     * 优惠卷id
     */
    private Long couponTaskId;
}
