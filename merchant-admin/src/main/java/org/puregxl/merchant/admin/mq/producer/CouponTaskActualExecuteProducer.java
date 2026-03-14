package org.puregxl.merchant.admin.mq.producer;

import cn.hutool.core.util.StrUtil;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.puregxl.merchant.admin.common.enums.CouponSendMessageTypeEnum;
import org.puregxl.merchant.admin.mq.base.BaseSendExtendDTO;
import org.puregxl.merchant.admin.mq.base.MessageWrapper;
import org.puregxl.merchant.admin.mq.event.CouponTaskExecuteEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.puregxl.merchant.admin.common.constant.RocketMQConstant.COUPON_ACTUAL_TOPIC;

@Component
public class CouponTaskActualExecuteProducer extends AbstractCommonSendProduceTemplate<CouponTaskExecuteEvent> {
    public CouponTaskActualExecuteProducer(RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendDTO(CouponTaskExecuteEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("优惠券立刻发送功能")
                .keys(messageSendEvent.getCouponTaskId().toString())
                .topic(COUPON_ACTUAL_TOPIC)
                .type(CouponSendMessageTypeEnum.COMMON_MESSAGE.getType())
                .sentTimeout(2000L)
                .build();
    }

    @Override
    protected Message<?> buildMessage(CouponTaskExecuteEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(keys, messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
