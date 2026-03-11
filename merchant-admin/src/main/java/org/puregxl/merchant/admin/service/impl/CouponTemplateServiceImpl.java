package org.puregxl.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.puregxl.merchant.admin.common.constant.MerchantAdminRedisConstant;
import org.puregxl.merchant.admin.common.context.UserContext;
import org.puregxl.merchant.admin.common.enums.CouponTemplateStatusEnum;
import org.puregxl.merchant.admin.dao.entity.CouponTemplateDO;
import org.puregxl.merchant.admin.dao.mapper.CouponTemplateMapper;
import org.puregxl.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import org.puregxl.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;
import org.puregxl.merchant.admin.service.CouponTemplateService;
import org.puregxl.merchant.admin.service.chain.MerchantAdminContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

import static org.puregxl.merchant.admin.common.enums.ChainEnum.MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY;


@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplateDO> implements CouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final MerchantAdminContext<CouponTemplateSaveReqDTO> merchantAdminContext;


    @Override
    public void createCouponTemplate(CouponTemplateSaveReqDTO requestParam) {

        merchantAdminContext.execute(MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY.name(), requestParam);

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
