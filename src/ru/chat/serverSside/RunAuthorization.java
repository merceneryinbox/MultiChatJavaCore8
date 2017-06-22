package ru.chat.serverSside;

import ru.chat.markerIface.AuthPacket;
import ru.chat.markerIface.DialogPacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
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
	private static String passFromDB;
	private static int    sessionIDd;
	
	public RunAuthorization(Socket socket){
		timeStampDef = new Date().getTime();
		sessionIDd = (int)(Math.random() * 1000000);
		
		try{
			this.socketClient = socket;
			Class.forName(dbDriver);
			connection = DriverManager.getConnection(url, logEnterDB, pasEnterDB);
			// запрос который вернёт или ничего если юзер не зарегистрирован и тогда его нужно зарегистрировать или
			// вернёт не ничего и тогда регистрацию можно пропустить
			pSCheckRequest = connection.prepareStatement(
				"select code from chatpro.users  where upper(login) = upper" + "(?)");
			// запрос который прописывает юзера в базу данных в таблицу users
			psSRegistration = connection.prepareStatement(
				"insert into chatpro.users (login,pass,code) values (?,?,?) on conflict (login) do nothing;");
			// запрос который вносит сессию юзера в базу данных в таблицу aprovedsessions для проверки в диалоге
			pSSesionAprove = connection.prepareStatement(
				"insert into chatpro.aprovedsessions (login,timestampforsess) values(?,?) ;");
			
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * When an object implementing interface <code>Runnable</code> is used
	 * to create a thread, starting the thread causes the object's
	 * <code>run</code> method to be called in that separately executing
	 * thread.
	 * <p>
	 * The general contract of the method <code>run</code> is that it may
	 * take any action whatsoever.
	 *
	 * @see Thread#run()
	 */
	@Override
	public void run(){
		try(ObjectOutputStream objectOutputStream = new ObjectOutputStream(socketClient.getOutputStream());
		    ObjectInputStream oIStreamClient = new ObjectInputStream(socketClient.getInputStream())){
			
			// читаем пакет авторизации от клиента
			AuthPacket authPacket = (AuthPacket)oIStreamClient.readObject();
			//вынимаем из него логин и пароль
			loginFromPacket = authPacket.getLoin();
			pasFromPacket = authPacket.getPass();
			
			backCodeFromDB = authorization(loginFromPacket, pasFromPacket);
			
			if(backCodeFromDB == - 1){
				objectOutputStream.writeObject(
					new DialogPacket("quit", "quit", "you are banned", sessionIDd, timeStampDef));
				objectOutputStream.flush();
			}
			else {
				sessionassigne();
				objectOutputStream.writeObject(new DialogPacket("ok", "ok", "ok author", sessionIDd, timeStampDef));
				//String log, String pass, String message, int sessionId, long timeStampFromDiPa
				objectOutputStream.flush();
			}
			// TODO отсюда реализовать поведение клиента, если он получил -1 тогда сам отключается, если 0 тогда
			// соединяется со вторым сервером(создаёт новый сокет и уже туда шлёт информацию).
			
		} catch(IOException e) {
			e.printStackTrace();
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
		} finally{
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
		try{
			psSRegistration.setString(1, l);
			psSRegistration.setString(2, p);
			psSRegistration.setInt(3, 0);
			psSRegistration.executeUpdate();
			
			pSCheckRequest.setString(1, l);
			resultSetCheck = pSCheckRequest.executeQuery();
			resultSetCheck.next();
			
			backCode = resultSetCheck.getInt("code");
			
		} catch(SQLException e) {
			e.printStackTrace();
		}
		return backCode;
	}
	
	public void sessionassigne(){
		try{
			pSSesionAprove.setString(1, loginFromPacket);
			pSSesionAprove.setLong(2, timeStampDef);
			pSSesionAprove.executeUpdate();
			
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}
}