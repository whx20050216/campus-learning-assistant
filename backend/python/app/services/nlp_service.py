import os
import re
import math
import logging
from typing import List, Dict, Tuple
from collections import Counter

import jieba
import jieba.analyse
import jieba.posseg as pseg

from app.models.schemas import KeywordItem, KnowledgePointItem, NlpResponse

logger = logging.getLogger(__name__)

# 加载停用词（内置简易停用词表）
_STOP_WORDS = set([
    "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也",
    "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这", "那",
    "之", "与", "及", "等", "或", "但", "而", "因", "于", "则", "以", "为", "被", "把",
    "将", "向", "从", "对", "关于", "由于", "根据", "通过", "以及", "及其", "其中", "可以",
    "进行", "需要", "已经", "正在", "具有", "作为", "成为", "表示", "包括", "例如", "如下",
    "如下所述", "所述", "所述的", "上述", "以下", "一下", "一些", "一种", "一个", "一些",
    "（", "）", "【", "】", "《", "》", "，", "。", "；", "：", "？", "！", "\"", "\"",
    "'", "'", "、", "·", "…", "——", "—", "\n", "\t", " ", "\r"
])


def _is_stop_word(word: str) -> bool:
    if len(word.strip()) <= 1:
        return True
    if word in _STOP_WORDS:
        return True
    # 纯数字、纯英文短词、纯标点
    if re.match(r"^[0-9]+$", word):
        return True
    if re.match(r"^[^\u4e00-\u9fa5a-zA-Z0-9]+$", word):
        return True
    return False


class NlpService:
    """
    NLP 服务类：关键词提取、摘要生成、知识点识别。
    """

    def __init__(self):
        pass

    def analyze(self, text: str) -> NlpResponse:
        """
        对文本进行完整 NLP 分析。
        """
        if not text or len(text.strip()) == 0:
            return NlpResponse(
                keywords=[],
                summary="",
                knowledge_points=[],
                source="local"
            )

        clean_text = re.sub(r'\s+', ' ', text).strip()

        keywords = self.extract_keywords(clean_text, top_k=10)
        summary = self.generate_summary(clean_text, length=100)
        knowledge_points = self.extract_knowledge_points(clean_text)

        return NlpResponse(
            keywords=keywords,
            summary=summary,
            knowledge_points=knowledge_points,
            source="local"
        )

    def extract_keywords(self, text: str, top_k: int = 10) -> List[KeywordItem]:
        """
        基于 TF-IDF 提取关键词。

        公式：
        TF(t,d) = count(t) / |words|
        IDF(t,D) = log(|D| / (|{d∈D : t∈d}| + 1))
        TF-IDF(t) = TF(t,d) × IDF(t,D)

        当前为单文档场景，采用 jieba 内置 IDF 词库作为语料库近似。
        """
        words = list(jieba.cut(text, cut_all=False))
        words = [w.strip() for w in words if not _is_stop_word(w.strip())]

        if not words:
            return []

        total_words = len(words)
        word_counts = Counter(words)

        # 使用 jieba 内置 IDF 词库
        tfidf_inst = jieba.analyse.default_tfidf
        idf_freq = tfidf_inst.idf_freq
        median_idf = tfidf_inst.median_idf

        tfidf_scores = []
        for word, count in word_counts.items():
            tf = count / total_words
            idf = idf_freq.get(word, median_idf)
            tfidf = tf * idf
            tfidf_scores.append((word, tfidf))

        # 按 TF-IDF 降序取 Top-K
        tfidf_scores.sort(key=lambda x: x[1], reverse=True)
        top = tfidf_scores[:top_k]

        return [
            KeywordItem(keyword=word, weight=round(score, 6), type="keyword")
            for word, score in top
        ]

    def generate_summary(self, text: str, length: int = 100) -> str:
        """
        基于 TextRank 生成摘要。

        公式：
        分句：S = {s₁, s₂, ..., sₙ}
        构建图 G(V,E)：顶点 V = S，边权
            wᵢⱼ = |{wₖ : wₖ∈sᵢ ∧ wₖ∈sⱼ}| / (log|sᵢ| + log|sⱼ|)
        迭代得分（阻尼系数 d = 0.85，收敛阈值 ε = 0.0001）：
            WS(vᵢ) = (1-d) + d × Σ(vⱼ∈In(vᵢ)) [wⱼᵢ / Σ(vₖ∈Out(vⱼ))wⱼₖ] × WS(vⱼ)
        取 Top-3 句子拼接为摘要，目标长度 100 字。
        """
        sentences = re.split(r'[。！？.!?]', text)
        sentences = [s.strip() for s in sentences if len(s.strip()) > 5]

        if len(sentences) == 0:
            return text[:length]
        if len(sentences) <= 3:
            summary = "。".join(sentences)
            if not summary.endswith("。"):
                summary += "。"
            return summary[:length]

        # 分词
        sent_words = []
        for sent in sentences:
            words = [w.strip() for w in jieba.cut(sent) if not _is_stop_word(w.strip())]
            sent_words.append(set(words))

        n = len(sentences)

        # 构建邻接矩阵（边权）
        weight_matrix = [[0.0] * n for _ in range(n)]
        for i in range(n):
            for j in range(n):
                if i == j:
                    continue
                common = sent_words[i] & sent_words[j]
                if not common:
                    continue
                denom = math.log(len(sent_words[i]) + 1) + math.log(len(sent_words[j]) + 1)
                if denom == 0:
                    continue
                weight_matrix[i][j] = len(common) / denom

        # TextRank 迭代
        d = 0.85
        eps = 0.0001
        ws = [1.0] * n

        for _ in range(100):  # 最多 100 轮
            new_ws = [0.0] * n
            for i in range(n):
                summation = 0.0
                for j in range(n):
                    if weight_matrix[j][i] == 0:
                        continue
                    out_sum = sum(weight_matrix[j][k] for k in range(n) if k != j)
                    if out_sum == 0:
                        continue
                    summation += (weight_matrix[j][i] / out_sum) * ws[j]
                new_ws[i] = (1 - d) + d * summation

            # 检查收敛
            delta = sum(abs(new_ws[i] - ws[i]) for i in range(n))
            ws = new_ws
            if delta < eps:
                break

        # 按得分排序，取 Top-3，保持原文顺序
        ranked = sorted(enumerate(ws), key=lambda x: x[1], reverse=True)
        top3_indices = sorted([idx for idx, _ in ranked[:3]])
        summary_sentences = [sentences[i] for i in top3_indices]

        summary = "。".join(summary_sentences)
        if not summary.endswith("。"):
            summary += "。"

        return summary[:length]

    def extract_knowledge_points(self, text: str) -> List[KnowledgePointItem]:
        """
        使用正则匹配提取知识点。

        模式：
        - 定义："XXX是指"、"XXX的定义为"、"XXX是..."
        - 公式：LaTeX 格式或特殊符号（∑、∫、=、+、-、×、÷）
        - 定理：包含"定理"、"引理"、"推论"、"性质"、"法则"
        - 例题：包含"例"、"例题"、"例如"、"示例"
        """
        results = []
        lines = re.split(r'[\n。！？]', text)

        for idx, line in enumerate(lines):
            line = line.strip()
            if len(line) < 5:
                continue

            kp_type = None

            # 定理/引理/推论
            if re.search(r'(定理|引理|推论|性质|法则)\s*\d*[：:.]', line):
                kp_type = "theorem"
            # 定义模式
            elif re.search(r'(.{2,20})(是指|的定义为|定义为|是|称作|称为)', line):
                kp_type = "definition"
            # 公式模式（LaTeX 或特殊符号）
            elif re.search(r'[\$\\∑∫∏∂√∞±×÷=<>≤≥≠≈∈∉⊂⊃∪∩→←⇒⇔]+', line):
                kp_type = "formula"
            # 例题模式
            elif re.search(r'^(例\s*\d*[\s：:.]|例题|例如|示例)', line):
                kp_type = "example"

            if kp_type:
                results.append(KnowledgePointItem(
                    content=line[:200],
                    type=kp_type,
                    position=f"第{idx+1}句"
                ))

        # 去重并限制数量
        seen = set()
        unique = []
        for kp in results:
            if kp.content not in seen:
                seen.add(kp.content)
                unique.append(kp)
                if len(unique) >= 20:
                    break

        return unique


# 全局单例
_nlp_service: NlpService = None


def get_nlp_service() -> NlpService:
    global _nlp_service
    if _nlp_service is None:
        _nlp_service = NlpService()
    return _nlp_service
