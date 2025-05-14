package com.cinefinder.global.util.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtUtil {

	private final SecretKey secretKey;
	private final Long expiredTimeMillis;
	private final Long refreshTimeMillis;

	public JwtUtil(@Value("${spring.jwt.secret}") String secret,
		@Value("${spring.jwt.expiration.access}") Long expiredTimeMillis,
		@Value("${spring.jwt.expiration.refresh}") Long refreshTimeMillis) {
		this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
			Jwts.SIG.HS256.key().build().getAlgorithm());
		this.expiredTimeMillis = expiredTimeMillis;
		this.refreshTimeMillis = refreshTimeMillis;
	}

	public String generateToken(String username) {
		return Jwts.builder()
			.subject(username)
			.issuedAt(new Date())
			.expiration(new Date(System.currentTimeMillis() + expiredTimeMillis))
			.signWith(secretKey)
			.compact();
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token);
			return true;
		} catch (ExpiredJwtException e) {
			throw e;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isExpired(String token) {
		try {
			Date expiration = Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getExpiration();
			return expiration.before(new Date());
		} catch (ExpiredJwtException e) {
			throw e;
		} catch (Exception e) {
			return true; // 만료
		}
	}

	public String generateRefreshToken(String username) {
		return Jwts.builder()
			.subject(username)
			.issuedAt(new Date())
			.expiration(new Date(System.currentTimeMillis() + refreshTimeMillis))
			.signWith(secretKey)
			.compact();
	}

	public Map<String, String> refreshToken(String refreshToken) {
		Claims claims = Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(refreshToken)
			.getPayload();

		if (claims.getExpiration().before(new Date())) {
			throw new RuntimeException("Refresh token expired, please login again");
		}

		String newAccessToken = generateToken(claims.getSubject());
		String newRefreshToken = generateRefreshToken(claims.getSubject());

		Map<String, String> tokens = new HashMap<>();
		tokens.put("accessToken", newAccessToken);
		tokens.put("refreshToken", newRefreshToken);

		return tokens;
	}

	public String getUsernameFromToken(String token) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload()
			.getSubject();
	}

	public String resolveToken(HttpServletRequest request) {
		String bearer = request.getHeader("Authorization");
		return (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
	}

	public long getExpirationMillis(String token) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload()
			.getExpiration()
			.getTime() - System.currentTimeMillis();
	}

}

