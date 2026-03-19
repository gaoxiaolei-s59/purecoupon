package org.pureglx.engine.mq.consumer;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.pureglx.engine.common.constant.RocketMQConstant;
import org.pureglx.engine.mq.base.MessageWrapper;
import org.pureglx.engine.mq.producer.UserCouponRedeemEventProducer;
import org.springframework.stereotype.Component;

@RocketMQMessageListener(
        topic = RocketMQConstant.COUPON_ENGINE_SERVICE_SUER_REDEEM_CLOSE_TOPIC,
        consumerGroup = RocketMQConstant.COUPON_ENGINE_SERVICE_SUER_REDEEM_CLOSE_GROUP
)
@Component
@Slf4j(topic = "UserCouponRedeemEventConsumer")
@RequiredArgsConstructor
public class UserCouponRedeemEventConsumer implements RocketMQListener<MessageWrapper<UserCouponRedeemEventProducer>> {



    @Override
    public void onMessage(MessageWrapper<UserCouponRedeemEventProducer> userCouponRedeemEventProducerMessageWrapper) {


        // 添加 Redis 用户领取的优惠券记录列表

        // 添加用户领取优惠券模板缓存记录
    }
}
