package servercode.TransactionManager;

import java.rmi.RemoteException;
import java.util.*;

import org.omg.CORBA.DynAnyPackage.Invalid;
import org.omg.PortableInterceptor.ACTIVE;

import servercode.ResInterface.ResourceManager;
import servercode.ResInterface.Transaction;

public class TransactionManager implements Transaction {

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


    public boolean commit(int xid) throws InvalidTransactionException, TransactionAbortedException, RemoteException {
    
    	synchronized(this.activeTransactions) {
	    	
	    	
	    	System.out.println("\nCommitting transaction no: " + xid);

			// if the transaction doesn't exist in the list of active transactions, throw an exception
			if (!this.activeTransactions.containsKey(xid)) {
            	throw new InvalidTransactionException(xid, "Invalid transaction id passed for commit.");
			}

			// remove the transaction from the list of transactions a
			ActiveTransaction t = this.activeTransactions.get(xid);

			try {
        
            	// remove the transaction from the list of active transactions
            	this.activeTransactions.remove(xid);
	            
				ActiveTransaction notExist = this.activeTransactions.get(xid);
            
	            if (notExist == null) {
		            System.out.println("Active transaction removed from TM.");
	            }

	            // return the commit result from t
	            return t.commit(xid);
	            
	            
	
	        } catch(InvalidTransactionException | TransactionAbortedException | RemoteException e) {
	
	            throw e;
	
	        }

	  
    	}
    	
    }


    public void abort(int xid) throws InvalidTransactionException, RemoteException {
    
    	synchronized(this.activeTransactions) {
	    	
	    	System.out.println("\nAborting transaction no: " + xid);
		
			// if the transaction doesn't exist in the list of active transactions, throw an exception
			if (!this.activeTransactions.containsKey(xid)) {
            	throw new InvalidTransactionException(xid, "Invalid transaction id passed to TM for abort: ." + xid);
			}

			ActiveTransaction t = this.activeTransactions.get(xid);
        

			try {
        
        		t.abort(xid);
				removeActiveTransaction(xid);
	        
				ActiveTransaction notExist = this.activeTransactions.get(xid);
            
				if (notExist == null) {
	           		System.out.println("Active transaction removed from TM: " + xid);
			   	}

            

			} catch(InvalidTransactionException | RemoteException e) {
            	throw e;
			}

	    	
    	}

	}


    public boolean transactionOperation(int xid, ResourceManager rm) throws InvalidTransactionException, RemoteException {

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
    

    private boolean addActiveManager(int xid, ResourceManager rm) {
    
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
	    	 
	    	 ActiveTransaction txn = new ActiveTransaction(xid, 30000, new ArrayList<ResourceManager>());
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
