package servercode.ResInterface;

import servercode.ResInterface.ResourceManager;
import servercode.TransactionManager.InvalidTransactionException;
import servercode.TransactionManager.TransactionAbortedException;

import java.rmi.RemoteException;

public interface Status {
	
	public boolean resolve(ResourceManager rm, int xid) throws RemoteException, InvalidTransactionException, TransactionAbortedException;
}