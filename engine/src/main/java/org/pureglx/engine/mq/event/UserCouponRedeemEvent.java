package org.pureglx.engine.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.pureglx.engine.dto.req.CouponTemplateRedeemReqDTO;
import org.pureglx.engine.dto.resp.CouponTemplateQueryRespDTO;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserCouponRedeemEvent {
    /**
     * Web 请求参数
     */
    private CouponTemplateRedeemReqDTO requestParam;

    /**
     * 领取次数
     */
    private Integer receiveCount;

    /**
     * 优惠券模板
     */
    private CouponTemplateQueryRespDTO couponTemplate;

    /**
     * 用户 ID
     */
    private String userId;
}
