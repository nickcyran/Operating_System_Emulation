
public class Hardware {
	public static final int PAGE_SIZE = 1024;
	public static final int MEMORY_SIZE = 1048576;
	public static int[][] TLB = new int[2][2];
	public static byte[] physicalMemory = new byte[MEMORY_SIZE];
	
	private Hardware() {}
	
	public static byte read(int address) {
		int virtualPage = address / PAGE_SIZE;
		int virtualOffset = address % PAGE_SIZE;
		
		int physicalPage = findMapping(virtualPage);
		
		// Mapping found => return physical memory at correct spot
		if(physicalPage != -1) {
			return physicalMemory[physicalPage * PAGE_SIZE + virtualOffset];
		}
		
		// Mapping not found
		OS.getMapping(virtualPage);
		physicalPage = findMapping(virtualPage);
		
		// Should not occur since Kernel will kill proc, but just in case
		return physicalPage != -1 ? physicalMemory[physicalPage * PAGE_SIZE + virtualOffset] : -1;
	}
	
	public static void write(int address, byte value) {
		int virtualPage = address / PAGE_SIZE;
		int virtualOffset = address % PAGE_SIZE;
		
		int physicalPage = findMapping(virtualPage);
		
		// Mapping found => assign physical memory the value
		if (physicalPage != -1) {
			physicalMemory[physicalPage * PAGE_SIZE + virtualOffset] = value;
		}
		
		OS.getMapping(virtualPage);

		physicalPage = findMapping(virtualPage);
		
		if(physicalPage != -1) {
			physicalMemory[physicalPage * PAGE_SIZE + virtualOffset] = value;
		}
	}
	
	private static int findMapping(int pageNumber) {
		for(int[] arr : TLB) {
			if(arr[0] == pageNumber) {
				return arr[1];
			}
		}
		return -1;
	}
}
