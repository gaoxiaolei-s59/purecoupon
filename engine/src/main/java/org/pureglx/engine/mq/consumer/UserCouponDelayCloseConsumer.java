package org.pureglx.engine.mq.consumer;


import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.pureglx.engine.common.constant.EngineRedisConstant;
import org.pureglx.engine.common.enums.UserCouponStatusEnum;
import org.pureglx.engine.dao.entity.UserCouponDO;
import org.pureglx.engine.dao.mapper.UserCouponMapper;
import org.pureglx.engine.mq.base.MessageWrapper;
import org.pureglx.engine.mq.event.UserCouponDelayCloseEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import static org.pureglx.engine.common.constant.RocketMQConstant.COUPON_ENGINE_SERVICE_SUER_DELAY_CLOSE;
import static org.pureglx.engine.common.constant.RocketMQConstant.COUPON_ENGINE_SERVICE_SUER_DELAY_CLOSE_GROUP;

@Slf4j(topic = "CouponTemplateDelayExecuteStatusConsumer")
@Component
@RocketMQMessageListener(
        topic = COUPON_ENGINE_SERVICE_SUER_DELAY_CLOSE,
        consumerGroup = COUPON_ENGINE_SERVICE_SUER_DELAY_CLOSE_GROUP
)
@RequiredArgsConstructor
public class UserCouponDelayCloseConsumer implements RocketMQListener<MessageWrapper<UserCouponDelayCloseEvent>> {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserCouponMapper userCouponMapper;

    @Override
    public void onMessage(MessageWrapper<UserCouponDelayCloseEvent> messageWrapper) {
        log.info("[消费者] 延迟关闭用户已领取优惠券 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));
        UserCouponDelayCloseEvent message = messageWrapper.getMessage();
        // 删除用户领取优惠券模板缓存记录
        String userListKey = String.format(EngineRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY, message.getUserId());
        String userListItemKey = StrUtil.builder().append(message.getCouponTemplateId()).append("_").append(message.getUserCouponId()).toString();

        //删除对应过期的优惠卷
        Long removed = stringRedisTemplate.opsForZSet().remove(userListKey, userListItemKey);
        if (removed == null || removed == 0L) {
            return;
        }

        //设置用户领劵记录为已过期
        UserCouponDO userCouponDO = UserCouponDO.builder()
                .status(UserCouponStatusEnum.EXPIRED.getCode()).build();

        LambdaUpdateWrapper<UserCouponDO> updateWrapper = Wrappers.lambdaUpdate(UserCouponDO.class)
                .eq(UserCouponDO::getId, message.getUserCouponId())
                .eq(UserCouponDO::getUserId, message.getUserId())
                .eq(UserCouponDO::getStatus, UserCouponStatusEnum.UNUSED.getCode())
                .eq(UserCouponDO::getCouponTemplateId, message.getCouponTemplateId());
        userCouponMapper.update(userCouponDO, updateWrapper);

    }
}
