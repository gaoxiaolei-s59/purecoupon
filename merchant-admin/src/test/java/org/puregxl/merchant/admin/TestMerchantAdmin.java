package org.puregxl.merchant.admin;


import org.junit.jupiter.api.Test;
import org.puregxl.merchant.admin.dao.entity.CouponTemplateDO;
import org.puregxl.merchant.admin.dao.mapper.CouponTemplateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest

public class TestMerchantAdmin {

    @Autowired
    private  CouponTemplateMapper couponTemplateMapper;

    @Test
    public void test(){

        CouponTemplateDO couponTemplateDO = new CouponTemplateDO();
        couponTemplateDO.setName("新用户满100减20券");
        couponTemplateDO.setShopNumber(10001L);
        couponTemplateDO.setSource(0);
        couponTemplateDO.setTarget(1);
        couponTemplateDO.setGoods("SPU10001");
        couponTemplateDO.setType(1);
        couponTemplateDO.setValidStartTime(LocalDateTime.now());
        couponTemplateDO.setValidEndTime(LocalDateTime.now().plusDays(30));
        couponTemplateDO.setStock(1000);
        couponTemplateDO.setReceiveRule("{\"limitPerUser\":1,\"receiveStartTime\":\"2026-03-10 00:00:00\",\"receiveEndTime\":\"2026-03-31 23:59:59\"}");
        couponTemplateDO.setConsumeRule("{\"minimumAmount\":100,\"discountAmount\":20,\"canStack\":false}");
        couponTemplateDO.setStatus(0);

        couponTemplateMapper.insert(couponTemplateDO);

    }
}
