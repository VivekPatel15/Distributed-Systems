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

public class AsyncJokeAdminClient {
	public static void main(String[] args) { // runs the Admin Client
		String serverName;
		if (args.length < 1) serverName = "localhost"; //default server
		else serverName = args[0]; // directed server input from the terminal

		System.out.println("Vivek Patel's AsyncJokeAdminClient, 1.0.\n");
		System.out.println("Using server: " + serverName + ", Port: 4555");
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in)); // creates inputstream for the Admin
		try {
			String mode; // creates the mode variable for the Admin. This keeps the input from the Admin.
			do {

				// The Admin Client's console will print the list of commands for the Admin. It can change the mode based on the input, 
				// shut down the server, or exit the Admin Client
				System.out.print("Input (j) for joke or (p) for proverb, (quit) to quit the Admin Client, or (s) to shut down the server: ");
				System.out.flush(); //writes the data from the input stream from the user
				mode = in.readLine(); // saves the Admin's input
				if (mode.indexOf("quit") < 0) // if "quit" isn't inputted, send the Admin's command to the server
					adminControl(mode, serverName);
			} while (mode.indexOf("quit") < 0); //if "quit" is anywhere within the input, close the client.
			System.out.println("Cancelled by user request."); // printed when the Admin closes the client.
		} catch (IOException x) {x.printStackTrace();}
	}

	static void adminControl(String mode, String serverName) { //contact's the server with the Admin's command
		Socket sock;
		BufferedReader fromServer;
		PrintStream toServer;
		String textFromServer;

		try {
			sock = new Socket(serverName, 4555); // connects to the server, uses a different port than regular Clients

			fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream())); //stream recieves input from the server
			toServer = new PrintStream(sock.getOutputStream()); //stream sends output to the server

			toServer.println(mode); toServer.flush(); // sends the data and makes sure of it

			sock.close(); // closes the connection to the server
		} catch (IOException x) { // catches an IO exception and prints it
			System.out.println("Socket error.");
			x.printStackTrace();
		}
	}
}