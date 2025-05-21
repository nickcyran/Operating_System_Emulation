
public class HelloWorld extends UserlandProcess{
	
	@Override
	public void main() {
		int target = OS.getPidByName("GoodbyeWorld");
		KernelMessage send = new KernelMessage(target, 0, new byte[]{});
		
		while(true) {

			OS.sendMessage(send);
			waitForMessage();
			System.out.println("Hello World!"); 

		}
	}
}
