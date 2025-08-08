package com.copilot.musiccopilot.tools;
import com.copilot.musiccopilot.tools.MusicGenerationTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest
public class MusicGenerationToolTest {

    @Autowired
    private MusicGenerationTool musicGenerationTool;

    @Test
    void generateMusic() {
        var result = musicGenerationTool.generateMusic("Give me a 15 second music clip in C minor");
        System.out.println(result);
    }
}
