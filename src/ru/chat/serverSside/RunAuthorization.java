package ru.chat.serverSside;

import ru.chat.markerIface.DialogPacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.*;
import java.util.Date;

/**
 * Created by mercenery on 16.06.2017.
 */
public class RunAuthorization implements Runnable{
	
	private static String dbDriver   = "org.postgresql.Driver";
	private static String url        = "jdbc:postgresql://localhost:5432/chatPro";
	private static String logEnterDB = "postgres";
	private static String pasEnterDB = "postgres";
	
	private static Socket            socketClient;
	private static Connection        connection;
	private static PreparedStatement pSCheckRequest;
	private static PreparedStatement psSRegistration;
	private static PreparedStatement pSSesionAprove;
	private static ResultSet         resultSetCheck;
	
	private static long   timeStampDef;
	private static String loginFromPacket;
	private static String pasFromPacket;
	private static int    backCodeFromDB;
	private static int    backCode;
	private static int    sessionIDd;
	
	public RunAuthorization(Socket socket){
		System.out.println(
			"Start constructor of RunAuthorization Runnable thread. Stamping time and generate session" + " number");
		timeStampDef = new Date().getTime();
		sessionIDd = (int)(Math.random() * 1000000);
		
		try{
			System.out.println(
				"Starts try block without resources, assigne received socket to RunAuthorization's " + "socket");
			this.socketClient = socket;
			System.out.println("Loading DB driver");
			Class.forName(dbDriver);
			System.out.println("Create DB connection");
			connection = DriverManager.getConnection(url, logEnterDB, pasEnterDB);
			// запрос который вернёт или ничего если юзер не зарегистрирован и тогда его нужно зарегистрировать или
			// вернёт не ничего и тогда регистрацию можно пропустить
			System.out.println("Generate prepareStatements for DB");
			pSCheckRequest = connection.prepareStatement(
				"select code from chatpro.users  where upper(login) = upper" + "(?)");
			// запрос который прописывает юзера в базу данных в таблицу users
			psSRegistration = connection.prepareStatement(
				"insert into chatpro.users (login,pass,code) values (?,?,?) on conflict (login) do nothing;");
			// запрос который вносит сессию юзера в базу данных в таблицу aprovedsessions для проверки в диалоге
			pSSesionAprove = connection.prepareStatement(
				"insert into chatpro.aprovedsessions (login,timestampforsess) values(?,?) ;");
			System.out.println("End constructor's try block");
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @see Thread#run()
	 */
	@Override
	public void run(){
		System.out.println(
			"Starts run() method of RunAuthorization create ObjOut & ObjInp streams to authorize the " + "client");
		try(ObjectOutputStream objectOutputStream = new ObjectOutputStream(socketClient.getOutputStream());
		    ObjectInputStream oIStreamClient = new ObjectInputStream(socketClient.getInputStream())){
			
			// читаем пакет авторизации от клиента
			System.out.println("Read authorization packet(DialogPacket class type) from channel");
			DialogPacket authPacket = (DialogPacket)oIStreamClient.readObject();
			//вынимаем из него логин и пароль
			System.out.println("Analize it and get login and passworg from it");
			loginFromPacket = authPacket.log;
			pasFromPacket = authPacket.pass;
			System.out.println("Starts authorization() method of RunAuthorization, wich returns approve code from DB "
			                   + "for this user");
			backCodeFromDB = authorization(loginFromPacket, pasFromPacket);
			System.out.println("Check if user is banned");
			if(backCodeFromDB == - 1){
				System.out.println("User banned, starts switch off dialog");
				
				if(! socketClient.isClosed()){
					System.out.println("Write last message if channel exists");
					objectOutputStream.writeObject(
						new DialogPacket("quit", "quit", "you are banned", sessionIDd, timeStampDef));
					objectOutputStream.flush();
				}
				System.out.println("Try close resources");
				try{
					resultSetCheck.close();
					pSCheckRequest.close();
					pSSesionAprove.close();
					socketClient.close();
					connection.close();
					System.out.println("System.exit(0) attempt");
					System.exit(0);
				} catch(SQLException e) {
					e.printStackTrace();
				}
			}
			else {
				System.out.println("User's session approved starts sessionassigne() method of RunAuthorization");
				sessionassigne();
				System.out.println("Session created for current user, he has one minute to confirm it by connecting "
				                   + "to DialogServer");
				System.out.println("Send Ok - packet to client");
				
				if(! socketClient.isClosed()){
					objectOutputStream.writeObject(new DialogPacket("ok", "ok", "ok author", sessionIDd, timeStampDef));
					//String log, String pass, String message, int sessionId, long timeStampFromDiPa
					objectOutputStream.flush();
				}
				else if(socketClient.isClosed()){
					System.out.println("Client's socket suddenly closed, close RunAuthorization resources");
					try{
						resultSetCheck.close();
						pSCheckRequest.close();
						pSSesionAprove.close();
						socketClient.close();
						connection.close();
						System.out.println("System.exit(-1) attempt");
						System.exit(- 1);
					} catch(SQLException e) {
						e.printStackTrace();
					}
				}
			}
			// TODO отсюда реализовать поведение клиента, если он получил -1 тогда сам отключается, если 0 тогда
			// соединяется со вторым сервером(создаёт новый сокет и уже туда шлёт информацию).
			
		} catch(IOException e) {
			e.printStackTrace();
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
		} finally{
			System.out.println("Finnalization try starts");
			try{
				resultSetCheck.close();
				pSCheckRequest.close();
				pSSesionAprove.close();
				socketClient.close();
				connection.close();
			} catch(SQLException e) {
				e.printStackTrace();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public int authorization(String l, String p){
		System.out.println("authorization() method in RunAuthorization starts");
		try{
			System.out.println("Preparestatement for registration prepared");
			psSRegistration.setString(1, l);
			psSRegistration.setString(2, p);
			psSRegistration.setInt(3, 0);
			System.out.println("Sending preparestatement registration");
			psSRegistration.executeUpdate();
			pSCheckRequest.setString(1, l);
			System.out.println("Catch respond about approving session from DB to resultSetCheck");
			resultSetCheck = pSCheckRequest.executeQuery();
			resultSetCheck.next();
			System.out.println("Catch backCode of approving session");
			backCode = resultSetCheck.getInt("code");
			
		} catch(SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Returning backCode");
		return backCode;
	}
	
	public void sessionassigne(){
		System.out.println("sessionassigne() method in RunAthorization starts");
		try{
			pSSesionAprove.setString(1, loginFromPacket);
			pSSesionAprove.setLong(2, timeStampDef);
			pSSesionAprove.executeUpdate();
			
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}
}
