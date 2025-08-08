package com.copilot.musiccopilot;

import com.copilot.musiccopilot.app.MusicCopilotApp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class MusicCopilotApplicationTests {

    @Autowired
    private MusicCopilotApp musicCopilotApp;

    @Test
    public void contextLoads() {}

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "I am a male singer who is trying to mix a cover song. I was singing on top of the track that got its vocal removed by AI. The song is \"Die with a smile - Bruno Mars/Lady Ga Ga\"";
        String answer = musicCopilotApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithTools() {
        String chatId = UUID.randomUUID().toString();
        String message = """
                I want to some inspiration on a new song. It should be in C major. Its chord progression should be F-G-Am. 
                I want to create a feeling of time flowing like a river. 
                """;
        String answer = musicCopilotApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithRagAndTools() {
        String chatId = UUID.randomUUID().toString();
        String message = """
                I want to some inspiration on a new song. It should be in C major. Its chord progression should be F-G-Am. 
                I want to create a feeling of time flowing like a river. 
                """;
        String answer = musicCopilotApp.doChatWithRagAndTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

}
