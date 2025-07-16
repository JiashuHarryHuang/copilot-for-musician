package com.copilot.musiccopilot.app;

import com.copilot.musiccopilot.advisor.MyLoggerAdvisor;
import com.copilot.musiccopilot.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class MusicCopilotApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            You are a professional musician copilot designed to assist with music production tasks, including mixing, arrangement, and idea development.
            
            You have three key capabilities:
            
            1. Music Production Expertise – Use your own training and knowledge to assist with questions about audio mixing, mastering, arrangement, instrumentation, and genre-specific techniques.
            
            2. Cheatsheet Knowledge Base – You have access to a specialized vector store containing reference materials, best practices, and tips from professional musicians. When users ask technical or stylistic questions, retrieve relevant content from this knowledge base to enhance your answers.
            
            3. Creative Tool Access – You can generate short music clips by calling external tools when the user requests music generation or sound previews. Use these tools when the user explicitly requests music examples, melodies, textures, or variations — or when it would meaningfully enhance your response.
            
            Always choose the most relevant resource: use your own knowledge when sufficient, refer to the knowledge base for deeper or niche topics, and invoke tools only when necessary to deliver concrete creative outputs.
            """;

    /**
     * 初始化 ChatClient
     *
     * @param dashscopeChatModel
     */
    public MusicCopilotApp(ChatModel dashscopeChatModel) {
//        // 初始化基于文件的对话记忆
//        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        // 初始化基于内存的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志 Advisor，可按需开启
                        new MyLoggerAdvisor()
//                        // 自定义推理增强 Advisor，可按需开启
//                       ,new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    record LoveReport(String title, List<String> suggestions) {

    }

    /**
     * AI 恋爱报告功能（实战结构化输出）
     *
     * @param message
     * @param chatId
     * @return
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    // AI 恋爱知识库问答功能

    @Resource
    private VectorStore MusicCopilotAppVectorStore;

    @Resource
    private Advisor MusicCopilotAppRagCloudAdvisor;

    @Resource
    private VectorStore pgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    /**
     * 和 RAG 知识库进行对话
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        // 查询重写
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                // 使用改写后的查询
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用 RAG 知识库问答
                .advisors(new QuestionAnswerAdvisor(MusicCopilotAppVectorStore))
                // 应用 RAG 检索增强服务（基于云知识库服务）
//                .advisors(MusicCopilotAppRagCloudAdvisor)
                // 应用 RAG 检索增强服务（基于 PgVector 向量存储）
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                // 应用自定义的 RAG 检索增强服务（文档查询器 + 上下文增强器）
//                .advisors(
//                        MusicCopilotAppRagCustomAdvisorFactory.createMusicCopilotAppRagCustomAdvisor(
//                                MusicCopilotAppVectorStore, "单身"
//                        )
//                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // AI 调用工具能力
    @Resource
    private ToolCallback[] allTools;

    /**
     * AI 恋爱报告功能（支持调用工具）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 综合 AI 聊天功能，支持工具调用和知识库问答（RAG）
     *
     * @param message 用户消息
     * @param chatId 会话 ID
     * @return AI 回复内容
     */
    public String doChatWithRagAndTools(String message, String chatId) {
        // 可选：先进行查询改写以优化问答效果
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);

        // 构建聊天请求
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(rewrittenMessage)
                // 会话上下文，用于多轮对话记忆
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 日志观察器（可选）
                .advisors(new MyLoggerAdvisor())
                // 启用工具能力
                .toolCallbacks(allTools)
                // 启用 RAG 知识库（可按需替换为 CloudRag / PgVector / 自定义）
                .advisors(new QuestionAnswerAdvisor(MusicCopilotAppVectorStore))
                // 如果你有多个 RAG 实现，可以继续添加如下：
                //.advisors(MusicCopilotAppRagCloudAdvisor)
                //.advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                //.advisors(MusicCopilotAppRagCustomAdvisorFactory.createMusicCopilotAppRagCustomAdvisor(...))
                // 发起调用
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // AI 调用 MCP 服务

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * AI 恋爱报告功能（调用 MCP 服务）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
