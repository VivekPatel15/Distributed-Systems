import java.io.*;
import java.net.*;

public class InetClient {
	public static void main(String[] args) { // runs the Client
		String serverName;
		if (args.length < 1) serverName = "localhost";
		else serverName = args[0];

		System.out.println("Clark Elliot's InetClient, 1.8.\n");
		System.out.println("Using server: " + serverName + ", Port: 1565");
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in)); // creates inputstream for the user
		try {
			String name;
			do {
				System.out.print("Enter a hostname or an IP address, (quit) to end: ");
				System.out.flush(); //writes the data from the input stream from the user
				name = in.readLine();
				if (name.indexOf("quit") < 0) // if "quit" isn't inputted, then send a getRemoteAddress signal to the server
					getRemoteAddress (name, serverName);
			} while (name.indexOf("quit") < 0); //if "quit" is anywhere within the input, close the client. 
												//This can be an issue with certain websites, such as the example given by Prof Elliot "quitsmoking.com"
			System.out.println("Cancelled by user request.");
		} catch (IOException x) {x.printStackTrace();}
	}

	static String toText (byte ip[]) { // Turns an IP address into a String
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < ip.length; i++) {
			if (i > 0) result.append(".");
			result.append (0xff & ip[i]);
		} return result.toString();
	}

	static void getRemoteAddress(String name, String serverName) { //contact's the server with the client's request (sends the name the user wrote in the client)
		Socket sock;
		BufferedReader fromServer;
		PrintStream toServer;
		String textFromServer;

		try {
			sock = new Socket(serverName, 1565); // connects to the server

			fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream())); //stream recieves input from the server
			toServer = new PrintStream(sock.getOutputStream()); //stream sends output to the server

			toServer.println(name); toServer.flush(); // sends the data and makes sure of it

			for (int i = 1; i <= 3; i++) { // waits for and recieves the input from the server and prints the data from the server 
				textFromServer = fromServer.readLine();
				if (textFromServer != null) System.out.println(textFromServer);
			}
			sock.close(); // closes the connection to the server
		} catch (IOException x) { // catches an IO exception and prints it
			System.out.println("Socket error.");
			x.printStackTrace();
		}
	}
}