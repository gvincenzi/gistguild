package com.gist.guild.commons.exception;

import com.gist.guild.commons.message.entity.Participant;
import lombok.Data;

@Data
public class GistGuildGenericException extends Exception{
	private Participant participant;
	public GistGuildGenericException(String message) {
		super(message);
	}
	public GistGuildGenericException(Participant participant, String message) {
		super(message);
		this.participant = participant;
	}
}
