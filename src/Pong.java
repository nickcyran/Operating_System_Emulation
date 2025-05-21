
public class Pong extends UserlandProcess{

	@Override
	void main() {
		int target = OS.getPidByName("Ping");
		System.out.println("I am PONG, ping = " + target);

		int i = 0;
		while(true) {
			var km = waitForMessage();
			System.out.println("PONG: " + km);
			OS.sendMessage(new KernelMessage(target, i++, new byte[]{}));
		}
	}
}
