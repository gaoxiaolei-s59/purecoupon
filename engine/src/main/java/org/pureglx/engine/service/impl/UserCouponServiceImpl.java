package org.pureglx.engine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.pureglx.engine.dao.entity.UserCouponDO;
import org.pureglx.engine.dao.mapper.UserCouponMapper;
import org.pureglx.engine.dto.req.CouponTemplateRedeemReqDTO;
import org.pureglx.engine.service.UserCouponService;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl extends ServiceImpl<UserCouponMapper, UserCouponDO> implements UserCouponService {


    @Override
    public void redeemUserCoupon(CouponTemplateRedeemReqDTO requestParam) {

    }


}
