
public abstract class UserlandProcess extends Process{
	protected int open(String s) {
		return OS.open(s);
	}
	
	protected void close(int id) {
		OS.close(id);
	}
	
	protected byte[] read(int id, int size) {	
		return OS.read(id, size);
	}


	protected void seek(int id, int to) {
		OS.seek(id, to);		
	}

	protected int write(int id, byte[] data) {
		return OS.write(id, data);
	}
	
	protected void exit() {
		OS.exit();
	}
	
	protected KernelMessage waitForMessage() {
		KernelMessage km;
		
		while((km = OS.waitForMessage()) == null) {
			try {
			    Thread.sleep(10); 		
			} catch (Exception e) { 
				e.printStackTrace();
			}
		}
		return km;
	}
}
