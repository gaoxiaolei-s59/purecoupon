package org.puregxl.distribution.mq.producter;

import cn.hutool.core.util.StrUtil;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.puregxl.distribution.mq.base.BaseSendExtendDTO;
import org.puregxl.distribution.mq.base.MessageWrapper;
import org.puregxl.distribution.mq.event.CouponTemplateDistributionEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Component
public class CouponExecuteDistributionProducer extends AbstractCommonSendProduceTemplate<CouponTemplateDistributionEvent>{

    public CouponExecuteDistributionProducer(RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendDTO(CouponTemplateDistributionEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("优惠券发放执行")
                .keys(String.valueOf(messageSendEvent.getCouponTaskId()))
                .topic("one-coupon_distribution-service_coupon-execute-distribution_topic")
                .sentTimeout(2000L)
                .build();
    }

    @Override
    protected Message<?> buildMessage(CouponTemplateDistributionEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(keys, messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
