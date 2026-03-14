package org.puregxl.merchant.admin.mq.producer;

import cn.hutool.core.util.StrUtil;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.puregxl.merchant.admin.common.enums.CouponSendMessageTypeEnum;
import org.puregxl.merchant.admin.mq.base.BaseSendExtendDTO;
import org.puregxl.merchant.admin.mq.base.MessageWrapper;
import org.puregxl.merchant.admin.mq.event.CouponTemplateDelayExecuteTaskEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.puregxl.merchant.admin.common.constant.RocketMQConstant.COUPON_TASK_TOPIC;

@Component
public class CouponTemplateDelayExecuteTaskProductor extends AbstractCommonSendProduceTemplate<CouponTemplateDelayExecuteTaskEvent>{

    @Value("${merchant.admin.task.delay.timestamp}")
    private long delayTimeStamp;

    public CouponTemplateDelayExecuteTaskProductor(RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendDTO(CouponTemplateDelayExecuteTaskEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("优惠卷防宕机消息队列")
                .keys(messageSendEvent.getCouponTaskId().toString())
                .topic(COUPON_TASK_TOPIC)
                .delayDeliverTime(delayTimeStamp)
                .type(CouponSendMessageTypeEnum.DELAY_MESSAGE.getType())
                .build();
    }

    @Override
    protected Message<?> buildMessage(CouponTemplateDelayExecuteTaskEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(keys, messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
