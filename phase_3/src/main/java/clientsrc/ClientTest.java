package clientsrc;

import servercode.ResInterface.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RMISecurityManager;

import java.util.*;
import java.util.concurrent.*;

import java.io.*;



class ClientTest implements Callable<List<Float>> {
	
	public int transactionTime;
	public int transactions;
	public int load;
	public int numTransactions;
	public int numClients;
	public boolean testOneRM;
	public MiddlewareServer mws;
	
	public ClientTest(MiddlewareServer mws, int load, int nuClients, int transactionTime, int transactions) {
		this.transactionTime = transactionTime;
		this.load = load;
		this.numClients = nuClients;
		this.testOneRM = true;
		this.mws = mws;
		this.transactions = transactions;
		
	}
	
	public List<Float> call() {

			List<Float> results = new ArrayList<Float>();
			results.add(new Float(0));
			results.add(new Float(0));
			results.add(new Float(0));
			results.add(new Float(0));
			

			Future<Float> result = null;
									
			try {
				ExecutorService worker = Executors.newSingleThreadExecutor();
			
				result = worker.submit(new Callable<Float>(){
					public Float call() throws Exception {
					  return new Float(testWrite());
					}
				});
				
				results.set(0, result.get());
							
				result = worker.submit(new Callable<Float>(){
					public Float call() throws Exception {
					  return new Float(testRead());
					}
				});
				
				results.set(1, result.get());
				
				this.testOneRM = false;
				
				result = worker.submit(new Callable<Float>(){
					public Float call() throws Exception {
					  return new Float(testWrite());
					}
				});
				
				results.set(2, result.get());
							
				result = worker.submit(new Callable<Float>(){
					public Float call() throws Exception {
					  return new Float(testRead());
					}
				});
				
				results.set(3, result.get());
				
				worker.shutdown();
				
			} catch (Exception e) {
				System.out.println(e);
			}
			
			return results;
							
	}
	
	public float testRead() {
	
		float avgTime = 0;
		float j = 0;
		
		while (j < transactions) {
			
			float readTime = responseTimeRead();
			
			avgTime += readTime;
			
/* 			if(j % 100 == 0) { System.out.println(readTime); } */
		
			j+= 1;
			
			// this.transactionTime - 2*readTime sleep
			float x = this.transactionTime - readTime;
			int sleep = (int) (readTime > this.transactionTime ? transactionTime : this.transactionTime - readTime + (((2 * Math.random()) - 1) * x) );
			try {
				
				Thread.sleep(sleep);
				
			} catch (Exception e) {
/* 				System.out.println("Quitting read test early due to exception: " + e); */
/* 				e.printStackTrace(); */
/* 				break; */
				
			}

		}
		
	
		return avgTime / j;
	}
	
	public float testWrite() {
		
		
		float avgTime = 0;
		float j = 0;
		
		while (j < transactions) {
			
			float writeTime = responseTimeWrite();
			
			avgTime += writeTime;
			
/* 			if(j % 100 == 0) { System.out.println(writeTime); } */
		
			j+= 1;
			
			float x = this.transactionTime - writeTime;
			int sleep = (int) (writeTime > this.transactionTime ? transactionTime : this.transactionTime - writeTime + (((2 * Math.random()) - 1) * x) );

			try {
				
				Thread.sleep(sleep);
				
			} catch (Exception e) {
				
				System.out.println("Quitting write test early due to exception: " + e);
				break;
				
			}

		}
		
		return avgTime / j;
	}
	

	
	private float responseTimeRead () {
	
		Date start = new Date();
		
		
		Random rand = new Random();
		
		try {
	
			int xid = mws.start();
	
			for(int i = 0; i < 10; i++) {
				
				int item_id = (int)(Math.random() * (transactions / 2) + 1);
		
				if(testOneRM) {
								
					switch((int) (Math.random() * 3)) {

						case 0: mws.queryFlight(xid, item_id); break;
						case 1: mws.queryCars(xid, "" + item_id); break;
						case 2: mws.queryRooms(xid, "" + item_id); break;
						
					}
					
				} else {
				
					int customerId = (int)(2 * Math.random() * transactions * numClients) + 1;
										
					mws.queryCustomerInfo(xid, customerId);
					
				}
			}
			
			mws.commit(xid);
			
		} catch (Exception e) {
			
/* 			Date end = new Date(); */
/* 			System.out.println("Ending testing early: exception thrown " + e.getStackTrace()[0]); */
/* 			e.printStackTrace(); */
/* 			return (float)(end.getTime() - start.getTime()); */
			
		}
		
						
		Date end = new Date();
		
		return (float)(end.getTime() - start.getTime());
	}
	
	private float responseTimeWrite () {

		Date start = new Date();
		Date end;
						
		try {
				
			int xid = mws.start();
			
			
			int item_id = (int)(Math.random() * (transactions/2) + 1);
			
			if(testOneRM) {
				for(int i = 0; i < 10; i++) {
				
					switch((int) (Math.random() * 3)) {
						case 0: mws.addFlight(xid, item_id, 5, 100); break;
						case 1: mws.addCars(xid, "" + item_id, 5, 100); break;
						case 2: mws.addRooms(xid, "" + item_id, 5, 100); break;
					}
				}
				
			} else {
				
				int customerId = (int)(2 * Math.random() * transactions * numClients) + 1;
				mws.newCustomer(xid, customerId);
				
				Vector flights = new Vector();
				flights.add(item_id);
				
				mws.itinerary(xid, customerId, flights, "" + item_id, (Math.random() >= 0.5), (Math.random() >= 0.5));	
				
			}
			
			
			mws.commit(xid);
				
		} catch (Exception e) {
/* 			end = new Date(); */
/* 			System.out.println("Ending testing early: exception thrown."); */
/* 			return (float)(end.getTime() - start.getTime()); */
		}
		
		end = new Date();
		return (float)(end.getTime() - start.getTime());
		
		
		
	}
	
	
	public static void main(String [] args) {
	
		int port = 1738;
		String server = "";
		int minload = 1;
		int maxload = 10;
		int nuClients = 1;
		int transactions = 1000;

        if (args.length == 5) {
        
            server = args[0];
            minload = Integer.parseInt(args[1]);
            maxload = Integer.parseInt(args[2]);
            nuClients = Integer.parseInt(args[3]);
            transactions = Integer.parseInt(args[4]);
            
        } else {
	        
	        System.out.println("Invalid number of arguments. Proper use is: javac ClientTest --mws server machine-- --min load-- --max load-- --nu clients-- --transactions--");
	        System.exit(-1);
        }

        
        try {
            // get a reference to the mwsregistry
            Registry registry = LocateRegistry.getRegistry(server, port);
           
            // get the proxy and the remote reference by mwsregistry lookup
            MiddlewareServer mws = (MiddlewareServer) registry.lookup("Gr17MiddlewareServer");


            if (mws == null) {
                System.out.println("Unsuccessful mws connect.");
                System.exit(-1);
            }
            
            
            
			for(int load = minload; load <= maxload; load++) {
			
				System.out.println("Testing | Load: " + load + " txns/sec, Clients: " + nuClients);
				System.out.println("-----------------");
				
			    int transactionTime = (int) ((nuClients / load) * 1000);
			    
			    ExecutorService workers = Executors.newCachedThreadPool();
				
				Collection<Callable<List<Float>>> tasks = new ArrayList<Callable<List<Float>>>();
				
				
				for (int i = 0; i < nuClients; i++) {
				
					ClientTest t = new ClientTest(mws, load, nuClients, transactionTime, transactions);
					tasks.add(t);
									
				}
				
				List<Future<List<Float>>> futures = new ArrayList<>();
				
				for(Callable task: tasks){
				    futures.add(workers.submit(task));
				}
								
				float writeOneRoundTrip = 0;
				float readOneRoundTrip = 0;
				float writeThreeRoundTrip = 0;
				float readThreeRoundTrip = 0;
				
				
				for(Future<List<Float>> future : futures) {
										
					writeOneRoundTrip += ((List<Float>) future.get()).get(0);
					readOneRoundTrip += ((List<Float>) future.get()).get(1);
					writeThreeRoundTrip += ((List<Float>) future.get()).get(2);
					readThreeRoundTrip += ((List<Float>) future.get()).get(3);
									
				}
								
				writeOneRoundTrip = writeOneRoundTrip / nuClients;
				readOneRoundTrip = readOneRoundTrip / nuClients;
				writeThreeRoundTrip = writeThreeRoundTrip / nuClients;
				readThreeRoundTrip = readThreeRoundTrip / nuClients;
				
				System.out.print("Write-only 1 RM round-trip time: ");
				System.out.println(writeOneRoundTrip + " ms");
				System.out.print("Read-only 1 RM round-trip time: ");
				System.out.println(readOneRoundTrip + " ms\n\n");
				System.out.print("Write-only 3 RM round-trip time: ");
				System.out.println(writeThreeRoundTrip + " ms");
				System.out.print("Read-only 3 RM round-trip time: ");
				System.out.println(readThreeRoundTrip + " ms\n\n");
				
				workers.shutdown();
			}
			
			
			System.out.println("Success! Bye!");
			System.exit(0);
            
            
            
            
            

        } catch (Exception e) {    
            e.printStackTrace();
        }
        
		
	}
	
}