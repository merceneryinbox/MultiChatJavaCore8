package ru.chat.serverSside;

import ru.chat.markerIface.DialogPacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.sql.*;

/**
 * Created by mercenery on 16.06.2017.
 */
public class RunDialog implements Runnable{
	private static Socket            socket;
	private static Connection        connectionDB;
	private static PreparedStatement pSControllUserIncom;
	private static PreparedStatement pSSaveFirstPackInUsers;
	private static PreparedStatement pSSaveStoryInSessions;
	private static PreparedStatement psSIlligalAttempt;
	
	private static ResultSet    resultSet;
	private static long         timeStampfromDB;
	private static long         timeStampServerDialog;
	private static long         timeStampFromUser;
	private static DialogPacket dialogPacket;
	private static String       messageFromUser;
	private static String       logFromUser;
	private static String       pasFromUser;
	private static int          sessionidFromUser;
	private        int          sessionidFromBase;
	
	String dbDriver = "org.postgresql.Driver";
	String urlDB    = "jdbc:postgresql://localhost:5432/chatPro";
	String dbLog    = "postgres";
	String dbPasg   = "postgres";
	
	public RunDialog(Socket socket){
		timeStampServerDialog = new java.util.Date().getTime();
		this.socket = socket;
		try{
//			this.socket.setSoTimeout(3000);
			Class.forName(dbDriver);
			connectionDB = DriverManager.getConnection(urlDB, dbLog, dbPasg);
			pSControllUserIncom = connectionDB.prepareStatement(
				"select * from chatpro.aprovedsessions where upper(login) = upper (?)");
			pSSaveFirstPackInUsers = connectionDB.prepareStatement(
				"insert into chatpro.sessionsstory (login,sessionid,messages,timeincome) "
				+ "values(?,?,'session start',?)");
			pSSaveStoryInSessions = connectionDB.prepareStatement(
				"insert into chatpro.sessionsstory (login, sessionid, messages, timeincome) values(?,?,?,?)");
			psSIlligalAttempt = connectionDB.prepareStatement(
				"insert into chatpro.illigalattempt(login,pas,mes,ses,timeoftheattempt,ipadressofattempt) values (?,?,"
				+ "?,?,?,?)");
			
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
		try(ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
		    ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream())){
			
			dialogPacket = (DialogPacket)objectInputStream.readObject();
			
			String ipadress = String.valueOf(socket.getInetAddress());
			logFromUser = dialogPacket.getLog();
			pasFromUser = dialogPacket.getPass();
			messageFromUser = dialogPacket.getMessage();
			sessionidFromUser = dialogPacket.getSessionId();
			timeStampFromUser = dialogPacket.getTimeStampFromDiPa();
			
			pSControllUserIncom.setString(1, logFromUser);
			resultSet = pSControllUserIncom.executeQuery();
			
			if(resultSet.next() == false){// если нет в базе текущих сессий выключаем канал
				closeSession();
			}
			
			// иначе продолжаем проверку
			timeStampfromDB = resultSet.getLong("timestampforsess");
			long deltaTime = timeStampServerDialog - timeStampfromDB;
			
			resultSet.close();
			pSControllUserIncom.close();
			
			//  выключаем канал если сессия устарела после записи о попытке
			if(deltaTime > 60000){
				
				psSIlligalAttempt.setString(1, logFromUser);
				psSIlligalAttempt.setString(2, pasFromUser);
				psSIlligalAttempt.setString(3, "error, you are disconnected");
				psSIlligalAttempt.setInt(4, sessionidFromUser);
				psSIlligalAttempt.setLong(5, timeStampServerDialog);
				psSIlligalAttempt.setString(6, ipadress);
				psSIlligalAttempt.executeUpdate();
				
				closeSession();
			}
			
			// иначе сохраняем первый пакет в таблицу сессий
			pSSaveFirstPackInUsers.setString(1, logFromUser);
			pSSaveFirstPackInUsers.setInt(2, sessionidFromUser);
			pSSaveFirstPackInUsers.setLong(3, timeStampServerDialog);
			pSSaveFirstPackInUsers.executeUpdate();
			pSSaveFirstPackInUsers.close();
			
			// и запускаем цикл диалога читаем сообщение от клиента
			while(! socket.isClosed()){
				
				//распаковываем пакет
				DialogPacket dialogInLoop       = (DialogPacket)objectInputStream.readObject();
				long         timeStampInSession = new java.util.Date().getTime();
				String       logInLoop          = dialogInLoop.getLog();
				int          sesInLoop          = dialogInLoop.getSessionId();
				String       mesInLoop          = dialogInLoop.getMessage();
				
				// сохраняем пакет в базе данных в таблице истории сообщений
				pSSaveStoryInSessions.setString(1, logInLoop);
				pSSaveStoryInSessions.setInt(2, sesInLoop);
				pSSaveStoryInSessions.setString(3, mesInLoop);
				pSSaveStoryInSessions.setLong(4, timeStampInSession);
				
				pSSaveStoryInSessions.executeUpdate();
				
				String serverEchoReply = "server echo : " + mesInLoop;
				//TODO дальше реализовать переговоры между юзерами на основе логинов(нужен пакет - Privat и таблица
				// привата)
				objectOutputStream.writeObject(
					new DialogPacket(logInLoop, pasFromUser, serverEchoReply, sesInLoop, timeStampInSession));
				objectOutputStream.flush();
				
				if(mesInLoop.equalsIgnoreCase("quit")){
					closeSession();
					break;
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void closeSession(){
		try{
			resultSet.close();
			pSControllUserIncom.close();
			pSSaveStoryInSessions.close();
			pSSaveFirstPackInUsers.close();
			connectionDB.close();
			socket.close();
		} catch(IOException e) {
			e.printStackTrace();
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}
}