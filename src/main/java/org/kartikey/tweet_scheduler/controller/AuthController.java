package org.kartikey.tweet_scheduler.controller;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.pkce.PKCE;
import com.github.scribejava.core.pkce.PKCECodeChallengeMethod;
import com.twitter.clientlib.auth.TwitterOAuth20Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import jakarta.servlet.http.HttpSession;
import com.github.scribejava.core.pkce.PKCE;
import java.util.UUID;

@RestController
public class AuthController {

    @Value("${TWITTER_OAUTH2_CLIENT_ID}")
    private String clientId;

    @Value("${TWITTER_OAUTH2_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${TWITTER_REDIRECT_URI}")
    private String redirectUri;

    @GetMapping("/auth-url")
    public String getAuthUrl(HttpSession session) {
        try {
            String scope = "offline.access tweet.read tweet.write users.read";

            PKCE pkce = new PKCE();
            pkce.setCodeChallenge("challenge"); // Ideally generate securely
            pkce.setCodeChallengeMethod(PKCECodeChallengeMethod.PLAIN);
            pkce.setCodeVerifier("challenge");

            String state = UUID.randomUUID().toString();

            // Save to session for later validation
            session.setAttribute("pkce", pkce);
            session.setAttribute("state", state);

            TwitterOAuth20Service service = new TwitterOAuth20Service(
                    clientId,
                    clientSecret,
                    redirectUri,
                    scope
            );
            String authUrl = service.getAuthorizationUrl(pkce, state);
            return "Visit this URL to authorize:<br/><a href='" + authUrl + "'>" + authUrl + "</a>";
        } catch (Exception e) {
            return "Error generating auth URL: " + e.getMessage();
        }
    }

    @GetMapping("/callback")
    public String handleCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpSession session
    ) {
        try {
            String sessionState = (String) session.getAttribute("state");
            PKCE pkce = (PKCE) session.getAttribute("pkce");

            if (sessionState == null || !sessionState.equals(state)) {
                return "Invalid state param. Possible CSRF attack.";
            }

            String scope = "offline.access tweet.read tweet.write users.read";

            TwitterOAuth20Service service = new TwitterOAuth20Service(
                    clientId,
                    clientSecret,
                    redirectUri,
                    scope
            );

            OAuth2AccessToken token = service.getAccessToken(pkce, code);

            return """
                    <h3>Tokens received. Save them in your Render environment variables:</h3>
                    <p><b>Access Token:</b> %s</p>
                    <p><b>Refresh Token:</b> %s</p>
                    <p><b>Expires In:</b> %d seconds</p>
                    """.formatted(token.getAccessToken(), token.getRefreshToken(), token.getExpiresIn());
        } catch (Exception e) {
            e.printStackTrace();
            return "Error exchanging code for tokens: " + e.getMessage();
        }
    }
}
