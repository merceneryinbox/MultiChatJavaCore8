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
		System.out.println("Client starts");
		while(untilAuthorize() != 0){
			System.out.println("while loop for authorization starts until it successfully ended");
			untilAuthorize();
		}
		System.out.println("Main try without resources block in Client starts");
		try{
			System.out.println("Creating BufferedReader, socketDialog, ooDialog, oiDialog");
			BufferedReader talk = new BufferedReader(new InputStreamReader(System.in));
			socketDialog = new Socket("localhost", 55555);
			ooDialog = new ObjectOutputStream(socketDialog.getOutputStream());
			oiDialog = new ObjectInputStream(socketDialog.getInputStream());
			System.out.println("Creating firstPackToDialog for the first message to RunDialog");
			firstPackToDialog = new DialogPacket(loginInClient, pasInClient,
			                                     "----------------------------------------------------",
			                                     sessionFromRunAuthorization, timeStampFromRunAuthorization);
			ooDialog.writeObject(firstPackToDialog);
			ooDialog.flush();
			System.out.println("First message in DialogPacket send");
			
			//принимаю ответ на первый пакет и вывожу на консоль
			System.out.println("Starts waits for RunDialog reply my first packet");
			
			dialogPacketInSessionFromServer = (DialogPacket)oiDialog.readObject();
			reply = dialogPacketInSessionFromServer.message;
			System.out.println("Reply for first packet received");
			System.out.println("Server replyed" + reply);
			
			System.out.println("Checking for keywor - quit");
			if(reply.equalsIgnoreCase("quit")){
				System.out.println("KeyWord - quit received, closing my resources");
				ooDialog.close();
				oiDialog.close();
				socketDialog.close();
			}
			//запускаю основной цикл общения
			System.out.println("Closing KeyWord not found, main while loop for talking with RunDialog starts");
			while(! socketDialog.isClosed()){
				
				System.out.print("input your message : ");
				message = talk.readLine();
				System.out.println("Saving my message into variable - message, creating DialogPacket in Client");
				dialogPacketInSessionFromUser = new DialogPacket(loginInClient, pasInClient, message, sessionIdInClient,
				                                                 new Date().getTime());
				ooDialog.writeObject(dialogPacketInSessionFromUser);
				ooDialog.flush();
				System.out.println("DialogPacket in main while loop in Client send");
				
				System.out.println("Checking for - quit keyword");
				if(message.equalsIgnoreCase("quit")){
					System.out.println("Keyword - quit found, close resources");
					if(oiDialog.available() != 0){
						System.out.println("Looking up for last send from RunDialog message if exists");
						dialogPacketInSessionFromServer = (DialogPacket)oiDialog.readObject();
						reply = dialogPacketInSessionFromServer.message;
						System.out.println("Printing last echoreply");
						System.out.println("Server replyed" + reply);
					}
					System.out.println("Closing resources, breaking main while loop");
					ooDialog.close();
					oiDialog.close();
					socketDialog.close();
					break;
				}
				System.out.println("Listen to channel, waits for reply packed");
				dialogPacketInSessionFromServer = (DialogPacket)oiDialog.readObject();
				reply = dialogPacketInSessionFromServer.message;
				
				System.out.println("Server packet found checking for - quit in message");
				if(reply.equalsIgnoreCase("quit")){
					System.out.println("Keyword - quit found, close resources, breaking main while loop");
					System.out.println("Server replyed" + reply);
					ooDialog.close();
					oiDialog.close();
					socketDialog.close();
					break;
				}
				
				
				System.out.println("Keyword - quit not found");
				System.out.println(reply);
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
	}
	
	public static int authoriseMe(){
		System.out.println("authorizeMe() method and its try-with-resources in client starts");
		try(
			// открываем сокет для проверки авторизации
			Socket socketAuth = new Socket("localhost", 12345);
			ObjectOutputStream oosAuthor = new ObjectOutputStream(socketAuth.getOutputStream());
			ObjectInputStream oisAutor = new ObjectInputStream(socketAuth.getInputStream())){
			
			System.out.println("BufferedReader in try in authorizeMe() in Client initializing");
			BufferedReader autorizeReader = new BufferedReader(new InputStreamReader(System.in));
			
			// запрашиваем логин и пароль на ввод от пользователя
			System.out.println("Login:");
			loginInClient = autorizeReader.readLine();
			System.out.println("Pass:");
			pasInClient = autorizeReader.readLine();

//			autorizeReader.close();
			// фолрмируем пакет авторизации и отправляем запрос делегату сервера для проверки возможности авторизации
			//String log, String pass, String message, int sessionId, long timeStampFromDiPa
			System.out.println("dialog packet performing to send to RunAuthorization");
			authPacket = new DialogPacket(loginInClient, pasInClient, "authorization", 0, 0);
			oosAuthor.writeObject(authPacket);
			oosAuthor.flush();
			System.out.println("Authorization request to RunAuthorization send");
			
			// ждём ответа от сервера авторизации с номером сессии
			System.out.println("waits for authorization request");
			dialogPackFromAuthorization = (DialogPacket)oisAutor.readObject();
			// проверяем не забанен ли я (если забанен - тогда в логине будет слово - quit и клиент закрывается
			String requestAproveLog = dialogPackFromAuthorization.log;
			sessionFromRunAuthorization = dialogPackFromAuthorization.sessionId;
			timeStampFromRunAuthorization = dialogPackFromAuthorization.timeStampFromDiPa;
			System.out.println("Reply packet from RunAuthorization received, checking allowing session start");
			if(requestAproveLog.equalsIgnoreCase("quit")){
				System.out.println("Session refused by RunAuthorization");
				System.out.println("you are banned");
				System.out.println("сообщение от сервера авторизации - " + dialogPackFromAuthorization.message);
				return - 1;
			}
			
			// во всех остальных случаях если я не забанен продолжаю работу - шлю первый диалоговый пакет диалоговому
			// серверу предварительно вставив в него номер сессии из пакета от сервера авторизации
			System.out.println("Session allowed returns 0");
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