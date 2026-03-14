package org.puregxl.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.puregxl.framework.exception.ClientException;
import org.puregxl.merchant.admin.common.context.UserContext;
import org.puregxl.merchant.admin.common.enums.CouponTaskSendTypeEnum;
import org.puregxl.merchant.admin.common.enums.CouponTaskStatusEnum;
import org.puregxl.merchant.admin.dao.entity.CouponTaskDO;
import org.puregxl.merchant.admin.dao.mapper.CouponTaskMapper;
import org.puregxl.merchant.admin.dto.req.CouponTaskCreateReqDTO;
import org.puregxl.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;
import org.puregxl.merchant.admin.service.CouponTaskService;
import org.puregxl.merchant.admin.service.CouponTemplateService;
import org.puregxl.merchant.admin.service.handler.excel.RowCountListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.puregxl.merchant.admin.common.constant.RocketMQConstant.COUPON_TASK_TOPIC;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponTaskServiceImpl extends ServiceImpl<CouponTaskMapper, CouponTaskDO> implements CouponTaskService {


    private final CouponTemplateService couponTemplateService;
    private final CouponTaskMapper couponTaskMapper;
    private final RocketMQTemplate rocketMQTemplate;

    @Value("${merchant.admin.task.delay.timestamp}")
    private long delayTimeStamp;

    /**
     * 创建异步线程池执行任务 加快接口返回速度
     */
    private final Executor executor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() * 2,
            60,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(), //使用无容量队列，在任务到来的时候优先扩展线程,
            new ThreadPoolExecutor.DiscardPolicy()
    );
    /**
     * 创建优惠卷任务
     * @param requestParam
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createCouponTask(CouponTaskCreateReqDTO requestParam) {
        // 验证非空参数
        // 验证参数是否正确，比如文件地址是否为我们期望的格式等
        // 验证参数依赖关系，比如选择定时发送，发送时间是否不为空等
        CouponTemplateQueryRespDTO couponTemplate = couponTemplateService.findCouponTemplate(requestParam.getCouponTemplateId());
        if (couponTemplate == null) {
            throw new ClientException("优惠券模板不存在，请检查提交信息是否正确");
        }
        // ......

        // 构建优惠券推送任务数据库持久层实体
        CouponTaskDO couponTaskDO = BeanUtil.copyProperties(requestParam, CouponTaskDO.class);
        couponTaskDO.setBatchId(IdUtil.getSnowflakeNextId());
        couponTaskDO.setOperatorId(Long.parseLong(UserContext.getUserId()));
        couponTaskDO.setShopNumber(UserContext.getShopNumber());
        couponTaskDO.setStatus(
                Objects.equals(requestParam.getSendType(), CouponTaskSendTypeEnum.IMMEDIATE.getType())
                        ? CouponTaskStatusEnum.IN_PROGRESS.getStatus()
                        : CouponTaskStatusEnum.PENDING.getStatus()
        );

        // 保存优惠券推送任务记录到数据库（先执行插入操作）
        couponTaskMapper.insert(couponTaskDO);

        //异步执行
        executor.execute(() -> refreshCouponTaskNum(requestParam.getFileAddress(), couponTaskDO.getId()));

        //使用mq延时队列保证宕机后的逻辑可恢复制性
        JSONObject messageBody = new JSONObject();
        messageBody.put("fileAddress", requestParam.getFileAddress());
        messageBody.put("couponTaskId", couponTaskDO.getId());

        String messageKeys = UUID.randomUUID().toString();

        Message<JSONObject> message = MessageBuilder
                .withPayload(messageBody)
                .setHeader(MessageConst.PROPERTY_KEYS, messageKeys)
                .build();
        String topic = COUPON_TASK_TOPIC;

        SendResult sendResult;
        try {
            //发送延时消息
            sendResult = rocketMQTemplate.syncSendDelayTimeMills(topic, message, delayTimeStamp);
            log.info("[生产者] 优惠券模板可靠性处理 - 发送结果：{}，消息ID：{}，消息Keys：{}", sendResult.getSendStatus(), sendResult.getMsgId(), messageKeys);
        } catch (Exception e) {
            log.error("[生产者]-优惠券模板可靠性处理-生产者-发送消息失败，消息体: {}" ,couponTaskDO.getId(), e);
        }

        //如果是立刻发送任务
        if (Objects.equals(requestParam.getSendType(), CouponTaskSendTypeEnum.IMMEDIATE.getType())) {
            JSONObject noDelaymessageBody = new JSONObject();
            messageBody.put("fileAddress", requestParam.getFileAddress());
            messageBody.put("couponTaskId", couponTaskDO.getId());

            String noDelayMessageKeys = UUID.randomUUID().toString();

            Message<JSONObject> noDelayMessage = MessageBuilder
                    .withPayload(messageBody)
                    .setHeader(MessageConst.PROPERTY_KEYS, noDelayMessageKeys)
                    .build();
            String couponTemplateDelayCloseTopic = COUPON_TASK_TOPIC;

            SendResult sendResults;
            try {
                //发送延时消息
                sendResults = rocketMQTemplate.syncSendDelayTimeMills(couponTemplateDelayCloseTopic, couponTemplateDelayCloseTopic, delayTimeStamp);
                log.info("[生产者] 优惠券模板立刻分发逻辑 - 发送结果：{}，消息ID：{}，消息Keys：{}", sendResults.getSendStatus(), sendResults.getMsgId(), messageKeys);
            } catch (Exception e) {
                log.error("[生产者]-优惠券模板立刻分发逻辑-生产者-发送消息失败，消息体: {}" ,couponTaskDO.getId(), e);
            }
        }

    }

    /**
     * 刷新优惠卷分发数据
     * @param file
     * @param couponTemplateId
     */
    private void refreshCouponTaskNum(String file, Long couponTemplateId) {
        // 通过 EasyExcel 监听器获取 Excel 中所有行数
        RowCountListener listener = new RowCountListener();
        EasyExcel.read(file, listener).sheet().doRead();

        // 为什么需要统计行数？因为发送后需要比对所有优惠券是否都已发放到用户账号
        int totalRows = listener.getRowCount();
        CouponTaskDO updateCouponTaskDO = CouponTaskDO.builder()
                .id(couponTemplateId)
                .sendNum(totalRows)
                .build();
        // 刷新保存到数据库的记录
        couponTaskMapper.updateById(updateCouponTaskDO);
    }


}
