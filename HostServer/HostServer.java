/* 2012-05-20 Version 2.0

Thanks John Reagan for this well-running code which repairs the original
obsolete code for Elliott's HostServer program. I've made a few additional
changes to John's code, so blame Elliott if something is not running.

-----------------------------------------------------------------------

Play with this code. Add your own comments to it before you turn it in.

-----------------------------------------------------------------------
NOTE: This is NOT a suggested implementation for your agent platform,
but rather a running example of something that might serve some of
your needs, or provide a way to start thinking about what YOU would like to do.
You may freely use this code as long as you improve it and write your own comments.

-----------------------------------------------------------------------

TO EXECUTE: 

1. Start the HostServer in some shell. >> java HostServer

1. start a web browser and point it to http://localhost:1565. Enter some text and press
the submit button to simulate a state-maintained conversation.

2. start a second web browser, also pointed to http://localhost:1565 and do the same. Note
that the two agents do not interfere with one another.

3. To suggest to an agent that it migrate, enter the string "migrate"
in the text box and submit. The agent will migrate to a new port, but keep its old state.

During migration, stop at each step and view the source of the web page to see how the
server informs the client where it will be going in this stateless environment.

-----------------------------------------------------------------------------------

COMMENTS:

This is a simple framework for hosting agents that can migrate from
one server and port, to another server and port. For the example, the
server is always localhost, but the code would work the same on
different, and multiple, hosts.

State is implemented simply as an integer that is incremented. This represents the state
of some arbitrary conversation.

The example uses a standard, default, HostListener port of 1565.

-----------------------------------------------------------------------------------

DESIGN OVERVIEW

Here is the high-level design, more or less:

HOST SERVER
  Runs on some machine
  Port counter is just a global integer incrememented after each assignment
  Loop:
    Accept connection with a request for hosting
    Spawn an Agent Looper/Listener with the new, unique, port

AGENT LOOPER/LISTENER
  Make an initial state, or accept an existing state if this is a migration
  Get an available port from this host server
  Set the port number back to the client which now knows IP address and port of its
         new home.
  Loop:
    Accept connections from web client(s)
    Spawn an agent worker, and pass it the state and the parent socket blocked in this loop
  
AGENT WORKER
  If normal interaction, just update the state, and pretend to play the animal game
  (Migration should be decided autonomously by the agent, but we instigate it here with client)
  If Migration:
    Select a new host
    Send server a request for hosting, along with its state
    Get back a new port where it is now already living in its next incarnation
    Send HTML FORM to web client pointing to the new host/port.
    Wake up and kill the Parent AgentLooper/Listener by closing the socket
    Die

WEB CLIENT
  Just a standard web browser pointing to http://localhost:1565 to start.

  -------------------------------------------------------------------------------*/
// Below is my original comments. -Vivek Patel

// import statements for the necessary functions and objects
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

class AgentWorker extends Thread {
	
	// initalizes the socket connection, the port number, and the agentHolder (which keeps track of the state) variables
	Socket sock; 
	agentHolder parentAgentHolder; 
	int localPort; 
	
	AgentWorker (Socket s, int prt, agentHolder ah) { // Constructs a worker with the given socket connection, port number, and agentstate
		sock = s;
		localPort = prt;
		parentAgentHolder = ah;
	}
	public void run() { // runs the agent worker and all its functions
		
		PrintStream out = null;
		BufferedReader in = null;
		String NewHost = "localhost"; // the hostname, in this case the localhost
		int NewHostMainPort = 1565;	// the main port the worker is listening at	
		String buf = "";
		int newPort;
		Socket clientSock;
		BufferedReader fromHostServer;
		PrintStream toHostServer;
		
		try {
			out = new PrintStream(sock.getOutputStream()); // the output stream
			in = new BufferedReader(new InputStreamReader(sock.getInputStream())); //the input stream

			String inLine = in.readLine(); // takes input from the client, input is the request
			StringBuilder htmlString = new StringBuilder(); // creates a StringBuilder object to be used for html. StringBuilder is mutable, unlike String.

			System.out.println();
			System.out.println("Request line: " + inLine); // prints the input to the console
			
			if(inLine.indexOf("migrate") > -1) { // If the request has the word migrate, it triggers this migrate command
				
				clientSock = new Socket(NewHost, NewHostMainPort); // creates a new socket for the client
				fromHostServer = new BufferedReader(new InputStreamReader(clientSock.getInputStream())); // creates an input stream for the new socket
				toHostServer = new PrintStream(clientSock.getOutputStream()); // creates an output stream to the new socket
				toHostServer.println("Please host me. Send my port! [State=" + parentAgentHolder.agentState + "]"); // sends a host request to the new socket and the agent state
				toHostServer.flush();
				
				for(;;) { // will continuously read the input and loop until a port is found and breaks the loop
					buf = fromHostServer.readLine(); // reads the input from the host server
					if(buf.indexOf("[Port=") > -1) { // if the input contains a port, break the loop
						break;
					}
				}
				
				String tempbuf = buf.substring( buf.indexOf("[Port=")+6, buf.indexOf("]", buf.indexOf("[Port=")) ); // pulls the port from the input and creates a string of it
				newPort = Integer.parseInt(tempbuf); // turns the port string into an integer
				System.out.println("newPort is: " + newPort);// prints to the console the new port number
				
				htmlString.append(AgentListener.sendHTMLheader(newPort, NewHost, inLine)); // creates the html header with the new port
				htmlString.append("<h3>We are migrating to host " + newPort + "</h3> \n"); // tells the user of the new port
				htmlString.append("<h3>View the source of this page to see how the client is informed of the new location.</h3> \n"); // adds a message to the user
				htmlString.append(AgentListener.sendHTMLsubmit()); // completes the html message to be sent to the user

				System.out.println("Killing parent listening loop."); // prints to the console that the parent port is being closed
				ServerSocket ss = parentAgentHolder.sock; // creates a variable for the parent port from the agent holder
				ss.close(); // closes the parent socket
				
				
			} else if(inLine.indexOf("person") > -1) { // if the request has the string "person" in it, then increment the state. 
														// the message will usually contain "person" as to indicate input from the user for messages other than migrate
				parentAgentHolder.agentState++;
				htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine)); // creates the html header
				htmlString.append("<h3>We are having a conversation with state   " + parentAgentHolder.agentState + "</h3>\n"); // tells the user of the updated agent state
				htmlString.append(AgentListener.sendHTMLsubmit()); // completes the html message to be sent to the user

			} else { //// if the request does not match anything, tell the user that the request is invalid. This may be due to a fav.ico request
				htmlString.append(AgentListener.sendHTMLheader(localPort, NewHost, inLine)); // creates the html header
				htmlString.append("You have not entered a valid request!\n"); // tells the user of their invalid request
				htmlString.append(AgentListener.sendHTMLsubmit()); // completes the html message to be sent to the user
				
		
			}
			AgentListener.sendHTMLtoStream(htmlString.toString(), out); // converts the html StringBuilder to a string, then converts that string to html, then sends the html message to the user
			sock.close(); // closes the socket
			
			
		} catch (IOException ioe) { // catches IO exceptions and prints it to the console
			System.out.println(ioe);
		}
	}
	
}
class agentHolder { // the agentHolder object holds the socket and state information that can be passed to other ports
	ServerSocket sock;
	int agentState;
	agentHolder(ServerSocket s) { sock = s;} // constructor for the agentHolder
}

class AgentListener extends Thread { // listens at a port for requests

	// initializes the socket and port number
	Socket sock;
	int localPort;

	AgentListener(Socket As, int prt) { // constructor for AgentListener
		sock = As;
		localPort = prt;
	}

	int agentState = 0; // initializes the agentState at a default value of 0
	
	public void run() {
		BufferedReader in = null;
		PrintStream out = null;
		String NewHost = "localhost"; // the hostname, in this case the localhost
		System.out.println("In AgentListener Thread"); // prints to the console that the AgentListener is active
		try {
			String buf;
			out = new PrintStream(sock.getOutputStream()); // creates an outputstream for the listener
			in =  new BufferedReader(new InputStreamReader(sock.getInputStream())); // creates an inputstream for the listener

			buf = in.readLine(); // reads the input fromt the inputstream
			
			if(buf != null && buf.indexOf("[State=") > -1) { // if the input contains a state, then store it
				String tempbuf = buf.substring(buf.indexOf("[State=")+7, buf.indexOf("]", buf.indexOf("[State="))); // pulls the state from the input and creates a string of it
				agentState = Integer.parseInt(tempbuf); // turns the state string into an int
				System.out.println("agentState is: " + agentState); // prints to the console the extracted agentState
					
			}
			
			System.out.println(buf); // prints the input string
			StringBuilder htmlResponse = new StringBuilder(); // creates a StringBuilder object to create an html message
			htmlResponse.append(sendHTMLheader(localPort, NewHost, buf)); // creates the html header
			htmlResponse.append("Now in Agent Looper starting Agent Listening Loop\n<br />\n"); // adds a message to the html StringBuilder 
			htmlResponse.append("[Port="+localPort+"]<br/>\n"); // adds the port number to the html message
			htmlResponse.append(sendHTMLsubmit()); // completes the html message to be sent to the user
			sendHTMLtoStream(htmlResponse.toString(), out); // sends the completed html message to the output stream
			
			ServerSocket servsock = new ServerSocket(localPort,2); // opens a connection at the port 
			agentHolder agenthold = new agentHolder(servsock); // creates a new agentHolder to hold the socket and state
			agenthold.agentState = agentState; // updates the agentState of the new agentHolder
			
			while(true) { // will continously wait for new connections
				sock = servsock.accept(); // creates a new connection 
				System.out.println("Got a connection to agent at port " + localPort); // prints to console of the new connection and the port
				new AgentWorker(sock, localPort, agenthold).start(); // creates a new AgentWorker for the connection
			}
		
		} catch(IOException ioe) { // catches IO exceptions and prints it to the console. In this case, it can be due to the intentional closing of a port
			System.out.println("Either connection failed, or just killed listener loop for agent at port " + localPort);
			System.out.println(ioe);
		}
	}

	static String sendHTMLheader(int localPort, String NewHost, String inLine) { // this is used to send the html header
		
		StringBuilder htmlString = new StringBuilder(); // creates a StringBuilder object to store the html message

		htmlString.append("<html><head> </head><body>\n"); // add html formatting
		htmlString.append("<h2>This is for submission to PORT " + localPort + " on " + NewHost + "</h2>\n"); // add port and host information
		htmlString.append("<h3>You sent: "+ inLine + "</h3>"); // adds input string
		htmlString.append("\n<form method=\"GET\" action=\"http://" + NewHost +":" + localPort + "\">\n");
		htmlString.append("Enter text or <i>migrate</i>:"); // adds message asking the user for input. Also mentions the "migrate" command
		htmlString.append("\n<input type=\"text\" name=\"person\" size=\"20\" value=\"YourTextInput\" /> <p>\n"); // adds input box
		
		return htmlString.toString(); // converts the StringBuilder to a String and returns it
	}

	static String sendHTMLsubmit() { // completes the html message
		return "<input type=\"submit\" value=\"Submit\"" + "</p>\n</form></body></html>\n";
	}

	static void sendHTMLtoStream(String html, PrintStream out) { // sends the html message to the output
		
		out.println("HTTP/1.1 200 OK");
		out.println("Content-Length: " + html.length()); // sends the length of the html string
		out.println("Content-Type: text/html");
		out.println("");		
		out.println(html); // sends the html string
	}
	
}

public class HostServer { // runs the HostServer, contains the main method

	public static int NextPort = 3000; // the base port for our server. The first port to be used is 3001
	
	public static void main(String[] a) throws IOException {
		int q_len = 6;
		int port = 1565; // port the server is listening at
		Socket sock;
		
		ServerSocket servsock = new ServerSocket(port, q_len);
		System.out.println("John Reagan's DIA Master receiver started at port 1565."); // prints to the console the server is running and at what port
		System.out.println("Connect from 1 to 3 browsers using \"http:\\\\localhost:1565\"\n"); // prints to the console how to connect to the server

		while(true) { // contunously loops, listening for connections
			
			NextPort = NextPort + 1; // will increment the port each time through the loop.
			sock = servsock.accept();
			System.out.println("Starting AgentListener at port " + NextPort); // prints to the console the port the AgentListener is listening at
			new AgentListener(sock, NextPort).start(); // creates the agent listener at the port
		}
		
	}
}