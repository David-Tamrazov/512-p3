package servercode.TransactionManager;

import java.rmi.RemoteException;
import java.util.*;
import servercode.ResInterface.ResourceManager;
import servercode.ResInterface.Transaction;
import java.sql.Timestamp;

public class ActiveTransaction implements Transaction {

    // list of managers active in this transaction
    private ArrayList<ResourceManager> activeManagers;

    // how long this transaction has to live 
    private int timeToLive;
    private int xid;
    private Date lastTransactionTime;

    public ActiveTransaction(int xid, int timeToLive, ArrayList<ResourceManager> resourceManagers) {
        setXID(xid);
        setTimeToLive(timeToLive);
        setActiveManagers(resourceManagers);
        setLastTransactionTime(new Date());
    }

    public void addActiveManager(ResourceManager manager) {

        // if the manager isn't already recorded as an active manager, add them to the list 
        if (!this.activeManagers.contains(manager)) {
            this.activeManagers.add(manager);
        }

    }

    // nothing for the transaction to do at start
    public int start()  {

        return 0;

    }

    public boolean commit(int xid) throws InvalidTransactionException, TransactionAbortedException, RemoteException {
				
        if (xid - this.xid != 0) {
            throw new InvalidTransactionException(xid, "Invalid transaction id passed to transaction commit.");
        }

        boolean success = false;

        // send the commit command to every resource manager involved in this transaction
        for (ResourceManager rm: this.activeManagers) {

            try {
            
            	System.out.println("AT is sending this id to RMs for commit: " + this.xid + "\n"); 

                success = rm.commit(this.xid);

            } catch(InvalidTransactionException | TransactionAbortedException | RemoteException e) {

                throw e;

            }

        }

        return success;
    }

    public void abort(int xid) throws InvalidTransactionException, RemoteException {

        if (xid != this.xid) {
            throw new InvalidTransactionException(xid, "Invalid transaction id passed to transaction commit.");
        }

        boolean success = false;

        // send the abort command to every resource manager involved in this transaction
        for (ResourceManager rm: this.activeManagers) {

            try {
            
            	System.out.println("AT is sending this id to RMs for abort: " + this.xid + "\n"); 

                rm.abort(xid);

            } catch(InvalidTransactionException | RemoteException e) {

                throw e;

            }

        }


    }
    
    public boolean shutdown() throws RemoteException {
	    
	    return true;
	    
    }

    public Date getLastTransationTime() {
        return this.lastTransactionTime;
    }

    public void updateLastTransaction() {
        this.lastTransactionTime =  new Date();
    }

    public int getTimeToLive() {
        return this.timeToLive;
    }
    
    public int getXID() {
	    return this.xid;
    }
    
    private void setLastTransactionTime(Date d) {
	    this.lastTransactionTime = d;
    }

    private void setXID(int i) {
        this.xid = i;
    }
    private void setTimeToLive(int i) {
        this.timeToLive = i;
    }

    private void setActiveManagers(ArrayList<ResourceManager> resourceManagers) {
        this.activeManagers = resourceManagers;
    }
}