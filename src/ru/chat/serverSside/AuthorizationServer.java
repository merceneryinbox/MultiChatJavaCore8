package ru.chat.serverSside;

import ru.chat.serverSside.threads.RunAuthorization;

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
	
	static Socket socket;
	ObjectOutputStream objectOutputStream;
	ObjectInputStream  objectInputStream;
	
	static ExecutorService serviceAurh;
	
	public static void main(String[] args){
		try(ServerSocket serverSocket = new ServerSocket(12345)){
			serviceAurh = Executors.newFixedThreadPool(50);
			
			while(! serverSocket.isClosed()){
				socket = serverSocket.accept();
				serviceAurh.execute(new RunAuthorization(socket));
			}
		} catch(IOException e1) {
			e1.printStackTrace();
		} finally{
			try{
				socket.close();
				serviceAurh.shutdown();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
}
