package org.puregxl.distribution.mq.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.BatchExecutorException;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.puregxl.distribution.common.constant.DistributionRedisConstant;
import org.puregxl.distribution.common.constant.EngineRedisConstant;
import org.puregxl.distribution.common.enums.CouponSourceEnum;
import org.puregxl.distribution.common.enums.CouponStatusEnum;
import org.puregxl.distribution.common.enums.CouponTaskStatusEnum;
import org.puregxl.distribution.dao.entity.CouponTaskDO;
import org.puregxl.distribution.dao.entity.CouponTaskFailDO;
import org.puregxl.distribution.dao.entity.CouponTemplateDO;
import org.puregxl.distribution.dao.entity.UserCouponDO;
import org.puregxl.distribution.dao.mapper.CouponTaskFailMapper;
import org.puregxl.distribution.dao.mapper.CouponTaskMapper;
import org.puregxl.distribution.dao.mapper.CouponTemplateMapper;
import org.puregxl.distribution.dao.mapper.UserCouponMapper;
import org.puregxl.distribution.mq.base.MessageWrapper;
import org.puregxl.distribution.mq.event.CouponTemplateDistributionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.puregxl.distribution.common.constant.RocketMQConstant.COUPON_DISTRIBUTION_TOPIC;
import static org.puregxl.distribution.common.constant.RocketMQConstant.COUPON_DISTRIBUTION_TOPIC_GROUP;

@Component
@RocketMQMessageListener(
        topic = COUPON_DISTRIBUTION_TOPIC,
        consumerGroup = COUPON_DISTRIBUTION_TOPIC_GROUP
)
@Slf4j(topic = "CouponExecuteDistributionConsumer")
@RequiredArgsConstructor
public class CouponExecuteDistributionConsumer implements RocketMQListener<MessageWrapper<CouponTemplateDistributionEvent>> {

    private final StringRedisTemplate stringRedisTemplate;
    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponTaskMapper couponTaskMapper;
    private static final String BATCH_SAVE_USER_COUPON_LUA_PATH = "lua/batch_user_coupon_list.lua";

    @Lazy
    @Autowired
    private CouponExecuteDistributionConsumer couponExecuteDistributionConsumer;
    @Autowired
    private CouponTaskFailMapper couponTaskFailMapper;

    /**
     * 执行消费逻辑
     *
     * @param messageWrapper
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(MessageWrapper<CouponTemplateDistributionEvent> messageWrapper) {
        log.info("[消费者] 优惠券任务执行推送@分发到用户账号 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));
        CouponTemplateDistributionEvent message = messageWrapper.getMessage();
        if (!message.getDistributionEndFlag()) {
            decrementCouponTemplateStockAndSaveUserCouponList(message);
            return;
        }

        //说明已经处理完成 没有数据了
        String batchUserSetKey = String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY, message.getCouponTaskId());
        Long batchUserIdsSize = stringRedisTemplate.opsForSet().size(batchUserSetKey);
        message.setBatchUserSetSize(batchUserIdsSize.intValue());

        decrementCouponTemplateStockAndSaveUserCouponList(message);
        List<String> batchUserMaps = stringRedisTemplate.opsForSet().pop(batchUserSetKey, Integer.MAX_VALUE);
        // 此时待保存入库用户优惠券列表如果还有值，就意味着可能库存不足引起的
        if (CollUtil.isNotEmpty(batchUserMaps)) {
            // 添加到 t_coupon_task_fail 并标记错误原因，方便后续查看未成功发送的原因和记录
            List<CouponTaskFailDO> couponTaskFailDOList = new ArrayList<>(batchUserMaps.size());
            for (String batchUserMapStr : batchUserMaps) {
                Map<Object, Object> objectMap = MapUtil.builder()
                        .put("rowCount", JSON.parseObject(batchUserMapStr).get("rowCount"))
                        .put("cause", "库存不足")
                        .build();
                CouponTaskFailDO couponTaskFailDO = CouponTaskFailDO.builder()
                        .batchId(message.getCouponTaskBatchId())
                        .jsonObject(com.alibaba.fastjson.JSON.toJSONString(objectMap))
                        .build();
                couponTaskFailDOList.add(couponTaskFailDO);
            }

            // 添加到 t_coupon_task_fail 并标记错误原因
            couponTaskFailMapper.insert(couponTaskFailDOList);
        }

        // 确保所有用户都已经接到优惠券后，设置优惠券推送任务完成时间
        CouponTaskDO couponTaskDO = CouponTaskDO.builder()
                .id(message.getCouponTaskId())
                .status(CouponTaskStatusEnum.SUCCESS.getStatus())
                .completionTime(new Date())
                .build();

        couponTaskMapper.updateById(couponTaskDO);

        clearCouponTaskRedisCache(message.getCouponTaskId());

    }

    /**
     * 从Redis 里面提取库存 然后执行分发逻辑 （数据库）
     *
     * @param event
     */
    @SneakyThrows
    private void decrementCouponTemplateStockAndSaveUserCouponList(CouponTemplateDistributionEvent event) {
        // 如果等于 0 意味着已经没有了库存，直接返回即可

        Integer couponTemplateStock = decrementCouponTemplateStock(event, event.getBatchUserSetSize());
        if (couponTemplateStock <= 0) {
            return;
        }

        //获取redis的用户列表
        String batchUserKey = String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY, event.getCouponTaskId());
        List<String> userList = stringRedisTemplate.opsForSet().pop(batchUserKey, couponTemplateStock);
        //初始化
        List<UserCouponDO> userCouponMap = new ArrayList<>(userList.size());
        for (String each : userList) {
            Date now = new Date();
            DateTime validEndTime = DateUtil.offsetHour(now, JSON.parseObject(event.getCouponTemplateConsumeRule()).getInteger("validityPeriod"));
            JSONObject userObject = JSONUtil.parseObj(each);
            //构建UserCouponDO
            UserCouponDO userCouponDO = UserCouponDO.builder()
                    .id(IdUtil.getSnowflakeNextId())
                    .userId(userObject.getLong("userId"))
                    .couponTemplateId(event.getCouponTemplateId())
                    .receiveTime(now)
                    .receiveCount(userObject.getInt("rowCount"))
                    .validStartTime(now)
                    .validEndTime(validEndTime)
                    .source(CouponSourceEnum.PLATFORM_DISTRIBUTION.getSource())
                    .status(CouponStatusEnum.UNUSED.getCode())
                    .rowNum(userObject.getInt("rowCount"))
                    .build();
            userCouponMap.add(userCouponDO);
        }

        // 平台优惠券每个用户限领一次。批量新增用户优惠券记录，底层通过递归方式直到全部新增成功
        batchSaveUserCouponList(event.getCouponTemplateId(), event.getCouponTaskBatchId(), userCouponMap);


        //最后把这些数据放入Redis的Zset里面
        DefaultRedisScript<Void> buildLuaScript = Singleton.get(BATCH_SAVE_USER_COUPON_LUA_PATH, () -> {
            DefaultRedisScript<Void> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(BATCH_SAVE_USER_COUPON_LUA_PATH)));
            redisScript.setResultType(Void.class);
            return redisScript;
        });

        List<String> userIDList = userCouponMap.stream().map(UserCouponDO::getUserId).map(String::valueOf).toList();

        List<String> couponIdList = userCouponMap.stream().map(
                each -> StrUtil.builder()
                        .append(event.getCouponTemplateId())
                        .append("_")
                        .append(each.getId())
                        .toString()
        ).map(String::valueOf).toList();
        List<String> keys = Arrays.asList(
                // 为什么要进行替换 %s 为空白字符串？因为后续代码需要使用 %s 进行动态值替换，但是当前 LUA 脚本中不需要，所以为了兼容后续不改动特此替换
                StrUtil.replace(EngineRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY, "%s", "")
        );
        List<String> args = ListUtil.of(
                JSONUtil.toJsonStr(userIDList),
                JSONUtil.toJsonStr(couponIdList),
                String.valueOf(new Date().getTime())
        );

        stringRedisTemplate.execute(buildLuaScript, keys, args.toArray());

    }


    /**
     * 批处理操作
     * @param couponTemplateId
     * @param couponTaskBatchId
     * @param userCouponMap
     */
    private void batchSaveUserCouponList(Long couponTemplateId, Long couponTaskBatchId, List<UserCouponDO> userCouponMap) {
        try {
            userCouponMapper.insert(userCouponMap, userCouponMap.size());
        } catch (Exception ex) {
            //如果出现异常 - 单条执行
            if (ex instanceof BatchExecutorException) {
                //先查询是否已经领取
                List<CouponTaskFailDO> couponTaskFailDOList = new ArrayList<>();
                List<UserCouponDO> toRemove = new ArrayList<>();
                userCouponMap.forEach(each -> {
                    try {
                        userCouponMapper.insert(each);
                    } catch (Exception e) {
                        Boolean hasReceived = couponExecuteDistributionConsumer.hasUserReceivedCoupon(couponTemplateId, each.getUserId());

                        if (hasReceived) {
                            // 添加到 t_coupon_task_fail 并标记错误原因，方便后续查看未成功发送的原因和记录
                            Map<Object, Object> objectMap = MapUtil.builder()
                                    .put("rowNum", each.getRowNum())
                                    .put("cause", "用户已领取该优惠券")
                                    .build();
                            CouponTaskFailDO couponTaskFailDO = CouponTaskFailDO.builder()
                                    .batchId(couponTaskBatchId)
                                    .jsonObject(com.alibaba.fastjson.JSON.toJSONString(objectMap))
                                    .build();
                            couponTaskFailDOList.add(couponTaskFailDO);

                            // 从 userCouponDOList 中删除已经存在的记录
                            toRemove.add(each);
                        }
                    }
                });

                couponTaskFailMapper.insert(couponTaskFailDOList);
                userCouponMap.removeAll(toRemove);
                return;
            }
            throw ex;
        }
    }

    /**
     * 查询用户是否已经领取过卷
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean hasUserReceivedCoupon(Long couponTemplateId, Long userId) {
        LambdaQueryWrapper<UserCouponDO> userCouponDOLambdaQueryWrapper = Wrappers.lambdaQuery(UserCouponDO.class)
                .eq(UserCouponDO::getCouponTemplateId, couponTemplateId)
                .eq(UserCouponDO::getUserId, userId);
        return userCouponMapper.selectOne(userCouponDOLambdaQueryWrapper) != null;
    }

    private void clearCouponTaskRedisCache(Long couponTaskId) {
        String progressKey = String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_PROGRESS_KEY, couponTaskId);
        String batchUserSetKey = String.format(DistributionRedisConstant.TEMPLATE_TASK_EXECUTE_BATCH_USER_KEY, couponTaskId);
        stringRedisTemplate.delete(List.of(progressKey, batchUserSetKey));
        log.info("[消费者] 优惠券任务执行推送@分发到用户账号 - 清理任务缓存完成, couponTaskId: {}", couponTaskId);
    }

    private Integer decrementCouponTemplateStock(CouponTemplateDistributionEvent event, Integer decrementStockSize) {
        if (decrementStockSize == null || decrementStockSize <= 0) {
            return 0;
        }
        Long couponTemplateId = event.getCouponTemplateId();
        int templateStock = couponTemplateMapper.decrementCouponTemplateStock(event.getShopNumber(), event.getCouponTemplateId(), decrementStockSize);

        /*
         * 扣减失败 递归调用
         * */
        if (!SqlHelper.retBool(templateStock)) {
            LambdaQueryWrapper<CouponTemplateDO> wrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                    .eq(CouponTemplateDO::getShopNumber, event.getShopNumber())
                    .eq(CouponTemplateDO::getId, couponTemplateId);

            CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(wrapper);

            return decrementCouponTemplateStock(event, couponTemplateDO.getStock());
        }

        return decrementStockSize;
    }


}
