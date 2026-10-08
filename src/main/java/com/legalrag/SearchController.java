package com.legalrag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 检索接口。
 *
 * <p>第一版只做「关键词 → 命中法条列表」，不调用大模型。
 * 这样不需要任何 API key 就能先把整条链路跑通。
 */
@RestController
public class SearchController {

    private final LawArticleRepository repository;

    public SearchController(LawArticleRepository repository) {
        this.repository = repository;
    }

    /** 例：GET /search?q=个人信息 */
    @GetMapping("/search")
    public List<LawArticle> search(@RequestParam("q") String keyword) {
        return repository.search(keyword);
    }

    /** 例：GET /articles —— 看全部已加载的法条 */
    @GetMapping("/articles")
    public List<LawArticle> all() {
        return repository.findAll();
    }

    /** 例：GET /health —— 探活，顺便告诉你加载了多少条 */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "articleCount", repository.count()
        );
    }
}
