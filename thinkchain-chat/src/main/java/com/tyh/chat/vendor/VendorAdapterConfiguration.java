package com.tyh.chat.vendor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 注册暂未实现官方 SDK 的厂商占位 {@link VendorChatAdapter} Bean（openai、zhipu）；
 * 接入真实 SDK 后可删除对应 {@link Bean} 并改为 {@code @Component} 实现类。
 *
 * @Author: GithubTang
 * @Description: 厂商占位适配器 Spring 配置
 * @Date: 2026/4/11
 * @Version: 1.0
 */
@Configuration
public class VendorAdapterConfiguration {

    @Bean
    public VendorChatAdapter openAiVendorPlaceholder() {
        return new PlaceholderVendorChatAdapter("openai");
    }

    @Bean
    public VendorChatAdapter zhipuVendorPlaceholder() {
        return new PlaceholderVendorChatAdapter("zhipu");
    }
}
