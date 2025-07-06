# Slogan生成API文档

## 非流式Slogan生成接口

### 接口描述
生成三个Slogan对象的数组，每个Slogan包含内容和核心创意两个字段。

### 接口信息
- **URL**: `/api/ai/slogan/generate`
- **Method**: `POST`
- **Content-Type**: `application/json`

### 请求参数
```json
{
  "prompt": "剧本描述内容",
  "scriptId": 1
}
```

| 参数名 | 类型 | 是否必须 | 描述 |
|--------|------|----------|------|
| prompt | String | 是 | 剧本描述内容，用于生成标语 |
| scriptId | Integer | 是 | 剧本ID |

### 响应格式
```json
[
  {
    "content": "第一个标语内容",
    "coreIdea": "第一个核心创意"
  },
  {
    "content": "第二个标语内容", 
    "coreIdea": "第二个核心创意"
  },
  {
    "content": "第三个标语内容",
    "coreIdea": "第三个核心创意"
  }
]
```

### 响应说明
- 成功时返回HTTP 200状态码和包含三个Slogan对象的数组
- 失败时返回HTTP 500状态码和空数组

### 请求示例
```bash
curl -X POST "http://localhost:8080/api/ai/slogan/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "一个发生在民国时期上海滩的悬疑剧本",
    "scriptId": 1
  }'
```

### 响应示例
```json
[
  {
    "content": "迷雾重重的上海滩，谁是幕后黑手？",
    "coreIdea": "在繁华与动荡交织的民国上海，每个人都有不可告人的秘密"
  },
  {
    "content": "十里洋场藏杀机，真相只有一个！",
    "coreIdea": "利用时代背景的复杂性，营造扑朔迷离的推理氛围"
  },
  {
    "content": "黄浦江畔的血色迷案，等你来破解",
    "coreIdea": "结合地域特色和历史背景，打造沉浸式悬疑体验"
  }
]
```

## 流式Slogan生成接口（已有）

### 接口信息
- **URL**: `/api/ai/slogan/stream`
- **Method**: `POST`
- **Content-Type**: `application/json`
- **Response**: `text/event-stream`

### 使用场景
- 非流式接口：适用于需要一次性获取完整结果的场景
- 流式接口：适用于需要实时显示生成过程的场景

## 错误处理
- 当AI服务不可用时，非流式接口会返回空数组
- 当解析AI响应失败时，会返回默认的三个标语
- 所有错误都会被记录在日志中

## 注意事项
1. 非流式接口会等待AI完全生成完毕后再返回结果
2. 如果AI生成的标语数量不足3个，会用默认标语补充
3. 如果AI生成的标语数量超过3个，只返回前3个
4. 每次调用都会生成新的标语，确保内容的多样性
