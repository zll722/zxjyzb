package com.edu.live.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.Deflater;

/**
 * Agora AccessToken2 动态生成工具（"007" 前缀格式）
 *
 * 对照 Agora 官方 Java 实现：
 *   https://github.com/AgoraIO/Tools/tree/master/DynamicKey/AgoraDynamicKey/java
 */
public final class AgoraTokenUtil {

    public static final int ROLE_PUBLISHER  = 1;
    public static final int ROLE_SUBSCRIBER = 2;

    private static final short PRIV_JOIN_CHANNEL         = 1;
    private static final short PRIV_PUBLISH_AUDIO_STREAM = 2;
    private static final short PRIV_PUBLISH_VIDEO_STREAM = 3;
    private static final short PRIV_PUBLISH_DATA_STREAM  = 4;

    private static final short SERVICE_TYPE_RTC = 1;
    private static final String TOKEN_VERSION   = "007";

    private AgoraTokenUtil() {}

    public static String buildRtcToken(String appId, String appCertificate,
                                       String channelName, int uid,
                                       int role,
                                       int tokenExpireSec, int privilegeExpireSec) {
        int issueTs = (int) (System.currentTimeMillis() / 1000);
        int salt = new SecureRandom().nextInt();

        Map<Short, Integer> privileges = new TreeMap<>();
        privileges.put(PRIV_JOIN_CHANNEL, privilegeExpireSec);
        if (role == ROLE_PUBLISHER) {
            privileges.put(PRIV_PUBLISH_AUDIO_STREAM, privilegeExpireSec);
            privileges.put(PRIV_PUBLISH_VIDEO_STREAM, privilegeExpireSec);
            privileges.put(PRIV_PUBLISH_DATA_STREAM, privilegeExpireSec);
        }

        ByteBuffer contentBuf = ByteBuffer.allocate(2048).order(ByteOrder.LITTLE_ENDIAN);
        packString(contentBuf, appId);
        contentBuf.putInt(issueTs);
        contentBuf.putInt(tokenExpireSec);
        contentBuf.putInt(salt);
        contentBuf.putShort((short) 1);
        contentBuf.putShort(SERVICE_TYPE_RTC);
        packMap(contentBuf, privileges);
        packString(contentBuf, channelName);
        packString(contentBuf, uid == 0 ? "" : String.valueOf(uid & 0xFFFFFFFFL));
        byte[] content = toBytes(contentBuf);

        byte[] signing = hmacSha256(intBytes(issueTs), appCertificate.getBytes(StandardCharsets.UTF_8));
        signing = hmacSha256(intBytes(salt), signing);
        byte[] signature = hmacSha256(signing, content);

        ByteBuffer tokenBuf = ByteBuffer.allocate(signature.length + content.length + 64).order(ByteOrder.LITTLE_ENDIAN);
        packBuffer(tokenBuf, signature);
        tokenBuf.put(content);

        return TOKEN_VERSION + Base64.getEncoder().encodeToString(compress(toBytes(tokenBuf)));
    }

    private static void packBuffer(ByteBuffer buf, byte[] data) {
        buf.putShort((short) data.length);
        buf.put(data);
    }

    private static void packString(ByteBuffer buf, String value) {
        packBuffer(buf, value.getBytes(StandardCharsets.UTF_8));
    }

    private static void packMap(ByteBuffer buf, Map<Short, Integer> map) {
        buf.putShort((short) map.size());
        for (Map.Entry<Short, Integer> e : map.entrySet()) {
            buf.putShort(e.getKey());
            buf.putInt(e.getValue());
        }
    }

    private static byte[] toBytes(ByteBuffer buf) {
        buf.flip();
        byte[] arr = new byte[buf.limit()];
        buf.get(arr);
        return arr;
    }

    private static byte[] intBytes(int value) {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value);
        return buffer.array();
    }

    private static byte[] compress(byte[] data) {
        Deflater deflater = new Deflater();
        ByteArrayOutputStream output = new ByteArrayOutputStream(data.length);
        try {
            deflater.setInput(data);
            deflater.finish();
            byte[] buffer = new byte[Math.max(data.length, 1024)];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 failed", e);
        }
    }
}
