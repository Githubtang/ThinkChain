package com.tyh.chat.service;

import com.tyh.chat.dto.ChatRequest;
import com.tyh.common.core.domain.AjaxResult;

import java.util.Set;

/**
 * AI 对话应用服务接口：支持统一 {@link ChatRequest} 多模态调用与纯文本快捷方法。
 *
 * @Author: GithubTang
 * @Description: 对话服务门面（注册表 + 能力校验 + 厂商适配器编排）
 * @Date: 2026/3/29
 * @Version: 1.0
 */
public interface ChatService {

    /**
     * 多模态/多轮：使用统一 {@link ChatRequest}；能力标签由调用方传入，并与 {@link com.tyh.chat.capability.ChatCapabilityDeriver} 推导结果合并后校验。
     *
     * @param request                对话请求
     * @param requiredCapabilities   调用方声明的必备能力，可为 null 或空（仅使用推导结果）
     * @return 统一 Ajax 封装，data 为模型文本回复
     */
    AjaxResult chat(ChatRequest request, Set<String> requiredCapabilities);

    /**
     * 纯文本单轮快捷方式：内部组装单条 user 文本 {@link ChatRequest} 后转调 {@link #chat(ChatRequest, Set)}。
     *
     * @param modelName              逻辑模型名
     * @param userInput              用户文本
     * @param requiredCapabilities   必备能力标签
     * @return 统一 Ajax 封装
     */
    AjaxResult chat(String modelName, String userInput, Set<String> requiredCapabilities);
}
