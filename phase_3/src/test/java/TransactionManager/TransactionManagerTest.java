package TransactionManager;

import org.omg.PortableInterceptor.ACTIVE;
import servercode.ResImpl.MiddlewareServerImpl;
import servercode.ResInterface.MiddlewareServer;
import servercode.ResInterface.ResourceManager;
import servercode.ResInterface.Transaction;
import servercode.TransactionManager.ActiveTransaction;
import servercode.TransactionManager.TransactionManager;
import servercode.LockManager.LockManager;

import org.junit.Assert;
import org.mockito.Mock;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;


public class TransactionManagerTest {

    MiddlewareServer mws = new MiddlewareServerImpl(new HashMap<String, ResourceManager>());


    @Test
    // should start a transaction with an incremented xid
    public void startTransaction() {


    }

}