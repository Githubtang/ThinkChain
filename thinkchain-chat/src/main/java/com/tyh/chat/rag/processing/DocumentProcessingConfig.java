package com.tyh.chat.rag.processing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 文档后台任务配置。
 *
 * <p>使用独立线程池，避免 PDF/Office 解析和向量接口等待占用普通 HTTP 请求线程。
 * 当前项目先采用单体应用内任务队列，不引入 MQ。</p>
 */
@Configuration
@EnableScheduling
public class DocumentProcessingConfig {

    @Bean(name = "documentTaskExecutor")
    public Executor documentTaskExecutor(DocumentProcessingProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(1, properties.getCorePoolSize()));
        executor.setMaxPoolSize(Math.max(properties.getCorePoolSize(), properties.getMaxPoolSize()));
        executor.setQueueCapacity(Math.max(1, properties.getQueueCapacity()));
        executor.setThreadNamePrefix("rag-document-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
