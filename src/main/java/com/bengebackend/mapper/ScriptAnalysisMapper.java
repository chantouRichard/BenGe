package com.bengebackend.mapper;

import com.bengebackend.model.ScriptAnalysis;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScriptAnalysisMapper {

    // 根据 scriptId 查询
    ScriptAnalysis getByScriptId(int scriptId);

    // 插入
    int insert(ScriptAnalysis analysis);

    // 更新
    int update(ScriptAnalysis analysis);
}
