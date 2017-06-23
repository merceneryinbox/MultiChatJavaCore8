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
	private static long         deltaTime;
	private static DialogPacket dialogPacket;
	private static DialogPacket dialogPacketToUser;
	private static String       messageFromUser;
	private static String       logFromUser;
	private static String       pasFromUser;
	private static int          sessionidFromUser;
	private static long         timeStampFromUser;
	
	String dbDriver = "org.postgresql.Driver";
	String urlDB    = "jdbc:postgresql://localhost:5432/chatPro";
	String dbLog    = "postgres";
	String dbPasg   = "postgres";
	
	public RunDialog(Socket socket){
		System.out.println(
			"RunDialog thread's constructor starts, time stamping for session alive check, socket initializing");
		timeStampServerDialog = new java.util.Date().getTime();
		this.socket = socket;
		try{
			System.out.println("Try without resources in RunDialog's constructor starts");
			System.out.println("Loading DB driver");
			Class.forName(dbDriver);
			System.out.println("Creating connection");
			connectionDB = DriverManager.getConnection(urlDB, dbLog, dbPasg);
			System.out.println("creating preparedstatemens");
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
			System.out.println("End of try without resources block in RunDialog's constructor");
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
		System.out.println("run() method in RunDialog starts, oostream & oistream creating in catch socket");
		try(ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
		    ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream())){
			System.out.println("Reading dialogpacket from user from socket");
			dialogPacket = (DialogPacket)objectInputStream.readObject();
			System.out.println("Gathering all data about connected users port");
			String ipadress = String.valueOf(socket.getInetAddress());
			logFromUser = dialogPacket.log;
			messageFromUser = dialogPacket.message;
			pasFromUser = dialogPacket.pass;
			sessionidFromUser = dialogPacket.sessionId;
			timeStampFromUser = dialogPacket.timeStampFromDiPa;
			System.out.println("Setting control Controlpreparedstatement's parameter");
			pSControllUserIncom.setString(1, logFromUser);
			System.out.println("saving result of Controlpreparedstatement execution into resultSet");
			resultSet = pSControllUserIncom.executeQuery();
			System.out.println("Check if user's account already exists in user table in DB");
			boolean checkInBase = resultSet.next();
			if(checkInBase == false){// если нет в базе текущих сессий выключаем канал
				System.out.println("User not allowed for chatting, close session");
				closeSession();
			}
			
			// иначе продолжаем проверку
			System.out.println("User's account found in DB continue checking him. Check delta time of timestamping to"
			                   + " make sure that session is not outdated");
			timeStampfromDB = resultSet.getLong("timestampforsess");
			deltaTime = timeStampServerDialog - timeStampfromDB;
			
			resultSet.close();
			pSControllUserIncom.close();
			
			//  выключаем канал если сессия устарела после записи о неудачной попытке в таблицу illegalattempt
			if(deltaTime > 60000){
				System.out.println("Session is out of date. Make record into DB about this fact");
				psSIlligalAttempt.setString(1, logFromUser);
				psSIlligalAttempt.setString(2, pasFromUser);
				psSIlligalAttempt.setString(3, "session is outdated, you are disconnected");
				psSIlligalAttempt.setInt(4, sessionidFromUser);
				psSIlligalAttempt.setLong(5, timeStampServerDialog);
				psSIlligalAttempt.setString(6, ipadress);
				psSIlligalAttempt.executeUpdate();
				
				closeSession();
			}
			
			// иначе сохраняем первый пакет в таблицу сессий
			System.out.println("Session outdated checking succesfull, preparing statement to save visible start of "
			                   + "session start in chatpro.sessionsstory table in DB");
			pSSaveFirstPackInUsers.setString(1, logFromUser);
			pSSaveFirstPackInUsers.setInt(2, sessionidFromUser);
			pSSaveFirstPackInUsers.setLong(3, timeStampServerDialog);
			pSSaveFirstPackInUsers.executeUpdate();
			pSSaveFirstPackInUsers.close();
			
			// отвечаем клиенту
			System.out.println("Try to reply to client about successful session checking");
			dialogPacketToUser = new DialogPacket(logFromUser, pasFromUser, "Talking start.", sessionidFromUser,
			                                      timeStampServerDialog);
			objectOutputStream.writeObject(dialogPacketToUser);
			objectOutputStream.flush();
			
			// и запускаем цикл диалога
			while(! socket.isClosed()){
				System.out.println("Starts main dialog loop in RunDialog");
				// читаем из канала пакет
				System.out.println("Reading from channel client's packet");
				DialogPacket dialogInLoop = (DialogPacket)objectInputStream.readObject();
				//распаковываем пакет
				System.out.println("Extract all from inside packet");
				long   timeStampInSession = new java.util.Date().getTime();
				String logInLoop          = dialogInLoop.log;
				int    sesInLoop          = dialogInLoop.sessionId;
				String mesInLoop          = dialogInLoop.message;
				
				// сохраняем пакет в базе данных в таблице истории сообщений
				System.out.println("Saving all data from packet to chatpro.sessionsstory table into DB");
				pSSaveStoryInSessions.setString(1, logInLoop);
				pSSaveStoryInSessions.setInt(2, sesInLoop);
				pSSaveStoryInSessions.setString(3, mesInLoop);
				pSSaveStoryInSessions.setLong(4, timeStampInSession);
				
				pSSaveStoryInSessions.executeUpdate();
				System.out.println("Try to echo to connected client if socket is alive");
				String serverEchoReply = "server echo : " + mesInLoop;
				//TODO дальше реализовать переговоры между юзерами на основе логинов(нужен пакет - Privat и таблица
				// привата в базе)
				// отвечаем клиенту
				if(! socket.isClosed()){
					objectOutputStream.writeObject(
						new DialogPacket(logInLoop, pasFromUser, serverEchoReply, sesInLoop, timeStampInSession));
					objectOutputStream.flush();
				}
				System.out.println("Checking if message from user has disconnecting keyword");
				if(mesInLoop.equalsIgnoreCase("quit")){
					System.out.println("Message from user has disconnecting keyword, close session");
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
			System.out.println("Starts closesession() method in RunDialog");
			resultSet.close();
			pSControllUserIncom.close();
			pSSaveStoryInSessions.close();
			pSSaveFirstPackInUsers.close();
			connectionDB.close();
			socket.close();
			System.exit(0);
		} catch(IOException e) {
			e.printStackTrace();
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}
}