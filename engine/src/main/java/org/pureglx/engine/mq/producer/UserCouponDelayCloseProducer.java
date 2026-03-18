package org.pureglx.engine.mq.producer;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.pureglx.engine.mq.base.BaseSendExtendDTO;
import org.pureglx.engine.mq.base.MessageWrapper;
import org.pureglx.engine.mq.event.UserCouponDelayCloseEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.pureglx.engine.common.constant.RocketMQConstant.COUPON_ENGINE_SERVICE_SUER_DELAY_CLOSE;

@Component
@Slf4j
public class UserCouponDelayCloseProducer extends AbstractCommonSendProduceTemplate<UserCouponDelayCloseEvent>{

    public UserCouponDelayCloseProducer(RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendDTO(UserCouponDelayCloseEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("延迟关闭用户已领取优惠券")
                .keys(String.valueOf(messageSendEvent.getUserCouponId()))
                .sentTimeout(2000L)
                .delayDeliverTime(messageSendEvent.getDelayTime())
                .type(1) //定时投递任务
                .topic(COUPON_ENGINE_SERVICE_SUER_DELAY_CLOSE).build();
    }

    @Override
    protected Message<?> buildMessage(UserCouponDelayCloseEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(requestParam.getKeys(), messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
