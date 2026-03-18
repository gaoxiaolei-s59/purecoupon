package org.pureglx.engine.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.pureglx.engine.dao.entity.CouponTemplateDO;

@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplateDO> {

    int decrementCouponTemplateStock(@Param("shopNumber")Long shopNumber, @Param("couponTemplateId") Long couponTemplateId, @Param("number") Integer number);
}
