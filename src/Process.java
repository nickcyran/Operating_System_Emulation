import java.util.concurrent.Semaphore;

public abstract class Process implements Runnable{
	private boolean quantumExpired;
	private Thread thread;
	public Semaphore sem;
	
	Process(){
		quantumExpired = false;
		thread = new Thread(this, this.getClass().getName());
		sem = new Semaphore(0);
		thread.start();
	}
	
	abstract void main();
	
	public void requestStop() {
		quantumExpired = true;
	}
	
	public boolean isStopped() {
		return sem.availablePermits() == 0;
	}
	
	public boolean isDone() {
		return !thread.isAlive();
	}
	
	public void start() {
		sem.release();
	}
	
	public void stop() {
		try {
			sem.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void cooperate() {
		if(quantumExpired) {
			quantumExpired = false;
			OS.switchProcess();
		}
	}
	
	@Override
	public void run() {
		stop();
		main();
	}
}
