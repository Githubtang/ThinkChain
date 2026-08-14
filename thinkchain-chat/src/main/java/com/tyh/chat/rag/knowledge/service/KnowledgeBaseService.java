package com.tyh.chat.rag.knowledge.service;

import com.tyh.chat.rag.knowledge.domain.KnowledgeBase;

import java.util.List;

/** 知识库元数据服务。知识库是多个长期文档的逻辑分组，并且归属于一个登录用户。 */
public interface KnowledgeBaseService {

    /** 根据知识库 ID 查询；不存在时返回 null。 */
    KnowledgeBase getById(String id);

    /** 按用户、名称、状态等非空字段查询知识库列表。 */
    List<KnowledgeBase> list(KnowledgeBase query);

    /** 创建知识库并补充 ID、默认状态和计数初始值。 */
    int create(KnowledgeBase knowledgeBase);

    /** 更新知识库名称、说明或状态等可变信息。 */
    int update(KnowledgeBase knowledgeBase);

    /** 只删除知识库元数据；需要级联清理文档时调用 ChatResourceDeletionService。 */
    int deleteById(String id);
}
