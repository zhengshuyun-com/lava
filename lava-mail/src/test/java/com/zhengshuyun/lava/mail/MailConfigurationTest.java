/*
 * Copyright 2026 zhengshuyun.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.zhengshuyun.lava.mail;

import com.zhengshuyun.lava.mail.provider.MailProviders;
import jakarta.mail.Session;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class MailConfigurationTest {
    @Test
    void validatesPortsTimeoutsAndAddresses() {
        assertThrows(IllegalArgumentException.class,
                () -> SmtpServerConfig.startTls("smtp.example.com", 65_536));
        assertThrows(IllegalArgumentException.class,
                () -> SmtpServerConfig.startTls("smtp.example.com", 0));
        assertThrows(IllegalArgumentException.class,
                () -> SmtpServerConfig.startTls("smtp example.com", 587));
        assertThrows(IllegalArgumentException.class,
                () -> new SmtpServerConfig(
                        "smtp.example.com", 587, MailSecurityMode.STARTTLS,
                        Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new SmtpServerConfig(
                        "smtp.example.com", 587, MailSecurityMode.STARTTLS,
                        Duration.ofNanos(1), Duration.ofSeconds(1), Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new MailAddress("a@example.com\r\nBcc:x@y"));
        assertThrows(IllegalArgumentException.class, () -> new MailAddress("not-an-address"));
        assertThrows(IllegalArgumentException.class,
                () -> new PasswordCredential("bad\nuser", "secret"));
        assertEquals("a@example.com", new MailAddress("a@example.com").address());
    }

    @Test
    void enablesIdentityChecksAndRequiresStartTls() {
        SmtpServerConfig smtp = SmtpServerConfig.startTls("smtp.example.com", 587);
        Session smtpSession = MailSessionFactory.smtp(smtp, new PasswordCredential("user", "secret"));
        Properties smtpProperties = smtpSession.getProperties();
        assertEquals("true", smtpProperties.getProperty("mail.smtp.starttls.enable"));
        assertEquals("true", smtpProperties.getProperty("mail.smtp.starttls.required"));
        assertEquals("true", smtpProperties.getProperty("mail.smtp.ssl.checkserveridentity"));
        assertEquals("false", smtpProperties.getProperty("mail.smtp.ssl.enable"));

        ImapServerConfig imap = ImapServerConfig.implicitTls("imap.example.com", 993);
        Properties imapProperties = MailSessionFactory.imap(
                imap, new PasswordCredential("user", "secret")).getProperties();
        assertEquals("true", imapProperties.getProperty("mail.imap.ssl.enable"));
        assertEquals("true", imapProperties.getProperty("mail.imap.ssl.checkserveridentity"));

        SmtpServerConfig plaintext = new SmtpServerConfig(
                "localhost", 2525, MailSecurityMode.PLAINTEXT,
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1));
        Properties plaintextProperties = MailSessionFactory.smtp(
                plaintext, new PasswordCredential("user", "secret")).getProperties();
        assertEquals("false", plaintextProperties.getProperty("mail.smtp.ssl.enable"));
        assertEquals("false", plaintextProperties.getProperty("mail.smtp.starttls.enable"));
    }

    @Test
    void configuresPasswordAndOauthAuthenticationWithoutEmbeddingSecretsInProperties() {
        PasswordCredential password = new PasswordCredential("user", "password-secret");
        Session passwordSession = MailSessionFactory.smtp(
                SmtpServerConfig.implicitTls("smtp.example.com", 465), password);
        Properties passwordProperties = passwordSession.getProperties();
        assertEquals("true", passwordProperties.getProperty("mail.smtp.ssl.enable"));
        assertEquals("false", passwordProperties.getProperty("mail.smtp.starttls.enable"));
        assertFalse(passwordProperties.toString().contains("password-secret"));
        assertEquals("password-secret", MailSessionFactory.authenticationSecret(password, null));

        OAuth2RefreshTokenCredential oauth = new OAuth2RefreshTokenCredential(
                "user@example.com", "client", "refresh-secret",
                URI.create("https://login.example.com/token"), List.of("mail"), null);
        Properties oauthProperties = MailSessionFactory.imap(
                ImapServerConfig.implicitTls("imap.example.com", 993), oauth).getProperties();
        assertEquals("XOAUTH2", oauthProperties.getProperty("mail.imap.auth.mechanisms"));
        assertEquals("true", oauthProperties.getProperty("mail.imap.auth.login.disable"));
        assertEquals("true", oauthProperties.getProperty("mail.imap.auth.plain.disable"));
        assertFalse(oauthProperties.toString().contains("refresh-secret"));
        assertEquals("access-token", MailSessionFactory.authenticationSecret(oauth, "access-token"));
        MailException missingToken = assertThrows(MailException.class,
                () -> MailSessionFactory.authenticationSecret(oauth, null));
        assertEquals(MailFailureKind.AUTHENTICATION, missingToken.kind());
    }

    @Test
    void oauthEndpointIsHttpsAndSecretsAreRedacted() {
        assertThrows(IllegalArgumentException.class, () -> new OAuth2RefreshTokenCredential(
                "user@example.com", "client", "refresh-secret",
                URI.create("http://localhost/token"), List.of("mail"), "client-secret"));
        assertThrows(IllegalArgumentException.class, () -> new OAuth2RefreshTokenCredential(
                "user@example.com", "client", "refresh-secret",
                URI.create("https://user@login.example.com/token"), List.of("mail"), null));
        assertThrows(IllegalArgumentException.class, () -> new OAuth2RefreshTokenCredential(
                "user@example.com", "client", "refresh-secret",
                URI.create("https://login.example.com/token#fragment"), List.of("mail"), null));
        assertThrows(IllegalArgumentException.class, () -> new OAuth2RefreshTokenCredential(
                "user@example.com", "client", "refresh-secret",
                URI.create("https://login.example.com/token"), List.of("mail send"), null));

        OAuth2RefreshTokenCredential credential = new OAuth2RefreshTokenCredential(
                "user@example.com", "client", "refresh-secret",
                URI.create("https://login.example.com/token"), List.of("mail"), "client-secret");
        String diagnostic = credential.toString();
        assertFalse(diagnostic.contains("refresh-secret"));
        assertFalse(diagnostic.contains("client-secret"));
        assertFalse(diagnostic.contains("clientId=client"));
        assertFalse(diagnostic.contains("login.example.com"));
        assertTrue(diagnostic.contains("<redacted>"));
        assertFalse(new PasswordCredential("user", "password-secret").toString()
                .contains("password-secret"));
    }

    @Test
    void providerPresetsUseTlsAndUriEndpoints() {
        assertEquals(MailSecurityMode.SSL_TLS, MailProviders.hotmail().imap().securityMode());
        assertEquals(MailSecurityMode.STARTTLS, MailProviders.hotmail().smtp().securityMode());
        assertEquals("https", Objects.requireNonNull(MailProviders.hotmail().oauth2())
                .tokenEndpoint().getScheme());
        assertEquals(MailSecurityMode.SSL_TLS, MailProviders.qq().smtp().securityMode());
        assertThrows(IllegalStateException.class, () -> MailProviders.qq().oauthCredential(
                "user", "client", "refresh", null));
    }

    @Test
    void centralizedLimitsRejectUnsafeCombinations() {
        assertThrows(IllegalArgumentException.class, () -> new MailLimits(10, 5, 8, 2));
        assertThrows(IllegalArgumentException.class, () -> new MailLimits(1, 1, 1, 0));
        assertEquals(20, MailLimits.DEFAULT.maxMimeDepth());
    }
}
