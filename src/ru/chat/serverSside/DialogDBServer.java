package ru.chat.serverSside;

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
		System.out.println(
			"DialogDBServer starts. Creating ServerSocket for connecting users after they overcame " + "authorization");
		try(ServerSocket serverDialogSocket = new ServerSocket(55555)){
			System.out.println("Creating pool of threads for 10 thousands users");
			exeDialogThread = Executors.newFixedThreadPool(10000);
			while(! serverDialogSocket.isClosed()){
				System.out.println("main connecting while loop starts");
				socket = serverDialogSocket.accept();
				exeDialogThread.execute(new RunDialog(socket));
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
