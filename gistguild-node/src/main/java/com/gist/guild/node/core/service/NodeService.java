package com.gist.guild.node.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.node.core.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;

public abstract class NodeService<T extends Document, S extends T> {
	protected static final String GENESIS = "GENESIS";

	@Autowired
	ObjectMapper objectMapper;

	@Value("${gistguild.difficult.level}")
	protected Integer difficultLevel;

	@Value("${spring.application.name}")
	protected String instanceName;

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
			}
			if (previousItem != null && !previousItem.getId().equals(currentItem.getPreviousId())) {
				result = false;
			}
			if (previousItem == null && !GENESIS.equals(currentItem.getPreviousId())) {
				result = false;
			}
			if (!NodeUtils.isHashResolved(currentItem, difficultLevel)) {
				result = false;
			}
			if (currentItem.getIsCorruptionDetected()) {
				result = false;
			}
		}

		return result;
	}

	public void init(List<T> content) throws GistGuildGenericException {
		Collections.sort(content);
		List<S> documents = getRepository().findAllByOrderByTimestampAsc();
		for (int i = 0; i < content.size(); i++) {
			if (i < documents.size()) {
				if (!documents.get(i).getId().equals(content.get(i).getId())) {
					throw new GistGuildGenericException("Guild registry has been corrupted");
				}
			} else {
				updateLocal(content.get(i));
			}
		}
	}

	public List<S> findAll(){
		return getRepository().findAllByOrderByTimestampAsc();
	}
}
