
public class Idle extends UserlandProcess{
	@Override
	public void main() {
		while(true) {
			cooperate();
			try { 
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
