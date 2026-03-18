package org.puregxl.distribution.mq;


import org.puregxl.distribution.mq.producter.CouponExecuteDistributionProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class test {
    @Autowired
    public CouponExecuteDistributionProducer couponExecuteDistributionProducer;


}
