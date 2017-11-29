package assignment4;
import java.io.*;
import java.net.*;
import java.util.*;
import cpsc441.a4.shared.*;

/**
 * Router Class
 * 
 * This class implements the functionality of a router
 * when running the distance vector routing algorithm.
 * 
 * The operation of the router is as follows:
 * 1. send/receive HELLO message
 * 2. while (!QUIT)
 *      receive ROUTE messages
 *      update mincost/nexthop/etc
 * 3. Cleanup and return
 * 
 *      
 * @author 	Majid Ghaderi
 * @version	3.0
 *
 */
public class Router {
	
	int routerId;
	String serverName;
	int serverPort;
	int updateInterval;
	Socket sock;
	ObjectInputStream dIn;
	ObjectOutputStream dOut;
	
	int [] linkcost ; // linkcost [ i ] is the cost of link to router i
	int[] nexthop; // nexthop[i] is the next hop node to reach router i
	int [][] mincost; // mincost[i] is the mincost vector of router i
	
	
	
	
	
    /**
     * Constructor to initialize the rouer instance 
     * 
     * @param routerId			Unique ID of the router starting at 0
     * @param serverName		Name of the host running the network server
     * @param serverPort		TCP port number of the network server
     * @param updateInterval	Time interval for sending routing updates to neighboring routers (in milli-seconds)
     */
	public Router(int routerId, String serverName, int serverPort, int updateInterval) {
		// to be completed
		this.routerId = routerId;
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.updateInterval = updateInterval;
	}
	

    /**
     * starts the router 
     * 
     * @return The forwarding table of the router
     */
	public RtnTable start() {
		// to be completed
		
		try{
			sock = new Socket(serverName, serverPort);
			dIn = (ObjectInputStream) sock.getInputStream();
			dOut = (ObjectOutputStream) sock.getOutputStream();
			
			DvrPacket dvr = new DvrPacket(this.routerId, DvrPacket.SERVER, DvrPacket.HELLO);
			dOut.writeObject(dvr);
			dOut.flush();
			
			DvrPacket serverResponse = (DvrPacket) dIn.readObject();
			processDvr(serverResponse);
			
			//start timer
			
			
			//loop until quit
			DvrPacket packet;
			do{
				
				packet = (DvrPacket) dIn.readObject();
				processDvr(packet);
				
				//initialize neighbors/next
				
				
			}while(packet.type != DvrPacket.QUIT);
			
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
		
		
		
		
		return new RtnTable();
	}
	
	//method to deal with the DVR packets
	public void processDvr(DvrPacket dvr){
		
		//see who sent the packet
		//check if it was the server
		if(dvr.sourceid == DvrPacket.SERVER){
			
			if(dvr.type == dvr.HELLO){
				mincost = new int[dvr.mincost.length][dvr.mincost.length];
				mincost[routerId] = dvr.mincost;
				System.out.println("Finished handshake.");
				
			}else if(dvr.type == dvr.QUIT){
				System.out.println("It's quiting time boys.");
				
			}else{
				
			}
		//else it was another router
		}else{
			
		}
		
		
	}

	
	
    /**
     * A simple test driver
     * 
     */
	public static void main(String[] args) {
		// default parameters
		int routerId = 0;
		String serverName = "localhost";
		int serverPort = 2227;
		int updateInterval = 1000; //milli-seconds
		
		if (args.length == 4) {
			routerId = Integer.parseInt(args[0]);
			serverName = args[1];
			serverPort = Integer.parseInt(args[2]);
			updateInterval = Integer.parseInt(args[3]);
		} else {
			System.out.println("incorrect usage, try again.");
			System.exit(0);
		}
			
		// print the parameters
		System.out.printf("starting Router #%d with parameters:\n", routerId);
		System.out.printf("Relay server host name: %s\n", serverName);
		System.out.printf("Relay server port number: %d\n", serverPort);
		System.out.printf("Routing update intwerval: %d (milli-seconds)\n", updateInterval);
		
		// start the router
		// the start() method blocks until the router receives a QUIT message
		Router router = new Router(routerId, serverName, serverPort, updateInterval);
		RtnTable rtn = router.start();
		System.out.println("Router terminated normally");
		
		// print the computed routing table
		System.out.println();
		System.out.println("Routing Table at Router #" + routerId);
		System.out.print(rtn.toString());
	}

}
