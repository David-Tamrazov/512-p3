package clientsrc;

import servercode.ResInterface.*;
import servercode.LockManager.DeadlockException;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;

import java.util.*;

import java.io.*;

    
public class Client {
    static String message = "blank";
    static MiddlewareServer mws;
    static String mwsHost;

    public static void main(String args[]) {
        Client obj = new Client();
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String command = "";
        Vector arguments  = new Vector();

        String server = "localhost";
        int port = 1738;

        if (args.length > 0) {
            server = args[0];
            mwsHost = server;
        }

        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        }

        if (args.length > 2) {
            System.out.println ("Usage: java client [mwshost [mwsport]]");
            System.exit(1);
        }
        
        try {
            // get a reference to the mwsregistry
            Registry registry = LocateRegistry.getRegistry(server, port);
            // get the proxy and the remote reference by mwsregistry lookup
            mws = (MiddlewareServer) registry.lookup("Gr17MiddlewareServer");


            if(mws !=null) {
                System.out.println("Successful");
                System.out.println("Connected to mws");
            } else {
                System.out.println("Unsuccessful");
            }
        
        } catch (Exception e) {    
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
        
        
        
        if (System.getSecurityManager() == null) {
            //System.setSecurityManager(new mwsSecurityManager());
        }

        
        System.out.println("\n\n\tClient Interface");
        System.out.println("Type \"help\" for list of supported commands");
        
        while (true) {

            System.out.print("\n>");

            try {
                //read the next command
                command = stdin.readLine();
            } catch (IOException io) {
                System.out.println("Unable to read from standard in");
                System.exit(1);
            }

            //remove heading and trailing white space
            command = command.trim();
            arguments = obj.parse(command);
            
            boolean completed = false;
            
            while (!completed) {
	            
	            try {
	            	
	            	obj.performCommand(arguments);
	            	completed = true;
		            
	            } catch (DeadlockException e) {
	            	System.out.println("Deadlock.");
	            	completed = true;
	            } catch (Exception e) {
		            
		            // standard error 
		            if (obj.connectedToMWS()) {
		            
			        	System.out.println("EXCEPTION:");
						System.out.println(e.getMessage());
						e.printStackTrace();
						completed = true;
		            } 
		            
		            // need to reconnect to the MWS
		            else {
			            obj.reconnectToMWS();
		            }
	           
	            }
	            
	            
	            
            }
            
            
           
         
        } //end of while(true)
    }
    
    public boolean connectedToMWS() {
    
    	try {
    	
	    	this.mws.ping("Still alive.");
	    	return true;
	    	
    	} catch (RemoteException e) {
	    	
	    	return false;
	    	
    	}
	    
    }
    
    public void performCommand(Vector arguments) throws Exception {
    
    	int Id, Cid;
        int flightNum;
        int flightPrice;
        int flightSeats;
        boolean Room;
        boolean Car;
        int price;
        int numRooms;
        int numCars;
        int customer;
        String location;
    
    	 
	    //decide which of the commands this was
        switch (findChoice((String) arguments.elementAt(0))) {

        case 1: //help section

            if (arguments.size() == 1) //command was "help"
                listCommands();
            else if (arguments.size() == 2) //command was "help <commandname>"
                listSpecific((String) arguments.elementAt(1));
            else //wrong use of help command
                System.out.println("Improper use of help command. Type help or help, <commandname>");
            break;

        case 2: //new flight

            if (arguments.size() != 5) {
                wrongNumber();
                break;
            }

            System.out.println("Adding a new Flight using id: " + arguments.elementAt(1));
            System.out.println("Flight number: " + arguments.elementAt(2));
            System.out.println("Add Flight Seats: " + arguments.elementAt(3));
            System.out.println("Set Flight Price: " + arguments.elementAt(4));



            Id = getInt(arguments.elementAt(1));
            flightNum = getInt(arguments.elementAt(2));
            flightSeats = getInt(arguments.elementAt(3));
            flightPrice = getInt(arguments.elementAt(4));

            if (mws.addFlight(Id, flightNum, flightSeats, flightPrice))
                System.out.println("Flight added");
            else
                System.out.println("Flight could not be added");

 

            break;

        case 3: //new Car

            if (arguments.size() != 5) {
                wrongNumber();
                break;
            }

            System.out.println("Adding a new Car using id: " + arguments.elementAt(1));
            System.out.println("Car Location: " + arguments.elementAt(2));
            System.out.println("Add Number of Cars: " + arguments.elementAt(3));
            System.out.println("Set Price: " + arguments.elementAt(4));


            Id = getInt(arguments.elementAt(1));
            location = getString(arguments.elementAt(2));
            numCars = getInt(arguments.elementAt(3));
            price = getInt(arguments.elementAt(4));

            if (mws.addCars(Id, location, numCars, price))
                System.out.println("Cars added");
            else
                System.out.println("Cars could not be added");



            break;

        case 4: //new Room

            if (arguments.size() != 5) {
                wrongNumber();
                break;
            }

            System.out.println("Adding a new Room using id: " + arguments.elementAt(1));
            System.out.println("Room Location: " + arguments.elementAt(2));
            System.out.println("Add Number of Rooms: " + arguments.elementAt(3));
            System.out.println("Set Price: " + arguments.elementAt(4));

            Id = getInt(arguments.elementAt(1));
            location = getString(arguments.elementAt(2));
            numRooms = getInt(arguments.elementAt(3));
            price = getInt(arguments.elementAt(4));

            if (mws.addRooms(Id, location, numRooms, price))
                System.out.println("Rooms added");
            else
                System.out.println("Rooms could not be added");


            break;

        case 5: //new Customer

            if (arguments.size() != 2) {
                wrongNumber();
                break;
            }

            System.out.println("Adding a new Customer using id:" + arguments.elementAt(1));

            Id = getInt(arguments.elementAt(1));
            customer = mws.newCustomer(Id);
            
            System.out.println("Added new customer with id: " + customer);

            break;

        case 6: //delete Flight

            if (arguments.size() != 3) {
                wrongNumber();
                break;
            }

            System.out.println("Deleting a flight using id: " + arguments.elementAt(1));
            System.out.println("Flight Number: " + arguments.elementAt(2));

            Id = getInt(arguments.elementAt(1));
            flightNum = getInt(arguments.elementAt(2));

            if (mws.deleteFlight(Id, flightNum))
                System.out.println("Flight Deleted");
            else
                System.out.println("Flight could not be deleted");

            break;

        case 7: //delete Car

            if (arguments.size() != 3) {
                wrongNumber();
                break;
            }

            System.out.println("Deleting the cars from a particular location  using id: " + arguments.elementAt(1));
            System.out.println("Car Location: " + arguments.elementAt(2));

            Id = getInt(arguments.elementAt(1));
            location = getString(arguments.elementAt(2));

            if (mws.deleteCars(Id, location))
                System.out.println("Cars Deleted");
            else
                System.out.println("Cars could not be deleted");

            break;

        case 8: //delete Room

            if (arguments.size() != 3) {
                wrongNumber();
                break;
            }

            System.out.println("Deleting all rooms from a particular location  using id: " + arguments.elementAt(1));
            System.out.println("Room Location: " + arguments.elementAt(2));

            Id = getInt(arguments.elementAt(1));
            location = getString(arguments.elementAt(2));

            if (mws.deleteRooms(Id, location))
                System.out.println("Rooms Deleted");
            else
                System.out.println("Rooms could not be deleted");


            break;

        case 9: //delete Customer

            if (arguments.size() != 3) {
                wrongNumber();
                break;
            }

            System.out.println("Deleting a customer from the database using id: " + arguments.elementAt(1));
            System.out.println("Customer id: " + arguments.elementAt(2));

            Id = getInt(arguments.elementAt(1));
            customer = getInt(arguments.elementAt(2));

            if (mws.deleteCustomer(Id, customer))
                System.out.println("Customer Deleted");
            else
                System.out.println("Customer could not be deleted");

            break;

        case 10: //querying a flight

            if (arguments.size() != 3) {
                wrongNumber();
                break;
            }

            System.out.println("Querying a flight using id: " + arguments.elementAt(1));
            System.out.println("Flight number: " + arguments.elementAt(2));

            Id = getInt(arguments.elementAt(1));
            flightNum = getInt(arguments.elementAt(2));
            int seats = mws.queryFlight(Id, flightNum);

            System.out.println("Number of seats available:" + seats);

            break;

        case 11: //querying a Car Location

            if (arguments.size() != 3) {
                wrongNumber();
                break;
            }

            System.out.println("Querying a car location using id: " + arguments.elementAt(1));
            System.out.println("Car location: " + arguments.elementAt(2));

            Id = getInt(arguments.elementAt(1));
            location = getString(arguments.elementAt(2));
            numCars = mws.queryCars(Id, location);

            System.out.println("number of Cars at this location:" + numCars);

            break;

        case 12: //querying a Room location

            if (arguments.size() != 3) {
                wrongNumber();
                break;
            }

            System.out.println("Querying a room location using id: " + arguments.elementAt(1));
            System.out.println("Room location: " + arguments.elementAt(2));

            Id = getInt(arguments.elementAt(1));
            location = getString(arguments.elementAt(2));
            numRooms = mws.queryRooms(Id, location);

            System.out.println("number of Rooms at this location:" + numRooms);

            break;

        case 13: //querying Customer Information

            if (arguments.size() != 3) {
                wrongNumber();
                break;
            }

            System.out.println("Querying Customer information using id: " + arguments.elementAt(1));
            System.out.println("Customer id: " + arguments.elementAt(2));

            Id = getInt(arguments.elementAt(1));
            customer = getInt(arguments.elementAt(2));
            String bill = mws.queryCustomerInfo(Id, customer);

            System.out.println("Customer info:" + bill);

            break;

        case 14: //querying a flight Price

            if (arguments.size() != 3) {
                wrongNumber();
                break;
            }

            System.out.println("Querying a flight Price using id: " + arguments.elementAt(1));
            System.out.println("Flight number: " + arguments.elementAt(2));

            Id = getInt(arguments.elementAt(1));
            flightNum = getInt(arguments.elementAt(2));
            price = mws.queryFlightPrice(Id, flightNum);

            System.out.println("Price of a seat:" + price);

            break;

        case 15: //querying a Car Price

            if (arguments.size() != 3) {
                wrongNumber();
                break;
            }

            System.out.println("Querying a car price using id: " + arguments.elementAt(1));
            System.out.println("Car location: " + arguments.elementAt(2));

            Id = getInt(arguments.elementAt(1));
            location = getString(arguments.elementAt(2));
            price = mws.queryCarsPrice(Id, location);

            System.out.println("Price of a car at this location:" + price);

            break;

        case 16: //querying a Room price

            if (arguments.size() != 3) {
                wrongNumber();
                break;
            }

            System.out.println("Querying a room price using id: " + arguments.elementAt(1));
            System.out.println("Room Location: " + arguments.elementAt(2));

            Id = getInt(arguments.elementAt(1));
            location = getString(arguments.elementAt(2));
            price = mws.queryRoomsPrice(Id, location);

            System.out.println("Price of Rooms at this location:" + price);

            break;

        case 17: //reserve a flight

            if (arguments.size() != 4) {
                wrongNumber();
                break;
            }

            System.out.println("Reserving a seat on a flight using id: " + arguments.elementAt(1));
            System.out.println("Customer id: " + arguments.elementAt(2));
            System.out.println("Flight number: " + arguments.elementAt(3));

            Id = getInt(arguments.elementAt(1));
            customer = getInt(arguments.elementAt(2));
            flightNum = getInt(arguments.elementAt(3));

            if (mws.reserveFlight(Id, customer, flightNum))
                System.out.println("Flight Reserved");
            else
                System.out.println("Flight could not be reserved.");

            break;

        case 18: //reserve a car

            if (arguments.size() != 4) {
                wrongNumber();
                break;
            }

            System.out.println("Reserving a car at a location using id: " + arguments.elementAt(1));
            System.out.println("Customer id: " + arguments.elementAt(2));
            System.out.println("Location: " + arguments.elementAt(3));

            Id = getInt(arguments.elementAt(1));
            customer = getInt(arguments.elementAt(2));
            location = getString(arguments.elementAt(3));

            if (mws.reserveCar(Id, customer, location))
                System.out.println("Car Reserved");
            else
                System.out.println("Car could not be reserved.");

            break;

        case 19: //reserve a room

            if (arguments.size() != 4) {
                wrongNumber();
                break;
            }

            System.out.println("Reserving a room at a location using id: " + arguments.elementAt(1));
            System.out.println("Customer id: " + arguments.elementAt(2));
            System.out.println("Location: " + arguments.elementAt(3));

            Id = getInt(arguments.elementAt(1));
            customer = getInt(arguments.elementAt(2));
            location = getString(arguments.elementAt(3));

            if (mws.reserveRoom(Id, customer, location))
                System.out.println("Room Reserved");
            else
                System.out.println("Room could not be reserved.");

            break;

        case 20: //reserve an Itinerary

            if (arguments.size() < 7) {
                wrongNumber();
                break;
            }

            System.out.println("Reserving an Itinerary using id:" + arguments.elementAt(1));
            System.out.println("Customer id:" + arguments.elementAt(2));

            for (int i = 0; i < arguments.size() - 6; i++) {

                System.out.println("Flight number" + arguments.elementAt(3 + i));
                System.out.println("Location for Car/Room booking:" + arguments.elementAt(arguments.size() - 3));
                System.out.println("Car to book?:" + arguments.elementAt(arguments.size() - 2));
                System.out.println("Room to book?:" + arguments.elementAt(arguments.size() - 1));

            }

            Id = getInt(arguments.elementAt(1));
            customer = getInt(arguments.elementAt(2));
            Vector flightNumbers = new Vector();

            for (int i = 0; i < arguments.size() - 6; i++) {
            	flightNumbers.addElement(arguments.elementAt(3 + i));
            }

            location = getString(arguments.elementAt(arguments.size() - 3));
            Car = getBoolean(arguments.elementAt(arguments.size() - 2));
            Room = getBoolean(arguments.elementAt(arguments.size() - 1));

            if (mws.itinerary(Id, customer, flightNumbers, location, Car, Room))
                System.out.println("Itinerary Reserved");
            else
                System.out.println("Itinerary could not be reserved.");

            break;

        case 21: //quit the client

            if (arguments.size() != 1) {
                wrongNumber();
                break;
            }

            System.out.println("Quitting client.");
            System.exit(1);

        case 22: //new Customer given id

            if (arguments.size() != 3) {
                wrongNumber();
                break;
            }

            System.out.println("Adding a new Customer using id:" + arguments.elementAt(1) + " and cid "
                    + arguments.elementAt(2));

            Id = getInt(arguments.elementAt(1));
            Cid = getInt(arguments.elementAt(2));
            boolean customerMade = mws.newCustomer(Id, Cid);
            
            if(customerMade) {
           		System.out.println("Added new customer with id: " + Cid);
            } else {
                System.out.println("Failed to add new customer with id: " + Cid);
            }


            break;
            
        case 23: //start
        
            System.out.println("Started transaction with ID: " + mws.start());
        	
        	break;
        	
        case 24: //commit

    		if(mws.commit(getInt(arguments.elementAt(1)))) {
        		System.out.println("Transaction " + getString(arguments.elementAt(1)) +  " committed");
        	} else {
            	System.out.println("Transaction could not be committed.");
        	}

        	
        	break;
        
        case 25: //abort

        	mws.abort(getInt(arguments.elementAt(1)));
        	System.out.println("Transaction " + getString(arguments.elementAt(1)) + " aborted");
  	
        	break;
        	
		case 26: // shutdown
			
			mws.shutdown();
			System.out.println("Shutting down the client.");
			System.exit(0);

			break;
        default:
            System.out.println("The interface does not support this command.");
            break;
            
        }
    }
        
    public Vector parse(String command) {
        Vector arguments = new Vector();
        StringTokenizer tokenizer = new StringTokenizer(command, ",");
        String argument = "";
        while (tokenizer.hasMoreTokens()) {
            argument = tokenizer.nextToken();
            argument = argument.trim();
            arguments.add(argument);
        }
        return arguments;
    }

    public int findChoice(String argument) {
        if (argument.compareToIgnoreCase("help") == 0)
            return 1;
        else if (argument.compareToIgnoreCase("newflight") == 0)
            return 2;
        else if (argument.compareToIgnoreCase("newcar") == 0)
            return 3;
        else if (argument.compareToIgnoreCase("newroom") == 0)
            return 4;
        else if (argument.compareToIgnoreCase("newcustomer") == 0)
            return 5;
        else if (argument.compareToIgnoreCase("deleteflight") == 0)
            return 6;
        else if (argument.compareToIgnoreCase("deletecar") == 0)
            return 7;
        else if (argument.compareToIgnoreCase("deleteroom") == 0)
            return 8;
        else if (argument.compareToIgnoreCase("deletecustomer") == 0)
            return 9;
        else if (argument.compareToIgnoreCase("queryflight") == 0)
            return 10;
        else if (argument.compareToIgnoreCase("querycar") == 0)
            return 11;
        else if (argument.compareToIgnoreCase("queryroom") == 0)
            return 12;
        else if (argument.compareToIgnoreCase("querycustomer") == 0)
            return 13;
        else if (argument.compareToIgnoreCase("queryflightprice") == 0)
            return 14;
        else if (argument.compareToIgnoreCase("querycarprice") == 0)
            return 15;
        else if (argument.compareToIgnoreCase("queryroomprice") == 0)
            return 16;
        else if (argument.compareToIgnoreCase("reserveflight") == 0)
            return 17;
        else if (argument.compareToIgnoreCase("reservecar") == 0)
            return 18;
        else if (argument.compareToIgnoreCase("reserveroom") == 0)
            return 19;
        else if (argument.compareToIgnoreCase("itinerary") == 0)
            return 20;
        else if (argument.compareToIgnoreCase("quit") == 0)
            return 21;
        else if (argument.compareToIgnoreCase("newcustomerid") == 0)
            return 22;
        else if (argument.compareToIgnoreCase("start") == 0)
        	return 23;
        else if (argument.compareToIgnoreCase("commit") == 0)
        	return 24;
        else if (argument.compareToIgnoreCase("abort") == 0)
        	return 25;
        else if (argument.compareToIgnoreCase("shutdown") == 0)
        	return 26;
        else
            return 666;

    }

    public void listCommands() {
        System.out.println("\nWelcome to the client interface provided to test your project.");
        System.out.println("Commands accepted by the interface are:");
        System.out.println("help");
        System.out
                .println("newflight\nnewcar\nnewroom\nnewcustomer\nnewcusomterid\ndeleteflight\ndeletecar\ndeleteroom");
        System.out.println("deletecustomer\nqueryflight\nquerycar\nqueryroom\nquerycustomer");
        System.out.println("queryflightprice\nquerycarprice\nqueryroomprice");
        System.out.println("reserveflight\nreservecar\nreserveroom\nitinerary");
        System.out.println("nquit");
        System.out.println("\ntype help, <commandname> for detailed info(NOTE the use of comma).");
    }

    public void listSpecific(String command) {
        System.out.print("Help on: ");
        switch (findChoice(command)) {
        case 1:
            System.out.println("Help");
            System.out.println("\nTyping help on the prompt gives a list of all the commands available.");
            System.out.println("Typing help, <commandname> gives details on how to use the particular command.");
            break;

        case 2: //new flight
            System.out.println("Adding a new Flight.");
            System.out.println("Purpose:");
            System.out.println("\tAdd information about a new flight.");
            System.out.println("\nUsage:");
            System.out.println("\tnewflight,<id>,<flightnumber>,<flightSeats>,<flightprice>");
            break;

        case 3: //new Car
            System.out.println("Adding a new Car.");
            System.out.println("Purpose:");
            System.out.println("\tAdd information about a new car location.");
            System.out.println("\nUsage:");
            System.out.println("\tnewcar,<id>,<location>,<numberofcars>,<pricepercar>");
            break;

        case 4: //new Room
            System.out.println("Adding a new Room.");
            System.out.println("Purpose:");
            System.out.println("\tAdd information about a new room location.");
            System.out.println("\nUsage:");
            System.out.println("\tnewroom,<id>,<location>,<numberofrooms>,<priceperroom>");
            break;

        case 5: //new Customer
            System.out.println("Adding a new Customer.");
            System.out.println("Purpose:");
            System.out.println("\tGet the system to provide a new customer id. (same as adding a new customer)");
            System.out.println("\nUsage:");
            System.out.println("\tnewcustomer,<id>");
            break;

        case 6: //delete Flight
            System.out.println("Deleting a flight");
            System.out.println("Purpose:");
            System.out.println("\tDelete a flight's information.");
            System.out.println("\nUsage:");
            System.out.println("\tdeleteflight,<id>,<flightnumber>");
            break;

        case 7: //delete Car
            System.out.println("Deleting a Car");
            System.out.println("Purpose:");
            System.out.println("\tDelete all cars from a location.");
            System.out.println("\nUsage:");
            System.out.println("\tdeletecar,<id>,<location>,<numCars>");
            break;

        case 8: //delete Room
            System.out.println("Deleting a Room");
            System.out.println("\nPurpose:");
            System.out.println("\tDelete all rooms from a location.");
            System.out.println("Usage:");
            System.out.println("\tdeleteroom,<id>,<location>,<numRooms>");
            break;

        case 9: //delete Customer
            System.out.println("Deleting a Customer");
            System.out.println("Purpose:");
            System.out.println("\tRemove a customer from the database.");
            System.out.println("\nUsage:");
            System.out.println("\tdeletecustomer,<id>,<customerid>");
            break;

        case 10: //querying a flight
            System.out.println("Querying flight.");
            System.out.println("Purpose:");
            System.out.println("\tObtain Seat information about a certain flight.");
            System.out.println("\nUsage:");
            System.out.println("\tqueryflight,<id>,<flightnumber>");
            break;

        case 11: //querying a Car Location
            System.out.println("Querying a Car location.");
            System.out.println("Purpose:");
            System.out.println("\tObtain number of cars at a certain car location.");
            System.out.println("\nUsage:");
            System.out.println("\tquerycar,<id>,<location>");
            break;

        case 12: //querying a Room location
            System.out.println("Querying a Room Location.");
            System.out.println("Purpose:");
            System.out.println("\tObtain number of rooms at a certain room location.");
            System.out.println("\nUsage:");
            System.out.println("\tqueryroom,<id>,<location>");
            break;

        case 13: //querying Customer Information
            System.out.println("Querying Customer Information.");
            System.out.println("Purpose:");
            System.out.println("\tObtain information about a customer.");
            System.out.println("\nUsage:");
            System.out.println("\tquerycustomer,<id>,<customerid>");
            break;

        case 14: //querying a flight for price 
            System.out.println("Querying flight.");
            System.out.println("Purpose:");
            System.out.println("\tObtain price information about a certain flight.");
            System.out.println("\nUsage:");
            System.out.println("\tqueryflightprice,<id>,<flightnumber>");
            break;

        case 15: //querying a Car Location for price
            System.out.println("Querying a Car location.");
            System.out.println("Purpose:");
            System.out.println("\tObtain price information about a certain car location.");
            System.out.println("\nUsage:");
            System.out.println("\tquerycarprice,<id>,<location>");
            break;

        case 16: //querying a Room location for price
            System.out.println("Querying a Room Location.");
            System.out.println("Purpose:");
            System.out.println("\tObtain price information about a certain room location.");
            System.out.println("\nUsage:");
            System.out.println("\tqueryroomprice,<id>,<location>");
            break;

        case 17: //reserve a flight
            System.out.println("Reserving a flight.");
            System.out.println("Purpose:");
            System.out.println("\tReserve a flight for a customer.");
            System.out.println("\nUsage:");
            System.out.println("\treserveflight,<id>,<customerid>,<flightnumber>");
            break;

        case 18: //reserve a car
            System.out.println("Reserving a Car.");
            System.out.println("Purpose:");
            System.out.println("\tReserve a car for a customer at a particular location.");
            System.out.println("\nUsage:");
            System.out.println("\treservecar,<id>,<customerid>,<location>");
            break;

        case 19: //reserve a room
            System.out.println("Reserving a Room.");
            System.out.println("Purpose:");
            System.out.println("\tReserve a room for a customer at a particular location.");
            System.out.println("\nUsage:");
            System.out.println("\treserveroom,<id>,<customerid>,<location>");
            break;

        case 20: //reserve an Itinerary
            System.out.println("Reserving an Itinerary.");
            System.out.println("Purpose:");
            System.out.println("\tBook one or more flights.Also book zero or more cars/rooms at a location.");
            System.out.println("\nUsage:");
            System.out.println(
                    "\titinerary,<id>,<customerid>,<flightnumber1>....<flightnumberN>,<LocationToBookCarsOrRooms>,<NumberOfCars>,<NumberOfRoom>");
            break;

        case 21: //quit the client
            System.out.println("Quitting client.");
            System.out.println("Purpose:");
            System.out.println("\tExit the client application.");
            System.out.println("\nUsage:");
            System.out.println("\tquit");
            break;

        case 22: //new customer with id
            System.out.println("Create new customer providing an id");
            System.out.println("Purpose:");
            System.out.println("\tCreates a new customer with the id provided");
            System.out.println("\nUsage:");
            System.out.println("\tnewcustomerid, <id>, <customerid>");
            break;

        default:
            System.out.println(command);
            System.out.println("The interface does not support this command.");
            break;
        }
    }

    public void wrongNumber() {
        System.out.println("The number of arguments provided in this command are wrong.");
        System.out.println("Type help, <commandname> to check usage of this command.");
    }

    public int getInt(Object temp) throws Exception {
        try {
            return (new Integer((String) temp)).intValue();
        } catch (Exception e) {
            throw e;
        }
    }

    public boolean getBoolean(Object temp) throws Exception {
        try {
            return (new Boolean((String) temp)).booleanValue();
        } catch (Exception e) {
            throw e;
        }
    }

    public String getString(Object temp) throws Exception {
        try {
            return (String) temp;
        } catch (Exception e) {
            throw e;
        }
    }
    
    
    private boolean reconnectToMWS() {

        // get hostname for MWS
        String hostname = this.mwsHost;

        
        System.out.print("\nMWS down, attempting reconnect.");
        
        int i = 0;

        while (true) {
        
            try {

                // connect to the registry at this hostname
                Registry registry = LocateRegistry.getRegistry(hostname, 1738);

                mws = (MiddlewareServer) registry.lookup("Gr17MiddlewareServer");
                
                mws.ping("MWS back online.");
                
                // we got here- ping didnt crash us, so we returned succesfully
                System.out.println("");
                
                System.out.println("Connected to the MWS succesfully");

                // return true
                return true;

            } catch (Exception e) {
            
				i++;
				
				if(i % 5 == 0) { System.out.print("."); }
				
				try {
					Thread.sleep(500);
				} catch(Exception ei) {
					
				}
/*                 System.err.println("Server exception: " + e.toString()); */

            }

        }
        

        // could not reconnect; return false

	}

}