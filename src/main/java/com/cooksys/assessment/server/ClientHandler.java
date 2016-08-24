package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	private Logger log = LoggerFactory.getLogger(ClientHandler.class);

	private Socket socket;

	public ClientHandler(Socket socket) {
		super();
		this.socket = socket;
	}

	private String client;
	public String reciever = "";

	private void send(Message message, Socket socket){
		try {
		ObjectMapper mapper = new ObjectMapper();
		
		String msg = mapper.writeValueAsString(message);
		PrintWriter writer = new PrintWriter(
				new OutputStreamWriter(socket.getOutputStream()));
		writer.write(msg);
		writer.flush();
		} catch (IOException e) {
			log.error("Something went wrong :/", e);
			e.printStackTrace();
		}
	}
	
	private void broadcast(Message message) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			String msg = mapper.writeValueAsString(message);

			for (Socket socket : Server.clientMap.values()) {
				log.info("broadcast sent to socket <{}>", socket);
				PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
				writer.write(msg);
				writer.flush();
			}
		} catch (IOException e) {
			log.error("Something went wrong :/", e);
			e.printStackTrace();
		}
	}

	public void run() {
		try {

			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);

				if (message.getCommand().charAt(0) == '@' && Server.clientMap.containsKey(message.getCommand().substring(1))) {
					reciever = message.getCommand().substring(1);
					message.setCommand("@");
					log.info("Command: <{}>; Reciever: <{}>", message.getCommand(), reciever);
				}

				switch (message.getCommand()) {
				
				case "connect":
					boolean ipInUse = false;
					for (String key : Server.clientMap.keySet()) {
						if (Server.clientMap.get(key).getInetAddress().equals(socket.getInetAddress())) {
							ipInUse = true;
							Server.clientMap.remove(key);
						}
					}
					log.info("IP in Use <{}>", ipInUse);
					if (Server.clientMap.containsKey(message.getUsername()) || message.getUsername().contains(" ")
							|| ipInUse) {
						message.setCommand("disconnect");
						message.setContents("\n The username '" + message.getUsername() + "' or IP"
								+ socket.getInetAddress()
								+ " is not available, please try another one! PS: empty spaces are not allowed");
						send(message, socket);
						this.socket.close();
					} else {
						log.info("user <{}> connected", message.getUsername());
						client = message.getUsername();
						Server.clientMap.put(client, socket);
						log.info("Users online <{}>", Server.clientMap.entrySet());
						message.setContents("I'm online!");
						broadcast(message);
					}
					break;
					
				case "disconnect":
					log.info("user <{}> disconnected", message.getUsername());
					Server.clientMap.remove(message.getUsername());
					message.setContents("I'm Gone!");
					broadcast(message);
					this.socket.close();
					break;
					
				case "echo":
					log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
					send(message, socket);
					break;
					
				case "broadcast":
					log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());
					broadcast(message);
					break;
					
				case "users":
					log.info("user <{}> asked for users list", message.getUsername());
					String usersList = "";
					for (String key : Server.clientMap.keySet()) {
						usersList += key + " " + Server.clientMap.get(key).getInetAddress() + "\n";
					}
					message.setContents("\n List of users: \n" + usersList);
					send(message, socket);
					break;
					
				case "@":
					log.info("From <{}> to <{}>: <{}>", message.getUsername(), reciever, message.getContents());
					send(message, Server.clientMap.get(reciever));
					if(!socket.equals(Server.clientMap.get(reciever))){
					send(message, socket);
					}
					break;
					
				default:
					message.setContents("---Invalid Command---");
					send(message, socket);
					break;
				}
			}
		} catch (IOException e) {
			Server.clientMap.remove(client);
			log.error("Something went wrong :/", e);
		}
	}

}
