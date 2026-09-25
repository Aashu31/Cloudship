package com.cloudship.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class CloudflareJwtValidator {

    private static final Logger log = LoggerFactory.getLogger(CloudflareJwtValidator.class);

    private static final String EXPECTED_ALGORITHM = "RS256";
    private static final int DEFAULT_CLOCK_SKEW_SECONDS = 30;

    private final CloudflareSecurityProperties properties;
    private final ConcurrentMap<String, JwkProvider> jwkProviderCache = new ConcurrentHashMap<>();

    public CloudflareJwtValidator(CloudflareSecurityProperties properties) {
        this.properties = properties;
    }

    public record VerifiedToken(String email, String subject, String name) {}

    /**
     * Validates a Cloudflare Access JWT assertion.
     * Verifies signature against Cloudflare's JWKS, checks AUD, ISS, and expiration.
     *
     * @param token JWT token string
     * @return VerifiedToken with extracted user details
     * @throws JWTVerificationException if token validation fails
     */
    public VerifiedToken validateToken(String token) {
        if (token == null || token.isBlank()) {
            log.warn("JWT validation failed: token is null or empty");
            throw new JWTVerificationException("Token is null or empty");
        }

        DecodedJWT decodedHeader;
        try {
            decodedHeader = JWT.decode(token);
        } catch (Exception e) {
            log.warn("JWT validation failed: malformed token");
            throw new JWTVerificationException("Malformed JWT token", e);
        }

        String keyId = decodedHeader.getKeyId();
        String algorithm = decodedHeader.getAlgorithm();

        if (!EXPECTED_ALGORITHM.equalsIgnoreCase(algorithm)) {
            log.warn("JWT validation failed: invalid algorithm '{}', expected '{}'", algorithm, EXPECTED_ALGORITHM);
            throw new JWTVerificationException("Unsupported algorithm: " + algorithm + ". Expected: " + EXPECTED_ALGORITHM);
        }

        if (keyId == null || keyId.isBlank()) {
            log.warn("JWT validation failed: missing Key ID (kid) header");
            throw new JWTVerificationException("Missing Key ID (kid) in token header");
        }

        String jwksUrl = properties.resolveJwksUrl();
        if (jwksUrl == null || jwksUrl.isBlank()) {
            log.error("Cloudflare Access JWKS URL or Team Domain is not configured");
            throw new IllegalStateException("Cloudflare Access team domain or JWKS URL is not configured");
        }

        try {
            JwkProvider jwkProvider = getOrCreateJwkProvider(jwksUrl);
            Jwk jwk = jwkProvider.get(keyId);
            if (jwk == null) {
                log.warn("JWT validation failed: JWK not found for kid '{}'", keyId);
                throw new JWTVerificationException("Invalid key ID: no matching JWK found");
            }

            RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();

            Algorithm rsaAlgorithm = Algorithm.RSA256(publicKey, null);

            var verifierBuilder = JWT.require(rsaAlgorithm)
                    .acceptLeeway(DEFAULT_CLOCK_SKEW_SECONDS);

            if (properties.getAud() != null && !properties.getAud().isBlank()) {
                verifierBuilder.withAudience(properties.getAud().trim());
            }

            String expectedIssuer = properties.getNormalizedTeamDomain();
            if (expectedIssuer != null && !expectedIssuer.isBlank()) {
                verifierBuilder.withIssuer(expectedIssuer);
            }

            JWTVerifier verifier = verifierBuilder.build();
            DecodedJWT verifiedJwt = verifier.verify(token);

            String email = verifiedJwt.getClaim("email").asString();
            if (email == null || email.isBlank()) {
                email = verifiedJwt.getClaim("sub").asString();
            }
            if (email == null || email.isBlank()) {
                log.warn("JWT validation failed: token does not contain an email or identity claim");
                throw new JWTVerificationException("Token does not contain an email or identity claim");
            }

            String name = verifiedJwt.getClaim("name").asString();
            if (name == null || name.isBlank()) {
                name = email.split("@")[0];
            }

            log.debug("JWT validation successful for user: {}", email.trim().toLowerCase());
            return new VerifiedToken(email.trim().toLowerCase(), verifiedJwt.getSubject(), name);

        } catch (JwkException e) {
            log.error("JWT validation failed: Failed to retrieve JWK for kid '{}' from JWKS '{}': {}", keyId, jwksUrl, e.getMessage());
            throw new JWTVerificationException("Invalid key ID or unable to fetch public key: " + e.getMessage(), e);
        } catch (MalformedURLException e) {
            log.error("JWT validation failed: Malformed JWKS URL: {}", jwksUrl, e);
            throw new IllegalStateException("Malformed JWKS URL: " + jwksUrl, e);
        }
    }

    private JwkProvider getOrCreateJwkProvider(String jwksUrl) throws MalformedURLException {
        JwkProvider existing = jwkProviderCache.get(jwksUrl);
        if (existing != null) {
            return existing;
        }

        URL url = new URL(jwksUrl);
        JwkProvider newProvider = new JwkProviderBuilder(url)
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build();

        jwkProviderCache.put(jwksUrl, newProvider);
        return newProvider;
    }
}
