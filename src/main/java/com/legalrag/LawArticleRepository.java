package com.legalrag;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 法条数据源。
 *
 * <p>第一版故意做得极简：启动时把 {@code classpath:law-articles.txt} 全部读进内存，
 * 检索就是在内存里做子串匹配。数据量小的时候完全够用，而且没有任何外部依赖
 * （不需要数据库、不需要向量库），跑起来不会因为环境问题失败。
 *
 * <p>后续演进方向：换 SQLite + FTS5 做全文检索，再往后接 embedding + 向量检索。
 * 只要保持 {@link #search(String)} 这个接口不变，上层代码不用改。
 */
@Repository
public class LawArticleRepository {

    private final List<LawArticle> articles = new ArrayList<>();

    public LawArticleRepository() {
        ClassPathResource resource = new ClassPathResource("law-articles.txt");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // 跳过空行和注释行
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                // 用 limit=3 切分，保证条文正文里即使出现 "|" 也不会被切坏
                String[] parts = line.split("\\|", 3);
                if (parts.length == 3) {
                    articles.add(new LawArticle(parts[0].trim(), parts[1].trim(), parts[2].trim()));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("加载 law-articles.txt 失败", e);
        }
    }

    /** 返回全部法条。 */
    public List<LawArticle> findAll() {
        return List.copyOf(articles);
    }

    /** 按关键词做最简单的子串匹配：命中法律名称、条号或正文任意一处即算命中。 */
    public List<LawArticle> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String kw = keyword.trim().toLowerCase();
        return articles.stream()
                .filter(a -> a.law().toLowerCase().contains(kw)
                        || a.articleNo().toLowerCase().contains(kw)
                        || a.content().toLowerCase().contains(kw))
                .toList();
    }

    /** 当前已加载的法条条数。 */
    public int count() {
        return articles.size();
    }
}
