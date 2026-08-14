package com.tyh.chat.security;

import com.tyh.common.utils.SecurityUtils;
import org.springframework.stereotype.Component;

/**
 * 获取当前登录用户 ID 的小型适配组件。
 *
 * <p>单独封装后，业务类不需要直接依赖 Spring Security 的静态上下文，单元测试也可以轻松模拟用户身份。</p>
 */
@Component
public class CurrentUserProvider {

    /** 从 JWT 登录上下文读取用户 ID；未登录时 SecurityUtils 会抛出认证异常。 */
    public String currentUserId() {
        return String.valueOf(SecurityUtils.getUserId());
    }
}
