package org.puregxl.merchant.admin.mq;


import org.puregxl.merchant.admin.mq.producer.CouponTemplateDelayExecuteTaskProductor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class test {


    @Autowired
    private final CouponTemplateDelayExecuteTaskProductor couponTemplateDelayExecuteTaskProductor;

    public test(CouponTemplateDelayExecuteTaskProductor couponTemplateDelayExecuteTaskProductor) {
        this.couponTemplateDelayExecuteTaskProductor = couponTemplateDelayExecuteTaskProductor;
    }





}
