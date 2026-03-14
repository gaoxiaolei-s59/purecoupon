package org.puregxl.merchant.admin.task;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import org.junit.jupiter.api.Test;
import org.puregxl.merchant.admin.common.enums.CouponTaskSendTypeEnum;
import org.puregxl.merchant.admin.common.enums.CouponTaskStatusEnum;
import org.puregxl.merchant.admin.dao.entity.CouponTaskDO;
import org.puregxl.merchant.admin.dao.mapper.CouponTaskMapper;
import org.puregxl.merchant.admin.dto.req.CouponTaskCreateReqDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.SimpleDateFormat;
import java.util.Objects;

@SpringBootTest
public class TaskExecuteTest {
    @Autowired
    private  CouponTaskMapper couponTaskMapper;


    @Test
    public void testInsertCouponTask() throws Exception {
        for (int i = 0 ; i < 1000 ; i++) {
            // 1. 构造请求参数
            CouponTaskCreateReqDTO requestParam = new CouponTaskCreateReqDTO();
            requestParam.setTaskName("发送百万优惠券推送任务");
            requestParam.setFileAddress("/Users/gaoxaiolei/IdeaProjects/Coupon/tmp/oneCoupon任务推送Excel.xlsx");
            requestParam.setNotifyType("0,3");
            requestParam.setCouponTemplateId("2031698834817851401");
            requestParam.setSendType(0);
            requestParam.setSendTime(
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .parse("2026-03-14 16:00:00")
            );

            // 2. 拷贝属性
            CouponTaskDO couponTaskDO = BeanUtil.copyProperties(requestParam, CouponTaskDO.class);

            // 3. 补充业务字段
            couponTaskDO.setBatchId(IdUtil.getSnowflakeNextId());

            // 这里测试环境先写死，避免 UserContext 取不到值报空指针
            couponTaskDO.setOperatorId(2031698834727555072L);
            couponTaskDO.setShopNumber(2031698834727550976L);

            couponTaskDO.setStatus(
                    Objects.equals(requestParam.getSendType(), CouponTaskSendTypeEnum.IMMEDIATE.getType())
                            ? CouponTaskStatusEnum.IN_PROGRESS.getStatus()
                            : CouponTaskStatusEnum.PENDING.getStatus()
            );

            // 4. 插入数据库
            couponTaskMapper.insert(couponTaskDO);

            // 5. 打印结果
            System.out.println("插入成功，主键 ID = " + couponTaskDO.getId());
            System.out.println("批次号 batchId = " + couponTaskDO.getBatchId());
        }
    }
}
