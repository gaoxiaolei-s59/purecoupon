package org.puregxl.merchant.admin.common.context;

import org.apache.catalina.User;

import java.util.Optional;

public class UserContext {

    private static final ThreadLocal<UserInfoDto> userContext = new ThreadLocal<>();

    /**
     * 设置用户上下文
     * @param userInfoDto
     */
    public void setUserContext(UserInfoDto userInfoDto) {
        userContext.set(userInfoDto);
    }

    /**
     * 删除用户上下文
     */
    public void removeUSerContext() {
        userContext.remove();
    }

    public String getUserName() {
        UserInfoDto userInfoDto = userContext.get();
        return Optional.ofNullable(userInfoDto).map(UserInfoDto::getUserName).orElse(null);
    }

    public String getUserId() {
        UserInfoDto userInfoDto = userContext.get();
        return Optional.ofNullable(userInfoDto).map(UserInfoDto::getUserID).orElse(null);
    }

    public Long shopNumber() {
        UserInfoDto userInfoDto = userContext.get();
        return Optional.ofNullable(userInfoDto).map(UserInfoDto::getShopNumber).orElse(null);
    }

}
