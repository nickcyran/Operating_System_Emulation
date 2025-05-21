import java.util.Random;

public class RandomDevice implements Device{
	private Random[] devices;
	
	RandomDevice(){
		devices = new Random[10];
	}
	
	@Override
	public int open(String s) {
		Random rand;
		
		if(s == null || s.isBlank()) {
			rand = new Random();
		}
		else {
			try {
				rand = new Random(Integer.parseInt(s));
			}
			catch(NumberFormatException e) {
				throw new NumberFormatException("open: parameter must be an int or empty");
			}
		}
		
		for(int i = 0; i < devices.length; i++) {	//find first empty, insert then return index
			if(devices[i] == null) {
				devices[i] = rand;
				return i;
			} 
		}
		return -1;									//no device slots available
	}

	@Override
	public void close(int id) {
		idInBounds(id, "close");
		devices[id] = null;
	}

	@Override
	public byte[] read(int id, int size) {
		idInBounds(id, "read");				
		Random device = devices[id];
		
		if(device != null) {					 //if the device was never opened throw error
			byte[] randomData = new byte[size];	 //fill array with random bytes and return
			device.nextBytes(randomData);
			return randomData;
		}
		
		throw new IllegalArgumentException("read: id {" + id + "} not opened");	
	}

	@Override
	public void seek(int id, int to) {
		read(id, to);				//read random bytes but not return them
	}

	@Override
	public int write(int id, byte[] data) {
		return 0;					//do nothing
	}
	
	private boolean idInBounds(int id, String type){
		if (id < 0 || id >= devices.length) {
			throw new ArrayIndexOutOfBoundsException(type + ": id {" + id + "} out of bounds for a[" + devices.length + "]");
		}
		return true;
	}
}
