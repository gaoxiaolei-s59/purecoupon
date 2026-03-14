package org.puregxl.merchant.admin.mq.producer;

import cn.hutool.core.util.StrUtil;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.puregxl.merchant.admin.common.enums.CouponSendMessageTypeEnum;
import org.puregxl.merchant.admin.mq.base.BaseSendExtendDTO;
import org.puregxl.merchant.admin.mq.base.MessageWrapper;
import org.puregxl.merchant.admin.mq.event.CouponTemplateDelayEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.puregxl.merchant.admin.common.constant.RocketMQConstant.COUPON_TOPIC;

@Component
public class CouponTemplateDelayExecuteStatusProductor extends AbstractCommonSendProduceTemplate<CouponTemplateDelayEvent>{
    public CouponTemplateDelayExecuteStatusProductor(RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendDTO(CouponTemplateDelayEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("优惠券模板关闭定时执行")
                .keys(messageSendEvent.getCouponTemplateId().toString())
                .topic(COUPON_TOPIC)
                .delayDeliverTime(messageSendEvent.getDelayTime())
                .type(CouponSendMessageTypeEnum.DELIVER_MESSAGE.getType())
                .build();
    }

    @Override
    protected Message<?> buildMessage(CouponTemplateDelayEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(keys, messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
