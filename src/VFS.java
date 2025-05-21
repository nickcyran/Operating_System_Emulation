import java.util.HashMap;

public class VFS implements Device{
	private record DeviceConnection (Device device, int local_id) {}
	
	private HashMap<String, Device> systemDeviceMap;
	private DeviceConnection[] devices;
	
	@SuppressWarnings("serial")
	VFS(){
		devices = new DeviceConnection[20];	
		systemDeviceMap = new HashMap<String,Device>(){{
			put("random", new RandomDevice());
			put("file", new FakeFileSystem());
		}};
	}
	
	@Override
	public int open(String s) {
		String[] strings = s.split(" ");
		if(strings.length != 2) {	//all must take 2 arguments
			throw new IllegalArgumentException("open: must be given a device type");
		}
		
		Device device = systemDeviceMap.get(strings[0].toLowerCase());
		if(device == null) { 
			return -1; 	//fail: unrecognized device
		}
									
		int deviceLocalID = device.open(strings[1]);
			
		if(deviceLocalID != -1) { 						//device could not open
			for(int i = 0; i < devices.length; i++) {	//find open deviceConnection slot
				if(devices[i] == null) {					
					devices[i] = new DeviceConnection(device, deviceLocalID);
					return i;
				}
			}
		}
		return -1;		//fail: if the device is unrecognized, or could not open
	}

	@Override
	public void close(int id) {
		Device device = devices[id].device;
		
		device.close(devices[id].local_id);	//close the instance in the device
		devices[id] = null;					//remove the vfs entry
	}

	@Override
	public byte[] read(int id, int size) {
		Device d = devices[id].device;
		return d.read(devices[id].local_id, size);
	}

	@Override
	public void seek(int id, int to) {
		Device d = devices[id].device;
		d.seek(devices[id].local_id, to);
	}

	@Override
	public int write(int id, byte[] data) {
		Device d = devices[id].device;
		return d.write(devices[id].local_id, data);
	}

}
