package com.tyh.chat.vendor;

import com.tyh.chat.dto.ChatRequest;
import com.tyh.chat.registry.ModelRegistry;

/**
 * 厂商聊天适配器：以各厂家官方 Java SDK 为主调用多模态/对话模型，与 LangChain4j 解耦；
 * 不同 {@code provider} 对应不同实现类，由 {@link VendorChatAdapterRegistry} 统一解析。
 *
 * @Author: GithubTang
 * @Description: 按厂商官方 SDK 调用多模态/对话模型；不同 provider 对应不同实现类
 * @Date: 2026/4/11
 * @Version: 1.0
 */
public interface VendorChatAdapter {

    /**
     * 厂商标识，与 {@code application-ai.yml} 中 {@code provider} 一致（小写），例如 dashscope、openai、zhipu。
     *
     * @return provider 字符串
     */
    String providerId();

    /**
     * 使用已解析的模型配置执行一次调用，返回模型文本回复。
     *
     * @param model   注册表中的模型条目（含 apiKey、modelName 等）
     * @param request 统一领域请求 {@link ChatRequest}
     * @return 助手文本内容
     * @throws Exception SDK 或网络异常
     */
    String invoke(ModelRegistry.ModelEntry model, ChatRequest request) throws Exception;
}
