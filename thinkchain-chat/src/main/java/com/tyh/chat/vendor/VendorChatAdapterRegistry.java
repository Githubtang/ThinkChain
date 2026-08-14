package com.tyh.chat.vendor;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 收集容器中全部 {@link VendorChatAdapter}，按 {@link VendorChatAdapter#providerId()} 建立索引，
 * 供业务层根据模型配置的 {@code provider} 解析具体 SDK 适配器。
 *
 * @Author: GithubTang
 * @Description: 厂商适配器注册表，按 provider 查找实现
 * @Date: 2026/4/11
 * @Version: 1.0
 */
@Component
public class VendorChatAdapterRegistry {

    private final Map<String, VendorChatAdapter> byProvider;

    /**
     * @param adapters 容器中全部 {@link VendorChatAdapter} Bean（含各厂商实现与占位）
     */
    public VendorChatAdapterRegistry(List<VendorChatAdapter> adapters) {
        // Spring 会把容器中所有 VendorChatAdapter 实现自动注入到这个列表。
        Map<String, VendorChatAdapter> map = new LinkedHashMap<>();
        for (VendorChatAdapter adapter : adapters) {
            String id = adapter.providerId().toLowerCase(Locale.ROOT);
            // 相同 provider 只能有一个实现，否则系统无法判断该调用哪一个。
            if (map.put(id, adapter) != null) {
                throw new IllegalStateException("重复的 VendorChatAdapter，providerId=" + id);
            }
        }
        this.byProvider = Map.copyOf(map);
    }

    /**
     * @param provider 与 YAML 中模型 {@code provider} 一致（忽略大小写）
     * @return 对应适配器
     * @throws IllegalArgumentException 未注册或 provider 为空
     */
    public VendorChatAdapter getRequired(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("模型 provider 不能为空");
        }
        String id = provider.toLowerCase(Locale.ROOT);
        VendorChatAdapter adapter = byProvider.get(id);
        if (adapter == null) {
            throw new IllegalArgumentException("未注册厂商适配器: " + provider + "，已注册: " + byProvider.keySet());
        }
        return adapter;
    }
}
