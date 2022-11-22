package com.gist.guild.node.core.service;

import com.gist.guild.commons.exception.GistGuildGenericException;
import com.gist.guild.commons.message.entity.Document;
import com.gist.guild.node.core.document.Participant;

import java.util.List;

public interface NodeService<T extends Document, S extends T> {
	S add(T document) throws GistGuildGenericException;
	S desactivate(T document) throws GistGuildGenericException;
	Boolean updateLocal(T document) throws GistGuildGenericException;
	Boolean validate(List<S> documents) throws GistGuildGenericException;
	List<S> findAll();
	void init(List<T> content) throws GistGuildGenericException;
}
