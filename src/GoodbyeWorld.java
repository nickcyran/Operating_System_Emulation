
public class GoodbyeWorld extends UserlandProcess{
	
	@Override
	public void main() {
		int target = OS.getPidByName("HelloWorld");
		KernelMessage send = new KernelMessage(target, 0, new byte[]{});
		
		while(true) {

			waitForMessage();
	
			OS.sendMessage(send);
			System.out.println("Goodbye world...");
		}
	}
}
