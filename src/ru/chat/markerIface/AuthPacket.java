package ru.chat.markerIface;

import java.io.Serializable;

/**
 * Created by mercenery on 16.06.2017.
 */
public class AuthPacket implements Serializable{
	String login;
	String pass;
	
	public AuthPacket(String login, String pass){
		this.login = login;
		this.pass = pass;
	}
	
	public String getPass(){
		return pass;
	}
	
	public void setPass(String pass){
		this.pass = pass;
	}
	
	public String getLoin(){
		
		return login;
	}
	
	public void setLogin(String loin){
		this.login = loin;
	}
}
