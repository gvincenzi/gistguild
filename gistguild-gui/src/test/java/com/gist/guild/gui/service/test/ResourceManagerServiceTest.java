package com.gist.guild.gui.service.test;

import com.gist.guild.commons.message.entity.Participant;
import com.gist.guild.gui.GistGuildGuiApplication;
import com.gist.guild.gui.client.DocumentClient;
import com.gist.guild.gui.service.ResourceManagerService;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Log
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {GistGuildGuiApplication.class })
@ActiveProfiles("test")
public class ResourceManagerServiceTest {
    @Autowired
    ResourceManagerService resourceManagerService;

    @Test
    public void addOrUpdateParticipantTest1(){
        Participant participant = new Participant();
        participant.setTelegramUserId(123456L);
        participant.setNickname("test");
        participant.setAdminPasswordEncoded("pass");

        resourceManagerService.addOrUpdateParticipant(participant);
    }

    @Test
    public void addOrUpdateParticipantTest2(){
        Participant participant = new Participant();
        participant.setTelegramUserId(123456L);
        participant.setNickname("test");
        participant.setAdminPasswordEncoded("pass");
        participant.setActive(Boolean.FALSE);

        resourceManagerService.addOrUpdateParticipant(participant);
    }

    @Test
    public void addOrUpdateParticipantTest3(){
        Participant participant = new Participant();
        participant.setTelegramUserId(123456L);
        participant.setNickname("test");
        participant.setActive(Boolean.FALSE);
        participant.setAdministrator(Boolean.TRUE);

        resourceManagerService.addOrUpdateParticipant(participant);
    }
}
