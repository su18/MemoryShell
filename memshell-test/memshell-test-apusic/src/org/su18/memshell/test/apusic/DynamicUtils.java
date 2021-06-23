package org.su18.memshell.test.apusic;

import sun.misc.BASE64Decoder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author su18
 */
public class DynamicUtils {


	public static String FILTER_CLASS_STRING = "yv66vgAAADQANwoABwAiCwAjACQIACUKACYAJwsAKAApBwAqBwArBwAsAQAGPGluaXQ+AQADKClWAQAEQ29kZQEAD0xpbmVOdW1iZXJUYWJsZQEAEkxvY2FsVmFyaWFibGVUYWJsZQEABHRoaXMBACpMb3JnL3N1MTgvbWVtc2hlbGwvdGVzdC90b21jYXQvVGVzdEZpbHRlcjsBAARpbml0AQAfKExqYXZheC9zZXJ2bGV0L0ZpbHRlckNvbmZpZzspVgEADGZpbHRlckNvbmZpZwEAHExqYXZheC9zZXJ2bGV0L0ZpbHRlckNvbmZpZzsBAAhkb0ZpbHRlcgEAWyhMamF2YXgvc2VydmxldC9TZXJ2bGV0UmVxdWVzdDtMamF2YXgvc2VydmxldC9TZXJ2bGV0UmVzcG9uc2U7TGphdmF4L3NlcnZsZXQvRmlsdGVyQ2hhaW47KVYBAA5zZXJ2bGV0UmVxdWVzdAEAHkxqYXZheC9zZXJ2bGV0L1NlcnZsZXRSZXF1ZXN0OwEAD3NlcnZsZXRSZXNwb25zZQEAH0xqYXZheC9zZXJ2bGV0L1NlcnZsZXRSZXNwb25zZTsBAAtmaWx0ZXJDaGFpbgEAG0xqYXZheC9zZXJ2bGV0L0ZpbHRlckNoYWluOwEACkV4Y2VwdGlvbnMHAC0HAC4BAAdkZXN0cm95AQAKU291cmNlRmlsZQEAD1Rlc3RGaWx0ZXIuamF2YQwACQAKBwAvDAAwADEBAA90aGlzIGlzIEZpbHRlciAHADIMADMANAcANQwAFAA2AQAob3JnL3N1MTgvbWVtc2hlbGwvdGVzdC90b21jYXQvVGVzdEZpbHRlcgEAEGphdmEvbGFuZy9PYmplY3QBABRqYXZheC9zZXJ2bGV0L0ZpbHRlcgEAE2phdmEvaW8vSU9FeGNlcHRpb24BAB5qYXZheC9zZXJ2bGV0L1NlcnZsZXRFeGNlcHRpb24BAB1qYXZheC9zZXJ2bGV0L1NlcnZsZXRSZXNwb25zZQEACWdldFdyaXRlcgEAFygpTGphdmEvaW8vUHJpbnRXcml0ZXI7AQATamF2YS9pby9QcmludFdyaXRlcgEAB3ByaW50bG4BABUoTGphdmEvbGFuZy9TdHJpbmc7KVYBABlqYXZheC9zZXJ2bGV0L0ZpbHRlckNoYWluAQBAKExqYXZheC9zZXJ2bGV0L1NlcnZsZXRSZXF1ZXN0O0xqYXZheC9zZXJ2bGV0L1NlcnZsZXRSZXNwb25zZTspVgAhAAYABwABAAgAAAAEAAEACQAKAAEACwAAAC8AAQABAAAABSq3AAGxAAAAAgAMAAAABgABAAAACQANAAAADAABAAAABQAOAA8AAAABABAAEQABAAsAAAA1AAAAAgAAAAGxAAAAAgAMAAAABgABAAAAEgANAAAAFgACAAAAAQAOAA8AAAAAAAEAEgATAAEAAQAUABUAAgALAAAAZAADAAQAAAAULLkAAgEAEgO2AAQtKyy5AAUDALEAAAACAAwAAAAOAAMAAAAfAAsAIQATACIADQAAACoABAAAABQADgAPAAAAAAAUABYAFwABAAAAFAAYABkAAgAAABQAGgAbAAMAHAAAAAYAAgAdAB4AAQAfAAoAAQALAAAAKwAAAAEAAAABsQAAAAIADAAAAAYAAQAAACkADQAAAAwAAQAAAAEADgAPAAAAAQAgAAAAAgAh";

	public static Class<?> getClass(String classCode) throws IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
		ClassLoader   loader        = Thread.currentThread().getContextClassLoader();
		BASE64Decoder base64Decoder = new BASE64Decoder();
		byte[]        bytes         = base64Decoder.decodeBuffer(classCode);

		Method   method = null;
		Class<?> clz    = loader.getClass();
		while (method == null && clz != Object.class) {
			try {
				method = clz.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
			} catch (NoSuchMethodException ex) {
				clz = clz.getSuperclass();
			}
		}

		if (method != null) {
			method.setAccessible(true);
			return (Class<?>) method.invoke(loader, bytes, 0, bytes.length);
		}

		return null;

	}
}