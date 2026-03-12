package org.puregxl.merchant.admin.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.puregxl.merchant.admin.dao.entity.CouponTemplateDO;

@Schema(description = "分页查询优惠卷模版参数")
@Data
public class CouponTemplatePageReqDTO extends Page<CouponTemplateDO> {


    /**
     * 优惠卷名称
     */
    @Schema(description = "优惠券名称",
            example = "用户下单满10减3特大优惠",
            required = true)
    private String name;


    /**
     * 优惠对象
     */
    @Schema(description = "优惠对象 0：商品专属 1：全店通用",
            example = "1",
            required = true)
    private Integer target;

    /**
     * 优惠卷编码
     */
    @Schema(description = "优惠商品编码")
    private String goods;

    /**
     * 优惠类型 0：立减券 1：满减券 2：折扣券
     */
    @Schema(description = "优惠类型 0：立减券 1：满减券 2：折扣券")
    private Integer type;
}
