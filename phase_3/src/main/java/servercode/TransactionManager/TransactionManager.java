package servercode.TransactionManager;

import java.rmi.RemoteException;
import java.util.*;

import servercode.RMEnums.RMType;

public class TransactionManager  {

    private Map<Integer, ActiveTransaction> activeTransactions;
    private int xid;

    public TransactionManager(Map<Integer, ActiveTransaction> activeTransactions) {
        setTransactionMap(activeTransactions);
        setXID();
    }

    public int start()  {

        // increment the transaction counter
        xid += 1;

        // add the transaction to the active transactions list
        addActiveTransaction(xid);

        // return the transaction id
        return xid;

    }



    public boolean transactionOperation(int xid, RMType rm) throws InvalidTransactionException, RemoteException {

    	// if we can't add the RM to the active transaction with this id, then such an active transaction does not exist
        if (!addActiveManager(xid, rm)) {
            throw new InvalidTransactionException(xid, "Invalid transaction id passed for txn operation");
        }
        
        
        synchronized(this.activeTransactions) {
	    
	    	// update the keepalive timer
			this.activeTransactions.get(xid).updateLastTransaction();

			return true;
        }

    }

	public void removeActiveTransaction(int xid) {
	
		synchronized(this.activeTransactions) {
			this.activeTransactions.remove(xid);
		}
		
	}

    public ActiveTransaction getActiveTransaction(int xid) throws InvalidTransactionException {

    	synchronized (this.activeTransactions) {

    		if (!this.activeTransactions.containsKey(xid)) {
    			throw new InvalidTransactionException(xid, "Invalid transaction passed to getActiveTransaction.");
			}

			return this.activeTransactions.get(xid);

		}
	}


	public boolean shutdown() throws RemoteException {
		return true;
	}


	public Map<Integer, ActiveTransaction> getActiveTransactions() {

		synchronized(this.activeTransactions) {

			Map<Integer, ActiveTransaction> shallowCopy = new HashMap<Integer, ActiveTransaction>();
			shallowCopy.putAll(this.activeTransactions);
			return shallowCopy;

		}

	}
    

    private boolean addActiveManager(int xid, RMType rm) {
    
    	synchronized(this.activeTransactions) {
	    	
	    	if (!this.activeTransactions.containsKey(xid)) {
            	return false;
			}

        	ActiveTransaction txn = this.activeTransactions.get(xid);
        	txn.addActiveManager(rm);
		
        	return true;
        	
    	}

        
    }

    private void addActiveTransaction(int xid) {
    
    	synchronized(this.activeTransactions) {
	    	 
	    	 ActiveTransaction txn = new ActiveTransaction(xid, 30000, new ArrayList<RMType>());
			 this.activeTransactions.put(xid, txn);

    	}
    }


    private void setTransactionMap(Map<Integer, ActiveTransaction> activeTransactions) {
        this.activeTransactions = activeTransactions;
    }


    private void setXID() {
        this.xid = 0;
    }

    
}
