
public class Ping extends UserlandProcess{

	@Override
	void main() {
		int target = OS.getPidByName("Pong");
		System.out.println("I am PING, pong = " + target);
		
		int i = 0;
		while(true) {

			OS.sendMessage(new KernelMessage(target, i++, new byte[]{}));
			var km = waitForMessage();
			System.out.println("PING: " + km);
		}
	}
}
