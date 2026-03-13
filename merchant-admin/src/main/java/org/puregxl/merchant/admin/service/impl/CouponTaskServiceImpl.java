package org.puregxl.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponTaskServiceImpl extends ServiceImpl<CouponTaskMapper, CouponTaskDO> implements CouponTaskService {


    private final CouponTemplateService couponTemplateService;
    private final CouponTaskMapper couponTaskMapper;
    /**
     * 创建优惠卷任务
     * @param requestParam
     */
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

        // 通过 EasyExcel 监听器获取 Excel 中所有行数
        RowCountListener listener = new RowCountListener();
        EasyExcel.read(requestParam.getFileAddress(), listener).sheet().doRead();

        // 为什么需要统计行数？因为发送后需要比对所有优惠券是否都已发放到用户账号
        int totalRows = listener.getRowCount();
        couponTaskDO.setSendNum(totalRows);

        // 保存优惠券推送任务记录到数据库
        couponTaskMapper.insert(couponTaskDO);
    }
}
