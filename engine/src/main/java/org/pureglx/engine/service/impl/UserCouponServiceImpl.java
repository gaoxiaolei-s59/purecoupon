package org.pureglx.engine.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Singleton;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.pureglx.engine.common.constant.EngineRedisConstant;
import org.pureglx.engine.common.context.UserContext;
import org.pureglx.engine.dao.entity.UserCouponDO;
import org.pureglx.engine.dao.mapper.UserCouponMapper;
import org.pureglx.engine.dto.req.CouponTemplateQueryReqDTO;
import org.pureglx.engine.dto.req.CouponTemplateRedeemReqDTO;
import org.pureglx.engine.dto.resp.CouponTemplateQueryRespDTO;
import org.pureglx.engine.service.CouponTemplateService;
import org.pureglx.engine.service.UserCouponService;
import org.puregxl.framework.exception.ClientException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCouponDO> implements UserCouponService {


    private final CouponTemplateService couponTemplateService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String STOCK_DECREMENT_AND_SAVE_USER_RECEIVE_LUA_PATH = "lua/stock_decrement_and_save_user_receive.lua";


    @Override
    public void redeemUserCoupon(CouponTemplateRedeemReqDTO requestParam) {
        //查询卷是否存在
        CouponTemplateQueryRespDTO couponTemplate = couponTemplateService.findCouponTemplate(BeanUtil.toBean(requestParam, CouponTemplateQueryReqDTO.class));
        if (couponTemplate == null) {
            throw new ClientException("用户卷不存在 检查数据的合法性");
        }
        //检查当前时间是否合法
        boolean isInTime = DateUtil.isIn(new Date(), couponTemplate.getValidStartTime(), couponTemplate.getValidEndTime());
        if (!isInTime) {
            throw new ClientException("不满足优惠卷过期时间");
        }

        /**
         * 获取消耗规则的每人限制领取数量
         */
        JSONObject jsonObject = JSON.parseObject(couponTemplate.getReceiveRule());
        String limitPerPerson = jsonObject.getString("limitPerPerson");
        String couponTemplateCacheKey = String.format(EngineRedisConstant.COUPON_TEMPLATE_KEY, requestParam.getCouponTemplateId());
        String userCouponTemplateLimitCacheKey = String.format(EngineRedisConstant.USER_COUPON_TEMPLATE_LIMIT_KEY, UserContext.getUserId(), requestParam.getCouponTemplateId());

        //lua脚本 基于给出的key判断当前用户有没有领取条件 如果有加入到redis重 没有的话返回false 和 用户领取的次数 正常扣减库存
        DefaultRedisScript<Long> longDefaultRedisScript = Singleton.get(STOCK_DECREMENT_AND_SAVE_USER_RECEIVE_LUA_PATH, () -> {
            DefaultRedisScript<Long> defaultRedisScript = new DefaultRedisScript<>();
            defaultRedisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(STOCK_DECREMENT_AND_SAVE_USER_RECEIVE_LUA_PATH)));
            defaultRedisScript.setResultType(Long.class);
            return defaultRedisScript;
        });

        stringRedisTemplate.execute(
                longDefaultRedisScript,
                ListUtil.of(couponTemplateCacheKey, userCouponTemplateLimitCacheKey),
                String.valueOf(couponTemplate.getValidEndTime().getTime()), limitPerPerson
        );



    }

}
