

public class Init extends UserlandProcess {
	public class MemoryReadWriteTest extends UserlandProcess {
	    @Override
	    void main() {
	    	// 2 pages pointer @ 0
	        int size = 2048; 
	        int pointer = OS.allocateMemory(size);
	        
	        // check for failure
	        if (pointer == -1) {
	            System.out.println("Memory allocation failed");
	            exit();
	        }

	        byte x = 12;
	        // write data into memory
	        for (int i = 0; i < size; i++) {
	            Hardware.write(pointer + i, x);
	        }

	        // check that the data aligns with expected values
	        boolean success = true;
	        for (int i = 0; i < size; i++) {
	            byte readByte = Hardware.read(pointer + i);
	            
	            if (readByte != x) {
	                success = false;
	                break;
	            }
	        }
	        
	        System.out.println("Memory Read/Write Test " + (success ? "Passed" : "Failed"));
	        System.out.println("--------------------------------------");
	        OS.freeMemory(pointer, size);  // Free allocated memory
	        exit();
	    }
	}
	
	public class MemoryExtensionTest extends UserlandProcess {
	    @Override
	    void main() {
	        int[] sizes = {1024, 2048, 3072}; 
	        int[] ptrs = new int[3];
	        boolean allAllocated = true;

	        for (int i = 0; i < sizes.length; i++) {
	            ptrs[i] = OS.allocateMemory(sizes[i]);
	            
	            if (ptrs[i] == -1) {
	                allAllocated = false;
	                break;
	            } else {
	                System.out.println("Allocated " + sizes[i] + " bytes @ " + ptrs[i]);
	            }
	        }
	        
	        System.out.println("Memory Extension Test " + (allAllocated ? "Passed" : "Failed"));
	        System.out.println("--------------------------------------");
	        
	        // free all the allocated memory
	        for (int pointer : ptrs) {
	            if (pointer != -1) {
	                OS.freeMemory(pointer, Hardware.PAGE_SIZE);
	            }
	        }
	        exit();
	    }
	}
	
	public class AccessViolationTest extends UserlandProcess {
	    @Override
	    void main() {
	        int size = 1024; 
	        int pointer = OS.allocateMemory(size);

	        if (pointer == -1) {
	            System.out.println("Memory allocation failed");
	            exit();
	        }

	        // access within bounds
	        Hardware.write(pointer, (byte) 99);
	        System.out.println("Access within bounds success! value: " + Hardware.read(pointer));

	        System.out.println("Attempting out-of-bounds access...");
            Hardware.write(pointer + size + 10, (byte) 99);  // Should cause segmentation fault
            
            
            System.err.println("I SHOULD NOT BE PRINTED");
            System.out.println("--------------------------------------");
	    }
	}
	
	public class PiggyProcess extends UserlandProcess {
		int f = 0;
		
		PiggyProcess(int j){
			f=j;
		}
        @Override
        void main() {
			int size = 100 * 1024; // Size of memory for the piggy process (100 * 1024 bytes)
			int pointer = OS.allocateMemory(size);

			// Write some data to memory
			byte x = (byte) (42 + f);
			for (int i = 0; i < size; i++) {
				Hardware.write(pointer + i, x);
			}

			// Verify data
			boolean success = true;
			for (int i = 0; i < size; i++) {
				if (Hardware.read(pointer + i) != x) {
					success = false;
					break;
				}
			}

			System.out.println("--------------------------------------\nPiggy Process Test " + (success ? "Passed" : "Failed"));

			// Free memory
			OS.freeMemory(pointer, size);
			exit();
		}
	}

	@Override
	void main() {
		OS.createProcess(new MemoryReadWriteTest());
		OS.createProcess(new MemoryExtensionTest());
		OS.createProcess(new AccessViolationTest());
		
		//Wait a few seconds to pass
		for (int i = 0; i < 20; i++) {
			OS.createProcess(new PiggyProcess(i * 3));
		}
	}
}