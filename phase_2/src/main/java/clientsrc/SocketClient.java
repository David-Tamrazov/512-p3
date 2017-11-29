package clientsrc;

//import ResImpl.SocketResponse;
import java.util.*;
import java.net.*;
import java.io.*;


public class SocketClient {
    static String message = "blank";

    public static void main(String args[]) {

        int port = 1090;
        String hostname = "";

        if (args.length == 1) {
            hostname = args[0];
        } else {
            System.out.println("Incorrect usage of socket client. Call SocketClient [hostname]");
            System.exit(-1);
        }
        

        try {
            Socket socket = new Socket(hostname, port);
            System.out.println("Connected to the socket server.");

			PrintWriter sendToServer = new PrintWriter(socket.getOutputStream(), true);
            ObjectInputStream streamFromServer = new ObjectInputStream(socket.getInputStream());
            
            BufferedReader userInput = new java.io.BufferedReader(new InputStreamReader(System.in));

            while (true) {
            	System.out.print("> ");
            	
                String readerInput = userInput.readLine();

                sendToServer.println(readerInput);
                Vector response = (Vector) streamFromServer.readObject();
				
				
				if((int)response.elementAt(0) == 201) {
			    	System.out.println("Thanks for playing, bye!");
			    	System.exit(1);
		    	}
		    	else {
			    	System.out.println((String) response.elementAt(1));
		    	}
			}

        } catch(IOException e) {
            System.out.println("Error connecting to server: " + e);
            System.out.println("Aborting client program.");
            System.exit(-1);
        } catch (ClassNotFoundException e) {
       	    System.out.println("Could not cast server response into SocketResponse.");
            System.out.println("Aborting client program.");
	    System.exit(-1);  
        }
    }
}
