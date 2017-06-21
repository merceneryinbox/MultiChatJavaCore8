package tmp;

import java.net.Socket;
import java.sql.*;

/**
 * Created by mercenery on 21.06.2017.
 */
public class testDB{
	private static Socket            socket;
	private static Connection        connectionDB;
	private static PreparedStatement pSControllUserIncom;
	private static PreparedStatement pSSaveFirstPackInUsers;
	private static PreparedStatement pSSaveStoryInSessions;
	private static PreparedStatement psSIlligalAttempt;
	public static void main(String[] args){
		try{
			String dbDriver = "org.postgresql.Driver";
			String urlDB    = "jdbc:postgresql://localhost:5432/chatPro";
			String dbLog    = "postgres";
			String dbPasg   = "postgres";
			
			Class.forName(dbDriver);
			connectionDB = DriverManager.getConnection(urlDB, dbLog, dbPasg);
			pSControllUserIncom = connectionDB.prepareStatement(
				"select * from chatpro.aprovedsessions where upper(login) = upper (?);");
			pSSaveFirstPackInUsers = connectionDB.prepareStatement(
				"insert into chatpro.sessionsstory (login,sessionid,messages,timeincome) " + "values(?,?,'session start',?);");
			pSSaveStoryInSessions = connectionDB.prepareStatement(
				"insert into chatpro.sessionsstory (login, sessionid, messages, timeincome) values(?,?,?,?);");
			psSIlligalAttempt = connectionDB.prepareStatement(
				"insert into chatpro.illigalattempt(login,pas,mes,ses,timeoftheattempt,ipadressofattempt) values (?,?,"
				+ "?,?,?,?);");
			
			pSSaveStoryInSessions.setString(1, "mercenery");
			pSSaveStoryInSessions.setInt(2, 0);
			pSSaveStoryInSessions.setString(3, "testmes");
			pSSaveStoryInSessions.setLong(4, new java.util.Date().getTime());
			
//			pSSaveStoryInSessions.executeUpdate();
			Statement st = connectionDB.createStatement();
			ResultSet rs = st.executeQuery("select * from chatpro.sessionsstory;");
			
			while(rs.next()){
				System.out.println(rs.getString(1));
				System.out.println(rs.getInt(2));
				System.out.println(rs.getString(3));
				System.out.println(rs.getLong(4));
			}
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
		} catch(SQLException e) {
			e.printStackTrace();
		}
		
	}
}
