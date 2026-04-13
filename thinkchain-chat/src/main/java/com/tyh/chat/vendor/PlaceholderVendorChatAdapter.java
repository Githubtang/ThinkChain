package com.tyh.chat.vendor;

import com.tyh.chat.dto.ChatRequest;
import com.tyh.chat.registry.ModelRegistry;

/**
 * 尚未接入官方 SDK 的厂商占位实现：保证 Spring 容器中存在对应 {@link VendorChatAdapter} Bean，
 * 实际调用时抛出明确提示，引导在 {@code com.tyh.chat.vendor} 下实现真实适配器。
 *
 * @Author: GithubTang
 * @Description: 占位适配器，避免未接入 SDK 的厂商导致启动失败
 * @Date: 2026/4/11
 * @Version: 1.0
 */
public final class PlaceholderVendorChatAdapter implements VendorChatAdapter {

    private final String providerId;

    public PlaceholderVendorChatAdapter(String providerId) {
        this.providerId = providerId;
    }

    @Override
    public String providerId() {
        return providerId;
    }

    @Override
    public String invoke(ModelRegistry.ModelEntry model, ChatRequest request) {
        throw new UnsupportedOperationException(
                "厂商 [" + providerId + "] 尚未接入官方 Java SDK：请在 com.tyh.chat.vendor 下实现 VendorChatAdapter 并注册为 Spring Bean。");
    }
}
