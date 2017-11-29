package servercode.ResImpl;

import servercode.LockManager.DeadlockException;
import servercode.RMEnums.RMType;
import servercode.ResInterface.*;
import servercode.TransactionManager.InvalidTransactionException;
import servercode.TransactionManager.TransactionAbortedException;
import servercode.TransactionManager.TransactionManager;
import servercode.TransactionManager.ActiveTransaction;

import javax.annotation.Resource;
import java.util.*;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RMISecurityManager;


public class MiddlewareServerImpl implements MiddlewareServer {
    

    Map<RMType, ResourceManager> resourceManagers;
    TransactionManager tm;
    Map<RMType, String> rmHosts;

    public MiddlewareServerImpl(Map<RMType, ResourceManager> map, Map<RMType, String> hosts) {

        // set the resource manager map
        resourceManagers = map;

        // start a new transaction manager
        tm = new TransactionManager(new HashMap<Integer, ActiveTransaction>());

        // set the active hosts map
        rmHosts = hosts;


        System.out.println("Starting the keepalive thread.");
        
        // start the keepalive thread
        startKeepalive();
		
		System.out.println("Ready to go.");

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

            for (RMType type : t.getResourceManagers()) {

                boolean committed = false;
                ResourceManager rm = this.getResourceManager(type);

                while(!committed) {

                    try {

                        // try to commit
                        rm.commit(xid);
                        committed = true;

                    } catch (RemoteException e) {

                        // if the RM crashes, reconnect to the RM
                        if (!reconnectToRM(type)) {

                            // couldn't reconnect to the RM- crash the server NEEDS WORK!!!
                            throw(e);
                        }

                    }


                }

                // transaction has committed - remove it from the TM
                tm.removeActiveTransaction(t.getXID());

            }

        // do we need to do something here???
        } catch(InvalidTransactionException | TransactionAbortedException e) {

            System.out.print(e);
            return false;

        }

        // succesful commit - return true
        return true;

    }



    public void abort(int xid) throws InvalidTransactionException, RemoteException {
        try {

            ActiveTransaction t = tm.getActiveTransaction(xid);

            for (RMType type : t.getResourceManagers()) {

                boolean aborted = false;
                ResourceManager rm = this.getResourceManager(type);

                while(!aborted) {

                    try {

                        // try to abort
                        rm.abort(xid);
                        aborted = true;

                    } catch (RemoteException e) {

                        // if the RM crashes, reconnect to the RM
                        if (!reconnectToRM(type)) {

                            // couldn't reconnect to the RM- crash the server NEEDS WORK!!!
                            throw(e);
                        }

                    }


                }

                // transaction has committed - remove it from the TM
                tm.removeActiveTransaction(t.getXID());

            }

            // do we need to do something here???
        } catch(InvalidTransactionException e) {

            System.out.print(e);

        }

        // succesful abort

    }

    private boolean reconnectToRM(RMType t) {

        // get hostname for this RM
        String hostname = this.rmHosts.get(t);

        // counter for how many times it should try to reconnect
        int i = 0;

        while (i < 200) {

            try {

                // connect to the registry at this hostname
                Registry registry = LocateRegistry.getRegistry(hostname, 1738);

                System.out.println("Connected to the registry succesfully");

                // try put the new object reference into our map
                this.resourceManagers.put(t, (ResourceManager) registry.lookup("Gr17ResourceManager"));

                // return true
                return true;

            } catch (Exception e) {

                System.err.println("Server exception: " + e.toString());
                e.printStackTrace();
                System.exit(1);

            }

        }


        // could not reconnect; return false
        return false;

    }

    private ResourceManager getResourceManager(RMType t) {
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

        }  catch (InvalidTransactionException e) {
	        
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

        } catch( InvalidTransactionException e) {

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

        }

    }


    // Delete cars from a location
    public boolean deleteCars(int id, String location) throws RemoteException {

        ResourceManager rm = this.getResourceManager(RMType.CAR);

        try{

            this.tm.transactionOperation(id, RMType.CAR);
            return rm.deleteCars(id, location);

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
            
            return this.getFlightManager().queryFlight(id, flightNum);

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

        try {

            this.tm.transactionOperation(id, RMType.FLIGHT);
            this.tm.transactionOperation(id, RMType.ROOM);
            this.tm.transactionOperation(id, RMType.CAR);

            return "\n" + this.getCarManager().queryCustomerInfo(id, customerID) + "\n" +
                    this.getFlightManager().queryCustomerInfo(id, customerID) + "\n" +
                    this.getRoomManager().queryCustomerInfo(id, customerID);

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

        try {

            for (Map.Entry<RMType, ResourceManager> entry : this.resourceManagers.entrySet()) {
                this.tm.transactionOperation(id, entry.getKey());
            }

            int cid = this.getCarManager().newCustomer(id);
            this.getFlightManager().newCustomer(id,cid);
            this.getRoomManager().newCustomer(id,cid);

            return cid;

        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }



    }

    // I opted to pass in customerID instead. This makes testing easier
    public synchronized boolean newCustomer(int id, int customerID) throws RemoteException {

        try {

            for (Map.Entry<RMType, ResourceManager> entry : this.resourceManagers.entrySet()) {
                this.tm.transactionOperation(id, entry.getKey());
            }

            return this.getRoomManager().newCustomer(id, customerID) &&
                    this.getCarManager().newCustomer(id, customerID) &&
                    this.getFlightManager().newCustomer(id, customerID);


        } catch( InvalidTransactionException e) {

            throw new RemoteException("Invalid transaction id passed: " + id);

        } catch (DeadlockException e) {

            handleDeadlock(e.getXID());
            throw new RemoteException("Transaction aborted: deadlock");

        }

    }


    // Deletes customer from the database. 
    public synchronized boolean deleteCustomer(int id, int customerID) throws RemoteException {

        try {

            for (Map.Entry<RMType, ResourceManager> entry : this.resourceManagers.entrySet()) {
                this.tm.transactionOperation(id, entry.getKey());
            }

            this.getCarManager().deleteCustomer(id, customerID);
            this.getFlightManager().deleteCustomer(id, customerID);

            return this.getRoomManager().deleteCustomer(id, customerID) &&
                    this.getCarManager().deleteCustomer(id, customerID) &&
                    this.getFlightManager().deleteCustomer(id, customerID);


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
            return this.getCarManager().reserveRoom(id, customerID, location);

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
            return this.getFlightManager().reserveFlight(id, customerID, flightNum);

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


        try {

            boolean success = false;

            // inform the transaction manager that the flight manager is involved in this transaction now
            this.tm.transactionOperation(id, RMType.FLIGHT);

            for (Object flightNum: (Vector)flightNumbers) {
    
                if (!this.getFlightManager().reserveFlight(id, customer, Integer.parseInt((String)flightNum))) {
                    return false;
                }
    
            }

            if (Car) {
                this.tm.transactionOperation(id, RMType.CAR);
                success = this.getCarManager().reserveCar(id, customer, location);
            }

            if (Room) {
                this.tm.transactionOperation(id, RMType.ROOM);
                success = this.getRoomManager().reserveRoom(id, customer, location);
            }

    
            return success;
           
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

    public ResourceManager getCarManager() {
        return this.resourceManagers.get("car");
    }

    public ResourceManager getRoomManager() {
        return this.resourceManagers.get("room");
    }

    public ResourceManager getFlightManager() {
        return this.resourceManagers.get("flight");
    }
    

    public static void sayHey() {
        
        System.out.println("Hey there from the MiddlewareServer");
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

            mws.getCarManager().ping("car");
            mws.getFlightManager().ping("flight");
            mws.getRoomManager().ping("room");
            
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

    private void handleDeadlock(int xid) {

        try {
            abort(xid);
        } catch (InvalidTransactionException | RemoteException e) {

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

						// if the transaction has run out of its time to live, abort it
						if(curr.getTime() - t.getLastTransationTime().getTime() > t.getTimeToLive()) {

							System.out.println("Aborting transaction: " + t.getXID());
							
							try {

								// abort the transaction
								abort(t.getXID());

						
							} catch (InvalidTransactionException | RemoteException e) {

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

}