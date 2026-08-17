package com.tyh.chat.vendor;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.model.ModelEntry;

import java.util.function.Consumer;

/**
 * 厂商聊天适配器：把项目统一的对话请求转换为某个模型厂商能够理解的 SDK 请求。
 *
 * <p>不同 {@code provider} 对应不同实现类，由 {@link VendorChatAdapterRegistry} 统一查找。
 * 这种“适配器模式”把厂商差异限制在小范围内，上层 ChatService 不需要出现大量 if/else。</p>
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
     * @return 统一厂商结果，包含助手文本、原始响应和可选 Token 用量
     * @throws Exception SDK 或网络异常
     */
    VendorChatResult invoke(ModelEntry model, ChatRequest request) throws Exception;

    /**
     * 执行流式调用，并把模型新生成的文本片段逐段交给 onDelta。
     *
     * <p>不支持原生流式的厂商默认退化为一次完整输出；DashScope 适配器会覆盖此方法，
     * 使用官方 SDK 的 streamCall 实现真正增量输出。</p>
     */
    default VendorChatResult stream(ModelEntry model, ChatRequest request,
                                    Consumer<String> onDelta) throws Exception {
        VendorChatResult result = invoke(model, request);
        if (result.getContent() != null && !result.getContent().isEmpty()) {
            onDelta.accept(result.getContent());
        }
        return result;
    }
}
