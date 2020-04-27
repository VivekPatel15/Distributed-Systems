/*--------------------------------------------------------

1. Vivek Patel / April 23, 2019:

2. Java build 1.8.0_201

3. Precise command-line compilation examples / instructions:

> javac JokeServer.java
> javac JokeClient.java
> javac JokeClientAdmin.java

or
> javac *.java
works too

4. Precise examples / instructions to run this program:

In separate shell windows:

> java JokeServer
> java JokeClient
> java JokeClientAdmin

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

Joke Client Admin can shut down the server, but if you want to start it again, you
can't without re-running the program. Also, the Admin controls are posted in the console, but 
it does not switch states with the <enter> button alone.

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

public class JokeClient {
	public static void main(String[] args) { // runs the Client and connects it to the Server
		String serverName;
		if (args.length < 1) serverName = "localhost"; //default server
		else serverName = args[0]; // directed server input from the terminal
		
		// creates the list of integers that will be used as a master copy for the 
		// index lists that represent the server state.
		// 0-3 was used because that is all the integers needed for the index
		List<Integer> list = new ArrayList<>();
		list.add(0);
		list.add(1);
		list.add(2);
		list.add(3);

		System.out.println("Vivek Patel's JokeClient, 1.0.\n");
		System.out.println("Using server: " + serverName + ", Port: 4545");
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in)); // creates inputstream for the Client
		try {
			//initializes the variables necessary for the Client. 
			String name; // name provided to the Server by the user
			String input; // the input stream from the user

			// the creaton of the index lists for the Jokes and Proverbs.
			// these index lists save the state of the conversation to the Server
			// but are not stored in any permanent file. Both index lists are 
			// shuffled as to create the random order for the jokes and proverb output
			List<Integer> indexListJ = new ArrayList<>(list);
			Collections.shuffle(indexListJ);
			List<Integer> indexListP = new ArrayList<>(list);
			Collections.shuffle(indexListP);

			System.out.print("What is your name?: "); // Asks the user for their name
			System.out.flush(); // writes the data from the input stream from the user
			name = in.readLine(); // saves the user's name
			do {
				// the user is prompted to press enter to recieve a joke or proverb. They can input "quit" to close the client.
				// most random inputs are ignored. The only issue is if there "quit" anywhere in the input
				System.out.print("Press <enter> for a joke or proverb or input (quit) to end: ");
				System.out.flush(); //writes the data from the input stream from the user
				input = in.readLine();
				if (input.indexOf("quit") < 0) // if "quit" isn't inputted, collect the necessary variables and call getJP 
												//to prepare the data to be sent to the server					
					getJP(name, indexListJ, indexListP, list, serverName);
			} while (input.indexOf("quit") < 0); //if "quit" is anywhere within the input, close the client, otherwise keep looping the above code.
			System.out.println("Cancelled by user request.");	// This message is printed when the user "quit"s the client			
		} catch (IOException x) {x.printStackTrace();}
	}

	// the method that takes the user's input and internal state variables and sends it to the server.
	//inputs are the name the user inputted, the index lists for both jokes and proverbs, the master list, and the output. 
	private static void getJP(String name, List<Integer> iLJ, List<Integer> iLP, List<Integer> list, String serverName) {
		Socket sock;
		BufferedReader fromServer;
		PrintStream toServer;
		String dataFromServer;
		
		String message; // this will house the joke or proverb sent from the server
		int jDex; // jDex and pDex are the index values sent from the server back to the client.
		int pDex; // They represent joke and proverb, respectively

		try {
			sock = new Socket(serverName, 4545); // connects to the server at port 4545

			fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream())); //stream recieves input from the server
			toServer = new PrintStream(sock.getOutputStream()); //stream sends output to the server
			
			//These create string versions of the index number, the first of each shuffled index list.
			// this way, both jokes and proverbs will have their own randomized order
			String indexJSend = iLJ.get(0).toString();
			String indexPSend = iLP.get(0).toString();

			// sends the data and makes sure of it. Packages the user's name and the 2 indexes for the joke or proverb
			toServer.println(name + ", " + indexJSend + ", " + indexPSend); toServer.flush();

			// This is the package from the server. Included is that message, the used index number that represents what
			// type of message was recieved, and a dummy index number. Here you can't tell what is the real index number is
			// or which list it's refering to.
			// The message is printed to the Client's console.
			dataFromServer = fromServer.readLine();
			String[] text = dataFromServer.split(", ");
			message = text[0];
			System.out.println(message);
			String jTemp = text[1];
			jDex = Integer.parseInt(jTemp);
			String pTemp = text[2];
			pDex = Integer.parseInt(pTemp);
			
			// these if statements will compare the 2 returned indexes. One of these will be a fake index of 5.
			// the smaller index will reveal which type of message was printed (joke or proverb), and will then remove 
			// the index from the parent index list. 

			// if after the removal of the index from the index list, the list is empty, the Client will print a message to
			// the console saying that all of that type of message has been seen. The index list is repopulated from the master
			// list and then reshuffled to be reused. This way we can not only have our randomized order of jokes/proverbs, but there will be no repeats either.
			if (jDex < pDex) {
				iLJ.remove(0);
				if (iLJ.size() == 0){
					System.out.println("JOKE CYCLE COMPLETE");
					iLJ.addAll(list);
					Collections.shuffle(iLJ);
				}
			}
			else {
				iLP.remove(0);
				if (iLP.size() == 0){
					System.out.println("PROVERB CYCLE COMPLETE");
					iLP.addAll(list);
					Collections.shuffle(iLP);
				}
			}
			sock.close(); // closes the connection to the server
		} catch (IOException x) { // catches an IO exception and prints it
			System.out.println("Socket error.");
			x.printStackTrace();
		}
	}
}