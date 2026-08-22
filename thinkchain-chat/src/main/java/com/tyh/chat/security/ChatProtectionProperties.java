package com.tyh.chat.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** AI 请求资源保护配置，对应 application-ai.yml 中 thinkchain.chat.protection。 */
@Component
@ConfigurationProperties(prefix = "thinkchain.chat.protection")
public class ChatProtectionProperties {

    /** 单个用户允许同时保持的 SSE 对话连接数。 */
    private int maxConcurrentStreams = 2;

    public int getMaxConcurrentStreams() {
        return maxConcurrentStreams;
    }

    public void setMaxConcurrentStreams(int maxConcurrentStreams) {
        this.maxConcurrentStreams = maxConcurrentStreams;
    }
}
