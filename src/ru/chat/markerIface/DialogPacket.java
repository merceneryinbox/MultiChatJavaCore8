package ru.chat.markerIface;

import java.io.Serializable;

/**
 * Created by mercenery on 15.06.2017.
 */
public class DialogPacket implements Serializable{
	private static String log;
	private static String pass;
	private static String message;
	private static int sessionId;
	private static long timeStampFromDiPa;
	
	public DialogPacket(String log, String pass, String message, int sessionId, long timeStampFromDiPa){
		
		this.log = log;
		this.pass = pass;
		this.message = message;
		this.sessionId = sessionId;
		this.timeStampFromDiPa = timeStampFromDiPa;
	}
	
	public static String getLog(){
		return log;
	}
	
	public static void setLog(String log){
		DialogPacket.log = log;
	}
	
	public static String getPass(){
		return pass;
	}
	
	public static void setPass(String pass){
		DialogPacket.pass = pass;
	}
	
	public static String getMessage(){
		return message;
	}
	
	public static void setMessage(String message){
		DialogPacket.message = message;
	}
	
	public static int getSessionId(){
		return sessionId;
	}
	
	public static void setSessionId(int sessionId){
		DialogPacket.sessionId = sessionId;
	}
	
	public static long getTimeStampFromDiPa(){
		return timeStampFromDiPa;
	}
	
	public static void setTimeStampFromDiPa(long timeStampFromDiPa){
		DialogPacket.timeStampFromDiPa = timeStampFromDiPa;
	}

}
