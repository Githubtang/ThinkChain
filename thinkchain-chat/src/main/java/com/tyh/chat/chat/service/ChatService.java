package com.tyh.chat.chat.service;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.common.core.domain.AjaxResult;

import java.util.Set;

/**
 * AI 对话应用服务。
 *
 * @Author: GithubTang
 * @Description: 对话服务门面
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public interface ChatService {

    AjaxResult chat(ChatRequest request, Set<String> requiredCapabilities);

    AjaxResult chat(String modelName, String userInput, Set<String> requiredCapabilities);
}
