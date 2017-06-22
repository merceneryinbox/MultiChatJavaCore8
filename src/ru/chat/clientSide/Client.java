package ru.chat.clientSide;

import ru.chat.markerIface.AuthPacket;
import ru.chat.markerIface.DialogPacket;

import java.io.*;
import java.net.Socket;
import java.util.Date;

/**
 * Created by mercenery on 20.06.2017.
 */
public class Client{
	private static DialogPacket dialogPackFromAuthorization;
	private static DialogPacket dialogPacketInSessionFromServer;
	private static DialogPacket dialogPacketInSessionFromUser;
	private static AuthPacket   authPacket;
	
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
		try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		    // открываем сокет для проверки авторизации
		    Socket socketAuth = new Socket("localhost", 12345);
		    ObjectOutputStream oosAuthor = new ObjectOutputStream(socketAuth.getOutputStream());
		    ObjectInputStream oisAutor = new ObjectInputStream(socketAuth.getInputStream());
		    // открываем сокет для общения с сервером диалога
		    Socket socketDialog = new Socket("localhost", 55555);
		    ObjectOutputStream ooDialog = new ObjectOutputStream(socketDialog.getOutputStream());
		    ObjectInputStream oiDialog = new ObjectInputStream(socketDialog.getInputStream())){
			
			// запрашиваем логин и пароль
			System.out.println("Login:");
			loginInClient = bufferedReader.readLine();
			System.out.println("Pass:");
			pasInClient = bufferedReader.readLine();
			// фолрмируем пакет авторизации и отправляем запрос делегату сервера для проверки возможности авторизации
			authPacket = new AuthPacket(loginInClient, pasInClient);
			oosAuthor.writeObject(authPacket);
			oosAuthor.flush();
			
			// ждём ответа от сервера авторизации с номером сессии в виде пакета диалога
			dialogPackFromAuthorization = (DialogPacket)oisAutor.readObject();
			// проверяем не забанен ли я (если забанен - тогда в логине будет слово - quit и клиент закрывается
			String requestAproveLog = dialogPackFromAuthorization.getLog();
			
			if(requestAproveLog.equalsIgnoreCase("quit")){
				System.out.println("you are banned");
				System.out.println("сообщение от сервера авторизации - " + dialogPackFromAuthorization.getMessage());
				System.exit(- 1);
			}
			
			// во всех остальных случаях если я не забанен продолжаю работу - шлю первый диалоговый пакет диалоговому
			// серверу предварительно вставив в него номер сессии из пакета от сервера авторизации
			sessionFromRunAuthorization = dialogPackFromAuthorization.getSessionId();
			oisAutor.close();
			oosAuthor.close();
			socketAuth.close();
			
			DialogPacket firstPackToDialog = new DialogPacket(loginInClient, pasInClient, "first message in session",
			                                                  sessionFromRunAuthorization,
			                                                  timeStampFromRunAuthorization);
			ooDialog.writeObject(firstPackToDialog);
			ooDialog.flush();
			//принимаю ответ на первый пакет и вывожу на консоль
			dialogPacketInSessionFromServer = (DialogPacket)oiDialog.readObject();
			System.out.println(dialogPacketInSessionFromServer.getMessage());
			
			// главный цикл общения
			while(! socketDialog.isClosed()){
				
				System.out.println("type in your message: ");
				
				message = bufferedReader.readLine();
				dialogPacketInSessionFromUser = new DialogPacket(loginInClient, pasInClient, message, sessionIdInClient,
				                                                 new Date().getTime());
				ooDialog.writeObject(dialogPacketInSessionFromUser);
				ooDialog.flush();
				
				if(message.equalsIgnoreCase("quit")){
					dialogPacketInSessionFromServer = (DialogPacket)oiDialog.readObject();
					reply = dialogPacketInSessionFromServer.getMessage();
					System.out.println("Server replyed" + reply);
					
					oiDialog.close();
					ooDialog.close();
					socketDialog.close();
					
					oisAutor.close();
					oosAuthor.close();
					socketAuth.close();
					System.exit(0);
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
