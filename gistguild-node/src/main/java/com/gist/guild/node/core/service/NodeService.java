package com.gist.guild.node.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.DistributionEventType;
import com.gist.guild.commons.message.DistributionMessage;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.node.core.configuration.MessageProperties;
import com.gist.guild.node.core.repository.DocumentRepository;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Collections;
import java.util.List;

@Slf4j
public abstract class NodeService<T extends Document, S extends T> {
	protected static final String GENESIS = "GENESIS";

	@Autowired
	ObjectMapper objectMapper;

	@Value("${gistguild.difficult.level}")
	protected Integer difficultLevel;

	@Value("${spring.application.name}")
	protected String instanceName;

	@Autowired
	protected MessageProperties messageProperties;

	protected abstract DocumentRepository<S> getRepository();

	public abstract S add(T document) throws GistGuildGenericException;
	public abstract Boolean updateLocal(T document) throws GistGuildGenericException;
	public abstract String calculateHash(S document) throws GistGuildGenericException;

	public Boolean validate(List<S> items) throws GistGuildGenericException {
		if (items == null) {
			throw new GistGuildGenericException("Iterable items collection is mandatory");
		}
		S currentItem;
		S previousItem;

		Collections.sort(items);

		Boolean result = true;
		for (int i = 0; i < items.size(); i++) {
			previousItem = i > 0 ? items.get(i - 1) : null;
			currentItem = items.get(i);
			if (!currentItem.getId().equals(calculateHash(currentItem))) {
				result = false;
				log.error("Corruption detected in ID");
			}
			if (previousItem != null && !previousItem.getId().equals(currentItem.getPreviousId())) {
				result = false;
				log.error("Corruption detected in PreviousId");
			}
			if (previousItem == null && !GENESIS.equals(currentItem.getPreviousId())) {
				result = false;
				log.error("Corruption detected in previousItem for non GENESIS block");
			}
			if (!NodeUtils.isHashResolved(currentItem, difficultLevel)) {
				result = false;
				log.error("Corruption detected in hash resolving");
			}
			if (currentItem.getIsCorruptionDetected()) {
				result = false;
				log.error("Corruption detected by another node");
			}
		}

		return result;
	}

	public void init(List<T> content) throws GistGuildGenericException {
		Collections.sort(content);
		List<S> documents = getRepository().findAllByOrderByTimestampAsc();
		for (int i = 0; i < content.size(); i++) {
			updateLocal(content.get(i));
			if (i < documents.size() && !documents.get(i).getId().equals(content.get(i).getId())) {
				throw new GistGuildGenericException("Guild registry has been corrupted");
			}
		}
	}

	public List<S> findAll(){
		return getRepository().findAllByOrderByTimestampAsc();
	}

	public void corruptionDetected(DistributionMessage<?> msg, Class classDetected, MessageChannel messageChannel) {
		log.error(String.format("Corruption detected on document [%s]: send message with Correlation ID [%s]", classDetected.getSimpleName(), msg.getCorrelationID()));
		DistributionMessage<List<Document>> responseMessage = new DistributionMessage<>();
		responseMessage.setCorrelationID(msg.getCorrelationID());
		responseMessage.setInstanceName(instanceName);
		responseMessage.setType(DistributionEventType.CORRUPTION_DETECTED);
		Message<DistributionMessage<List<Document>>> responseMsg = MessageBuilder.withPayload(responseMessage).build();
		messageChannel.send(responseMsg);
	}
}
