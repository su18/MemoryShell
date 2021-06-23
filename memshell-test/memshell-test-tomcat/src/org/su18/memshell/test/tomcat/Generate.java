package org.su18.memshell.test.tomcat;

import sun.misc.BASE64Encoder;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author su18
 */
public class Generate {

	public static void main(String[] args) throws IOException {
		FileInputStream fileInputStream = new FileInputStream("/Users/phoebe/IdeaProjects/MemoryShell/out/production/memshell-test-glassfish/org/su18/memshell/test/glassfish/TestGrizzlyFilter.class");
		byte[]          bytes           = toByteArray(fileInputStream);
		BASE64Encoder   encoder         = new BASE64Encoder();
		System.out.println(encoder.encode(bytes).replace("\n", ""));


	}

	public static byte[] toByteArray(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[]                buffer = new byte[10240];
		int                   n      = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
		return output.toByteArray();
	}

}
