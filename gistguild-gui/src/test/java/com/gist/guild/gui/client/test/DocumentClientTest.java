package com.gist.guild.gui.client.test;

import com.gist.guild.gui.GistGuildGuiApplication;
import com.gist.guild.gui.client.DocumentClient;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Log
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {GistGuildGuiApplication.class })
@ActiveProfiles("test")
public class DocumentClientTest {
    @MockBean
    DocumentClient documentClient;

    @Test
    public void test(){

    }
}
