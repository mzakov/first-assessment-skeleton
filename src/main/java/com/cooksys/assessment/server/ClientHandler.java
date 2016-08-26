package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private ConcurrentHashMap<String, Socket> map = Map.getInstance().clientMap;

	private void send(Message message, Socket socket) {
		try {
			ObjectMapper mapper = new ObjectMapper();

			String msg = mapper.writeValueAsString(message);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
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

			for (Socket socket : map.values()) {
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
				// direct messaging preparation
				if (message.getCommand().charAt(0) == '@' && map.containsKey(message.getCommand().substring(1))) {
					reciever = message.getCommand().substring(1);
					message.setCommand("@");
					log.info("Command: <{}>; Reciever: <{}>", message.getCommand(), reciever);
				}
				// one user per IP part-2
				if (!map.containsKey(message.getUsername()) && !message.getCommand().equals("connect")) {
					socket.close();
				}

				switch (message.getCommand()) {

				case "connect":
					// one user per IP part-1
					map.forEach((k, v) -> {
						if (v.getInetAddress().equals(socket.getInetAddress()))
							map.remove(k);
					});
					// name is taken or has ' ' in it
					if (map.containsKey(message.getUsername()) || message.getUsername().contains(" ") || message.getUsername().length()>10) {
						message.setCommand("disconnect");
						message.setContents("\n The username '" + message.getUsername() + "' or IP"
								+ socket.getInetAddress()
								+ " is not available, please try another one! PS: more than 10 chars and empty spaces are not allowed");
						send(message, socket);
						this.socket.close();
					} else {
						log.info("user <{}> connected", message.getUsername());
						client = message.getUsername();
						map.put(client, socket);
						log.info("Users online <{}>", map.entrySet());
						message.setContents("I'm online!");
						broadcast(message);
					}
					break;

				case "disconnect":
					log.info("user <{}> disconnected", message.getUsername());
					map.remove(message.getUsername());
					message.setContents("I'm Gone!");
					broadcast(message);
					this.socket.close();
					break;

				case "echo":
					if(message.getContents().length()>343) {
						message.setContents("Message should be less than 300 chars!");
					}
					log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
					// the kick command
					if (message.getContents().contains("kick")
							&& map.containsKey(message.getContents().substring(message.getContents().indexOf("kick")+ 5))) {
						String user = message.getContents().substring(message.getContents().indexOf("kick")+ 5);
						map.remove(user);
						message.setContents(user + " was kicked!");
						broadcast(message);
					} else {
						send(message, socket);
					}
					break;

				case "broadcast":
					if(message.getContents().length()>343) {
						message.setContents("Message should be less than 300 chars!");
					}
					log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());
					broadcast(message);
					break;

				case "users":
					log.info("user <{}> asked for users list", message.getUsername());
					String usersList = "";
					for (String key : map.keySet()) {
						usersList += key + " " + map.get(key).getInetAddress() + "\n";
					}
					message.setContents("\n List of users: \n" + usersList);
					send(message, socket);
					break;

				case "@":
					if(message.getContents().length()>343) {
						message.setContents("Message should be less than 300 chars!");
					}
					log.info("From <{}> to <{}>: <{}>", message.getUsername(), reciever, message.getContents());
					send(message, map.get(reciever));
					if (!socket.equals(map.get(reciever))) {
						send(message, map.get(message.getUsername()));
					}
					// messaging to multiple users
					Pattern p = Pattern.compile("\\@(.*?)\\ ");
					Matcher m = p.matcher(message.getContents() + ' ');
					List<String> users = new ArrayList<String>();
					while (m.find()) {
						users.add(m.group().substring(1, m.group().length() - 1));
					}
					for (String user : users) {
						if (map.containsKey(user))
							send(message, map.get(user));
					}
					break;

				default:
					message.setContents("---Invalid Command---");
					send(message, map.get(message.getUsername()));
					break;
				}
			}
		} catch (IOException e) {
			map.remove(client);
			log.error("Something went wrong :/", e);
		}
	}

}
