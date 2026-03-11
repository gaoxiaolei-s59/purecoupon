package org.puregxl.merchant.admin.service.chain;

import org.springframework.core.Ordered;

public interface MerchantAdminAbstractChainHandler<T> extends Ordered {
    /**
     * 执行逻辑接口
     * @param requestParam
     */
   void handler(T requestParam);

    /**
     * 标识
     * @return
     */
   String mark();
}
