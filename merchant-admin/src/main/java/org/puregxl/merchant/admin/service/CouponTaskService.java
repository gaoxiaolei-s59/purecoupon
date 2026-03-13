package org.puregxl.merchant.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.puregxl.merchant.admin.dao.entity.CouponTaskDO;
import org.puregxl.merchant.admin.dto.req.CouponTaskCreateReqDTO;

public interface CouponTaskService extends IService<CouponTaskDO> {
    /**
     * 创建优惠卷任务
     * @param requestParam
     */
    void createCouponTask(CouponTaskCreateReqDTO requestParam);
}
