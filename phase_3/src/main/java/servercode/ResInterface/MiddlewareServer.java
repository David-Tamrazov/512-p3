package servercode.ResInterface;

import java.rmi.registry.Registry;
import java.rmi.Remote;
import java.rmi.RemoteException;
import servercode.RMEnums.RMType;

import java.util.*;

// for now this extends Remote
// However, we eventually want it to be able to do everything a ResourceManager does since the client will only interface with MiddlewareServer
    // book customer
    // book flight
    // itinerary, et...
// Therefore, it'll be easier to just have this extend ResourceManager and then override all of those methods in the class implementation 

public interface MiddlewareServer extends ResourceManager {
   	public boolean crash(String which) throws RemoteException; 
    public void connectToManagers(String [] activeManagers) throws RemoteException;
    public ResourceManager getResourceManager(RMType type) throws RemoteException;

}