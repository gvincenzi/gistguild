package com.gist.guild.commons.exception;

import com.gist.guild.commons.message.entity.Participant;

public class GistGuildInsufficientQuantityException extends GistGuildGenericException{
	private Participant participant;
	public GistGuildInsufficientQuantityException(Participant participant, String message) {
		super(message);
		this.participant = participant;
	}
}
