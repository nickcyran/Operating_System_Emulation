import java.util.Arrays;
import java.util.LinkedList;

enum PriorityType {
	REAL_TIME, INTERACTIVE, BACKGROUND
}

public class PCB {
	public static int nextPID;
	
	private int PID;
	private int timeoutCount;
	private int[] devices;
	
	private String name;
	private UserlandProcess ULP;
	private PriorityType priority;
	public LinkedList<KernelMessage> msgQueue;
	public VirtualToPhysicalMapping[] pageMap;

	static {
		nextPID = 0;
	}
	
	public void printList() {
		System.out.println("msgs(" + name +"): " + msgQueue.toString());
	}
	
	PCB (UserlandProcess up) {					//old calls still work with a default to Interactive priority. 
		this(up, PriorityType.INTERACTIVE);
	}
	
	PCB (UserlandProcess up, PriorityType pt) {
		timeoutCount = 0;
		PID = nextPID++;
		ULP = up;
		priority = pt;
		
		devices = new int[10];
		Arrays.fill(devices, -1);
		name = ULP.getClass().getSimpleName();
		msgQueue = new LinkedList<>();
		
		pageMap = new VirtualToPhysicalMapping[100];
	}
	
	public void stop() {
		ULP.stop();
		
		while(!ULP.isStopped()) {
			ULP.stop();
			
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean isDone() {
		return ULP.isDone();
	}
	
	public void start() {
		ULP.start();
	}
	
	public void requestStop() {
		ULP.requestStop();
	}
	
	public UserlandProcess getUP() {
		return ULP;
	}
	
	public PriorityType getPriority() {
		return priority;
	}
	
	public void setPriority(PriorityType pt) {
		priority = pt;
	}
	
	public int getPID() {
		return PID;
	}

	public void demoteIfPriorityAbuse() {
		if(timeoutCount > 4) {
			timeoutCount = 0;
			
			priority = switch(priority) {
				case REAL_TIME-> PriorityType.INTERACTIVE;
				case INTERACTIVE-> PriorityType.BACKGROUND;
				case BACKGROUND -> PriorityType.BACKGROUND;
			};
		}
		timeoutCount++;
	}
	
	public KernelMessage removeMessage() {
		if(!msgQueue.isEmpty()) {
			return msgQueue.remove();
		}
		return null;
	}
	
	public void addMessageToQueue(KernelMessage km) {
		msgQueue.add(km);
	}
	
	public void resetTimeoutCount() {
		timeoutCount = 0;
	}
	
	public int[] getDevices() {
		return devices;
	}
	
	@Override
	public String toString() {
		return "[PID: " + PID + ";Priority: " + priority +"; " + ULP + "]";
	}

	public String getName() {
		return name;
	}
}
