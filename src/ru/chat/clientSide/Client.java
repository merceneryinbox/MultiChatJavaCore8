package ru.chat.clientSide;

import ru.chat.markerIface.DialogPacket;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * Created by mercenery on 20.06.2017.
 */
public class Client{
	
	private static Socket socketDialog;
	
	private static DialogPacket dialogPackFromAuthorization;
	private static DialogPacket dialogPacketInSessionFromServer;
	private static DialogPacket dialogPacketInSessionFromUser;
	private static DialogPacket authPacket;
	private static DialogPacket firstPackToDialog;
	
	private static ObjectOutputStream ooDialog;
	private static ObjectInputStream  oiDialog;
	
	private static String message;
	private static String reply;
	private static int    sessionIdInClient;
	private static int    sessionFromRunAuthorization;
	private static long   timeStampFromRunAuthorization;
	private static String loginInClient;
	private static String pasInClient;
	
	public Client(){
	}
	
	public static void main(String[] args){
		while(untilAuthorize() != 0){
			untilAuthorize();
		}
		
		
		try{
			BufferedReader talk = new BufferedReader(new InputStreamReader(System.in));
			socketDialog = new Socket("localhost", 55555);
			ooDialog = new ObjectOutputStream(socketDialog.getOutputStream());
			oiDialog = new ObjectInputStream(socketDialog.getInputStream());
			
			firstPackToDialog = new DialogPacket(loginInClient, pasInClient,
			                                     "----------------------------------------------------",
			                                     sessionFromRunAuthorization, timeStampFromRunAuthorization);
			ooDialog.writeObject(firstPackToDialog);
			ooDialog.flush();
			
			//принимаю ответ на первый пакет и вывожу на консоль
			if(oiDialog.available() != 0){
				dialogPacketInSessionFromServer = (DialogPacket)oiDialog.readObject();
				reply = dialogPacketInSessionFromServer.message;
				System.out.println(reply);
			}
			//запускаю основной цикл общения
			while(! socketDialog.isClosed()){
				
				System.out.println("input your message");
				message = talk.readLine();
				dialogPacketInSessionFromUser = new DialogPacket(loginInClient, pasInClient, message, sessionIdInClient,
				                                                 new Date().getTime());
				ooDialog.writeObject(dialogPacketInSessionFromUser);
				ooDialog.flush();
				
				if(message.equalsIgnoreCase("quit")){
					if(oiDialog.available() != 0){
						dialogPacketInSessionFromServer = (DialogPacket)oiDialog.readObject();
						reply = dialogPacketInSessionFromServer.message;
					}
					System.out.println("Server replyed" + reply);
					ooDialog.close();
					oiDialog.close();
					socketDialog.close();
					break;
				}
//				if(oiDialog.available() != 0){
					dialogPacketInSessionFromServer = (DialogPacket)oiDialog.readObject();
					reply = dialogPacketInSessionFromServer.message;
					System.out.println(reply);
//				}
			}
		} catch(UnknownHostException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
		} finally{
			try{
				ooDialog.close();
				oiDialog.close();
				socketDialog.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
//
//		try{
//			oiDialog.close();
//			ooDialog.close();
//			socketDialog.close();
//		} catch(IOException e) {
//			e.printStackTrace();
//		}
	}
	
	public static int authoriseMe(){
		try(
			// открываем сокет для проверки авторизации
			Socket socketAuth = new Socket("localhost", 12345);
			ObjectOutputStream oosAuthor = new ObjectOutputStream(socketAuth.getOutputStream());
			ObjectInputStream oisAutor = new ObjectInputStream(socketAuth.getInputStream())){
			BufferedReader autorizeReader = new BufferedReader(new InputStreamReader(System.in));
			
			// запрашиваем логин и пароль на ввод от пользователя
			System.out.println("Login:");
			loginInClient = autorizeReader.readLine();
			System.out.println("Pass:");
			pasInClient = autorizeReader.readLine();

//			autorizeReader.close();
			// фолрмируем пакет авторизации и отправляем запрос делегату сервера для проверки возможности авторизации
			//String log, String pass, String message, int sessionId, long timeStampFromDiPa
			authPacket = new DialogPacket(loginInClient, pasInClient, "authorization", 0, 0);
			oosAuthor.writeObject(authPacket);
			oosAuthor.flush();
			
			// ждём ответа от сервера авторизации с номером сессии
			dialogPackFromAuthorization = (DialogPacket)oisAutor.readObject();
			// проверяем не забанен ли я (если забанен - тогда в логине будет слово - quit и клиент закрывается
			String requestAproveLog = dialogPackFromAuthorization.log;
			sessionFromRunAuthorization = dialogPackFromAuthorization.sessionId;
			timeStampFromRunAuthorization = dialogPackFromAuthorization.timeStampFromDiPa;
			
			if(requestAproveLog.equalsIgnoreCase("quit")){
				System.out.println("you are banned");
				System.out.println("сообщение от сервера авторизации - " + dialogPackFromAuthorization.message);
				return - 1;
			}
			
			// во всех остальных случаях если я не забанен продолжаю работу - шлю первый диалоговый пакет диалоговому
			// серверу предварительно вставив в него номер сессии из пакета от сервера авторизации
			sessionFromRunAuthorization = dialogPackFromAuthorization.sessionId;
			timeStampFromRunAuthorization = dialogPackFromAuthorization.timeStampFromDiPa;
			return 0;
		} catch(IOException e) {
			e.printStackTrace();
			return - 1;
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
			return - 1;
		}
	}
	
	private static int untilAuthorize(){
		return authoriseMe();
	}
}