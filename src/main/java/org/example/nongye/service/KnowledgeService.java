package org.example.nongye.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.example.nongye.entity.KnowledgeDoc;
import org.example.nongye.mapper.KnowledgeDocMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 农事知识库 RAG 检索服务
 * 流程：知识入库时配置好正文 → 首次检索时批量调 DashScope embeddings 向量化并回写数据库
 *      → 检索时查询向量化 → Java 余弦相似度排序取 top-k（数据量小，全表扫足够快）
 * 降级：无 API Key 或向量化失败时自动退化为关键词包含匹配，保证演示不中断
 */
@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);
    private static final String EMBED_URL =
            "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings";

    @Autowired
    private KnowledgeDocMapper docMapper;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.embedding-model:text-embedding-v3}")
    private String embeddingModel;

    /** 最低相关度门槛：低于该余弦相似度的片段不返回，避免"答非所问" */
    @Value("${ai.rag-min-score:0.32}")
    private double minScore;

    /** 文档向量内存缓存：id → double[]，避免每次请求重复计算/读库 */
    private volatile Map<Long, double[]> vectorCache = new HashMap<>();
    /** 向量缓存对应的文档数量，用于检测知识库变化后自动重建 */
    private volatile int cachedDocCount = -1;

    /** 启动时异步预热向量，不阻塞应用启动 */
    @PostConstruct
    public void warmUp() {
        new Thread(this::ensureVectors, "knowledge-warmup").start();
    }

    /**
     * RAG 检索入口：返回最相关的 topK 条知识
     */
    public List<Map<String, Object>> search(String query, int topK) {
        List<KnowledgeDoc> docs = docMapper.selectAll();
        if (docs.isEmpty()) return new ArrayList<>();

        // 向量可用 → 余弦相似度；否则 → 关键词匹配
        if (!apiKey.isEmpty() && ensureVectors()) {
            double[] qv = embedOne(query);
            if (qv != null) {
                List<Map<String, Object>> scored = new ArrayList<>();
                for (KnowledgeDoc doc : docs) {
                    double[] dv = vectorCache.get(doc.getId());
                    if (dv == null) continue;
                    double score = cosine(qv, dv);
                    // 相关度门槛：不达标的知识不返回，避免模型拿无关内容编答案
                    if (score < minScore) continue;
                    Map<String, Object> item = toItem(doc);
                    item.put("score", Math.round(score * 1000) / 1000.0);
                    scored.add(item);
                }
                scored.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
                return scored.subList(0, Math.min(topK, scored.size()));
            }
        }
        return keywordSearch(docs, query, topK);
    }

    /** 关键词降级检索：按字面包含打分 */
    private List<Map<String, Object>> keywordSearch(List<KnowledgeDoc> docs, String query, int topK) {
        List<Map<String, Object>> scored = new ArrayList<>();
        for (KnowledgeDoc doc : docs) {
            int score = 0;
            for (String key : splitKeywords(query)) {
                if (doc.getTitle().contains(key) || doc.getContent().contains(key)) score += 2;
                if (doc.getCategory() != null && doc.getCategory().contains(key)) score += 1;
            }
            if (score > 0) {
                Map<String, Object> item = toItem(doc);
                item.put("score", score / 10.0);
                scored.add(item);
            }
        }
        scored.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        return scored.subList(0, Math.min(topK, scored.size()));
    }

    /** 极简分词：按常见标点/空白切分并过滤短词 */
    private List<String> splitKeywords(String query) {
        List<String> keys = new ArrayList<>();
        for (String w : query.split("[，。？！、,.?\\s]+")) {
            if (w.length() >= 2) keys.add(w);
        }
        if (keys.isEmpty() && !query.isEmpty()) keys.add(query);
        return keys;
    }

    /**
     * 确保所有文档已向量化（内存缓存 + 数据库回写）
     * @return true 表示向量可用
     */
    private boolean ensureVectors() {
        try {
            int count = docMapper.countAll();
            if (count == cachedDocCount && !vectorCache.isEmpty()) return true;

            Map<Long, double[]> fresh = new HashMap<>();
            for (KnowledgeDoc doc : docMapper.selectAll()) {
                double[] vec = parseVector(doc.getEmbedding());
                if (vec == null) {
                    // 未向量化：调 embeddings 接口计算并回写数据库，重启后无需重算
                    vec = embedOne(doc.getTitle() + "\n" + doc.getContent());
                    if (vec != null) {
                        docMapper.updateEmbedding(doc.getId(), JSONUtil.toJsonStr(toList(vec)));
                    }
                }
                if (vec != null) fresh.put(doc.getId(), vec);
            }
            vectorCache = fresh;
            cachedDocCount = count;
            log.info("知识库向量化完成，共 {} 条", fresh.size());
            return fresh.size() == count && count > 0;
        } catch (Exception e) {
            log.error("知识库向量化失败，将使用关键词匹配", e);
            return false;
        }
    }

    /** 单条文本向量化 */
    private double[] embedOne(String text) {
        try {
            JSONObject body = new JSONObject()
                    .set("model", embeddingModel)
                    .set("input", text);
            HttpResponse resp = HttpRequest.post(EMBED_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(20000)
                    .execute();
            if (!resp.isOk()) {
                log.error("Embeddings 接口返回非 200：{}", resp.body());
                return null;
            }
            JSONArray emb = JSONUtil.parseObj(resp.body())
                    .getJSONArray("data").getJSONObject(0).getJSONArray("embedding");
            double[] vec = new double[emb.size()];
            for (int i = 0; i < emb.size(); i++) vec[i] = emb.getDouble(i);
            return vec;
        } catch (Exception e) {
            log.error("调用向量化接口失败", e);
            return null;
        }
    }

    /** 余弦相似度 */
    private double cosine(double[] a, double[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return (na == 0 || nb == 0) ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private double[] parseVector(String json) {
        try {
            if (json == null || json.isEmpty()) return null;
            JSONArray arr = JSONUtil.parseArray(json);
            double[] vec = new double[arr.size()];
            for (int i = 0; i < arr.size(); i++) vec[i] = arr.getDouble(i);
            return vec;
        } catch (Exception e) {
            return null;
        }
    }

    private List<Double> toList(double[] vec) {
        List<Double> list = new ArrayList<>(vec.length);
        for (double v : vec) list.add(v);
        return list;
    }

    private Map<String, Object> toItem(KnowledgeDoc doc) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", doc.getId());
        item.put("title", doc.getTitle());
        item.put("category", doc.getCategory());
        item.put("content", doc.getContent());
        item.put("source", doc.getSource());
        item.put("docVersion", doc.getDocVersion());
        return item;
    }
}
