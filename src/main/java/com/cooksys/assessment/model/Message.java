package com.cooksys.assessment.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Message {

	private String username;
	private String command;
	private String contents;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getContents() {
		return contents;
	}

	public void setContents(String contents) {
		this.contents = new SimpleDateFormat("MM/dd/yy HH:mm:ss").format(new Date()) + 
		" " + username + " (" + command + "): " + contents;
	}

}
