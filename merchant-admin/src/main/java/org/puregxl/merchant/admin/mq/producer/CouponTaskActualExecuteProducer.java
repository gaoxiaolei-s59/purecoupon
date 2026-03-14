package org.puregxl.merchant.admin.mq.producer;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.puregxl.merchant.admin.common.enums.CouponSendMessageTypeEnum;
import org.puregxl.merchant.admin.mq.base.BaseSendExtendDTO;
import org.puregxl.merchant.admin.mq.event.CouponTemplateDelayEvent;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import static org.puregxl.merchant.admin.common.constant.RocketMQConstant.COUPON_TOPIC;

@Component
public class CouponTaskActualExecuteProducer extends AbstractCommonSendProduceTemplate<CouponTemplateDelayEvent> {
    public CouponTaskActualExecuteProducer(RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendDTO(CouponTemplateDelayEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("优惠券立刻发送功能")
                .keys(messageSendEvent.getCouponTemplateId().toString())
                .topic(COUPON_TOPIC)
                .type(CouponSendMessageTypeEnum.COMMON_MESSAGE.getType())
                .build();
    }

    @Override
    protected Message<?> buildMessage(CouponTemplateDelayEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        return null;
    }
}
