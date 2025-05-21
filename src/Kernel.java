import java.util.Arrays;
import java.util.Random;

public class Kernel extends Process implements Device{
	private boolean[] pagesInUse;
	private int nextPageToWriteOut;
	private int swapfileID;
	
	private Scheduler scheduler;
	private VFS vfs;
	private Random random;
	
	Kernel() {
		scheduler = new Scheduler(this);
		vfs = new VFS();
		swapfileID = vfs.open("file swapfile");
		nextPageToWriteOut = 0;
		
		random = new Random();
		pagesInUse = new boolean[Hardware.MEMORY_SIZE / Hardware.PAGE_SIZE];
	}

	@Override
	void main() { 
		while (true) {
			switch (OS.currentCall) {
				case CREATE_PROCESS -> setCreateProcess(scheduler);
				case SWITCH_PROCESS -> switchProcess();
				case SLEEP -> sleep((Integer) OS.parameters.get(0));
				case EXIT -> exit();
				case OPEN -> setReturnValue(open((String) OS.parameters.get(0)));
				case CLOSE -> close((int) OS.parameters.get(0));
				case READ -> setReturnValue(read((int)OS.parameters.get(0), (int)OS.parameters.get(1)));
				case SEEK -> seek((int)OS.parameters.get(0), (int)OS.parameters.get(1));
				case WRITE -> setReturnValue(write((int)OS.parameters.get(0), (byte[])OS.parameters.get(1)));
				case GET_PID -> getPID();
				case GET_PID_BY_NAME -> getPidByName((String) OS.parameters.get(0));
				case SEND_MSG -> sendMessage((KernelMessage) OS.parameters.get(0));
				case WAIT_MSG -> waitForMessage();
				case GET_MAPPING -> getMapping((int) OS.parameters.get(0));
				case ALLOCATE_MEMORY -> allocateMemory((int) OS.parameters.get(0));
				case FREE_MEMORY -> freeMemory((int) OS.parameters.get(0), (int) OS.parameters.get(1));
				default -> throw new IllegalArgumentException("Unexpected value: " + OS.currentCall);
			}
			
			scheduler.currentProcess.start();
			stop();
		}
	}
	
	public void freeMemory(int pointer, int size) {
		int virtualPagePointer = pointer / Hardware.PAGE_SIZE;
		int pagesToFree = size / Hardware.PAGE_SIZE;
		VirtualToPhysicalMapping[] pcbPageMap = scheduler.currentProcess.pageMap;

		if (!isFreeLegal(virtualPagePointer, pagesToFree, pcbPageMap)) {
			OS.returnValue = false;
		} else {
			for (int i = virtualPagePointer, pagesFreed = 0; i < pcbPageMap.length; i++, pagesFreed++) {
				if (pagesFreed == pagesToFree) {
					OS.returnValue = true;
					break;
				}

				VirtualToPhysicalMapping mapping = pcbPageMap[i];
				if (mapping != null && mapping.physicalPageNumber != -1) {
					// free the page in use
					pagesInUse[mapping.physicalPageNumber] = false;
				}

				// free the virtual mapping
				pcbPageMap[i] = null;
			}
		}
	}

	public boolean isFreeLegal(int vpPtr, int pagesToFree, VirtualToPhysicalMapping[] pageMap) {
	    for (int i = vpPtr, count = 0; i < pageMap.length; i++, count++) {
	        if (count == pagesToFree) {
	            return true;
	        }

	        // Ensure the mapping exists and is valid
	        VirtualToPhysicalMapping mapping = pageMap[i];
	        if (mapping == null || mapping.physicalPageNumber == -1 || !pagesInUse[mapping.physicalPageNumber]) {
	            return false;
	        }
	    }
	    return true;
	}

	public void allocateMemory(int size) {
		int pagesToAdd = size / Hardware.PAGE_SIZE;
		int contiguousPages = 0;
		int pointer = -1;

		VirtualToPhysicalMapping[] pcbPageMap = scheduler.currentProcess.pageMap;

		for (int i = 0; i < pcbPageMap.length; i++) {
			if (pcbPageMap[i] != null) {
				contiguousPages = 0;
				pointer = -1;
			} else {
				if (contiguousPages == 0) {
					pointer = i;
				}

				contiguousPages++;

				if (contiguousPages == pagesToAdd) {
					// allocate physical pages
					int[] physicalPages = pagesToUse(pagesToAdd);

					// if there is not enough physical mapping space then return a failure
					if (physicalPages.length != pagesToAdd) {
						pointer = -1;
					} else {
						for (int j = pointer, x = 0; j < pointer + pagesToAdd; j++, x++) {
							pcbPageMap[j] = new VirtualToPhysicalMapping();
							pcbPageMap[j].physicalPageNumber = physicalPages[x];
						}
					}
					break;
				}
			}
		}

		// if there is not enough pages at end of the array then fail
		OS.returnValue = (contiguousPages == pagesToAdd) ? pointer * Hardware.PAGE_SIZE : -1;
	}
	
	private int[] pagesToUse(int size) {
		int count = 0;
		int[] pagesFound = new int[size];
		
		for(int i = 0; i < pagesInUse.length; i++) {
			if(count == size) {
				break;
			}
			
			if(!pagesInUse[i]) {
				pagesFound[count] = i;
				count++;
			}
		}
		
		if(count == size) {
			for(int i = 0; i < pagesFound.length; i++) {
				pagesInUse[pagesFound[i]] = true;
			}
		}
	
		return pagesFound;
	}
	
	public void switchProcess() {
		// clear the TLB on switch
		for(int i = 0; i < Hardware.TLB.length; i++) {
			for(int j = 0; j < Hardware.TLB[i].length; j++) {
				Hardware.TLB[i][j] = -1;
			}
		}
		
		scheduler.switchProcess();
	}
	
	private byte[] getPageData(VirtualToPhysicalMapping mapping) {
		byte[] data = new byte[Hardware.PAGE_SIZE];
		
		for(int i = 0; i < Hardware.PAGE_SIZE; i++) {
			data[i] = Hardware.physicalMemory[(mapping.physicalPageNumber * Hardware.PAGE_SIZE) + i];
		}
		
		return data;
	}
	
	private void populatePhysicalMemory(int start, byte[] data) {
		for(int i = 0; i < Hardware.PAGE_SIZE; i++) {
			Hardware.physicalMemory[start + i] = data[i];
		}
	}
	
	private void getMapping(int virtualPageNumber) {
		VirtualToPhysicalMapping mapping = scheduler.currentProcess.pageMap[virtualPageNumber];
	
		
		if (mapping != null) {
			int physicalPageNumber = findAvailablePage();
		
			// New physical page found
			if (physicalPageNumber != -1) {
				mapping.physicalPageNumber = physicalPageNumber;
		
				// all 0s => data doesnt exist then populate memory w 0s
				byte[] pageData = new byte[Hardware.PAGE_SIZE];
				
				// Data exists on disk
				if(mapping.onDiskPageNumber != -1) {
					// load old data in and populate physical page
					vfs.seek(swapfileID, mapping.onDiskPageNumber * Hardware.PAGE_SIZE);
					pageData = vfs.read(swapfileID, Hardware.PAGE_SIZE);
				}
				
				populatePhysicalMemory(mapping.physicalPageNumber * Hardware.PAGE_SIZE, pageData);
		
			}
			// No page available => start page swap
			else {
				PCB victim = null;
				VirtualToPhysicalMapping victimPage = null;

				// Pick random proc -> repeat until physical memory found
				while (victimPage == null) {
					victim = scheduler.getRandomProcess();
					victimPage = findUtilizedPage(victim);
				}
				
				// Assign a new block of the swap file if they didnt have one already
				if (victimPage.onDiskPageNumber == -1) {
					victimPage.onDiskPageNumber = nextPageToWriteOut;
					nextPageToWriteOut++;
				}
				
				// Write the victim page to disk
				byte[] victimData = getPageData(victimPage);
				vfs.seek(swapfileID, nextPageToWriteOut);
				
				vfs.write(swapfileID, victimData);
				
				// Set currentProc physical page to victims old page
				int physicalPageIndex = victimPage.physicalPageNumber;
				victimPage.physicalPageNumber = -1;
				mapping.physicalPageNumber = physicalPageIndex;
			}
		}
		else {
			exit();	// kill and switch currentProcess
			System.out.println("!---- SEGMENTATION FAULT ----!");
			return;
		}
		
		
		int indexTLB = random.nextInt(2);
		Hardware.TLB[indexTLB][0] = virtualPageNumber;
		Hardware.TLB[indexTLB][1] = mapping.physicalPageNumber;
	}
	
	private VirtualToPhysicalMapping findUtilizedPage(PCB victim) {
		VirtualToPhysicalMapping[] pageMap = victim.pageMap;
		
		// Find a page in the processthat has physical memory
		for(int i = 0; i < pageMap.length; i++) {
			if(pageMap[i] != null && pageMap[i].physicalPageNumber != -1) {
				return pageMap[i];
			}
		}
		return null;
	}
	
	private int findAvailablePage() {
	    for (int i = 0; i < pagesInUse.length; i++) {
	        if (!pagesInUse[i]) {
	            pagesInUse[i] = true;
	            return i;
	        }
	    }
	    return -1; 
	}
	
	public void exitCleanup() {
		VirtualToPhysicalMapping[] pageMap = scheduler.currentProcess.pageMap;
		for(int i = 0; i < pageMap.length; i++) {
			VirtualToPhysicalMapping mapping = pageMap[i];
			
			if(mapping != null) {
				// unallocate physical pages
				if(mapping.physicalPageNumber != -1) {
					pagesInUse[mapping.physicalPageNumber] = false;
				}
				
				
				
				pageMap[i] = null;
			}
		}
		scheduler.currentProcess = null;
	}
	
	public void exit() {
		exitCleanup();
		switchProcess();
	}

	public void sleep(int ms) {
		scheduler.sleep(ms);
		switchProcess();
	}
	
	public PCB getCurrentProcess() {
		return scheduler.currentProcess;
	}

	@Override
	public int open(String s) {
		int[] localDevices = scheduler.currentProcess.getDevices();
		
		for(int i = 0; i < localDevices.length; i++) {
			if(localDevices[i] == -1) {	//open slot found
				int id = vfs.open(s);	//open device
				
				if(id == -1) {			//if vfs failed this fails
					return -1;
				}
				
				localDevices[i] = id;	//set pcb entry to id
				return i;				//return pcb entry index
			}
		}
		return -1; 						//no open slots found: fail
	}

	@Override
	public void close(int id) {
		int[] localDevices = scheduler.currentProcess.getDevices();

		vfs.close(localDevices[id]);	//close the device in vfs
		localDevices[id] = -1;			//remove device from PCB array
	}

	@Override
	public byte[] read(int id, int size) {
		int[] localDevices = scheduler.currentProcess.getDevices();
		return	vfs.read(localDevices[id], size);
	}

	@Override
	public void seek(int id, int to) {
		int[] localDevices = scheduler.currentProcess.getDevices();
		vfs.seek(localDevices[id], to);
	}

	@Override
	public int write(int id, byte[] data) {
		int[] localDevices = scheduler.currentProcess.getDevices();
		return vfs.write(localDevices[id], data);
	}
	
	public void sendMessage(KernelMessage km) {
		scheduler.sendMsg(new KernelMessage(km));
	}
	
	public void waitForMessage() {
		OS.returnValue = scheduler.waitMsg();
	}
	
	public void getPID() {
		OS.returnValue = scheduler.currentProcess.getPID();
	}
	
	public void getPidByName(String s) {
		OS.returnValue = scheduler.getPidByName(s);
	}
	
	/*
	 * NOTE: these synchronized static methods are not necessary as 2 threads will not be
	 * active at the same time; however stylistically this is the correct choice
	 * when mutating a static member
	 */
	private static synchronized void setCreateProcess(Scheduler sched) {
		UserlandProcess process = (UserlandProcess) OS.parameters.get(0);
	
		PCB pcb = (OS.parameters.size() > 1) ? new PCB(process, (PriorityType) OS.parameters.get(1)) : new PCB(process);
		OS.returnValue = sched.createProcess(pcb);
	}
	
	private static synchronized void setReturnValue(Object obj) {
		OS.returnValue = obj;
	}
}
