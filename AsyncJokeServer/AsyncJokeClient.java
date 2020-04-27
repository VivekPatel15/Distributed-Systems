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
import java.lang.*;
import java.util.*;

class UDPWorker extends Thread{ // Worker thread that will access the UDP server and recieve input from the Async Joke Server
	int udpPort;

	UDPWorker(int p){ // Constructor for UDP Worker
		udpPort = p;
	}

	public void run(){ // runs the Worker thread
		try{
			DatagramSocket dataSock = new DatagramSocket(udpPort); // creats the connection to the UDP server
			byte[] serverData = new byte[1024]; // creates a byte[] object to store the input from the UDP server
			while(true){
				DatagramPacket dataPack = new DatagramPacket(serverData, serverData.length); // creates a packet to be used to collect the packet from the UDP server
				dataSock.receive(dataPack); // collects the packet from the UDP server
				String data = new String(dataPack.getData()); // converts the packet to a string
				String[] text = data.split(", "); // converts the string to a string list, separating the different values sent by the AsyncJokeServer
				AsyncJokeClient.message = text[0]; // extracts the joke/proverb
				// extracts the joke and proverb index numbers and converts them to ints.
				String jTemp = text[1];
				AsyncJokeClient.jDex = Integer.parseInt(jTemp);
				String pTemp = text[2];
				AsyncJokeClient.pDex = Integer.parseInt(pTemp);	

				AsyncJokeClient.ping = true; // lets the Client know that the message has arrived from the UDP server
			}
		}
		catch(Exception e) { e.printStackTrace(); }
	}
}

public class AsyncJokeClient {
	static String message; // initializes the message string so the UDP worker can send it to the Client
	static int jDex; // initializes the index numbers so the UDP worker can send it to the Client
	static int pDex;
	static boolean ping = false; // initializes the ping for the UDP worker to communicate with the Client on the status of the message

	public static void main(String[] args) {
		String serverName;
		if (args.length < 1) serverName = "localhost"; //default server
		else serverName = args[0]; // directed server input from the terminal
		int udpPort = 9876; // Port for the UDP server

		// initializes the index lists for both the joke and proverb states.
		// builds the index lists from 0-3 as these are how many jokes/proverbs are in each list
		// randomizes the indexes to create a random output
		List<Integer> indexListJ = new ArrayList<>(4);
		listBuilder(indexListJ);
		Collections.shuffle(indexListJ);
		List<Integer> indexListP = new ArrayList<>(4);
		listBuilder(indexListP);
		Collections.shuffle(indexListP);

		System.out.println("Vivek Patel's AsyncJokeClient, 1.0.\n"); // prints to the console of the Client startup and server connection
		System.out.println("Using server: " + serverName + ", Port: 4545");
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in)); // creates inputstream for the Client
		try {
			//initializes the variables necessary for the Client. 
			String name; // name provided to the Server by the user
			String input; // the input stream from the user

			System.out.print("What is your name?: "); // Asks the user for their name
			System.out.flush(); // writes the data from the input stream from the user
			name = in.readLine(); // saves the user's name
			
			UDPWorker udpW = new UDPWorker(udpPort); // creates a new UDP worker at the referenced port number
			udpW.start(); // starts the UDP worker
			do {
				// the user is prompted to press enter to recieve a joke or proverb. They can input "quit" to close the client.
				// most random inputs are ignored. The only issue is if there "quit" anywhere in the input
				System.out.print("Press <enter> for a joke or proverb or input (quit) to end: ");
				System.out.flush(); 
				input = in.readLine(); //writes the data from the input stream from the user
				if (input.indexOf("quit") < 0) { // if "quit" isn't inputted, collect the necessary variables and call getJP 
												//to prepare the data to be sent to the server	
					getJP(name, indexListJ, indexListP, serverName, udpPort); // sends the request for a joke or proverb to the Server along with randomized index numbers
					while (ping == false){ // while the UDP worker has not recieved a message, ask for 2 numbers to add together
						System.out.print("While we wait, please enter 2 numbers separated by a space.: ");
						input = in.readLine();
						getSum(input); // takes the user input and sums the numbers together
					}
					System.out.println(message); // print the message when it arrives. The UDP worker will flag ping as true.
					indexUpdate(jDex, pDex, indexListJ, indexListP); // update the index of the used joke/proverb
				} ping = false; // reset the ping flag
			} while (input.indexOf("quit") < 0); //if "quit" is anywhere within the input, close the client, otherwise keep looping the above code.
			System.out.println("Cancelled by user request.");	// This message is printed when the user "quit"s the client
			System.exit(1);	// kill the server		
		} catch (IOException ioe) {ioe.printStackTrace();} // catches IO exceptions and prints an error message to the client
	}

	// Creates the request and sends it to the AsyncJokeServer
	private static void getJP(String name, List<Integer> indexListJ, List<Integer> indexListP, String serverName, int udpPort) {
		Socket sock;
		PrintStream toServer;
		
		try {
			sock = new Socket(serverName, 4545); // connects to the server at port 4545
			toServer = new PrintStream(sock.getOutputStream()); //stream sends output to the server
			
			// convert the index numbers and udp port number to strings
			String indexJSend = indexListJ.get(0).toString();
			String indexPSend = indexListP.get(0).toString();
			String udpPortSend = Integer.toString(udpPort);
			
			// sends the request to the AsyncJokeServer. The server will recieve the client name, index numbers for the joke and proverb lists, and the UDP port number
			toServer.println(name + ", " + indexJSend + ", " + indexPSend + ", " + udpPortSend); 
			toServer.flush();
			sock.close(); // closes the connection
		} catch (IOException x) { // catches IO exceptions and prints an error message
			System.out.println("Socket error.");
			x.printStackTrace();			
		}
	}
	
	public static void indexUpdate(int j, int p, List<Integer> indexListJ, List<Integer> indexListP) { // updates the index that was used in the AsyncJokeServer
		
		// determines if a joke or proverb was sent by comparing the two index numbers sent back.
		// the used number will be between 0-3, so a dummy number of 5 is used to represent the unused index.
		// takes the smallest value to determine the correct index list to update, then deletes the number from the index.
		// since the client sends only the first number of a randomized list, the first number of the used index is deleted
		// if the list is empty, send a message to the console and repopulate and randomize the list
		if (jDex < pDex) {
			indexListJ.remove(0);
			if (indexListJ.size() == 0){
				System.out.println("JOKE CYCLE COMPLETE");
				listBuilder(indexListJ);
				Collections.shuffle(indexListJ);
			}
		}
		else {
			indexListP.remove(0);
			if (indexListP.size() == 0){
				System.out.println("PROVERB CYCLE COMPLETE");
				listBuilder(indexListP);
				Collections.shuffle(indexListP);
			}
		}	
	}

	private static void listBuilder (List<Integer> list){  // tool to repopulate a list
		list.add(0);
		list.add(1);
		list.add(2);
		list.add(3);
	}
	
	private static void getSum (String ns){ // sums the two numbers from a user input
		String[] toSum = ns.split(" "); // converts the input string to an array
		String a = toSum[0]; // pulls the two numbers from the array
		String b = toSum[1];
		int x = Integer.parseInt(a); // converts the strings to int
		int y = Integer.parseInt(b);
		int sum = x + y; // sums the two ints

		System.out.println("The sum of " + a + " + " + b + " is: " + sum + ("!")); // print the completes sum statement
	}
}