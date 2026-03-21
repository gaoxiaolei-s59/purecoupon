package org.puregxl.merchant.admin.template;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.Test;
import org.puregxl.merchant.admin.dao.entity.CouponTemplateDO;
import org.puregxl.merchant.admin.dao.mapper.CouponTemplateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class test {

    @Autowired
    public CouponTemplateMapper couponTemplateMapper;
    @Test
    void test() {
        LambdaQueryWrapper<CouponTemplateDO> queryWrapper = Wrappers.lambdaQuery(CouponTemplateDO.class)
                .eq(CouponTemplateDO::getShopNumber, 2031698834727518208L)
                .eq(CouponTemplateDO::getId, 2035236407418814466L);
        CouponTemplateDO couponTemplateDO = couponTemplateMapper.selectOne(queryWrapper);

        System.out.println(couponTemplateDO);

    }
}
