
public class Tests extends UserlandProcess{
	public class Test1 extends UserlandProcess{
		int count = 0;
		@Override
		void main() {
			while(true) {
				cooperate();
				System.err.println("[REAL TIME]: I've printed " + count++ +  " times!");
				
				try {
				    Thread.sleep(50); 		
				} catch (Exception e) {}
			}
		}
	}
	
	public class Test2 extends UserlandProcess{
		int count = 0;
		@Override
		void main() {
			while(true) {
				cooperate();
				System.err.println("-[INTERACTIVE]: I've printed " + count++ +  " times!");
				
				try {
				    Thread.sleep(50); 		
				} catch (Exception e) {}
			}
		}
	}
	
	public class Test3 extends UserlandProcess{
		int count = 0;
		@Override
		void main() {
			while(true) {
				cooperate();
				System.err.println("--[BACKGROUND]: I've printed " + count++ +  " times!");
				
				try {
				    Thread.sleep(50); 		
				} catch (Exception e) {}
			}
		}
	}
	
	public class Test4 extends UserlandProcess {
	    int count = 0;

	    @Override
	    void main() {
	        while (true) {
	            cooperate();
	            System.err.println("---[REAL TIME SLEEP]: I've printed " + count++ +  " times!");
	            
	            OS.sleep(10); 
	           
	            try {
	                Thread.sleep(50); // Sleep briefly to simulate some other work
	            } catch (Exception e) {}
	        }
	    }
	}
	
	public class Test5 extends UserlandProcess{
		int count = 0;
		@Override
		void main() {
			while(true) {
				cooperate();
				System.err.println("Exiting in " + (40 - count) + " runs");
				
				if(count++ > 39) {
					OS.exit();
				}
				
				try {
				    Thread.sleep(50); 		
				} catch (Exception e) {}
			}
		}
	}
	
	
	@Override
	void main() {
		OS.createProcess(new Test1(), PriorityType.REAL_TIME);
		OS.createProcess(new Test2(), PriorityType.INTERACTIVE);
		OS.createProcess(new Test3(), PriorityType.BACKGROUND);
		OS.createProcess(new Test4(), PriorityType.REAL_TIME);
		OS.createProcess(new Test5(), PriorityType.REAL_TIME);
	}
}
