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

@RestController
public class AuthController {

    @Value("${TWITTER_OAUTH2_CLIENT_ID}")
    private String clientId;

    @Value("${TWITTER_OAUTH2_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${TWITTER_REDIRECT_URI}")
    private String redirectUri;

    private PKCE pkce;
    private String state;

    @GetMapping("/auth-url")
    public String getAuthUrl() {
        try {
            String scope = "offline.access tweet.read tweet.write users.read";

            pkce = new PKCE();
            pkce.setCodeChallenge("challenge"); // in production: generate a secure random string
            pkce.setCodeChallengeMethod(PKCECodeChallengeMethod.PLAIN);
            pkce.setCodeVerifier("challenge");

            state = UUID.randomUUID().toString();

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
    public String handleCallback(@RequestParam String code, @RequestParam String state) {
        try {
            if (!state.equals(this.state)) {
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

