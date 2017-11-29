package servercode.ResImpl;

import java.util.*;
import java.io.Serializable;

public class Command implements Serializable {

	enum TYPE {nCar, dCar, rCar, qCar, pCar,
		nFlight, dFlight, rFlight, qFlight, pFlight,
		nRoom, dRoom, rRoom, qRoom, pRoom,
		nCustomer, nCustomerID, dCustomer, qCustomer, itinerary,
		Q, H, NULL
	};

	private static final String[] availableCommands = new String[] {
		"help",
		"newcar",
		"deletecar",
        "querycar",
        "reservecar",
        "querycarprice",
        "newflight",
        "deleteflight",
        "queryflight",
        "reserveflight",
        "queryflightprice",
        "newroom",
        "deleteroom",
        "queryroom",
        "reserveroom",
        "queryroomprice",
        "newcustomer",
        "newcustomerid",
        "deletecustomer",
        "querycustomer",
        "itinerary",
        "quit"
	};
	
	private Vector arguments;
	private String command;
	private TYPE type;
	
	Command(String command) {
	
		arguments = new Vector();
        StringTokenizer tokenizer = new StringTokenizer(command, ",");
        String argument = "";
        while (tokenizer.hasMoreTokens()) {
            argument = tokenizer.nextToken();
            argument = argument.trim();
            arguments.add(argument);
        }
        
        this.command = command;
        this.type = findCommandType();
	}
	
		
	public TYPE findCommandType() {
		switch((String) arguments.elementAt(0)) {
			case "help": return TYPE.H;
            case "newcar": return TYPE.nCar;
            case "deletecar": return TYPE.dCar;
            case "querycar": return TYPE.qCar;
            case "reservecar": return TYPE.rCar;
            case "querycarprice": return TYPE.pCar;
            case "newflight": return TYPE.nFlight;
            case "deleteflight": return TYPE.dFlight;
            case "queryflight": return TYPE.qFlight;
            case "reserveflight": return TYPE.rFlight;
            case "queryflightprice": return TYPE.pFlight;
            case "newroom": return TYPE.nRoom;
            case "deleteroom": return TYPE.dRoom;
            case "queryroom": return TYPE.qRoom;
            case "reserveroom": return TYPE.rRoom;
            case "queryroomprice": return TYPE.pRoom;
            case "newcustomer": return TYPE.nCustomer;
            case "newcustomerid": return TYPE.nCustomerID;
            case "deletecustomer": return TYPE.dCustomer;
            case "querycustomer": return TYPE.qCustomer;
            case "itinerary": return TYPE.itinerary;
            case "quit": return TYPE.Q;
            default:
            	return TYPE.NULL;
		}
	}
	
	public String isHelp() {
		String response = "";
	
		//if command is just help
		if(arguments.size() == 1) {
			
			response += "\nWelcome to the client interface provided to test your project."
			+ "\nCommands accepted by the interface are:";
	        for(String availableCommand : availableCommands) { response += "\n" + availableCommand; }
	        response += "\ntype help, <commandname> for detailed info(NOTE the use of comma).";

		} else {
		
			Command helpCommand = new Command((String) arguments.elementAt(1));
			
			if(helpCommand.getType() != TYPE.NULL) {
	
				switch (helpCommand.getCommand()) {
		            case "help":
		                response += "Help\n";
		                response += "\nTyping help on the prompt gives a list of all the commands available.\n";
		                response += "Typing help, <commandname> gives details on how to use the particular command.\n";
		                break;
		
		            case "newflight": //new flight
		                response += "Adding a new Flight.\n";
		                response += "Purpose:\n";
		                response += "\tAdd information about a new flight.\n";
		                response += "\nUsage:\n";
		                response += "\tnewflight,<id>,<flightnumber>,<flightSeats>,<flightprice>\n";
		                break;
		
		            case "newcar": //new Car
		                response += "Adding a new Car.\n";
		                response += "Purpose:\n";
		                response += "\tAdd information about a new car location.\n";
		                response += "\nUsage:\n";
		                response += "\tnewcar,<id>,<location>,<numberofcars>,<pricepercar>\n";
		                break;
		
		            case "newroom": //new Room
		                response += "Adding a new Room.\n";
		                response += "Purpose:\n";
		                response += "\tAdd information about a new room location.\n";
		                response += "\nUsage:\n";
		                response += "\tnewroom,<id>,<location>,<numberofrooms>,<priceperroom>\n";
		                break;
		
		            case "newcustomer": //new Customer
		                response += "Adding a new Customer.\n";
		                response += "Purpose:\n";
		                response += "\tGet the system to provide a new customer id. (same as adding a new customer\n";
		                response += "\nUsage:\n";
		                response += "\tnewcustomer,<id>\n";
		                break;
		                
		            case "newcustomerid": //new customer with id
		                response += "Create new customer providing an id\n";
		                response += "Purpose:\n";
		                response += "\tCreates a new customer with the id provided\n";
		                response += "\nUsage:\n";
		                response += "\tnewcustomerid, <id>, <customerid>\n";
		                break;    
		
		            case "deleteflight": //delete Flight
		                response += "Deleting a flight\n";
		                response += "Purpose:\n";
		                response += "\tDelete a flight's information.\n";
		                response += "\nUsage:\n";
		                response += "\tdeleteflight,<id>,<flightnumber>\n";
		                break;
		
		            case "deletecar": //delete Car
		                response += "Deleting a Car\n";
		                response += "Purpose:\n";
		                response += "\tDelete all cars from a location.\n";
		                response += "\nUsage:\n";
		                response += "\tdeletecar,<id>,<location>,<numCars>\n";
		                break;
		
		            case "deleteroom": //delete Room
		                response += "Deleting a Room\n";
		                response += "\nPurpose:\n";
		                response += "\tDelete all rooms from a location.\n";
		                response += "Usage:\n";
		                response += "\tdeleteroom,<id>,<location>,<numRooms>\n";
		                break;
		
		            case "deletecustomer": //delete Customer
		                response += "Deleting a Customer\n";
		                response += "Purpose:\n";
		                response += "\tRemove a customer from the database.\n";
		                response += "\nUsage:\n";
		                response += "\tdeletecustomer,<id>,<customerid>\n";
		                break;
		
		            case "queryflight": //querying a flight
		                response += "Querying flight.\n";
		                response += "Purpose:\n";
		                response += "\tObtain Seat information about a certain flight.\n";
		                response += "\nUsage:\n";
		                response += "\tqueryflight,<id>,<flightnumber>\n";
		                break;
		
		            case "querycar": //querying a Car Location
		                response += "Querying a Car location.\n";
		                response += "Purpose:\n";
		                response += "\tObtain number of cars at a certain car location.\n";
		                response += "\nUsage:\n";
		                response += "\tquerycar,<id>,<location>\n";
		                break;
		
		            case "queryroom": //querying a Room location
		                response += "Querying a Room Location.\n";
		                response += "Purpose:\n";
		                response += "\tObtain number of rooms at a certain room location.\n";
		                response += "\nUsage:\n";
		                response += "\tqueryroom,<id>,<location>\n";
		                break;
		
		            case "querycustomer": //querying Customer Information
		                response += "Querying Customer Information.\n";
		                response += "Purpose:\n";
		                response += "\tObtain information about a customer.\n";
		                response += "\nUsage:\n";
		                response += "\tquerycustomer,<id>,<customerid>\n";
		                break;
		
		            case "queryflightprice": //querying a flight for price 
		                response += "Querying flight.\n";
		                response += "Purpose:\n";
		                response += "\tObtain price information about a certain flight.\n";
		                response += "\nUsage:\n";
		                response += "\tqueryflightprice,<id>,<flightnumber>\n";
		                break;
		
		            case "querycarprice": //querying a Car Location for price
		                response += "Querying a Car location.\n";
		                response += "Purpose:\n";
		                response += "\tObtain price information about a certain car location.\n";
		                response += "\nUsage:\n";
		                response += "\tquerycarprice,<id>,<location>\n";
		                break;
		
		            case "queryroomprice": //querying a Room location for price
		                response += "Querying a Room Location.\n";
		                response += "Purpose:\n";
		                response += "\tObtain price information about a certain room location.\n";
		                response += "\nUsage:\n";
		                response += "\tqueryroomprice,<id>,<location>\n";
		                break;
		
		            case "reserveflight": //reserve a flight
		                response += "Reserving a flight.\n";
		                response += "Purpose:\n";
		                response += "\tReserve a flight for a customer.\n";
		                response += "\nUsage:\n";
		                response += "\treserveflight,<id>,<customerid>,<flightnumber>\n";
		                break;
		
		            case "reservecar": //reserve a car
		                response += "Reserving a Car.\n";
		                response += "Purpose:\n";
		                response += "\tReserve a car for a customer at a particular location.\n";
		                response += "\nUsage:\n";
		                response += "\treservecar,<id>,<customerid>,<location>\n";
		                break;
		
		            case "reserveroom": //reserve a room
		                response += "Reserving a Room.\n";
		                response += "Purpose:\n";
		                response += "\tReserve a room for a customer at a particular location.\n";
		                response += "\nUsage:\n";
		                response += "\treserveroom,<id>,<customerid>,<location>\n";
		                break;
		
		            case "itinerary": //reserve an Itinerary
		                response += "Reserving an Itinerary.\n";
		                response += "Purpose:\n";
		                response += "\tBook one or more flights.Also book zero or more cars/rooms at a location.\n";
		                response += "\nUsage:\n";
		                response += "\titinerary,<id>,<customerid>,<flightnumber1>....<flightnumberN>,<LocationToBookCarsOrRooms>,<NumberOfCars>,<NumberOfRoom>\n";
		                break;
		
		            case "quit": //quit the client
		                response += "Quitting client.\n";
		                response += "Purpose:\n";
		                response += "\tExit the client application.\n";
		                response += "\nUsage:\n";
		                response += "\tquit\n";
		                break;
		
		            default:
		                response += command + "\n";
		                response += "The interface does not support this command.\n";
		                break;
	            }
			} else {
				response = "The interface does not support this command: " + helpCommand.getCommand() + "\n";
			}
		}
		
		return response;
		
	}
	
	public Vector getArguments() {
		return arguments;
	}
	
	public String getCommand() {
		return command;
	}
	
	public TYPE getType() {
		return type;
	}
}

			