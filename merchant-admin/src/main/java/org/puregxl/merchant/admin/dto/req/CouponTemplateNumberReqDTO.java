package org.puregxl.merchant.admin.dto.req;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "增加优惠卷模版发行数")
@Data
public class CouponTemplateNumberReqDTO {
    /**
     * 优惠券模板id
     */
    @Schema(description = "优惠券模板id",
            example = "1810966706881941507",
            required = true)
    private String couponTemplateId;

    /**
     * 增加发行数量
     */
    @Schema(description = "增加发行数量",
            example = "100",
            required = true)
    private Integer number;
}
