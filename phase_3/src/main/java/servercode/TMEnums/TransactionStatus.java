package servercode.TMEnums; 

import servercode.ResInterface.ResourceManager;
import servercode.ResInterface.Status;
import servercode.TransactionManager.InvalidTransactionException;
import servercode.TransactionManager.TransactionAbortedException;

import java.rmi.RemoteException;

public enum TransactionStatus implements Status { 
	
	ACTIVE {
		public boolean resolve(ResourceManager rm, int xid) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
			
			// do nothing - nothing to resolve
			return true;
			
		}
		
	}, 
	VOTE_REQUESTED {
		public boolean resolve(ResourceManager rm, int xid) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
		
			try {
			
				return rm.voteRequest(xid);
				
			} catch (RemoteException | InvalidTransactionException e) {
				
				throw(e);
				
			}
			
		}
	
	},
	COMMITTED {
		public boolean resolve(ResourceManager rm, int xid) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
				
			try {
			
				return rm.commit(xid);
				
			} catch (RemoteException | InvalidTransactionException | TransactionAbortedException e) {
				
				throw(e);
				
			}
			
		}
	
	}, 
	ABORTED {
		public boolean resolve(ResourceManager rm, int xid) throws RemoteException, InvalidTransactionException, TransactionAbortedException {
		
			try {
			
				rm.abort(xid);
				return true;
				
			} catch (RemoteException | InvalidTransactionException | TransactionAbortedException e) {
				
				throw(e);
				
			}
			
		}
	
	};
}