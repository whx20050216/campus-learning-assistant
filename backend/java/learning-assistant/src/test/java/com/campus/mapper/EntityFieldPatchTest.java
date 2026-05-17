package com.campus.mapper;

import com.campus.learning.LearningAssistantApplication;
import com.campus.learning.entity.Keyword;
import com.campus.learning.entity.KnowledgePoint;
import com.campus.learning.mapper.KeywordMapper;
import com.campus.learning.mapper.KnowledgePointMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = LearningAssistantApplication.class)
@Transactional
@Rollback
class EntityFieldPatchTest {

    @Autowired
    private KeywordMapper keywordMapper;

    @Autowired
    private KnowledgePointMapper knowledgePointMapper;

    @Test
    void testKeywordTypeField() {
        Keyword keyword = new Keyword();
        keyword.setMaterialId(1L);
        keyword.setKeyword("测试关键词");
        keyword.setWeight(0.5f);
        keyword.setType("keypoint");

        int inserted = keywordMapper.insert(keyword);
        assertEquals(1, inserted, "Keyword 插入应成功");
        assertNotNull(keyword.getId(), "插入后 ID 应自动生成");

        Keyword fetched = keywordMapper.selectById(keyword.getId());
        assertNotNull(fetched, "应能从数据库查询到记录");
        assertEquals("keypoint", fetched.getType(), "数据库中 type 字段应为 'keypoint'");
    }

    @Test
    void testKnowledgePointPositionField() {
        KnowledgePoint kp = new KnowledgePoint();
        kp.setMaterialId(1L);
        kp.setContent("测试知识点");
        kp.setType("definition");
        kp.setPosition("第3页第2段");

        int inserted = knowledgePointMapper.insert(kp);
        assertEquals(1, inserted, "KnowledgePoint 插入应成功");
        assertNotNull(kp.getId(), "插入后 ID 应自动生成");

        KnowledgePoint fetched = knowledgePointMapper.selectById(kp.getId());
        assertNotNull(fetched, "应能从数据库查询到记录");
        assertEquals("第3页第2段", fetched.getPosition(), "数据库中 position 字段应为 '第3页第2段'");
    }
}
