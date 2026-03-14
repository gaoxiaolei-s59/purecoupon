package org.puregxl.merchant.admin.job;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.puregxl.merchant.admin.common.enums.CouponTaskStatusEnum;
import org.puregxl.merchant.admin.dao.entity.CouponTaskDO;
import org.puregxl.merchant.admin.dao.mapper.CouponTaskMapper;
import org.puregxl.merchant.admin.mq.event.CouponTaskExecuteEvent;
import org.puregxl.merchant.admin.mq.producer.CouponTaskActualExecuteProducer;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class CouponTaskJobHandler extends IJobHandler {

    /**
     * 每次最大拉取的任务数量
     */
    private static final int MAX_LIMIT = 100;
    private final CouponTaskMapper couponTaskMapper;
    private final CouponTaskActualExecuteProducer couponTaskActualExecuteProducer;

    @XxlJob(value = "couponTemplateTask")
    @Override
    public void execute() throws Exception {
        long initID = 0;
        Date now = new Date();
        log.info("[XXL-JOB] 开始执行优惠券定时任务, 当前时间: {}", now);
        while (true) {
            List<CouponTaskDO> couponTaskDOList = findTask(initID, now);

            if (CollUtil.isEmpty(couponTaskDOList)) {
                log.info("[XXL-JOB] 当前没有待执行优惠券任务");
                break;
            }

            for (CouponTaskDO couponTaskDO : couponTaskDOList) {
                //执行消费逻辑
                distributeCoupon(couponTaskDO);
            }

            if (couponTaskDOList.size() < MAX_LIMIT) {
                break;
            }

            // 更新 initId 为当前列表中最大 ID
            initID = couponTaskDOList.stream()
                    .mapToLong(CouponTaskDO::getId)
                    .max()
                    .orElse(initID);
        }
    }

    public void distributeCoupon (CouponTaskDO couponTaskDO) {
        CouponTaskDO build = CouponTaskDO.builder()
                .id(couponTaskDO.getId())
                .status(CouponTaskStatusEnum.IN_PROGRESS.getStatus()).build();//修改状态为运行中

        couponTaskMapper.updateById(build);

        //发送到下游执行分发逻辑
        CouponTaskExecuteEvent couponTaskExecuteEvent = CouponTaskExecuteEvent.builder()
                .couponTaskId(couponTaskDO.getId()).build();
        couponTaskActualExecuteProducer.sendMessage(couponTaskExecuteEvent);
    }

    private List<CouponTaskDO> findTask(long initID, Date now) {
        LambdaQueryWrapper<CouponTaskDO> queryWrapper = Wrappers.lambdaQuery(CouponTaskDO.class)
                .eq(CouponTaskDO::getStatus, CouponTaskStatusEnum.PENDING.getStatus())
                .le(CouponTaskDO::getSendTime, now)
                .gt(CouponTaskDO::getId, initID)
                .last("LIMIT " + MAX_LIMIT);

        List<CouponTaskDO> couponTaskDOS = couponTaskMapper.selectList(queryWrapper);
        return couponTaskDOS;
    }

}
