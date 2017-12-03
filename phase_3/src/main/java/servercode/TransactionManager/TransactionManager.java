package servercode.TransactionManager;

import java.rmi.RemoteException;
import java.util.*;
import java.io.Serializable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import servercode.RMEnums.RMType;
import servercode.TMEnums.TransactionStatus;
import servercode.ResInterface.Status;

public class TransactionManager implements Serializable {

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
    
    private void writeSelf() {
    	try {
	    	FileOutputStream fos = new FileOutputStream("/tmp/comp512gr17p3.tm.ser");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this);
			oos.close();
			fos.close();
			System.out.println("Serialized data is saved in tm.ser");
    	} catch (Exception e) {
	    	System.out.println("Exception: " + e);
	    	e.printStackTrace();
    	}
    }

    private Object readSelf() {

        Object o = null;
        
        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream("/tmp/comp512gr17p3.tm.ser"))) {
            o = (Object) ois.readObject();

        } catch(Exception e) {

            System.out.println("Err: " + e);
            e.printStackTrace();

        }

        return o;

    }



    public boolean transactionOperation(int xid, RMType rm) throws InvalidTransactionException, RemoteException {

    	// if we can't add the RM to the active transaction with this id, then such an active transaction does not exist
        if (!addActiveManager(xid, rm)) {
            throw new InvalidTransactionException(xid, "Invalid transaction id passed for txn operation");
        }
        
        
        synchronized(this.activeTransactions) {
	    
	    	// update the keepalive timer
			this.activeTransactions.get(xid).updateLastTransaction();
			
			writeSelf();

			return true;
        }

    }

	public void removeActiveTransaction(int xid) {
	
		synchronized(this.activeTransactions) {
			this.activeTransactions.remove(xid);
			
			writeSelf();
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
	
	public void updateTransactionStatus(ActiveTransaction t, Status s) {
		
		t.updateStatus(s);
		writeSelf();
			
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
	    	
	    	if (!this.activeTransactions.containsKey(xid) || this.activeTransactions.get(xid) .getStatus() == TransactionStatus.ABORTED) {
            	return false;
			}

        	ActiveTransaction txn = this.activeTransactions.get(xid);
        	txn.addActiveManager(rm);
		
        	return true;
        	
    	}

        
    }

    private void addActiveTransaction(int xid) {
    
    	synchronized(this.activeTransactions) {
	    	 
	    	 ActiveTransaction txn = new ActiveTransaction(xid, 60000, new ArrayList<RMType>(), TransactionStatus.ACTIVE);
			 this.activeTransactions.put(xid, txn);
			 
			 writeSelf();
    	}
    }


    private void setTransactionMap(Map<Integer, ActiveTransaction> activeTransactions) {
        this.activeTransactions = activeTransactions;
    }


    private void setXID() {
        this.xid = 0;
    }

    
}
