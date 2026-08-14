package com.tyh.chat.rag.session.service;

import com.tyh.chat.rag.session.domain.SessionDocument;

/**
 * 会话临时文档解析接口。
 *
 * <p>处理过程与知识库文档类似，但数据作用域是单个会话，适合用户只在当前对话中使用的资料。</p>
 */
public interface SessionDocumentParseService {

    /** 读取文件、重新生成切片并更新解析状态，失败时返回状态为 FAILED 的文档。 */
    SessionDocument parse(String documentId);
}
