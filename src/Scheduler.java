import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Scheduler {
	private EnumMap<PriorityType, LinkedList<PCB>> queues;
	private PriorityQueue<SleepingProcess> sleepingProcesses;
	
	private HashMap<Integer, PCB> PCBmap;
	private HashMap<Integer, PCB> msgMap;
	
	private Kernel kernel;
	private Clock clock;
	private Random rand;
	public PCB currentProcess;
	
	
	private record SleepingProcess(PCB process, Instant wakeTime) {}
    
	@SuppressWarnings("serial")
	Scheduler(Kernel k){
		PCBmap = new HashMap<>();
		msgMap = new HashMap<>();
		
		queues = new EnumMap<>(PriorityType.class) {{
				put(PriorityType.INTERACTIVE, new LinkedList<>());
				put(PriorityType.BACKGROUND, new LinkedList<>());
				put(PriorityType.REAL_TIME, new LinkedList<>());
			}};
			
		sleepingProcesses = new PriorityQueue<>((a, b) -> a.wakeTime.compareTo(b.wakeTime));

		new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
            	if(currentProcess != null) {
    				currentProcess.demoteIfPriorityAbuse();	
    				currentProcess.requestStop();
    			}
            }
        }, 250, 250);
		
		rand = new Random();
		clock = Clock.systemDefaultZone(); 
		kernel = k;
	}
	
	public int createProcess(PCB process) {
		PCBmap.put(process.getPID(), process);
		getQueue(process).add(process);

		if(currentProcess == null || currentProcess.isDone()) {	 //initial process
			switchProcess();
		}
		
		return process.getPID();
	}
	
	public void switchProcess() {
		handleWakeup();

		if (currentProcess != null) {
			if (currentProcess.isDone()) { // when process dies close all devices
				PCBmap.remove(currentProcess.getPID()); // remove the closed process from pcbmap
				int[] localDevices = currentProcess.getDevices();

				for (int i = 0; i < localDevices.length; i++) {
					if (localDevices[i] != -1) kernel.close(i);
				}
				
				kernel.exitCleanup();
			} else {
				getQueue(currentProcess).add(currentProcess);
			}
		}
		currentProcess = chooseQueue().remove(); // ProcessChange: removes first -> current
	}
	
	public PCB getRandomProcess() {
		//return a random  PCB
		var values = new LinkedList<>(PCBmap.values()); 
		return values.get(rand.nextInt(values.size()));
	}
	
	private void handleWakeup() {
		for(SleepingProcess sp: sleepingProcesses) {
			if(sp.wakeTime.isBefore(clock.instant())) { // no more processes can be awoken
				break; 		
			}
			
			var pcb = sleepingProcesses.remove().process;
			getQueue(pcb).add(pcb);
		}
	}

	public LinkedList<PCB> chooseQueue() {
	    var realTimeQueue = queues.get(PriorityType.REAL_TIME);
	    var interactiveQueue = queues.get(PriorityType.INTERACTIVE);
	    var backgroundQueue = queues.get(PriorityType.BACKGROUND);

	    if (realTimeQueue.isEmpty() && interactiveQueue.isEmpty() && backgroundQueue.isEmpty()) {
	        throw new NullPointerException("No programs exist? This shouldn't be possible...");
	    }

	    if (!realTimeQueue.isEmpty() && (rand.nextInt(10) < 6)) {
	        return realTimeQueue;
	    } else if (!interactiveQueue.isEmpty()) {
	        return (rand.nextInt(4) < 3) ? interactiveQueue : backgroundQueueOrDefault(backgroundQueue, interactiveQueue);
	    } else {
	        return backgroundQueue; // Only background queue available
	    }
	}
	
	public void sleep(int ms) {
		Instant wakeup = clock.instant().plusMillis(ms);
		currentProcess.resetTimeoutCount();
		sleepingProcesses.add(new SleepingProcess(currentProcess, wakeup));
		currentProcess = null;       // proccess is sleeping so nothing is currently set
	}
	
	public int getPidByName(String s) {
		// check all processess for a match
		for(var list : queues.values()) { 
			for(var pcb : list) {
				if(pcb.getName().equals(s)) {
					return pcb.getPID();
				}
			}
		}
		// if no process found check sleeping process
		for(var sleeping : sleepingProcesses) {
			if(sleeping.process.getName().equals(s)) {
				return sleeping.process.getPID();
			}
		}
		
		for(var waiting : msgMap.values()) {
			if(waiting.getName().equals(s)) {
				return waiting.getPID();
			}
		}
	
		// no processes found return error
		return -1;
	}
	
	public void sendMsg(KernelMessage km) {
		km.senderPID = currentProcess.getPID();
		PCB target = PCBmap.get(km.targetPID);
		
		if(target != null) {
			target.msgQueue.add(km);
			
			if(msgMap.remove(km.targetPID) != null) {
				getQueue(target).add(target);
			}
		}
	}
	
	public KernelMessage waitMsg() {
		var km = currentProcess.removeMessage();
		
		if(km == null) {
			msgMap.put(currentProcess.getPID(), currentProcess);
			currentProcess = null;
			switchProcess();
		}
		return km;
	}
	
	private LinkedList<PCB> getQueue(PCB pcb){
		return queues.get(pcb != null ? pcb.getPriority() : null);
	}

	private LinkedList<PCB> backgroundQueueOrDefault(LinkedList<PCB> backgroundQueue, LinkedList<PCB> interactiveQueue) {
		//if background queue has no elements; but interactive does; default interactive
	    return interactiveQueue.isEmpty() ? backgroundQueue : interactiveQueue;	
	}
}
