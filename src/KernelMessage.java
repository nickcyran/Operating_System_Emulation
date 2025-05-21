
public class KernelMessage {
	public int senderPID;
	public int targetPID;
	private int what;
	
	private byte[] data;
	
	KernelMessage(int target, int w, byte[] d){
		targetPID = target;
		what = w;
		data = d;
	}
	
	KernelMessage(KernelMessage km){
		senderPID = km.senderPID;
		targetPID = km.targetPID;
		what = km.what;
		data = km.data;
	}
	
	public void setSenderPID(int pid) {
		senderPID = pid;
	}
	
	public int getTargetPID() {
		return targetPID;
	}
	
	@Override
	public String toString() {
		return "from: " + senderPID + " to: " + targetPID + " what: " + what;
	}
}
