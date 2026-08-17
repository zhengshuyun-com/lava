/*
 * Copyright 2026 zhengshuyun.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.zhengshuyun.lava.crypto;

import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECParameterSpec;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PemKeyUtilsTest {

    @Test
    void everySupportedCurveRoundTripsAndSignsWithoutProviderMutation() throws Exception {
        List<String> providersBefore = providerNames();
        for (EcKeyUtils.Curve curve : EcKeyUtils.Curve.values()) {
            KeyPair pair = EcKeyUtils.generate(curve);
            String privatePem = PemKeyUtils.toPem(pair.getPrivate());
            String publicPem = PemKeyUtils.toPem(pair.getPublic());
            ECPrivateKey restoredPrivate = PemKeyUtils.readEcPrivateKey(privatePem);
            var restoredPublic = PemKeyUtils.readEcPublicKey(publicPem);

            Signature signer = Signature.getInstance(signatureAlgorithm(curve));
            signer.initSign(restoredPrivate);
            signer.update(new byte[]{1, 2, 3, 4});
            byte[] signature = signer.sign();
            Signature verifier = Signature.getInstance(signatureAlgorithm(curve));
            verifier.initVerify(restoredPublic);
            verifier.update(new byte[]{1, 2, 3, 4});

            assertTrue(verifier.verify(signature));
            assertEquals(((ECPrivateKey) pair.getPrivate()).getS(), restoredPrivate.getS());
        }
        assertEquals(providersBefore, providerNames());
    }

    @Test
    void pemOutputIsCanonicalAndContainsNoExtraMaterial() {
        KeyPair pair = EcKeyUtils.generate();
        String pem = PemKeyUtils.toPem(pair.getPrivate());
        String[] lines = pem.split("\\n");

        assertEquals("-----BEGIN PRIVATE KEY-----", lines[0]);
        assertEquals("-----END PRIVATE KEY-----", lines[lines.length - 1]);
        for (int index = 1; index < lines.length - 2; index++) {
            assertEquals(64, lines[index].length());
        }
        assertTrue(lines[lines.length - 2].length() <= 64);
    }

    @Test
    void strictParserRejectsWrongDuplicateAndEmbeddedBoundaries() {
        KeyPair pair = EcKeyUtils.generate();
        String privatePem = PemKeyUtils.toPem(pair.getPrivate());
        String publicPem = PemKeyUtils.toPem(pair.getPublic());

        assertThrows(CryptoException.class, () -> PemKeyUtils.readEcPrivateKey(publicPem));
        assertThrows(CryptoException.class, () -> PemKeyUtils.readEcPublicKey(privatePem));
        assertThrows(CryptoException.class,
                () -> PemKeyUtils.readEcPrivateKey(privatePem + privatePem));
        assertThrows(CryptoException.class,
                () -> PemKeyUtils.readEcPrivateKey(privatePem.replace(
                        "-----END PRIVATE KEY-----",
                        "-----BEGIN PUBLIC KEY-----\n-----END PRIVATE KEY-----")));
        assertThrows(CryptoException.class,
                () -> PemKeyUtils.readEcPrivateKey("-----BEGIN PRIVATE KEY-----\n!!!!\n-----END PRIVATE KEY-----"));
        assertThrows(CryptoException.class,
                () -> PemKeyUtils.readEcPrivateKey("x".repeat(PemKeyUtils.DEFAULT_MAX_PEM_CHARACTERS + 1)));
    }

    @Test
    void rsaKeysAndRsaDerAreRejected() throws Exception {
        KeyPair rsa = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        assertThrows(CryptoException.class, () -> PemKeyUtils.toPem(rsa.getPrivate()));
        assertThrows(CryptoException.class, () -> PemKeyUtils.toPem(rsa.getPublic()));

        String encodedRsaPrivate = toPrivatePem(rsa.getPrivate().getEncoded());
        assertThrows(CryptoException.class, () -> PemKeyUtils.readEcPrivateKey(encodedRsaPrivate));
    }

    @Test
    void nonExportableHsmStyleKeyHasExplicitFailure() {
        CryptoException missingEncoding = assertThrows(
                CryptoException.class,
                () -> PemKeyUtils.toPem(new NonExportableEcPrivateKey("PKCS#8")));
        CryptoException missingFormat = assertThrows(
                CryptoException.class,
                () -> PemKeyUtils.toPem(new NonExportableEcPrivateKey(null)));
        CryptoException inaccessibleFormat = assertThrows(
                CryptoException.class,
                () -> PemKeyUtils.toPem(new NonExportableEcPrivateKey(null, true)));

        assertFalse(missingEncoding.getMessage().contains("null"));
        assertFalse(missingFormat.getMessage().contains("null"));
        assertFalse(inaccessibleFormat.getMessage().contains("null"));
    }

    @Test
    void generatedKeysUseOnlyTheRequestedCurves() {
        assertEquals(256, ((java.security.interfaces.ECPublicKey) EcKeyUtils.generate(EcKeyUtils.Curve.P256)
                .getPublic()).getParams().getCurve().getField().getFieldSize());
        assertEquals(384, ((java.security.interfaces.ECPublicKey) EcKeyUtils.generate(EcKeyUtils.Curve.P384)
                .getPublic()).getParams().getCurve().getField().getFieldSize());
        assertEquals(521, ((java.security.interfaces.ECPublicKey) EcKeyUtils.generate(EcKeyUtils.Curve.P521)
                .getPublic()).getParams().getCurve().getField().getFieldSize());
        assertEquals("secp256r1", EcKeyUtils.Curve.P256.standardName());
    }

    private static String signatureAlgorithm(EcKeyUtils.Curve curve) {
        return switch (curve) {
            case P256 -> "SHA256withECDSA";
            case P384 -> "SHA384withECDSA";
            case P521 -> "SHA512withECDSA";
        };
    }

    private static List<String> providerNames() {
        return Arrays.stream(Security.getProviders()).map(Provider::getName).toList();
    }

    private static String toPrivatePem(byte[] encoded) {
        String base64 = java.util.Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(encoded);
        return "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----\n";
    }

    private static final class NonExportableEcPrivateKey implements ECPrivateKey {

        private final @Nullable String format;
        private final boolean throwOnFormat;

        NonExportableEcPrivateKey(@Nullable String format) {
            this(format, false);
        }

        NonExportableEcPrivateKey(@Nullable String format, boolean throwOnFormat) {
            this.format = format;
            this.throwOnFormat = throwOnFormat;
        }

        @Override
        public String getAlgorithm() {
            return "EC";
        }

        @Override
        public @Nullable String getFormat() {
            if (throwOnFormat) {
                throw new UnsupportedOperationException("provider does not expose format");
            }
            return format;
        }

        @Override
        public byte @Nullable [] getEncoded() {
            return null;
        }

        @Override
        public BigInteger getS() {
            return BigInteger.ONE;
        }

        @Override
        public @Nullable ECParameterSpec getParams() {
            return null;
        }
    }
}
