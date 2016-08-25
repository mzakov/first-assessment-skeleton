package com.cooksys.assessment.server;

import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Map {
	private final static Map INSTANCE = new Map();

	ConcurrentHashMap<String, Socket> clientMap = new ConcurrentHashMap<String, Socket>();

	private Map() {
	}

	public static Map getInstance() {

		return INSTANCE;

	}

}
