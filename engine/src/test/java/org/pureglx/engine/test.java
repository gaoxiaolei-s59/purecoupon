package org.pureglx.engine;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import org.junit.jupiter.api.Test;
import org.pureglx.engine.common.constant.EngineRedisConstant;
import org.pureglx.engine.common.context.UserContext;
import org.pureglx.engine.dto.resp.CouponTemplateQueryRespDTO;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

import static org.pureglx.engine.common.constant.EngineRedisConstant.COUPON_TEMPLATE_KEY;

@SpringBootTest
public class test {

    public StringRedisTemplate stringRedisTemplate;

    @Test
    void test() {
        // Step 1: 获取 Redis 中的用户优惠券列表
        Set<String> rangeUserCoupons = stringRedisTemplate.opsForZSet().range(String.format(EngineRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY, UserContext.getUserId()), 0, -1);

        // String userCouponItemCacheKey = StrUtil.builder()
        //                .append(requestParam.getCouponTemplateId())
        //                .append("_")
        //                .append(userCouponDO.getId())
        //                .toString();


        // 构建 Redis Key 列表
        List<String> couponTemplateIds = rangeUserCoupons.stream()
                .map(each -> StrUtil.split(each, "_").get(0))
                .map(each ->  String.format(COUPON_TEMPLATE_KEY, each))
                .toList();

        // 同步获取 Redis 数据并进行解析、转换和分区
        List<Object> rawCouponDataList = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            couponTemplateIds.forEach(each -> connection.hashCommands().hGetAll(each.getBytes()));
            return null;
        });

        List<CouponTemplateQueryRespDTO> couponTemplateQueryRespDTOS = JSON.parseArray(JSON.toJSONString(rawCouponDataList), CouponTemplateQueryRespDTO.class);


    }


}
