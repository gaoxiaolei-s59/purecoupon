package org.puregxl.settlement;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.junit.jupiter.api.Test;
import org.puregxl.settlement.common.constant.EngineRedisConstant;
import org.puregxl.settlement.dao.entity.UserCouponDO;
import org.puregxl.settlement.dao.mapper.UserCouponMapper;
import org.puregxl.settlement.dto.resp.CouponTemplateQueryRespDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

import static org.puregxl.settlement.common.constant.EngineRedisConstant.COUPON_TEMPLATE_KEY;

@SpringBootTest
public class test {

    @Autowired
    public StringRedisTemplate stringRedisTemplate;

    @Autowired
    public UserCouponMapper userCouponMapper;

    @Test
    void test() {
        // Step 1: 获取 Redis 中的用户优惠券列表
        Set<String> rangeUserCoupons = stringRedisTemplate.opsForZSet().range(String.format(EngineRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY, "2033459759583223808"), 0, -1);

        // String userCouponItemCacheKey = StrUtil.builder()
        //                .append(requestParam.getCouponTemplateId())
        //                .append("_")
        //                .append(userCouponDO.getId())
        //                .toString();


        // 构建 Redis Key 列表 coupon_engine:user-template-list:2033459759583223808
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

        for (CouponTemplateQueryRespDTO couponTemplateQueryRespDTO : couponTemplateQueryRespDTOS) {
            System.out.println(couponTemplateQueryRespDTO);
        }
    }

    @Test
    void testPineLine() {

        /**
         * coupon_engine:template:2031563926153838593
         * coupon_engine:template:2031593742290665473
         */
        // 构建 Redis Key 列表 coupon_engine:user-template-list:2033459759583223808
        List<String> couponTemplateIds = ListUtil.of("2031563926153838593", "2031593742290665473");

        List<String> list = couponTemplateIds.stream().map(each -> String.format(COUPON_TEMPLATE_KEY, each)).toList();
        // 同步获取 Redis 数据并进行解析、转换和分区
        List<Object> rawCouponDataList = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            list.forEach(each -> connection.hashCommands().hGetAll(each.getBytes()));
            return null;
        });

        List<CouponTemplateQueryRespDTO> couponTemplateQueryRespDTOS = JSON.parseArray(JSON.toJSONString(rawCouponDataList), CouponTemplateQueryRespDTO.class);


        for (CouponTemplateQueryRespDTO couponTemplateQueryRespDTO : couponTemplateQueryRespDTOS) {
            System.out.println(couponTemplateQueryRespDTO);
        }
    }

    @Test
    void test1() {
        LambdaQueryWrapper<UserCouponDO> eq = Wrappers.lambdaQuery(UserCouponDO.class)
                .eq(UserCouponDO::getCouponTemplateId, 2035236407418814466L)
                .eq(UserCouponDO::getUserId, 1810518709471555585L);

        UserCouponDO userCouponDO = userCouponMapper.selectOne(eq);

        System.out.println(userCouponDO);

    }



}
