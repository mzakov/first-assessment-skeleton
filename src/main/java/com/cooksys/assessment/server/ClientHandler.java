package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

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
	
	//makes a concurrent map for all the clients
	public static ConcurrentHashMap<String, Socket> clientMap = new ConcurrentHashMap<String, Socket>();
	
	private String client;
	public String reciever = "";
	
	public void run() {
		try {

			ObjectMapper mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);

				if (message.getCommand().charAt(0) == '@' && clientMap.containsKey(message.getCommand().substring(1))){
					reciever = message.getCommand().substring(1);
					message.setCommand("@");
					log.info("Command: <{}>; Reciever: <{}>", message.getCommand(), reciever);
				}
				
				switch (message.getCommand()) {
					case "connect":
						if (clientMap.containsKey(message.getUsername())){
							message.setCommand("disconnect");
							message.setContents("The username '" + message.getUsername() + "' is not available, please try another one!");
							String response = mapper.writeValueAsString(message);
							writer.write(response);
							writer.flush();
							this.socket.close();
						} else {
						log.info("user <{}> connected", message.getUsername());
						client = message.getUsername();
						clientMap.put(client, socket);								//adds the client to the map
						log.info("Users online <{}>", clientMap.entrySet());		//prints out the map in console
						message.setContents("I'm online!");
						String conn = mapper.writeValueAsString(message);
						for (Socket socket : clientMap.values()) {
							log.info("broadcast sent to socket <{}>", socket);
							PrintWriter broadWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
							broadWriter.write(conn);
							broadWriter.flush();
						}
						}
						break;
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						clientMap.remove(message.getUsername());					//removes the client from the list
						message.setContents("I'm Gone!");
						String dis = mapper.writeValueAsString(message);
						for (Socket socket : clientMap.values()) {
							log.info("broadcast sent to socket <{}>", socket);
							PrintWriter broadWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
							broadWriter.write(dis);
							broadWriter.flush();
						}
						this.socket.close();
						break;
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						String response = mapper.writeValueAsString(message);
						writer.write(response);
						writer.flush();
						break;
					case "broadcast":
						log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());
						String broadcast = mapper.writeValueAsString(message);
						for (Socket socket : clientMap.values()) {
							log.info("broadcast sent to socket <{}>", socket);
							PrintWriter broadWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
							broadWriter.write(broadcast);
							broadWriter.flush();
						}
						break;
					case "users":
						log.info("user ,<{}> asked for users list", message.getUsername());
						message.setContents("Active users => " + clientMap.keySet().toString());
						String users = mapper.writeValueAsString(message);
						writer.write(users);
						writer.flush();
						break;
					case "@":
						log.info("From <{}> to <{}>: <{}>", message.getUsername(), reciever, message.getContents());
						String msg = mapper.writeValueAsString(message);
						PrintWriter directWriter = new PrintWriter(new OutputStreamWriter(clientMap.get(reciever).getOutputStream()));
						directWriter.write(msg);
						directWriter.flush();
						break;
				}
			}
		} catch (IOException e) {
			clientMap.remove(client);
			log.error("Something went wrong :/", e);
		}
	}

}
