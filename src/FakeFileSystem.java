import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FakeFileSystem implements Device {
	private RandomAccessFile[] files;

	public FakeFileSystem() {
		files = new RandomAccessFile[10];
	}

	@Override
	public int open(String filename) {
		if (filename == null || filename.isEmpty()) {
			throw new IllegalArgumentException("open: parameter must be a string");
		}

		for (int i = 0; i < files.length; i++) {
			if (files[i] == null) {
				try {
					files[i] = new RandomAccessFile(filename, "rwd");
					return i; // Return the index as the ID
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					return -1; // Indicate failure
				}
			}
		}
		return -1; // No available slots
	}

	@Override
	public void close(int id) {
		idInBounds(id, "close");

		try {
			if (files[id] != null) {
				files[id].close();
				files[id] = null; 		//clear the closed file from the array
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public byte[] read(int id, int size) {
		idInBounds(id, "read");
		byte[] buffer = new byte[size];
		try {
			if (files[id] != null) {
				files[id].read(buffer);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buffer;
	}

	@Override
	public void seek(int id, int to) {
		idInBounds(id, "seek");
		try {
			if (files[id] != null) {
				files[id].seek(to);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int write(int id, byte[] data) {
		idInBounds(id, "write");
		try {
			if (files[id] != null) {
				files[id].write(data);
				return data.length; 	//return number of bytes written
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0; 		//no bytes written
	}

	private boolean idInBounds(int id, String type) {
		if (id < 0 || id >= files.length) {
			throw new ArrayIndexOutOfBoundsException(
					type + ": id {" + id + "} out of bounds for a[" + files.length + "]");
		}
		return true;
	}
}