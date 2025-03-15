# AI筛选简历

## 项目描述

使用大模型 + 工具判断候选人的简历，判断流程如下：

1.分析简历排版

2.获取简历内容

3.调用工具分析 github

4.分析简历

另外：你也可以换多模态的大模型将第二步和第四步合成一步

## 使用说明

1.在 application.properties 设置大模型服务商信息

2.访问 http://127.0.0.1:9501/api/resume/analyze?resumeUrl=<简历地址>


项目来自于本人的社区AI专栏: https://code.xhyovo.cn/





