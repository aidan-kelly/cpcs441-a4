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
	boolean[] neighborMatrix; //code was sending to non-neighbors before, this will simplify it.
	
	Timer timer;
	int numRouters;	
	
	
	
    /**
     * Constructor to initialize the rouer instance 
     * 
     * @param routerId			Unique ID of the router starting at 0
     * @param serverName		Name of the host running the network server
     * @param serverPort		TCP port number of the network server
     * @param updateInterval	Time interval for sending routing updates to neighboring routers (in milli-seconds)
     */
	public Router(int routerId, String serverName, int serverPort, int updateInterval) {

		//Initialize all the passed in vars.
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

		//open the tcp connection
		try{
			sock = new Socket(serverName, serverPort);
			dIn = new ObjectInputStream(sock.getInputStream());
			dOut = new ObjectOutputStream(sock.getOutputStream());
			
			//send the server our hello pkt
			DvrPacket dvr = new DvrPacket(this.routerId, DvrPacket.SERVER, DvrPacket.HELLO);
			dOut.writeObject(dvr);
			dOut.flush();
			
			//process the servers response.
			DvrPacket serverResponse = (DvrPacket) dIn.readObject();
			processDvr(serverResponse);
			
			//creating the nexthop
			nexthop = new int[numRouters];
			for(int i = 0; i<numRouters; i++){
				
				if(i==routerId){
					
					nexthop[i] = i;
					
				}else if(linkcost[i] != DvrPacket.INFINITY){
					
					nexthop[i] = i;
					
				}else{
					
					nexthop[i] = -1;
					
				}
				
			}
			
			//start timer
			timer = new Timer(true);
			timer.scheduleAtFixedRate(new TimeoutHandler(this), updateInterval, updateInterval);
			
			//loop until quit
			DvrPacket packet = new DvrPacket();
			do{
				try{
					packet = (DvrPacket) dIn.readObject();
					processDvr(packet);
				}catch(Exception e){
					System.out.println(e.getMessage());;
				}
								
			}while(packet.type != DvrPacket.QUIT);
			
			//clean up time
			dIn.close();
			dOut.close();
			timer.cancel();
			sock.close();
			
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
		
		//returns the table
		return new RtnTable(mincost[routerId], nexthop);
	}
	
	//method to deal with the DVR packets
	public void processDvr(DvrPacket dvr){
		
		//see who sent the packet
		//check if it was the server
		if(dvr.sourceid == DvrPacket.SERVER){
			
			//if it's the hello pkt
			if(dvr.type == dvr.HELLO){
				
				//set up all the needed arrays
				numRouters = dvr.mincost.length;
				mincost = new int[numRouters][numRouters];
				mincost[routerId] = dvr.mincost;
				linkcost = new int[numRouters];
				linkcost = dvr.mincost;
				
				//set up a matrix that lets us easily know if a node is a neighbor
				neighborMatrix = new boolean[numRouters];
				for(int i =0; i<numRouters; i++){
					if(linkcost[i] == 0 || linkcost[i] == DvrPacket.INFINITY){
						neighborMatrix[i] = false;
					}else{
						neighborMatrix[i] = true;
					}
				}
				
				System.out.println("Finished handshake.");
				
			//if quit pkt, let user know
			}else if(dvr.type == dvr.QUIT){
				System.out.println("It's quiting time boys.");
				
				
			//topology has changed here.
			}else{
				
				//recreate the arrays
				numRouters = dvr.mincost.length;
				mincost = new int[numRouters][numRouters];
				mincost[routerId] = dvr.mincost;
				linkcost = new int[numRouters];
				linkcost = dvr.mincost;
				
				neighborMatrix = new boolean[numRouters];
				for(int i =0; i<numRouters; i++){
					if(linkcost[i] == 0 || linkcost[i] == DvrPacket.INFINITY){
						neighborMatrix[i] = false;
					}else{
						neighborMatrix[i] = true;
					}
				}
				
				//let user know
				System.out.println("The topology has changed.");
			}
			
		//else it was another router.
		//need to check if this changes our mincost vector.
		//if so send it out and reset timer.
		}else{
			mincost[dvr.sourceid] = dvr.mincost;
			
			//boolean minChanged = false;
			
			for(int i = 0; i < numRouters; i++) {
				
				if(i == routerId) {
					continue;
				}
				
				//use the bellman-ford algorithm here
				//check if what we have (mincost[routerId][i] is less efficent than going to this node and then traveling to i using it's path.
				if(mincost[routerId][i] > linkcost[dvr.sourceid] + mincost[dvr.sourceid][i]) {
					
					//if it is, we need to update our mincost vector to make it as efficent as possible.
					mincost[routerId][i] = linkcost[dvr.sourceid] + mincost[dvr.sourceid][i];
					
					//need to remember what node to go through to get the efficient path
					nexthop[i] = dvr.sourceid;
					
					//sending to all neighbors
					sendPkts();
					
					timer.cancel();
					timer.scheduleAtFixedRate(new TimeoutHandler(this), updateInterval, updateInterval);
				}
					
			}
		
		}
	}
	
	//what we do when it times out
	public void sendPkts(){
		
		for(int i = 0; i<numRouters; i++){
						
			if(neighborMatrix[i]){
				try{
					
					DvrPacket toSend = new DvrPacket(routerId,i, DvrPacket.ROUTE, mincost[routerId]);
					dOut.writeObject(toSend);
					dOut.flush();
					
				}catch (Exception e){
					
					System.out.println(e.getMessage());
					
				}
			}
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

