package com.gist.guild.distribution.spike.controller.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.DocumentRepositoryMethodParameter;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Log
@RunWith(SpringRunner.class)
@WebMvcTest(DistributionController.class)
@ActiveProfiles("test")
public class DistributionControllerGetDocumentClassTest {
    private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private MockMvc mvc;

    @MockBean
    DeliveryValenceService deliveryValenceService;

    private DocumentRepositoryMethodParameter getParam() throws JsonProcessingException {
        String json = "{\"type\":\"java.lang.String\",\"value\":\"test@test.it\"}";
        return mapper.readValue(json, DocumentRepositoryMethodParameter.class);
    }

    @WithMockUser(value = "test")
    @Test
    public void entryOk() throws Exception {
        String method = "findByTelegramUserId";
        String documentClass = "Participant";
        List<DocumentRepositoryMethodParameter> params = new ArrayList<>(1);
        params.add(getParam());

        DistributionMessage<Void> distributionMessage = new DistributionMessage<>();
        distributionMessage.setCorrelationID(UUID.randomUUID());
        distributionMessage.setType(DistributionEventType.GET_DOCUMENT);
        distributionMessage.setDocumentClass(Participant.class);
        distributionMessage.setDocumentRepositoryMethod(method);
        distributionMessage.setParams(params);

        Mockito.when(deliveryValenceService.sendDocumentClassRequest(documentClass,method,params)).thenReturn(distributionMessage);
        mvc.perform(post("/document/"+documentClass+"/"+method)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(params)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    @WithMockUser(value = "test")
    @Test
    public void entryKo() throws Exception {
        String method = "findByTelegramUserId";
        String documentClass = "UnknownDocumentClass";
        List<DocumentRepositoryMethodParameter> params = new ArrayList<>(1);
        params.add(getParam());

        DistributionMessage<Void> distributionMessage = new DistributionMessage<>();
        distributionMessage.setCorrelationID(UUID.randomUUID());
        distributionMessage.setType(DistributionEventType.GET_DOCUMENT);
        distributionMessage.setDocumentClass(Participant.class);
        distributionMessage.setDocumentRepositoryMethod(method);
        distributionMessage.setParams(params);

        Mockito.when(deliveryValenceService.sendDocumentClassRequest(documentClass,method,params)).thenThrow(ClassNotFoundException.class);
        mvc.perform(post("/document/"+documentClass+"/"+method)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(params)))
                .andDo(print())
                .andExpect(status().isNotAcceptable())
                .andReturn();
    }

    @WithAnonymousUser
    @Test
    public void entryNoUser() throws Exception {
        String method = "findByTelegramUserId";
        String documentClass = "Participant";
        List<DocumentRepositoryMethodParameter> params = new ArrayList<>(1);
        params.add(getParam());

        DistributionMessage<Void> distributionMessage = new DistributionMessage<>();
        distributionMessage.setCorrelationID(UUID.randomUUID());
        distributionMessage.setType(DistributionEventType.GET_DOCUMENT);
        distributionMessage.setDocumentClass(Participant.class);
        distributionMessage.setDocumentRepositoryMethod(method);
        distributionMessage.setParams(params);

        Mockito.when(deliveryValenceService.sendDocumentClassRequest(documentClass, method, params)).thenReturn(distributionMessage);
        mvc.perform(post("/document/" + documentClass + "/" + method)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(params)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andReturn();
    }
}
