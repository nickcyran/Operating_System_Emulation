import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Stack;

enum CallType {
	CREATE_PROCESS, SWITCH_PROCESS, SLEEP, EXIT, OPEN, CLOSE, READ, WRITE, SEEK, GET_PID, GET_PID_BY_NAME, SEND_MSG, WAIT_MSG, GET_MAPPING, ALLOCATE_MEMORY, FREE_MEMORY
}

public class OS {
	private static Kernel kernel;
	
	public static CallType currentCall;
	public static final ArrayList<Object> parameters;
	public static Object returnValue;
	
	static {
		parameters = new ArrayList<>();
	}
	
	private OS() {}
	
	public static void startup(UserlandProcess init) {
		kernel = new Kernel();
		createProcess(init);
		createProcess(new Idle());
	}
	
	public static int createProcess(UserlandProcess up) {
		return createProcess(up, PriorityType.INTERACTIVE);
	}
	
	public static int createProcess(UserlandProcess up, PriorityType pt) {
		invokeKernel(CallType.CREATE_PROCESS, up, pt);
		
		while(returnValue == null) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		return (int) returnValue();
	}
	
	public static int allocateMemory(int size) {
		if(size % Hardware.PAGE_SIZE != 0) {
			return -1;
		}
		
		invokeKernel(CallType.ALLOCATE_MEMORY, size);
		return (int) returnValue();
	}
	
	public static boolean freeMemory(int pointer, int size) {
		if(size % Hardware.PAGE_SIZE != 0 || pointer % Hardware.PAGE_SIZE != 0) {
			return false;
		}
		
		invokeKernel(CallType.FREE_MEMORY, pointer, size);
		return (boolean) returnValue();
	}
	
	public static void getMapping(int virtualPageNumber) {
		invokeKernel(CallType.GET_MAPPING, virtualPageNumber);
	}
	
	public static void switchProcess() {
		invokeKernel(CallType.SWITCH_PROCESS);
	}
	
	public static void exit() {
		invokeKernel(CallType.EXIT);
	}

	public static void sleep(int ms) {    
		invokeKernel(CallType.SLEEP, ms);
	}

	public static int open(String s) {
		invokeKernel(CallType.OPEN, s);
		return (int) returnValue();
	}

	public static void close(int id) {
		invokeKernel(CallType.CLOSE, id);
	}

	public static byte[] read(int id, int size) {
		invokeKernel(CallType.READ, id, size);
		return (byte[]) returnValue();
	}
	
	public static void seek(int id, int to) {
		invokeKernel(CallType.SEEK, id, to);
	}

	public static int write(int id, byte[] data) {
		invokeKernel(CallType.WRITE, id, data);
		return (int) returnValue();
	}
	
	public static void sendMessage(KernelMessage km) {
		invokeKernel(CallType.SEND_MSG, km);
	}
	
	public static KernelMessage waitForMessage() {
		invokeKernel(CallType.WAIT_MSG);
		return (KernelMessage) returnValue();
	}

	public static int getPID() {
		invokeKernel(CallType.GET_PID);
		return (int) returnValue();
	}
	
	public static int getPidByName(String s) {
		invokeKernel(CallType.GET_PID_BY_NAME, s);
		return (int) returnValue();
	}
	
	private static void invokeKernel(CallType x, Object... o) {
		parameters.clear();

		if (o != null && o.length > 0) {
			parameters.addAll(Arrays.asList(o));
		}

		currentCall = x;

		var currentP = kernel.getCurrentProcess();
		kernel.start();

		if (currentP != null) {
			currentP.getUP().stop();
		}
	}
	
	private static Object returnValue() {
		Object value = returnValue;	

//		returnValue = null;			//clears the return value when done
		return value;
	}
}
