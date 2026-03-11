package org.puregxl.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.puregxl.framework.exception.ClientException;
import org.puregxl.merchant.admin.common.constant.MerchantAdminRedisConstant;
import org.puregxl.merchant.admin.common.context.UserContext;
import org.puregxl.merchant.admin.common.enums.CouponTemplateStatusEnum;
import org.puregxl.merchant.admin.common.enums.DiscountTargetEnum;
import org.puregxl.merchant.admin.dao.entity.CouponTemplateDO;
import org.puregxl.merchant.admin.dao.mapper.CouponTemplateMapper;
import org.puregxl.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import org.puregxl.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;
import org.puregxl.merchant.admin.service.CouponTemplateService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplateDO> implements CouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final int maxStock = 20000000;

    @Override
    public void createCouponTemplate(CouponTemplateSaveReqDTO requestParam) {
        // 验证必填参数是否为空或空的字符串
        if (StrUtil.isEmpty(requestParam.getName())) {
            throw new ClientException("优惠券名称不能为空");
        }

        if (ObjectUtil.isEmpty(requestParam.getSource())) {
            throw new ClientException("优惠券来源不能为空");
        }

        if (ObjectUtil.isEmpty(requestParam.getTarget())) {
            throw new ClientException("优惠对象不能为空");
        }

        if (ObjectUtil.isEmpty(requestParam.getType())) {
            throw new ClientException("优惠类型不能为空");
        }

        if (ObjectUtil.isEmpty(requestParam.getValidStartTime())) {
            throw new ClientException("有效期开始时间不能为空");
        }

        if (ObjectUtil.isEmpty(requestParam.getValidEndTime())) {
            throw new ClientException("有效期结束时间不能为空");
        }

        if (ObjectUtil.isEmpty(requestParam.getStock())) {
            throw new ClientException("库存不能为空");
        }

        if (StrUtil.isEmpty(requestParam.getReceiveRule())) {
            throw new ClientException("领取规则不能为空");
        }

        if (StrUtil.isEmpty(requestParam.getConsumeRule())) {
            throw new ClientException("消耗规则不能为空");
        }
        boolean flag = false;
        for (DiscountTargetEnum value : DiscountTargetEnum.values()) {
            if (requestParam.getTarget() == value.getType()) {
                flag = true;
            }
        }
        if (!flag) {
            throw new ClientException("优惠类型不存在");
        }

        Date now = new Date();
        //有效期时间不能早于当前时间 当前时间不能晚于有效期时间
        if (!requestParam.getValidStartTime().before(now) || !requestParam.getValidEndTime().after(now)) {

        }

        if (requestParam.getStock() < 0 || requestParam.getSource() > maxStock) {
            throw new ClientException("库存信息错误");
        }

        if (!JSON.isValid(requestParam.getReceiveRule())) {
            throw new ClientException("领取规则JSON格式错误");
        }

        if (!JSON.isValid(requestParam.getConsumeRule())) {
            throw new ClientException("消耗规则JSON格式错误");
        }

       if (ObjectUtil.equals(requestParam.getTarget(), DiscountTargetEnum.PRODUCT_SPECIFIC)) {
           //调用商品中台服务
       }

       //新增信息到数据库中
        CouponTemplateDO couponTemplateDO = BeanUtil.toBean(requestParam, CouponTemplateDO.class);
        couponTemplateDO.setShopNumber(UserContext.getShopNumber());
        couponTemplateDO.setStatus(CouponTemplateStatusEnum.ACTIVE.getStatus());
        couponTemplateMapper.insert(couponTemplateDO);
        //调用Redis服务把信息放入redis

        // 缓存预热：通过将数据库的记录序列化成 JSON 字符串放入 Redis 缓存
        CouponTemplateQueryRespDTO actualRespDTO = BeanUtil.toBean(couponTemplateDO, CouponTemplateQueryRespDTO.class);
        Map<String, Object> cacheTargetMap = BeanUtil.beanToMap(actualRespDTO, false, true);
        Map<String, String> actualCacheTargetMap = cacheTargetMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() != null ? entry.getValue().toString() : ""
                ));

        String couponTemplateCacheKey = String.format(MerchantAdminRedisConstant.COUPON_TEMPLATE_KEY, couponTemplateDO.getId());
        stringRedisTemplate.opsForHash().putAll(couponTemplateCacheKey, actualCacheTargetMap);

    }
}
