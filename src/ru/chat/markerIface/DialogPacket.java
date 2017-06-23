package ru.chat.markerIface;

import java.io.Serializable;

/**
 * Created by mercenery on 15.06.2017.
 */
public class DialogPacket implements Serializable{
	public static final long serialVersionUID = 1L;
	public String log;
	public String pass;
	public String message;
	public int    sessionId;
	public long   timeStampFromDiPa;
	
	public DialogPacket(String log, String pass, String message, int sessionId, long timeStampFromDiPa){
		
		this.log = log;
		this.pass = pass;
		this.message = message;
		this.sessionId = sessionId;
		this.timeStampFromDiPa = timeStampFromDiPa;
	}
}