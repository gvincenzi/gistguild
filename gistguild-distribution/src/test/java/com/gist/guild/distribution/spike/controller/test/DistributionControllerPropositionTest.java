package com.gist.guild.distribution.spike.controller.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.DocumentProposition;
import com.gist.guild.commons.message.entity.Participant;
import com.gist.guild.distribution.domain.service.valence.DeliveryValenceService;
import com.gist.guild.distribution.spike.controller.DistributionController;
import lombok.extern.java.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Log
@RunWith(SpringRunner.class)
@WebMvcTest(DistributionController.class)
@ActiveProfiles("test")
public class DistributionControllerPropositionTest {
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private MockMvc mvc;

    @MockBean
    DeliveryValenceService deliveryValenceService;

    protected static DocumentProposition getNewDocument(String json) throws JsonProcessingException {
        log.info(json);
        DocumentProposition documentProposition = mapper.readValue(json, DocumentProposition.class);
        log.info(mapper.writeValueAsString(documentProposition));

        return documentProposition;
    }

    private DocumentProposition getEntryProposition() throws JsonProcessingException {
        String json = "{\n" +
                "    \"documentPropositionType\" : \"USER_CANCELLATION\",\n" +
                "    \"description\" : \"GIST Item\",\n" +
                "    \"documentClass\" : \"Participant\",\n" +
                "    \"document\" : {\n" +
                "      \"mail\":\"test@test.it\",\n" +
                "      \"telegramUserId\":\"478956\"\n" +
                "      }\n" +
                "    }\n" +
                "}";
        DocumentProposition proposition = getNewDocument(json);
        return proposition;
    }

    @WithMockUser(value = "test")
    @Test
    public void entryOk() throws Exception {
        DocumentProposition proposition = getEntryProposition();
        DistributionMessage<DocumentProposition> distributionMessage = new DistributionMessage<>();
        distributionMessage.setCorrelationID(UUID.randomUUID());
        distributionMessage.setType(DistributionEventType.ENTRY_PROPOSITION);
        distributionMessage.setContent(proposition);
        Mockito.when(deliveryValenceService.propose(proposition)).thenReturn(distributionMessage);
        mvc.perform(post("/document")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(proposition)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    @WithMockUser(value = "test")
    @Test
    public void entryKo() throws Exception {
        DocumentProposition proposition = getEntryProposition();
        DistributionMessage<DocumentProposition> distributionMessage = new DistributionMessage<>();
        distributionMessage.setType(DistributionEventType.ENTRY_PROPOSITION);
        distributionMessage.setContent(proposition);
        Mockito.when(deliveryValenceService.propose(proposition)).thenReturn(distributionMessage);
        mvc.perform(post("/document")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(proposition)))
                .andDo(print())
                .andExpect(status().isNotAcceptable())
                .andReturn();
    }

    @WithMockUser(value = "test")
    @Test
    public void entryKo2() throws Exception {
        DocumentProposition proposition = getEntryProposition();
        DistributionMessage<DocumentProposition> distributionMessage = new DistributionMessage<>();
        distributionMessage.setType(DistributionEventType.ENTRY_PROPOSITION);
        distributionMessage.setContent(proposition);
        Mockito.when(deliveryValenceService.propose(proposition)).thenThrow(ClassNotFoundException.class);
        mvc.perform(post("/document")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(proposition)))
                .andDo(print())
                .andExpect(status().isNotAcceptable())
                .andReturn();
    }

    @WithAnonymousUser
    @Test
    public void entryNoUser() throws Exception {
        DocumentProposition proposition = getEntryProposition();
        DistributionMessage<DocumentProposition> distributionMessage = new DistributionMessage<>();
        distributionMessage.setType(DistributionEventType.ENTRY_PROPOSITION);
        distributionMessage.setContent(proposition);
        Mockito.when(deliveryValenceService.propose(proposition)).thenReturn(distributionMessage);
        mvc.perform(post("/document")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(proposition)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andReturn();
    }
}
