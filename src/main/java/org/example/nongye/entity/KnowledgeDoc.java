package org.example.nongye.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 农事知识库文档（AI RAG 检索用）
 * embedding 由应用首次检索时自动向量化回写
 */
@Data
public class KnowledgeDoc {
    private Long id;
    private String title;
    private String category;
    private String content;
    /** 依据来源（标准号/发布机构/平台整理） */
    private String source;
    /** 知识版本（标准版本或整理月份） */
    private String docVersion;
    /** 向量 JSON 数组字符串 */
    private String embedding;
    private LocalDateTime createTime;
}
