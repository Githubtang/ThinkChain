package com.tyh.chat.service.impl;

import com.tyh.chat.capability.CapabilityValidator;
import com.tyh.chat.capability.ChatCapabilityDeriver;
import com.tyh.chat.dto.ChatRequest;
import com.tyh.chat.dto.Content;
import com.tyh.chat.dto.Message;
import com.tyh.chat.registry.ModelRegistry;
import com.tyh.chat.service.ChatService;
import com.tyh.chat.vendor.VendorChatAdapter;
import com.tyh.chat.vendor.VendorChatAdapterRegistry;
import com.tyh.common.core.domain.AjaxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 对话服务实现：从 {@link ModelRegistry} 取模型、{@link CapabilityValidator} 校验能力、
 * {@link VendorChatAdapterRegistry} 解析厂商 {@link VendorChatAdapter} 并执行 SDK 调用。
 *
 * @Author: GithubTang
 * @Description: 基于厂商 SDK 适配器的 AI 对话编排服务
 * @Date: 2026/4/11
 * @Version: 1.0
 */
@Service
@Primary
public class AiChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final ModelRegistry modelRegistry;
    private final CapabilityValidator capabilityValidator;
    private final VendorChatAdapterRegistry vendorChatAdapterRegistry;

    public AiChatService(ModelRegistry modelRegistry,
                         CapabilityValidator capabilityValidator,
                         VendorChatAdapterRegistry vendorChatAdapterRegistry) {
        this.modelRegistry = modelRegistry;
        this.capabilityValidator = capabilityValidator;
        this.vendorChatAdapterRegistry = vendorChatAdapterRegistry;
    }

    @Override
    public AjaxResult chat(ChatRequest request, Set<String> requiredCapabilities) {
        try {
            if (request.getModel() == null || request.getModel().isBlank()) {
                return AjaxResult.error("模型名称不能为空");
            }
            ModelRegistry.ModelEntry model = modelRegistry.getModel(request.getModel().trim());

            Set<String> required = new LinkedHashSet<>();
            if (requiredCapabilities != null) {
                required.addAll(requiredCapabilities);
            }
            required.addAll(ChatCapabilityDeriver.derive(request));

            capabilityValidator.validate(model, required);

            VendorChatAdapter adapter = vendorChatAdapterRegistry.getRequired(model.getProvider());
            String text = adapter.invoke(model, request);
            return AjaxResult.success(text);
        } catch (IllegalArgumentException e) {
            return AjaxResult.error(e.getMessage());
        } catch (UnsupportedOperationException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("模型调用失败", e);
            return AjaxResult.error("模型调用失败: " + e.getMessage());
        }
    }

    @Override
    public AjaxResult chat(String modelName, String userInput, Set<String> requiredCapabilities) {
        ChatRequest request = new ChatRequest();
        request.setModel(modelName);
        Message message = new Message();
        message.setRole("user");
        Content content = new Content();
        content.setType("text");
        content.setText(userInput != null ? userInput : "");
        List<Message> messages = new ArrayList<>();
        message.setContents(List.of(content));
        messages.add(message);
        request.setMessages(messages);
        return chat(request, requiredCapabilities);
    }
}
