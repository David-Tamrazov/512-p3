package servercode.ResImpl;

import servercode.LockManager.DeadlockException;
import servercode.RMEnums.RMType;
import servercode.ResInterface.*;
import servercode.TransactionManager.InvalidTransactionException;
import servercode.TransactionManager.TransactionAbortedException;
import servercode.TransactionManager.TransactionManager;
import servercode.TransactionManager.ActiveTransaction;

import servercode.TMEnums.*;

import javax.annotation.Resource;
import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RMISecurityManager;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;


public class MiddlewareServerImpl implements MiddlewareServer {
    

    Map<RMType, ResourceManager> resourceManagers;
    TransactionManager tm;
    Map<RMType, String> rmHosts;
    
    protected final static String fileHome = "/tmp/comp512gr17p3";

    public MiddlewareServerImpl(Map<RMType, ResourceManager> map, Map<RMType, String> hosts) {

        // set the resource manager map
        resourceManagers = map;

        // start a new transaction manager
        tm = setTransactionManager(fileHome + "." + "tm.ser");

        // set the active hosts map
        rmHosts = hosts;


        System.out.println("Starting the keepalive thread.");
        
        // start the keepalive thread
        startKeepalive();
		
		System.out.println("Ready to go.");

    }
    
    private TransactionManager setTransactionManager(String filepath) {
	    
	    File tmFile = new File(filepath);
	    
		if(tmFile.exists()) { 
		
			// return the TM read from file 
			return (TransactionManager)readObjFromFile("tm.ser");
			
		} else {
		
			// return a newly initia-jizzed TM
			return new TransactionManager(new HashMap<Integer, ActiveTransaction>());
		}
	
		
    }

    public void connectToManagers(String [] activeManagers) throws RemoteException {
        
        RMType[] resources = new RMType[] { RMType.CAR, RMType.FLIGHT, RMType.ROOM };

        for (int i = 0; i < activeManagers.length; i++) {

            try {
                                            
                Registry registry = LocateRegistry.getRegistry(activeManagers[i], 1738);
                System.out.println("Connected to the registry succesfully");

                this.resourceManagers.put(resources[i], (ResourceManager) registry.lookup("Gr17ResourceManager"));
                this.rmHosts.put(resources[i], activeManagers[i]);

            } catch (Exception e) {

                System.err.println("Server exception: " + e.toString());
                e.printStackTrace();
                System.exit(1);

            }
            
        }
    }

    public int start()  {

        return tm.start();

    }
    
    

    public boolean commit(int xid) throws InvalidTransactionException, TransactionAbortedException, RemoteException {

        try {

            ActiveTransaction t = tm.getActiveTransaction(xid);
          
            
            // set the transaction status as pending a vote request 
            tm.updateTransactionStatus(t,TransactionStatus.VOTE_REQUESTED);
            
            
            // resolve the vote request
            boolean votedCommit = resolveTransaction(t);
            
            // set the status of the transaction depending on the result of the vote request
            Status s = votedCommit ? TransactionStatus.COMMITTED : TransactionStatus.ABORTED;
            
            // update the status of the transaction 
            tm.updateTransactionStatus(t,s);
            
            
            // resolve the transaction again 
            boolean resolved = resolveTransaction(t);
            
            // transaction has committed or aborted- remove it from the TM
			tm.removeActiveTransaction(t.getXID());
            
            // succesful commit - return true
			return votedCommit && resolved;
           
        } catch(RemoteException re) {
         	
            // exception thrown even after trying to reconnect to the RM - crash the server gracefully 
            manageCrash();
            return false;
	        
        }

    }
    
    



    public void abort(int xid) throws InvalidTransactionException, RemoteException, TransactionAbortedException {
       
        try {

            ActiveTransaction t = tm.getActiveTransaction(xid);
            
            // set the transaction status to aborted
            tm.updateTransactionStatus(t, TransactionStatus.ABORTED);
			
			
			// resolve the transaction
			resolveTransaction(t);
                        
            // transaction has committed - remove it from the TM
            tm.removeActiveTransaction(t.getXID());
            


		} catch (RemoteException e) {
			
			// exception thrown even after trying to reconnect to the RM - crash the server gracefully 
			manageCrash();
            
        } catch(TransactionAbortedException | InvalidTransactionException e) {

			// do we need to do something here??? lol
            throw(e);

        }

        // succesful abort

    }
    
    
    private void manageCrash() {
	    
	    // abort all transactions 
	    // sys exit 
	    
    }
    
       
    

    public ResourceManager getResourceManager(RMType t) {
        return this.resourceManagers.get(t);
    }
        

    // Create a new flight, or add seats to existing flight
    //  NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {

        ResourceManager rm = this.getResourceManager(RMType.FLIGHT);


        try {

            this.tm.transactionOperation(id, RMType.FLIGHT);
            rm.addFlight(id, flightNum, flightSeats, flightPrice);
            return true;
            
        } catch (RemoteException e) {
        
			// run an infinite loop and try to reconnect to the RM
			if (reconnectToRM(RMType.FLIGHT)) {
				
				return addFlight(id, flightNum, flightSeats, flightPrice);
				
			}
				
			return false;
			
        } catch (InvalidTransactionException e) {
	        
	        System.out.println("Invalid transaction ID passed.");
	        return false;
	        
        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }


    }


    
    public boolean deleteFlight(int id, int flightNum) throws RemoteException {

        ResourceManager rm = this.getResourceManager(RMType.FLIGHT);

        try{

            this.tm.transactionOperation(id, RMType.FLIGHT);
            return rm.deleteFlight(id, flightNum);

        } catch (RemoteException e) {
        
			// run an infinite loop and try to reconnect to the RM
			if (reconnectToRM(RMType.FLIGHT)) {
				
				return deleteFlight(id, flightNum);
				
			}
				
			return false;
			
        } catch(InvalidTransactionException e) {

            System.out.print(e);
            return false;

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }


    }



    // Create a new room location or add rooms to an existing location
    //  NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int id, String location, int count, int price) throws RemoteException {

        ResourceManager rm = this.getResourceManager(RMType.ROOM);

        try{

            this.tm.transactionOperation(id, RMType.ROOM);
            return rm.addRooms(id, location, count, price);

        } catch (RemoteException e) {
        
			// run an infinite loop and try to reconnect to the RM
			if (reconnectToRM(RMType.ROOM)) {
				
				return addRooms(id, location, count, price);
				
			}
				
			return false;
			
        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }


    }

    // Delete rooms from a location
    public boolean deleteRooms(int id, String location) throws RemoteException {

        ResourceManager rm = this.getResourceManager(RMType.ROOM);

        try{

            this.tm.transactionOperation(id, RMType.ROOM);
            return rm.deleteRooms(id, location);

        } catch (RemoteException e) {
        
			// run an infinite loop and try to reconnect to the RM
			if (reconnectToRM(RMType.ROOM)) {
				
				// try the operation again
				return deleteRooms(id, location);
				
			}
			
			return false;
				
			
        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }

        
    }

    // Create a new car location or add cars to an existing location
    //  NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int id, String location, int count, int price) throws RemoteException {

        ResourceManager rm = this.getResourceManager(RMType.CAR);

        try{

            this.tm.transactionOperation(id, RMType.CAR);
            return rm.addCars(id, location, count, price);

        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        } catch (RemoteException e) {
        
	        if (reconnectToRM(RMType.CAR)) {
		        
		        // try run the method again
		        return addCars(id, location, count, price);
		        
	        }
	        
	        return false;
        }

    }


    // Delete cars from a location
    public boolean deleteCars(int id, String location) throws RemoteException {

        ResourceManager rm = this.getResourceManager(RMType.CAR);

        try{

            this.tm.transactionOperation(id, RMType.CAR);
            return rm.deleteCars(id, location);

        } catch (RemoteException e) {
        
	        if (reconnectToRM(RMType.CAR)) {
		        
		        // try run the method again
		        return deleteCars(id, location);
		        
	        }
	        
	        return false;
	        
	        
        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }


    }



    // Returns the number of empty seats on this flight
    public int queryFlight(int id, int flightNum) throws RemoteException {

        ResourceManager rm = this.getResourceManager(RMType.FLIGHT);

        try{

            while(!this.tm.transactionOperation(id, RMType.FLIGHT)) {}
            
            return this.getResourceManager(RMType.FLIGHT).queryFlight(id, flightNum);

        } catch (RemoteException e) {
        
	        if (reconnectToRM(RMType.FLIGHT)) {
		        
		        // try run the method again
		        return queryFlight(id, flightNum);
		        
	        }
	        
	        return 0;
	        
	        
        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }


    }

    // Returns the number of reservations for this flight. 
//    public int queryFlightReservations(int id, int flightNum)
//        throws RemoteException
//    {
//        Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") called" );
//        RMInteger numReservations = (RMInteger) readData( id, Flight.getNumReservationsKey(flightNum) );
//        if ( numReservations == null ) {
//            numReservations = new RMInteger(0);
//        } // if
//        Trace.info("RM::queryFlightReservations(" + id + ", #" + flightNum + ") returns " + numReservations );
//        return numReservations.getValue();
//    }


    // Returns price of this flight
    public int queryFlightPrice(int id, int flightNum ) throws RemoteException {

        ResourceManager rm = this.getResourceManager(RMType.FLIGHT);

        try {

            tm.transactionOperation(id, RMType.FLIGHT);
            return rm.queryFlightPrice(id, flightNum);

        } catch (RemoteException e) {
        
	        if (reconnectToRM(RMType.FLIGHT)) {
		        
		        // try run the method again
		        return queryFlightPrice(id, flightNum);
		        
	        }
	        
	        return 0;
	        
	        
        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }

    }


    // Returns the number of rooms available at a location
    public int queryRooms(int id, String location) throws RemoteException {

        ResourceManager rm = this.getResourceManager(RMType.ROOM);

        try{

            this.tm.transactionOperation(id, RMType.ROOM);
            return rm.queryRooms(id, location);

        } catch (RemoteException e) {
        
	        if (reconnectToRM(RMType.ROOM)) {
		        
		        // try run the method again
		        return queryRooms(id, location);
		        
	        }
	        
	        return 0;
	        
	        
        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }

    }


    
    
    // Returns room price at this location
    public int queryRoomsPrice(int id, String location) throws RemoteException {

        ResourceManager rm = this.getResourceManager(RMType.ROOM);

        try{

            this.tm.transactionOperation(id, RMType.ROOM);
            return rm.queryRoomsPrice(id, location);

        } catch (RemoteException e) {
        
	        if (reconnectToRM(RMType.ROOM)) {
		        
		        // try run the method again
		        return queryRoomsPrice(id, location);
		        
	        }
	        
	        return 0;
	        
	        
        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }

    }


    // Returns the number of cars available at a location
    public int queryCars(int id, String location) throws RemoteException {

        ResourceManager rm = this.getResourceManager(RMType.CAR);

        try{

            this.tm.transactionOperation(id, RMType.CAR);
            return rm.queryCars(id, location);

        } catch (RemoteException e) {
        
	        if (reconnectToRM(RMType.CAR)) {
		        
		        // try run the method again
		        return queryCars(id, location);
		        
	        }
	        
	        return 0;
	        
	        
        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }

    }


    // Returns price of cars at this location
    public int queryCarsPrice(int id, String location) throws RemoteException {

        ResourceManager rm = this.getResourceManager(RMType.CAR);

        try{

            this.tm.transactionOperation(id, RMType.CAR);
            return rm.queryCarsPrice(id, location);

        } catch (RemoteException e) {
        
	        if (reconnectToRM(RMType.CAR)) {
		        
		        // try run the method again
		        return queryCarsPrice(id, location);
		        
	        }
	        
	        return 0;
	        
        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }


    }

    // Returns data structure containing customer reservation info. Returns null if the
    //  customer doesn't exist. Returns empty RMHashtable if customer exists but has no
    //  reservations.
    

    // return a bill
    public String queryCustomerInfo(int id, int customerID) throws RemoteException {

       	RMType attempt = RMType.CAR;
       	
        try {
        
			
            this.tm.transactionOperation(id, RMType.FLIGHT);            
            this.tm.transactionOperation(id, RMType.ROOM); 
            this.tm.transactionOperation(id, RMType.CAR);
            
         
            String str = "\n" + this.getResourceManager(RMType.CAR).queryCustomerInfo(id, customerID);
            
            attempt = RMType.FLIGHT;
            
            str += "\n" + this.getResourceManager(RMType.FLIGHT).queryCustomerInfo(id, customerID);
            
            attempt = RMType.ROOM; 
            
            return str += "\n" + this.getResourceManager(RMType.ROOM).queryCustomerInfo(id, customerID);


        } catch (RemoteException e) {
        
        	if (reconnectToRM(attempt)) {
	        	return queryCustomerInfo(id, customerID);
        	}
        	
        	return "";
        	
        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }

    }

    // customer functions
    // new customer just returns a unique customer identifier
    
    public synchronized int newCustomer(int id) throws RemoteException {

		RMType attempt = RMType.CAR;
					
        try {

            for (Map.Entry<RMType, ResourceManager> entry : this.resourceManagers.entrySet()) {
                this.tm.transactionOperation(id, entry.getKey());
            }
			
            int cid = this.getResourceManager(RMType.CAR).newCustomer(id);
            
            attempt = RMType.FLIGHT;
            
            this.getResourceManager(RMType.FLIGHT).newCustomer(id,cid);
            
            attempt = RMType.ROOM;
            
            this.getResourceManager(RMType.ROOM).newCustomer(id,cid);

            return cid;

        } catch(RemoteException e) {
        	
        	if (reconnectToRM(attempt)) {
	        	return newCustomer(id);
        	}
        	
        	return 0;
        	
        } catch( InvalidTransactionException e) {
 
            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }



    }
    
    
/*
    private ArrayList<RMType> getClone(ArrayList<RMType> e) {
	    
	    ArrayList<RMType> temp = new ArrayList<RMType>();
	    
	    for (RMType type: e) {
		    temp.push(e);
	    }
	    
	    return temp;
    }
*/

    // I opted to pass in customerID instead. This makes testing easier
    public synchronized boolean newCustomer(int id, int customerID) throws RemoteException {
    

		RMType attempt = RMType.CAR;
		
        try {

            for (Map.Entry<RMType, ResourceManager> entry : this.resourceManagers.entrySet()) {
                this.tm.transactionOperation(id, entry.getKey());
            }
			
			boolean car = this.getResourceManager(RMType.CAR).newCustomer(id, customerID);
			
			attempt = RMType.FLIGHT;
			
			boolean flight = this.getResourceManager(RMType.FLIGHT).newCustomer(id, customerID);
			
			attempt = RMType.ROOM;
			
			boolean room = this.getResourceManager(RMType.ROOM).newCustomer(id, customerID);
			
			
            return car && flight && room;

        } catch(RemoteException e) {
        
        	if (reconnectToRM(attempt)) {
	        	return newCustomer(id, customerID);
        	}
        	
        	return false;
        
        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }

    }


    // Deletes customer from the database. 
    public synchronized boolean deleteCustomer(int id, int customerID) throws RemoteException {

		RMType attempt = RMType.CAR;
		
        try {

            for (Map.Entry<RMType, ResourceManager> entry : this.resourceManagers.entrySet()) {
                this.tm.transactionOperation(id, entry.getKey());
            }
			
			boolean car = this.getResourceManager(RMType.CAR).deleteCustomer(id, customerID);
			
			attempt = RMType.FLIGHT;
			
			boolean flight = this.getResourceManager(RMType.FLIGHT).deleteCustomer(id, customerID);
			
			attempt = RMType.ROOM;
			
			boolean room = this.getResourceManager(RMType.ROOM).deleteCustomer(id, customerID);
	
			
            return car && flight && room;

        } catch(RemoteException e) {
        
        	if (reconnectToRM(attempt)) {
	        	return deleteCustomer(id, customerID);
        	}
        	
        	return false;
        
        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }
    }
    
    // Adds car reservation to this customer. 
    public boolean reserveCar(int id, int customerID, String location) throws RemoteException {

        ResourceManager rm = this.getResourceManager(RMType.CAR);

        try {

            this.tm.transactionOperation(id, RMType.CAR);
            return rm.reserveCar(id, customerID, location);

        } catch (RemoteException e) {
        
        	if (reconnectToRM(RMType.CAR)) {
	        	return reserveCar(id, customerID, location);	
        	}
        	
        	return false;
        	
        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }

    }


    // Adds room reservation to this customer. 
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {

        ResourceManager rm = this.getResourceManager(RMType.ROOM);

        try {

            this.tm.transactionOperation(id, RMType.ROOM);
            return this.getResourceManager(RMType.ROOM).reserveRoom(id, customerID, location);

        } catch (RemoteException e) {
        
        	if (reconnectToRM(RMType.ROOM)) {
	        	return reserveRoom(id, customerID, location);	
        	}
        	
        	return false;
        	
        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }

    }
    // Adds flight reservation to this customer.  
    public boolean reserveFlight(int id, int customerID, int flightNum) throws RemoteException {

        ResourceManager rm = this.getResourceManager(RMType.FLIGHT);

        try {

            this.tm.transactionOperation(id, RMType.FLIGHT);
            return rm.reserveFlight(id, customerID, flightNum);

        } catch (RemoteException e) {
        
        	if (reconnectToRM(RMType.FLIGHT)) {
	        	return reserveFlight(id, customerID, flightNum);	
        	}
        	
        	return false;
        	
        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }


    }
    
    // Reserve an itinerary 
    public boolean itinerary(int id,int customer,Vector flightNumbers,String location, boolean Car, boolean Room) throws RemoteException {

        System.out.println("Reserving an Itinerary using id:" + id);
        System.out.println("Customer id:" + customer);

		RMType attempt = RMType.FLIGHT;

        try {

            boolean success = false;           

            // inform the transaction manager that the flight manager is involved in this transaction now
            this.tm.transactionOperation(id, RMType.FLIGHT);

            for (Object flightNum: (Vector)flightNumbers) {
    
                if (!this.getResourceManager(RMType.FLIGHT).reserveFlight(id, customer, Integer.parseInt((String)flightNum))) {
                    return false;
                }
    
            }
            
			
			
            if (Car) {
            
            	attempt = RMType.CAR;
                this.tm.transactionOperation(id, RMType.CAR);
                success = this.getResourceManager(RMType.CAR).reserveCar(id, customer, location);
            }

            if (Room) {
            	attempt = RMType.ROOM;
                this.tm.transactionOperation(id, RMType.ROOM);
                success = this.getResourceManager(RMType.ROOM).reserveRoom(id, customer, location);
            }

    
            return success;
           
        } catch (RemoteException e) {
        
        	if (reconnectToRM(attempt)) {
	        	return itinerary(id, customer, flightNumbers, location, Car, Room);
        	}
        	
        	return false;
        	
        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }
    }

    public void ping(String ping) throws RemoteException {
        System.out.println(ping);
    }
   

    public static void main(String args[]) {

        // Figure out where server is running
        String objName = "Gr17MiddlewareServer";
        String server = "localhost";
        int port = 1738;

        String [] activeManagers = new String[3];


        // Production code 

        if(args.length == 3) {
            activeManagers[0] = args[0];
            activeManagers[1] = args[1];
            activeManagers[2] = args[2];
        } else {
            System.err.println ("Wrong usage");
            System.out.println("Usage: java ResImpl.MiddlewareServer [firstResourceManager] [secondResourceManager] [thirdResourceManager] ");
            System.exit(1);
        }

        // // Test with one machine

        try {
            // Instantiate a new middleware server object and bind it to the registry for the client to interface with 
            MiddlewareServerImpl obj = new MiddlewareServerImpl(new HashMap<RMType, ResourceManager>(), new HashMap<RMType, String>());
            MiddlewareServer mws = (MiddlewareServer) UnicastRemoteObject.exportObject(obj, 0);
            
            // Locate the registry            
            Registry registry;
            registry = LocateRegistry.getRegistry(server, port);

            // Bind the server to the registry
            registry.rebind(objName, mws);

            // Bind to the active resource managers passed to the server
            mws.connectToManagers(activeManagers);

            mws.getResourceManager(RMType.CAR).ping("car");
            mws.getResourceManager(RMType.FLIGHT).ping("flight");
            mws.getResourceManager(RMType.ROOM).ping("room");
            
            System.out.println("Middleware server is ready.");

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
            System.exit(1);
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
             System.setSecurityManager(new RMISecurityManager());
        }


    }
    
    public boolean shutdown() throws RemoteException {
	    
	    for (Map.Entry<RMType, ResourceManager> rmEntry : this.resourceManagers.entrySet()) {
		    
		    try {
		    	rmEntry.getValue().shutdown();
		    } catch(RemoteException e) {
			    // do nothing for now
		    }
		    
	    }
	    
	    System.exit(0);
	    return true;
    }
    
    private static Object readObjFromFile(String filepath) {

        Object o = null;

        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileHome + "." + filepath))) {

            o = (Object) ois.readObject();

        } catch(Exception e) {

            System.out.println("Err: " + e);
            e.printStackTrace();

        }

        return o;

    }
    
    
    private boolean resolveTransaction(ActiveTransaction t) throws RemoteException, TransactionAbortedException {
    
    	Status s = t.getStatus();
    
		
    	for (RMType type: t.getResourceManagers()) {
    	
    		boolean resolved = false;
    		
    		while (!resolved) {
	    		
	    		
	    		try {
	    	
	    			// get the corresponding RM 
	    			ResourceManager rm = this.getResourceManager(type);
		    		
		    		System.out.println("Resolving transaction #" + t.getXID() + ", with status: " + s.toString() + ", for RM: " + type.toString());
		    		
		    		// resolve - if at any point we fail return false
		    		if (!s.resolve(rm, t.getXID())) {
			    		return false;
		    		}	
		    		
		    		resolved = true;
		    	
				} catch (RemoteException e) {
	    		
		    		System.out.println("Exception" + e);
		        
		        	System.out.println("Lost connection to rm: " + type);
		    	
		    		reconnectToRM(type);
		            
		            System.out.println("Recovered connection to RM: " + type);
		    	
				} catch (InvalidTransactionException | TransactionAbortedException i) {
					return false;
				}
	    	
		
	    		
    		}
	    	
	    		    	
    	}
    	
    	return true;
		
	}
	
	
	
	
	private boolean recoverTransaction(ActiveTransaction t) throws RemoteException {
	
		try {
		
			// go through the commit process again 
			if (t.getStatus() == TransactionStatus.VOTE_REQUESTED) {
				return commit(t.getXID());
			} 
			
			// just resolve the transaction - commit, abort, active
			return resolveTransaction(t);
			
			
		} catch (RemoteException e) {
		
			// reconnecting proved impossible elsewhere; crash the server 
			manageCrash();
			return false;
			
		} catch (InvalidTransactionException | TransactionAbortedException e) {
			//we'll worry about this later
			tm.removeActiveTransaction(t.getXID());
			return false;
		}
		 
	
    }
    

    private boolean reconnectToRM(RMType t) {

        // get hostname for this RM
        String hostname = this.rmHosts.get(t);

        // counter for how many times it should try to reconnect
        int i = 0;
        
        System.out.print("\n" + t + " RM down, attempting reconnect.");

        while (true) {
        
            try {

                // connect to the registry at this hostname
                Registry registry = LocateRegistry.getRegistry(hostname, 1738);

                // try put the new object reference into our map
                this.resourceManagers.put(t, (ResourceManager) registry.lookup("Gr17ResourceManager"));
                            
                // ping to check for liveliness
                this.resourceManagers.get(t).ping("Hello");
                
                // we got here- ping didnt crash us, so we returned succesfully
                System.out.println("");
                
                System.out.println("Connected to the registry succesfully");

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


    private void handleDeadlock(int xid) {

        try {
            abort(xid);
        } catch (InvalidTransactionException | TransactionAbortedException | RemoteException e) {

            System.out.println("Invalid transaction passed to abort.");
        }

    }
    
    private void startKeepalive() {
    
    	Thread keepalive = new Thread() {
        
			public void run() {
				
				while (true) {
				
					// get the transaction map
					Map<Integer,ActiveTransaction> activeTransactionMap = tm.getActiveTransactions();

					// iterate over each transaction
					for(Map.Entry<Integer, ActiveTransaction> activeTransaction : activeTransactionMap.entrySet()) {
            
						ActiveTransaction t = activeTransaction.getValue();
						Date curr = new Date();

						// if the transaction has run out of its time to live, 
						if(curr.getTime() - t.getLastTransationTime().getTime() > t.getTimeToLive() && t.getStatus() == TransactionStatus.ACTIVE) {

							System.out.println("Aborting transaction: " + t.getXID());
							
							try {

								// abort the transaction
								abort(t.getXID());

						
							} catch (InvalidTransactionException | TransactionAbortedException | RemoteException e) {

								System.out.println("Keepalive thread interrupted.");
							}
						} 
					}


					try {
						
                		Thread.sleep(100);
					

					} catch (InterruptedException e) {

						Thread.interrupted();

					}

				}
			}
			
		};
		
		keepalive.start();
	    
    }
    
    @Override 
    public boolean voteRequest(int xid) throws RemoteException, InvalidTransactionException {
	   return true;
    }

}