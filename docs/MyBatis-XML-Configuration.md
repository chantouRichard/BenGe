# MyBatis Mapper XML 配置说明

## 修改内容

已将所有Mapper接口从注解方式改为XML配置方式，提供更灵活的SQL配置和维护能力。

## 文件结构

```
src/main/resources/mapper/
├── ScriptMapper.xml           # 剧本数据访问XML配置
├── ScriptHistoryMapper.xml    # 剧本历史数据访问XML配置
├── ScriptAnalysisMapper.xml   # 剧本分析数据访问XML配置
└── VisualElementMapper.xml    # 可视化元素数据访问XML配置
```

## 修改对比

### 原注解方式
```java
@Select("SELECT * FROM script WHERE id = #{id} AND is_deleted = 0")
Script selectById(Integer id);
```

### 现XML方式
```xml
<select id="selectById" parameterType="java.lang.Integer" resultMap="BaseResultMap">
    SELECT 
    <include refid="Base_Column_List"/>
    FROM script 
    WHERE id = #{id} AND is_deleted = 0
</select>
```

## XML配置优势

1. **复杂SQL支持**: 支持复杂的动态SQL、多表关联查询
2. **代码复用**: 通过`<sql>`标签定义可复用的SQL片段
3. **结果映射**: 通过`<resultMap>`精确控制字段映射关系
4. **维护便利**: SQL与Java代码分离，便于DBA优化SQL
5. **动态SQL**: 支持`<if>`, `<choose>`, `<foreach>`等动态标签

## 主要XML标签说明

### resultMap
```xml
<resultMap id="BaseResultMap" type="com.bengebackend.model.Script">
    <id column="id" property="id" jdbcType="INTEGER"/>
    <result column="title" property="title" jdbcType="VARCHAR"/>
    <!-- 更多字段映射 -->
</resultMap>
```

### sql片段
```xml
<sql id="Base_Column_List">
    id, title, content, is_deleted, last_updated, user_id, stage
</sql>
```

### select查询
```xml
<select id="selectById" parameterType="java.lang.Integer" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM script 
    WHERE id = #{id} AND is_deleted = 0
</select>
```

### insert插入
```xml
<insert id="insert" parameterType="com.bengebackend.model.Script" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO script (title, content, user_id, is_deleted, last_updated, stage)
    VALUES (#{title}, #{content}, #{userId}, #{isDeleted}, #{lastUpdated}, #{stage})
</insert>
```

### update更新
```xml
<update id="update">
    UPDATE script 
    SET title = #{title}, content = #{content}, stage = #{stage}, last_updated = #{lastUpdated}
    WHERE id = #{id}
</update>
```

### delete删除
```xml
<delete id="deleteById" parameterType="java.lang.Integer">
    DELETE FROM script_history WHERE id = #{id}
</delete>
```

## 配置文件修改

在`application.yaml`中确保配置正确：

```yaml
mybatis:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
  type-aliases-package: com.bengebackend.model
```

## 接口修改

所有Mapper接口只保留方法签名，移除了所有MyBatis注解：

```java
@Mapper
public interface ScriptMapper {
    Script selectById(Integer id);
    List<Script> selectByUserId(Integer userId);
    int insert(Script script);
    int update(@Param("id") Integer id, @Param("title") String title, 
               @Param("content") String content, @Param("stage") Integer stage,
               @Param("lastUpdated") LocalDateTime lastUpdated);
    int deleteById(Integer id);
}
```

## 注意事项

1. **命名空间**: XML文件的namespace必须与对应的Mapper接口全限定名一致
2. **方法名**: XML中的id必须与接口中的方法名一致
3. **参数类型**: 复杂参数需要使用`@Param`注解或parameterType指定
4. **返回类型**: 通过resultType或resultMap指定返回类型
5. **自增主键**: 使用`useGeneratedKeys="true" keyProperty="id"`获取自增主键值

## 未来扩展

XML方式为后续功能扩展提供了更大的灵活性：
- 支持复杂的关联查询
- 支持动态条件查询
- 支持批量操作
- 支持存储过程调用
- 支持自定义类型处理器
