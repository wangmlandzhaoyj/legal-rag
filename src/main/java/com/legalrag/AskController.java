package com.legalrag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于检索到的法条回答问题。
 *
 * <p>这是「先检索、再作答」这个思路的落地：先用 {@link LawArticleRepository} 把相关条文找出来，
 * 再把条文连同问题一起交给大模型，并要求它**只能依据给出的条文回答**。
 *
 * <p>为什么这么做——直接问模型「民法典第几条规定了隐私权」，它会流畅地编出一个不存在的条号。
 * 法律场景下，这种错误的危害比答不出来大得多。把条文先查出来喂给它，模型的角色就从
 * 「凭记忆回忆」变成「依据材料归纳」，每句话都能追溯到出处。
 *
 * <p>返回结果里带上 {@code sources}，就是命中的原始条文，方便人工核对模型有没有胡说。
 */
@RestController
public class AskController {

    /**
     * 系统提示词。这里的几条约束是这个项目最核心的东西——
     * 它把「法律场景不能容忍编造」这个业务约束，翻译成了模型能执行的规则。
     */
    private static final String SYSTEM_PROMPT = """
            你是一个法律条文助手。

            必须遵守的规则：
            1. 只能依据【可用条文】中给出的条文回答问题，不得引用任何未提供的法条。
            2. 引用时写清法律名称和条号，例如「《个人信息保护法》第十三条」。
            3. 如果【可用条文】中没有能支撑该问题的条文，直接回答「检索到的条文中没有相关规定」，
               不要用你记忆中的其他法条补充。
            4. 只做条文的整理与解释，不给出法律意见。
            """;

    private final LawArticleRepository repository;
    private final ChatClient chatClient;

    public AskController(LawArticleRepository repository, ChatModel chatModel) {
        this.repository = repository;
        this.chatClient = ChatClient.create(chatModel);
    }

    /** 例：GET /ask?q=处理个人信息需要满足什么条件 */
    @GetMapping("/ask")
    public Answer ask(@RequestParam("q") String question) {
        List<LawArticle> hits = repository.search(question);

        String context = hits.isEmpty()
                ? "（无）"
                : hits.stream()
                        .map(a -> "《" + a.law() + "》" + a.articleNo() + "：" + a.content())
                        .collect(Collectors.joining("\n\n"));

        String userMessage = "【可用条文】\n" + context + "\n\n【问题】\n" + question;

        String answer = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .call()
                .content();

        return new Answer(question, answer, hits);
    }

    /**
     * @param question 原始问题
     * @param answer   模型生成的回答
     * @param sources  本次检索命中的条文原文（用于核对回答是否有依据）
     */
    public record Answer(String question, String answer, List<LawArticle> sources) {
    }
}
