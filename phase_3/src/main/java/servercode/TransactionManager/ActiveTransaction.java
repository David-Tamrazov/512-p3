package servercode.TransactionManager;

import java.rmi.RemoteException;
import java.util.*;
import java.io.Serializable;

import servercode.RMEnums.RMType;
import servercode.ResInterface.Status;

public class ActiveTransaction implements Serializable {

    // list of managers active in this transaction
    private List<RMType> activeManagers;

    // how long this transaction has to live 
    private int timeToLive;
    private int xid;
    private Date lastTransactionTime;
    private Status status;

    public ActiveTransaction(int xid, int timeToLive, List<RMType> resourceManagers, Status s) {
        setXID(xid);
        setTimeToLive(timeToLive);
        setActiveManagers(resourceManagers);
        setLastTransactionTime(new Date());
        setStatus(s);
    }

    public void addActiveManager(RMType manager) {

        // if the manager isn't already recorded as an active manager, add them to the list 
        if (!this.activeManagers.contains(manager)) {
            this.activeManagers.add(manager);
        }

    }


    
    public boolean shutdown() throws RemoteException {
	    
	    return true;
	    
    }

    public List<RMType> getResourceManagers() {

        List<RMType> clone = new ArrayList<RMType>();
        for (RMType t : this.activeManagers) clone.add(t);
        return clone;

    }

    public Date getLastTransationTime() {
        return this.lastTransactionTime;
    }
    
    public Status getStatus() {
	    return this.status;
    }
    
    public void updateStatus(Status s) {
	    setStatus(s);
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
    
    private void setStatus(Status s) {
	    this.status = s;
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

    private void setActiveManagers(List<RMType> resourceManagers) {
        this.activeManagers = resourceManagers;
    }
}