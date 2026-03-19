package org.pureglx.engine.mq.producer;

import cn.hutool.core.util.StrUtil;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.pureglx.engine.mq.base.BaseSendExtendDTO;
import org.pureglx.engine.mq.base.MessageWrapper;
import org.pureglx.engine.mq.event.UserCouponRedeemEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserCouponRedeemEventProducer extends AbstractCommonSendProduceTemplate<UserCouponRedeemEvent>{

    public UserCouponRedeemEventProducer(RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendDTO(UserCouponRedeemEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("用户兑换优惠券")
                .keys(UUID.randomUUID().toString())
                .topic("one-coupon_engine-service_coupon-redeem_topic")
                .sentTimeout(2000L)
                .build();
    }

    @Override
    protected Message<?> buildMessage(UserCouponRedeemEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(requestParam.getKeys(), messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
