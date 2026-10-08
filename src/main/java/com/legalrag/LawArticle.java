package com.legalrag;

/**
 * 一条法律条文。
 *
 * @param law       法律名称，如「中华人民共和国个人信息保护法」
 * @param articleNo 条号，如「第十三条」
 * @param content   条文正文
 */
public record LawArticle(String law, String articleNo, String content) {
}
