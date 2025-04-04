# GeminiChatPlugin
GeminiChatPlugin 是一个基于 Paper 1.21 的 Minecraft 服务器插件，允许玩家通过聊天与 Gemini AI 进行交互。插件支持预设配置，玩家可以切换不同的预设以改变 AI 的行为。
![](https://github.com/LanRhyme/GeminiChatPlugin/blob/master/5fc50da6a1ff4b81ef752d2a7152ecc6.png)


## 特点
>  1.通过聊天与 Gemini AI 交互
> 
>  2.支持预设配置，玩家可以切换不同的 AI 行为



## 配置指南
`config.yml`示例
```
proxyUrl: "http://your-reverse-proxy.com"
apiKey: "YOUR_GEMINI_API_KEY"
```

•   proxyUrl  ：你的 Gemini API 代理 URL

•   apiKey  ：你的 Gemini API 密钥（在此链接获取:https://aistudio.google.com/app/apikey）



## 预设配置
**1.创建预设文件**

•在`presets`文件夹中创建新的 YAML 文件，例如`example.yml`

**预设文件示例**

```
system_prompt: "You are a friendly assistant. Answer the user's questions about minecraft."
```

**3.切换预设**

• OP 玩家可以使用   `/switchPreset <preset_name>`   命令切换预设
• 使用命令`/presetmenu`可以打开预设菜单查看预设的详细信息并进行切换



## 使用方法
> •玩家在聊天中输入   `#*`   开头的消息，例如  ` #*你好，Gemini！`
> 
> •消息内容将发送到 Gemini API，AI 的回复将显示在聊天中



## 许可证
• 本插件遵循 [GPL 3.0](https://www.gnu.org/licenses/gpl-3.0.html) 开源协议
