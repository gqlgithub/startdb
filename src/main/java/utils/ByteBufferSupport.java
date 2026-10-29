package utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-23 8:09
 **/
public class ByteBufferSupport {

    public static final MethodHandle INVOKER;

    static {
        MethodHandle invoke;
        try {
            Class<?> unsafeClass = Class.forName("unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            invoke = MethodHandles.lookup()
                    .findVirtual(unsafeClass, "invokeCleaner", MethodType.methodType(void.class, ByteBuffer.class))
                    .bindTo(theUnsafe.get(null));
        } catch (Exception e) {
            invoke = null;
        }
        INVOKER = invoke;
    }

    public static void unmap(ByteBuffer byteBuffer) {
        try {
            INVOKER.invoke(byteBuffer);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

}
