package chatRoom;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;

public class Server {
	static BufferedReader br = null;
	static List<UserInfo> listOflockedIds;
	static List<ServerInfo> listOfservers;
	static List<UserInfo> listOfusers;
	static List<chatRoomInfo> listOfrooms;
	static List<chatRoomInfo> listOfLockedRooms;
	static List<LoginInfo> listOfAuthUsers;

	static String currentServerId;
	
	static String isNew;
	
	static Server sslServer;
	static SSLServerSocket ssllisteningSocket;
	static SSLServerSocket ssllisteningServerSocket;
	
	public Server() {
		listOfAuthUsers = new ArrayList<LoginInfo>();
		listOflockedIds = new ArrayList<UserInfo>();
		listOfservers = new ArrayList<ServerInfo>();
		listOfusers = new ArrayList<UserInfo>();
		listOfrooms = new ArrayList<chatRoomInfo>();
		listOfLockedRooms = new ArrayList<chatRoomInfo>();
		
	}

	
	public static void main(String[] arstring) throws Exception {
		
		System.setProperty("javax.net.ssl.keyStore","C:/UniMelb/dsassignment2/SSLDemo/chatroom.jks");		
		System.setProperty("javax.net.ssl.keyStorePassword","chatroom");
		System.setProperty("javax.net.ssl.trustStore","C:/UniMelb/dsassignment2/SSLDemo/servertrust.jks");
		System.setProperty("javax.net.ssl.trustStorePassword","chatroom");
		
		// Enable debugging to view the handshake and communication which happens between the SSLClient and the SSLServer
		System.setProperty("javax.net.debug","all");
		
	    	String configFile= null;
		
		System.out.println("reading command line options");
		
		Options options = new Options();
		options.addOption("l",true,"configuration file path");
		options.addOption("n",true,"server id");
		
		CommandLineParser parser = new DefaultParser();
		
		CommandLine cmd = null;
		try {
			cmd = parser.parse( options, arstring);
		} catch (org.apache.commons.cli.ParseException e) {
			
			e.printStackTrace();
		}
		
		if(cmd.hasOption("l")){
			configFile = cmd.getOptionValue("l");
		}
		
		if(cmd.hasOption("n")){
			currentServerId = cmd.getOptionValue("n");
		}
		Server server = new Server();
		server.execute(currentServerId, configFile);
	}
	
	
	public void execute(String currentServerId, String configFile) throws Exception {
		ServerInfo s;
		int serverPort = 4444;
		
		
		
		try{
			
			br = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), "UTF-8"));
			String line;
	        while ((line = br.readLine()) != null)
	        {   
	        	String [] tokens = line.split("\\n");
	        	
	        	for (int i=0 ; i< tokens.length ; i++){
	        		
	        		String [] finalTakens = new String[]{"","","","",""};
	        		String [] newTokens = line.split("\\t");
				for(int j=0;j<newTokens.length;j++){
	        			finalTakens[j]=newTokens[j];
	        			
	        		}
	        		s = new ServerInfo(finalTakens[0],finalTakens[1],Integer.parseInt(finalTakens[2]),Integer.parseInt(finalTakens[3]),finalTakens[4]);
	        		listOfservers.add(s);
	        	}
	        
	        }
	        
	        for (ServerInfo server: listOfservers){
				if (!(server.getServerId().equals("AS"))){
					chatRoomInfo room = new chatRoomInfo();
					room.chatRoomId ="MainHall-"+ server.getServerId();
					room.owner = "";
					room.serverId = server.getServerId();
					listOfrooms.add(room);
				}
			}
	        
			}catch (Exception e){
				System.out.println("configuration file was not found");
				e.printStackTrace();
			}
			
			//serverInfo(listOfservers);
			int serverPort2 = 0;
			for (ServerInfo server : listOfservers ){
				if (server.getServerId().equals(currentServerId)){
	    			serverPort = server.getClientsPort();
	    			serverPort2 = server.getServersPort();
				isNew = server.getIsNew();
	    		}
	        	
	        }
			
			if (currentServerId.equals("AS")){
				File file = new File("loginFile.txt");
				String path = file.getAbsolutePath();
				 try {
					System.out.println("reading Users's login information");
					br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
					String line;
					while ((line = br.readLine()) != null)
					{   
						String [] tokens = line.split("\\n");
						
						for (int i=0 ; i< tokens.length ; i++){
							
							String [] newTokens = line.split("\\t");
							System.out.println(newTokens[0]);
							System.out.println(newTokens[1]);
								 LoginInfo login = new LoginInfo();
								 login.loginUsername = newTokens[0];
								 login.loginPassword = newTokens[1];
								 listOfAuthUsers.add(login);
						}
					
					}
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			//Create SSL server socket
			SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory
					.getDefault();
			try {
			
				SSLServerSocket ssllisteningServerSocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(serverPort2);
				System.out.println("Server is listenting on port " + serverPort2);
				ServerConnection serverConnection = new ServerConnection(listeningServerSocket, currentServerId);

				ServerHeartbeatSensor serverHeartbeatSensor = new ServerHeartbeatSensor(this);
				
				//add new server
				ServerAdmin serverAdmin = new ServerAdmin(currentServerId, configFile);
				
				if(isNew.equals("N")){
					
					MessageHandler.newServer(currentServerId);
					newSerChange(configFile);
				}

				if (!currentServerId.equals("AS")){
					// Create a server socket listening on port 4444
					SSLServerSocket ssllisteningSocket = (SSLServerSocket) sslserversocketfactory.createServerSocket(serverPort);
					
					System.out.println(Thread.currentThread().getName() + 
							" - Server listening on port" + serverPort);
					
					//Listen for incoming connections for ever
					while (true) {
						//Accept an incoming client connection request
						SSLSocket clientSocket = (SSLSocket) ssllisteningSocket.accept();
						System.out.println(Thread.currentThread().getName() 
								+ " - Client conection accepted");
						
						//Create one thread per connection, each thread will be
						//responsible for listening for messages from the client, placing them
						//in a queue, and creating another thread that processes the messages
						//placed in the queue
						ClientConnection clientConnection = new ClientConnection(clientSocket, currentServerId);
						clientConnection.setName("Thread" + currentServerId);
						clientConnection.start();
						
						//Register the new connection with the client manager
						ServerState.getInstance().clientConnected(clientConnection);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if(ssllisteningSocket != null) {
					try {
						ssllisteningSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
	}

	public void deleteServer(String serverId) {
		Iterator<ServerInfo> serverIterator = listOfservers.iterator();
		while (serverIterator.hasNext()) {
			ServerInfo serverInfo = serverIterator.next();
			if(serverInfo.getServerId().equals(serverId)) {
				serverIterator.remove();
				break;
			}
		}
		Iterator<chatRoomInfo> roomIterator = listOfrooms.iterator();
		while (roomIterator.hasNext()) {
			chatRoomInfo roomInfo = roomIterator.next();
			if (roomInfo.getserverId().equals(serverId)) {
				String chatRoomId = roomInfo.chatRoomId;
				ArrayList<ClientConnection> connectedClients = new ArrayList<>(ServerState.getInstance().getConnectedClients());
				for (ClientConnection client : connectedClients){
					if (client.chatRoom.equals(chatRoomId)){
						ServerState.getInstance().clientDisconnected(client);
					}
				}
				roomIterator.remove();
			}
		}
	}

	public static void addServer(String serverId, String address, int cPort, int sPort) {
		ServerInfo serverInfo = new ServerInfo(serverId, address, cPort, sPort);
		listOfservers.add(serverInfo);

		chatRoomInfo room = new chatRoomInfo();
		room.chatRoomId ="MainHall-"+ serverId;
		room.owner = "";
		room.serverId = serverId;
		listOfrooms.add(room);
	}
	
	public void newSerChange(String configFile){
		try {
			
		    BufferedWriter fWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile)));
		    BufferedWriter OWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile,true)));
		    int temp=0;
			for(ServerInfo server: listOfservers){   
				
				String serverId=server.getServerId();
				String serverAdd=server.getServerAddress();
				String serverPort=String.valueOf(server.getClientsPort());
				String serverPort2=String.valueOf(server.getServersPort());
				String outMsg = "";
				outMsg = outMsg.concat(serverId+"	"+serverAdd+"	"+serverPort+"	"+serverPort2);
				
			
				if(temp==0){
					System.out.println(outMsg);
					fWriter.write(outMsg+"\n");
					
					fWriter.flush();
					temp=temp+1;
				}
				else{
					OWriter.write(outMsg+"\n");
					OWriter.flush();
				}
			
			}
			fWriter.close();
			OWriter.close();
			
			
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		 
	public void serverInfo(List<ServerInfo> s){
		Server.listOfservers = s;
	}
	public List<ServerInfo> getServerInfo() {
		return listOfservers;
	}

	
}
