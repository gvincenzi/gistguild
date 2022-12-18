package com.gist.guild.commons.exception;

import com.gist.guild.commons.message.entity.Participant;

public class GistGuildInsufficientCreditException extends GistGuildGenericException{
	private Participant participant;
	public GistGuildInsufficientCreditException(Participant participant, String message) {
		super(message);
		this.participant = participant;
	}
}
