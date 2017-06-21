package ru.chat.serverSside.threads;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by mercenery on 16.06.2017.
 */
public class DialogDBServer{
	private static Socket          socket;
	private static ExecutorService exeDialogThread;
	
	public static void main(String[] args){
		try(ServerSocket serverDialogSocket = new ServerSocket(55555)){
			exeDialogThread = Executors.newFixedThreadPool(10000);
			while(! serverDialogSocket.isClosed()){
				socket = serverDialogSocket.accept();
				exeDialogThread.execute(new RunDialog(socket));
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
