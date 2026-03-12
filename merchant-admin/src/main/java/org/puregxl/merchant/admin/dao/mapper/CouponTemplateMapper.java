package org.puregxl.merchant.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.puregxl.merchant.admin.dao.entity.CouponTemplateDO;

@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplateDO> {

    int increaseNumberCouponTemplate(@Param("shopNumber") Long shopNumber, @Param("couponTemplateId") String couponTemplateId, @Param("number") Integer number);
}
