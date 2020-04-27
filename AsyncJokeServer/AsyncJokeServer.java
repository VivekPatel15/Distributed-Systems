/*--------------------------------------------------------

1. Vivek Patel / June 2, 2019:

2. Java build 1.8.0_201

3. Precise command-line compilation examples / instructions:

> javac *.java


4. Precise examples / instructions to run this program:

In separate shell windows:

> java AsyncJokeServer
> java AsyncJokeClient
> java AsyncJokeAdminClient

All acceptable commands are displayed on the various consoles.

This runs across machines, in which case you have to pass the IP address of
the server to the clients. For exmaple, if the server is running at
140.192.1.22 then you would type:

> java JokeClient 140.192.1.22
> java JokeClientAdmin 140.192.1.22

5. List of files needed for running the program.

 a. JokeServer.java
 b. JokeClient.java
 c. JokeClientAdmin.java

5. Notes:

AdminLooper and implementation in main() method comes from Prof. Elliot’s code:
http://condor.depaul.edu/elliott/435/hw/programs/joke/joke-threads.html

AsyncJokeClient takes input from the user. First, it asks for the name of the user, then it asks for a simple
<enter> to receive a joke or proverb. Client will then send the request to the AsyncJokeServer. Client will listen
on a UDP server for a response. While waiting for a response, the client will as the user to input 2 numbers with a 
space separating the two. Once the Client recieves a joke/proverb from the UDP server, it will store the message and 
update its state. The Client will only print a message after completing a sum.

AsyncJokeServer takes input from the AsyncJokeClient and, in a seperate port, AsyncJokeAdminClient. The request from
the client is processed by checking the current output mode (jokes or proverbs) then, before packaging and sending 
data to the UDP server, the thread will sleep for 40 seconds. After the 40s, the Server will pacage the data that will
be sent to the UDP server. 

AsyncJokeAdminClient is the same as the previous iteration. Takes command inputs from the user and sends them to the server.
Admin can change the server's output mode or close the Server.

The state is stored on the Client using 2 separate Lists. It is very sloppy, but it worked for me.
The Client makes a list of integers, which is then copied to 2 separate lists: one for jokes and 
one for proverbs. The state lists are then shuffled. We take the first of both lists and send it to
the Server. The Server takes these indexes and picks the one corresponding to the mode and sends it
to the appropriate output method. Then the joke/proverb that has the same index as the Client's is 
sent along with the index and a large number back to the client. The Client then processes this by 
printing out the joke/proverb, removing the index from the temp list that it came from (which is 
determined by comparing the index and the large number) and then checking if the temp list is empty.
If the temp list is empty, then the appropriate cycle completion is annouced and the temp list is 
repopulated and shuffled using the master list.

Help for learning UDP server basics:
https://systembash.com/a-simple-java-udp-server-and-udp-client/

Jokes/Proverbs credits:
JA: https://www.reddit.com/r/Jokes/comments/7he12e/i_hope_elon_musk_never_gets_involved_in_a_scandal/
JB: https://www.reddit.com/r/Jokes/comments/7v0je6/my_girlfriend_is_like_the_square_root_of_100/
JC: https://www.reddit.com/r/Jokes/comments/6zukvv/i_invited_my_girlfriend_to_go_to_the_gym_with_me/
JD: https://www.reddit.com/r/Jokes/comments/basb7p/as_i_handed_my_dad_his_50th_birthday_card_he/
PA: http://www.goldenproverbs.com/au_confucius.html
PB: http://www.goldenproverbs.com/au_plato.html
PC: http://www.goldenproverbs.com/au_gandhi.html
PD: http://www.goldenproverbs.com/au_verne.html

----------------------------------------------------------*/

import java.io.*;
import java.net.*;
import java.util.*;

// Communicates with the client, recieving 2 index numbers and a name,and sends either a joke or proverb, 
//depending on the mode set by the Admin, joke by default. Utilizes a  UDP connection to send the joke/proverb
//to the client.
class ClientWorker extends Thread {  
	Socket sock;
	ClientWorker (Socket s) {sock = s;}	// constructor for the Client Worker

	public void run() { //the main code for conversation with a client. 
		//PrintStream out = null;
		BufferedReader in = null;
		int udpPort;
		
		//initialize joke and proverb lists
		ArrayList<String> jokes = new ArrayList<String>(4);
		ArrayList<String> proverbs = new ArrayList<String>(4);

		InetAddress udpAddress = sock.getInetAddress(); // grabs the IP Address
		
		StringBuilder toSend = new StringBuilder(); // creates a StringBuilder object to be sent to the client

		try { // tries to create an input reader and a Datagram Socket
			in = new BufferedReader(new InputStreamReader (sock.getInputStream()));
			//out = new PrintStream(sock.getOutputStream());
			DatagramSocket dataSock = new DatagramSocket();
			
			try { // tries to read the input from the client, process it, and send the output to the client
				// Initializes variables needed by the ClientWorker to process the input and prepare for output
				// indexJ and indexP refer to the saved state stored on the client. 
				String tempID; 
				String name;
				String mode;
				tempID = in.readLine(); 			// the input from the client, containing a string of a name, 2 index values, and the UDP port the Server needs to connect to
				String id[] = tempID.split(", ");	 // splits the input into a String array so the input can be set to the appropriate variables
				name = id[0]; 						// this is the name the user entered into the Client
				
				//Creates temporary strings from the input that are then converted to integers to be used as index
				//values to reference the joke and proverb lists
				String inJTemp = id[1]; 			
				int indexJ = Integer.parseInt(inJTemp); 
				String inPTemp = id[2];					
				int indexP = Integer.parseInt(inPTemp);
				
				String udpTemp = id[3]; // pulls the udp port from the input
				udpPort = Integer.parseInt(udpTemp); // converts the udp port string to an int
				
				// Creates the lists for the jokes and proverbs. They are in 2 separate lists.
				jokeBuilder(name, jokes);
				proverbBuilder(name, proverbs);
				
				// checks the current mode and then sends the corresponding list, index number, and
				//PrintStream to create and send the output joke/proverb. Prints on the server as to
				//what the server is sending and to whom.
				mode = AdminWorker.mode; 

				Thread.sleep(40000); // the thread will sleep for 40 seconds before returning to the task at hand.
									// this is to give the client time to interact with the user (in this case requesting 2 numbers and summing them) 

				if (mode == "joke") { // if the mode is set to "joke," then send a joke
					System.out.println("Sending joke to " + name); // prints to the console who will recieve the joke	
					sendJoke(jokes, indexJ, toSend, dataSock, udpAddress, udpPort);								
				}
				else { // if the mode is set to "proverb" then send a proverb
					System.out.println("Sending proverb to " + name);// prints to the console who will recieve the proverb
					sendProverb(proverbs, indexP, toSend, dataSock, udpAddress, udpPort);						
				}
			} catch (IOException x) { // catches an IO exception and prints an error message
				System.out.println("Server read error");
				x.printStackTrace();
			} catch (InterruptedException ie) {ie.printStackTrace();
			}
			sock.close(); // closes the connection to this specific client
		} catch (IOException ioe) {System.out.println(ioe);} // catches an IO exception and prints an error message	
	}

	// the list builders for each Jokes and Proverbs. Here we take the name from the client and add it to the joke output. 
	static void jokeBuilder(String name, ArrayList<String> jokes){
		jokes.add("JA " + name + ": I hope Elon Musk never gets involved in a scandal. Elongate would be really drawn out.");
		jokes.add("JB " + name + ": My girlfriend is like the square root of -100. A solid 10 but also imaginary.");
		jokes.add("JC " + name + ": I invited my girlfriend to go to the gym with me and then I didn\'t show. I hope she gets the message that we\'re not working out.");
		jokes.add("JD " + name + ": As I handed my dad his 50th birthday card he looked at me with tears in his eyes and said\" Y'know one would have been enough.\"");
	}

	static void proverbBuilder(String name, ArrayList<String> proverbs){
		proverbs.add("PA " + name + ": He who learns but does not think is lost! He who thinks but does not learn is in great danger. -Confucius");
		proverbs.add("PB " + name + ": Wise men speak because they have something to say; Fools because they have to say something. -Plato");
		proverbs.add("PC " + name + ": An eye for an eye only ends up making the whole world blind. -Mahatma Gandhi");
		proverbs.add("PD " + name + ": Science my lad is made up of mistakes but they are mistakes which it is useful to make because they lead little by little to the truth. -Jules Verne");
	}

	// the calls to send the joke or proverb to the UDP server. The Client will connect to the UDP server to collect the joke/proverb
 
	static void sendJoke(ArrayList<String> list, int jDex, StringBuilder sb, DatagramSocket ds, InetAddress udpAddress, int udpPort) {
		byte[] packet = new byte[1024]; 

		String jp = list.get(jDex); // grabs the randomly selected joke from the list
		 
		sb.append(jp + ", " + jDex + ", " + 5 + ", \n"); // appends the joke, used index number, dummy index number, and a line break to the StringBuilder to be sent to the client
		packet = sb.toString().getBytes(); // converts the StringBuilder to a string, then to bytes
		DatagramPacket dataPack = new DatagramPacket(packet, packet.length, udpAddress, udpPort); // creates a packet from the converted bytes
		try {
			ds.send(dataPack); // sends the packet to the UDP Server
		} catch (IOException ioe) {ioe.printStackTrace(); //catches IO exceptions and prints an error message
		}
		ds.close(); // closes the connection to the UDP server
	}
	
	static void sendProverb(ArrayList<String> list, int pDex, StringBuilder sb, DatagramSocket ds, InetAddress udpAddress, int udpPort) {
		byte[] packet = new byte[1024];

		String jp = list.get(pDex); // grabs the randomly selected joke from the list
		 
		sb.append(jp + ", " + 5 + ", " + pDex + ", \n"); // appends the proverb, dummy index number, used index number, and a line break to the StringBuilder to be sent to the client
		packet = sb.toString().getBytes(); // converts the StringBuilder to a string, then to bytes
		DatagramPacket dataPack = new DatagramPacket(packet, packet.length, udpAddress, udpPort); // creates the packet from the converted bytes
		try {
			ds.send(dataPack); // sends the packet to the UDP Server
		} catch (IOException ioe) {ioe.printStackTrace(); //catches IO exceptions and prints an error message
		}
		ds.close(); // closes the connection to the UDP server
	}
}

// Communicates with the Admin Client to set the mode or shut the server down. Input from
// the admin is a single character. Admin controls are posted in the Admin Client console.
class AdminWorker extends Thread { 
	Socket sock;
	AdminWorker (Socket s) {sock = s;}
	public static String mode = "joke";

	public void run() { // runs the Admin server functions (mode swap or shut down)
		PrintStream out = null;
		BufferedReader in = null;
		try { // tries to create an input reader and an output reader
			in = new BufferedReader(new InputStreamReader (sock.getInputStream()));
			out = new PrintStream(sock.getOutputStream());
			try { // takes the input from the Admin Client, processes the command, and will do 1 of 3 things
				// 1) switch the mode to Joke and print a message confirming the switch, 2) same as 1), but with Proverb, 3) Shut the server down
				// 4) do nothing and post a message on the Server.
				String modeInput;
				modeInput = in.readLine(); // takes the input
				switch (modeInput) {
					case "j":
						modeSwitch("j");
						break;
					case "p":
						modeSwitch("p");
						break;
					case "s":
						System.out.println("Shutting down due to Admin command.");
						System.exit(1);
						break;
					default:
						System.out.println("Invalid command.");
						break;					
				}
			} catch (IOException x) { // catches an IO exception and prints an error message
				System.out.println("Server read error");
				x.printStackTrace();
			}
			sock.close(); // closes the connection to this specific client
		} catch (IOException ioe) {System.out.println(ioe);} // catches an IO exception and prints an error message	
	}

	// Switches the mode based on the input from the Admin Client
	static void modeSwitch(String m){
		if (m == "j"){ 
			mode = "joke"; 
			System.out.println("Currently telling jokes.");
		}
		else if ( m == "p"){
			mode = "proverb";
			System.out.println("Currently telling proverbs.");
		}
	}
}

// Creates the loop that connects to the Admin Client. The server will wait and listen for the
// Admin to send a command. Neither Admin Client nor JokeServer is needed to be active when starting 
// the other. 
class AdminLooper implements Runnable {
  public static boolean adminControlSwitch = true;

  public void run(){
    System.out.println("In the admin looper thread");
    
    int q_len = 6;
    int port = 4555; // the Admin Client's port, different fromt the Client port
    Socket sock;

    try{
      ServerSocket servsock = new ServerSocket(port, q_len);
      while (adminControlSwitch) {
	sock = servsock.accept();
	new AdminWorker (sock).start(); 
      }
    }catch (IOException ioe) {System.out.println(ioe);}
  }
 }


public class AsyncJokeServer{ 
	//Starts the server and deploys workers as needed when each Client connects
	public static void main(String[] args) throws IOException {
		int q_len = 6; 
		int port = 4545; // the port connection the server is listening to for Clients
		Socket sock;
		
		// Initializes the loop for the Admin Client to connect to
		AdminLooper AL = new AdminLooper(); 
    	Thread t = new Thread(AL);
    	t.start();

		ServerSocket servsock = new ServerSocket(port, q_len); //initializes the server socket and port

		System.out.println ("Vivek Patel's AsyncJokeServer 1.0 starting up, listening at port 4545 (client) and 4555 (admin).\n");
		while (true) { // listens for a client to connect
			sock = servsock.accept(); // accepts the client
			new ClientWorker(sock).start(); // creates a worker for the client
		}
	}
}