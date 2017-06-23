package ru.chat.serverSside;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by mercenery on 16.06.2017.
 */
public class AuthorizationServer{
	
	static Socket          socket;
	static ExecutorService serviceAurh;
	
	public static void main(String[] args){
		System.out.println("Main server(AuthorizationServer) starts, creating serversocket and waits for users");
		try(ServerSocket serverSocket = new ServerSocket(12345)){
			System.out.println("Creating pool of separate threads for 50 connecting users at the same time");
			serviceAurh = Executors.newFixedThreadPool(50);
			
			while(! serverSocket.isClosed()){
				System.out.println("Main while loop for giving away threads starts");
				socket = serverSocket.accept();
				serviceAurh.execute(new RunAuthorization(socket));
			}
		} catch(IOException e1) {
			e1.printStackTrace();
		} finally{
			System.out.println("Finally closing resources in AuthorizationServer starts");
			try{
				socket.close();
				serviceAurh.shutdown();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
}