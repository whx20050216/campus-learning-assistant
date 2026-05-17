package com.campus.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("keyword")
public class Keyword {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long materialId;

    private String keyword;

    private Float weight;

    @TableField("type")
    private String type = "keyword";
}
