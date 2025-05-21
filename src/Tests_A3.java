import java.util.Arrays;

public class Tests_A3 extends UserlandProcess{
	private static final String pass = "\u001B[32mPASS\u001B[0m"; // Green
	private static final String fail = "\u001B[31mFAIL\u001B[0m"; // Red
	
	private String test(boolean b) {
		return b ? pass : fail ;
	}
	
	public class TestRandom extends UserlandProcess{
		@Override
		void main() {
			 	System.out.println("Opening device with seed '42':");
		        int id1 = open("random 42");
		        System.out.println("Device ID: " + id1);

		        System.out.println("Reading 5 bytes from device 0:");
		        byte[] data2 = read(id1, 5);
		        System.out.println("Random Data: " + Arrays.toString(data2));

		        // Test seeking
		        System.out.println("Seeking on device 0:");
		        seek(id1, 5);  // This will read but not return data

		        // Test closing devices
		        System.out.println("Closing device 0:");
		        close(id1);
		        
		        int id2 = open("random 42");
		
		        System.out.println("Writing to device 1 (should do nothing):");
		        int writtenBytes = write(id2, new byte[] {1, 2, 3});
		        System.out.println("Written bytes count: " + writtenBytes);
		        
		        exit();
		}
	}
	
	public class TestFFS_1 extends UserlandProcess{
		@Override
		void main() {
			System.out.println("[TestFFS_1]---------------------------------------");
			
			System.out.println("--[open_tests]------------------------------------");
				int id_1 = open("file test.txt");
				System.out.println("id_1 == " + id_1 + ": " + test(id_1 == 0));
			
				int id_2 = open("file test_2.txt");
				System.out.println(("id_2 == " + id_2 + ": " + test(id_2 == 1)));
			
				// fill the array to test fail
				int id_3 = open("file 3");
				int id_4 = open("file 4");
				int id_5 = open("file 5");
				int id_6 = open("file 6");
				int id_7 = open("file 7");
				int id_8 = open("file 8");
				int id_9 = open("file 9");
				int id_10 =open("file 10");
			
				int id_fail = open("file 11");
				System.out.println(("id_fail == " + id_fail + ": " + test(id_fail == -1)));
			
			System.out.println("--[closing all but 1 and 2...]");
				close(id_3);
				close(id_4);
				close(id_5);
				close(id_6);
				close(id_7);
				close(id_8);
				close(id_9);
				close(id_10);
				
			System.out.println("--[read/write/seek_test]------------------------------------");
				System.out.println(new String(read(id_1, 30)));	
				seek(id_1, 0);
				write(id_1, "[Overwrite]".getBytes());
				seek(id_1, 24);
				write(id_1, "at end".getBytes());
				seek(id_1, 0);
				System.out.println(new String(read(id_1, 30)));	
				
				write(id_2, "Start of file 2".getBytes());
				seek(id_2, 0);
				System.out.println(new String(read(id_2, 15)));	
				close(id_1);
				close(id_2);
				
				exit();
		}
	}
	
	@Override
	void main() {
		OS.createProcess(new TestRandom());
		OS.createProcess(new TestFFS_1());
	}
}
