package org.puregxl.merchant.admin.common.context;

import org.apache.catalina.User;

import java.util.Optional;

public class UserContext {

    private static final ThreadLocal<UserInfoDto> userContext = new ThreadLocal<>();

    /**
     * 设置用户上下文
     * @param userInfoDto
     */
    public static void setUserContext(UserInfoDto userInfoDto) {
        userContext.set(userInfoDto);
    }

    /**
     * 删除用户上下文
     */
    public static void removeUserContext() {
        userContext.remove();
    }

    /**
     * 获取用户名
     * @return
     */
    public static String getUserName() {
        UserInfoDto userInfoDto = userContext.get();
        return Optional.ofNullable(userInfoDto).map(UserInfoDto::getUserName).orElse(null);
    }


    /**
     * 获取用户id
     * @return
     */
    public static String getUserId() {
        UserInfoDto userInfoDto = userContext.get();
        return Optional.ofNullable(userInfoDto).map(UserInfoDto::getUserID).orElse(null);
    }

    /**
     * 获取商品序列号
     * @return
     */
    public static Long shopNumber() {
        UserInfoDto userInfoDto = userContext.get();
        return Optional.ofNullable(userInfoDto).map(UserInfoDto::getShopNumber).orElse(null);
    }

}
