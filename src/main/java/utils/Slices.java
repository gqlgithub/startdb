package utils;

import com.google.common.base.Preconditions;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.charset.*;
import java.util.Base64;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-03 18:22
 **/
public class Slices {

    public final static Slice EMPTY_SLICE = new Slice(0);

    public static void writeVariableLengthAndValue(SliceOutput out, Slice value) {
        VariableLengthQuantity.writeVariableLengthInt(out, value.length());
        out.writeBytes(value);
    }

    public static Slice readVariableLengthAndValue(SliceInput input) {
        int length = VariableLengthQuantity.readVariableLengthInt(input);
        return input.readSlice(length);
    }

    public static Slice alloc(int length) {
        return new Slice(length);
    }

    public static Slice wrapBuffer(byte[] array) {
        if (array.length == 0){
            return EMPTY_SLICE;
        }
        return new Slice(array);
    }

    public static Slice copiedBuffer(ByteBuffer source, int offset, int length) {
        int newPosition = source.position() + offset;
        ByteBuffer buffer = source.duplicate()
                .order(ByteOrder.LITTLE_ENDIAN)
                .clear()
                .position(newPosition)
                .limit(newPosition + length);
        return copiedBuffer(buffer);
    }

    public static Slice copiedBuffer(String str, Charset charset) {
        return new Slice(str.getBytes(charset));
    }

    public static Slice copiedBuffer(ByteBuffer source) {
        Preconditions.checkState(source != null, "source is null");
        Slice slice = alloc(source.limit() - source.position());
        slice.setBytes(0, source.duplicate().order(ByteOrder.LITTLE_ENDIAN));
        return slice;
    }

    public static Slice ensureSize(Slice cur, int expectSize) {
        if (cur == null) {
            cur = Slices.EMPTY_SLICE;
        }

        int curLen = cur.length();
        if (curLen >= expectSize) {
            return cur;
        }
        int newSize = curLen == 0 ? 1 : curLen;

        while (newSize < expectSize) {
            newSize <<= 1;
        }

        Slice newSlice = Slices.alloc(newSize);
        newSlice.setBytes(0, 0, cur.getRawArray(), curLen);
        return newSlice;
    }

    public static final ThreadLocal<Map<Charset, CharsetDecoder>> dMap = new ThreadLocal<>(){
        @Override
        protected Map<Charset, CharsetDecoder> initialValue() {
            return new IdentityHashMap<>();
        }
    };

    // todo fix int i, int length, Slice slice -> ByteBuffer
    public static String decodeString(ByteBuffer byteBuffer, Charset charset) {
        // get decode
        CharsetDecoder decoder = getDecoder(charset);
        // decode ByteBuffer , CharBuffer
        CharBuffer charBuffer = CharBuffer.allocate(
                (int) ((double) byteBuffer.remaining() * decoder.maxCharsPerByte()));
        CoderResult coderResult = decoder.decode(byteBuffer, charBuffer, true);
        try{
            if (!coderResult.isUnderflow()){
                coderResult.throwException();
            }
            decoder.flush(charBuffer);
            if(!coderResult.isUnderflow()){
                coderResult.throwException();
            }

        }catch (CharacterCodingException exception){
            throw new IllegalStateException(exception);
        }

        return charBuffer.flip().toString();
    }

    public static CharsetDecoder getDecoder(Charset charset){
        Map<Charset, CharsetDecoder> map = dMap.get();
        CharsetDecoder decoder = map.get(charset);
        if (decoder != null){
            decoder.reset();
            decoder.onMalformedInput(CodingErrorAction.REPLACE);
            decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
            return decoder;
        }

        decoder = charset.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);
        decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        map.put(charset,decoder);
        return decoder;
    }
}
