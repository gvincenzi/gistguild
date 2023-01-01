package com.gist.guild.node.core.service.test;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.node.GistGuildNodeApplication;
import com.gist.guild.node.core.document.Participant;
import com.gist.guild.node.core.repository.ParticipantRepository;
import com.gist.guild.node.core.service.NodeService;
import com.gist.guild.node.spike.client.SpikeClient;
import lombok.extern.java.Log;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Log
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { GistGuildNodeApplication.class })
@ActiveProfiles("test")
public class NodeServiceImplTest {
	@Autowired
	NodeService<com.gist.guild.commons.message.entity.Participant, Participant> serviceUnderTest;
	
	@MockBean
	ParticipantRepository participantRepository;

	@MockBean
	SpikeClient spikeClient;
	
	@Before
	public void init(){
		Mockito.when(participantRepository.findAllByOrderByTimestampAsc()).thenReturn(new ArrayList<>());
		Mockito.when(participantRepository.save(Mockito.any(Participant.class))).then(i -> i.getArgument(0));
	}
	
	@Test(expected = GistGuildGenericException.class)
	public void testException() throws GistGuildGenericException {
		serviceUnderTest.add(null);
	}
	
	@Test
	public void testOK() throws GistGuildGenericException {
		List<Participant> items = new ArrayList<>();
		
		long now = System.currentTimeMillis();
		Mockito.when(participantRepository.findTopByOrderByTimestampDesc()).thenReturn(null);
		Participant item = serviceUnderTest.add(getDocumentMock("Genesis block"));
		log.info("Block mined in "+ (System.currentTimeMillis()-now) + "ms >> " + item);
		
		now = System.currentTimeMillis();
		Mockito.when(participantRepository.findTopByOrderByTimestampDesc()).thenReturn(item);
		Participant item1 = serviceUnderTest.add(getDocumentMock("Item 1"));
		log.info("Item added in "+ (System.currentTimeMillis()-now) + "ms >> " + item1);
		
		now = System.currentTimeMillis();
		Mockito.when(participantRepository.findTopByOrderByTimestampDesc()).thenReturn(item1);
		Participant item2 = serviceUnderTest.add(getDocumentMock("Item 2"));
		log.info("Item mined in "+ (System.currentTimeMillis()-now) + "ms >> " + item2);
		
		items.add(item);
		items.add(item1);
		items.add(item2);

		Assert.assertTrue(serviceUnderTest.validate(items));
	}

	@Test(expected = GistGuildGenericException.class)
	public void testDesactivateKO() throws GistGuildGenericException {
		List<Participant> items = new ArrayList<>(1);
		items.add(getDocumentMock("Genesis block"));
		Mockito.when(participantRepository.findTopByOrderByTimestampDesc()).thenReturn(null);
		Mockito.when(participantRepository.findByTelegramUserId(getDocumentMock("Genesis block").getTelegramUserId())).thenReturn(items);
		Participant item = serviceUnderTest.add(null);
	}

	@Test
	public void testKO() throws GistGuildGenericException {
		List<Participant> items = new ArrayList<>();

		long now = System.currentTimeMillis();
		Mockito.when(participantRepository.findTopByOrderByTimestampDesc()).thenReturn(null);
		Participant item = serviceUnderTest.add(getDocumentMock("Genesis block"));
		log.info("Block mined in "+ (System.currentTimeMillis()-now) + "ms >> " + item);

		now = System.currentTimeMillis();
		Mockito.when(participantRepository.findTopByOrderByTimestampDesc()).thenReturn(item);
		Participant item1 = serviceUnderTest.add(getDocumentMock("Item 1"));
		log.info("Item added in "+ (System.currentTimeMillis()-now) + "ms >> " + item1);

		now = System.currentTimeMillis();
		Mockito.when(participantRepository.findTopByOrderByTimestampDesc()).thenReturn(item1);
		Participant item2 = serviceUnderTest.add(getDocumentMock("Item 2"));
		log.info("Item mined in "+ (System.currentTimeMillis()-now) + "ms >> " + item2);

		item2.setTimestamp(item2.getTimestamp().plus(Period.ofDays(3)));

		items.add(item);
		items.add(item1);
		items.add(item2);

		Assert.assertFalse(serviceUnderTest.validate(items));
	}

	@Test
	public void testKO2() throws GistGuildGenericException {
		List<Participant> items = new ArrayList<>();

		long now = System.currentTimeMillis();
		Mockito.when(participantRepository.findTopByOrderByTimestampDesc()).thenReturn(null);
		Participant item = serviceUnderTest.add(getDocumentMock("Genesis block"));
		log.info("Block mined in "+ (System.currentTimeMillis()-now) + "ms >> " + item);

		now = System.currentTimeMillis();
		Mockito.when(participantRepository.findTopByOrderByTimestampDesc()).thenReturn(item);
		Participant item1 = serviceUnderTest.add(getDocumentMock("Item 1"));
		log.info("Item added in "+ (System.currentTimeMillis()-now) + "ms >> " + item1);

		now = System.currentTimeMillis();
		Mockito.when(participantRepository.findTopByOrderByTimestampDesc()).thenReturn(item1);
		Participant item2 = serviceUnderTest.add(getDocumentMock("Item 2"));
		log.info("Item mined in "+ (System.currentTimeMillis()-now) + "ms >> " + item2);

		item2.setPreviousId(UUID.randomUUID().toString());

		items.add(item);
		items.add(item1);
		items.add(item2);

		Assert.assertFalse(serviceUnderTest.validate(items));
	}

	@Test
	public void testKO3() throws GistGuildGenericException {
		List<Participant> items = new ArrayList<>();

		long now = System.currentTimeMillis();
		Mockito.when(participantRepository.findTopByOrderByTimestampDesc()).thenReturn(null);
		Participant item = serviceUnderTest.add(getDocumentMock("Genesis block"));
		log.info("Block mined in "+ (System.currentTimeMillis()-now) + "ms >> " + item);

		now = System.currentTimeMillis();
		Mockito.when(participantRepository.findTopByOrderByTimestampDesc()).thenReturn(item);
		Participant item1 = serviceUnderTest.add(getDocumentMock("Item 1"));
		log.info("Item added in "+ (System.currentTimeMillis()-now) + "ms >> " + item1);

		now = System.currentTimeMillis();
		Mockito.when(participantRepository.findTopByOrderByTimestampDesc()).thenReturn(item1);
		Participant item2 = serviceUnderTest.add(getDocumentMock("Item 2"));
		log.info("Item mined in "+ (System.currentTimeMillis()-now) + "ms >> " + item2);

		item2.setPreviousId(UUID.randomUUID().toString());

		items.add(item);
		items.add(item1);
		items.add(item2);

		Assert.assertFalse(serviceUnderTest.validate(items));
	}

	@Test
	public void testInit() throws GistGuildGenericException {
		List<com.gist.guild.commons.message.entity.Participant> items = new ArrayList<>();

		long now = System.currentTimeMillis();
		Mockito.when(participantRepository.findTopByOrderByTimestampDesc()).thenReturn(null);
		Participant item = serviceUnderTest.add(getDocumentMock("Genesis block"));
		log.info("Block mined in "+ (System.currentTimeMillis()-now) + "ms >> " + item);

		now = System.currentTimeMillis();
		Mockito.when(participantRepository.findTopByOrderByTimestampDesc()).thenReturn(item);
		Participant item1 = serviceUnderTest.add(getDocumentMock("Item 1"));
		log.info("Item added in "+ (System.currentTimeMillis()-now) + "ms >> " + item1);

		now = System.currentTimeMillis();
		Mockito.when(participantRepository.findTopByOrderByTimestampDesc()).thenReturn(item1);
		Participant item2 = serviceUnderTest.add(getDocumentMock("Item 2"));
		log.info("Item mined in "+ (System.currentTimeMillis()-now) + "ms >> " + item2);

		items.add(item);
		items.add(item1);
		items.add(item2);

		serviceUnderTest.init(items);
	}

	@Test
	public void calculateHashTest() throws Exception{
		Participant item = new Participant();
		serviceUnderTest.updateLocal(item);
	}

	public Participant getDocumentMock(String title){
		Participant participant = new Participant();
		participant.setNickname("test");
		participant.setTelegramUserId(478956L);
		return participant;
	}
}
