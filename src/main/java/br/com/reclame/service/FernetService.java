package br.com.reclame.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class FernetService {
    private final byte[] key;
    private final SecureRandom random = new SecureRandom();

    public FernetService(@Value("${reclame.encryption-key:}") String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException("ENCRYPTION_KEY não configurada.");
        }
        try {
            this.key = Base64.getUrlDecoder().decode(encodedKey.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("ENCRYPTION_KEY inválida.", e);
        }
        if (key.length != 32) throw new IllegalStateException("ENCRYPTION_KEY deve ser uma chave Fernet de 32 bytes.");
    }

    public String encrypt(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            byte[] signingKey = java.util.Arrays.copyOfRange(key, 0, 16);
            byte[] encryptionKey = java.util.Arrays.copyOfRange(key, 16, 32);
            byte[] iv = new byte[16]; random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new IvParameterSpec(iv));
            byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            ByteBuffer body = ByteBuffer.allocate(1 + 8 + 16 + ciphertext.length);
            body.put((byte) 0x80).putLong(Instant.now().getEpochSecond()).put(iv).put(ciphertext);

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            byte[] signature = mac.doFinal(body.array());

            ByteBuffer token = ByteBuffer.allocate(body.capacity() + signature.length);
            token.put(body.array()).put(signature);
            return Base64.getUrlEncoder().encodeToString(token.array());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criptografar dados.", e);
        }
    }
}
