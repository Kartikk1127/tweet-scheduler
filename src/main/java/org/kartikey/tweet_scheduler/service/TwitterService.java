package org.kartikey.tweet_scheduler.service;

import com.twitter.clientlib.ApiException;
import com.twitter.clientlib.TwitterCredentialsOAuth2;
import com.twitter.clientlib.api.TwitterApi;
import com.twitter.clientlib.model.TweetCreateRequest;
import com.twitter.clientlib.model.TweetCreateResponse;
import org.kartikey.tweet_scheduler.model.TwitterToken;
import org.kartikey.tweet_scheduler.repository.TwitterTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TwitterService {

    private static final Logger log = LoggerFactory.getLogger(TwitterService.class);

    @Value("${TWITTER_OAUTH2_CLIENT_ID}")
    private String clientId;

    @Value("${TWITTER_OAUTH2_CLIENT_SECRET}")
    private String clientSecret;

    private final TwitterTokenRepository tokenRepository;

    private TwitterApi apiInstance;
    private AtomicLong tokenExpiryTime = new AtomicLong(0);

    private String accessToken;
    private String refreshToken;

    @Autowired
    public TwitterService(TwitterTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing TwitterService...");
        loadTokensFromDbOrEnv();
        if (isConfigured()) {
            initApi();
            log.info("Twitter API initialized successfully");
        } else {
            log.warn("Twitter credentials not configured.");
        }
    }

    private void loadTokensFromDbOrEnv() {
        var tokens = tokenRepository.findAll();
        if (!tokens.isEmpty()) {
            var t = tokens.get(0);
            this.accessToken = t.getAccessToken();
            this.refreshToken = t.getRefreshToken();
            tokenExpiryTime.set(t.getExpiryTime());
            log.info("Loaded Twitter tokens from DB");
        } else {
            this.accessToken = System.getenv("TWITTER_OAUTH2_ACCESS_TOKEN");
            this.refreshToken = System.getenv("TWITTER_OAUTH2_REFRESH_TOKEN");
            tokenExpiryTime.set(System.currentTimeMillis() + (60 * 60 * 1000));
            log.info("Loaded Twitter tokens from env vars");
        }
    }

    private boolean isConfigured() {
        return clientId != null && !clientId.isEmpty() &&
                accessToken != null && !accessToken.isEmpty() &&
                refreshToken != null && !refreshToken.isEmpty();
    }

    private void initApi() {
        TwitterCredentialsOAuth2 creds = new TwitterCredentialsOAuth2(
                clientId, clientSecret, accessToken, refreshToken
        );
        this.apiInstance = new TwitterApi(creds);
    }
    public synchronized void ensureTokenValid() throws IOException, InterruptedException {
        if (!isConfigured()) {
            throw new IOException("Twitter credentials not configured");
        }
        long now = System.currentTimeMillis();
        if (now >= tokenExpiryTime.get()) {
            refreshAccessToken();
        } else if (apiInstance == null) {
            initApi();
        }
    }

    private void refreshAccessToken() throws IOException, InterruptedException {
        log.info("Refreshing Twitter access token...");

        String form = "grant_type=refresh_token" +
                "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8) +
                "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8);

        String basicAuth = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twitter.com/2/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + basicAuth)
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var json = mapper.readTree(response.body());

            accessToken = json.get("access_token").asText();
            if (json.has("refresh_token")) {
                refreshToken = json.get("refresh_token").asText();
            }
            int expiresIn = json.get("expires_in").asInt();
            tokenExpiryTime.set(System.currentTimeMillis() + (expiresIn * 1000L));

            // Save to DB
            var tokenEntity = tokenRepository.findAll().stream().findFirst().orElse(new TwitterToken());
            tokenEntity.setAccessToken(accessToken);
            tokenEntity.setRefreshToken(refreshToken);
            tokenEntity.setExpiryTime(tokenExpiryTime.get());
            tokenRepository.save(tokenEntity);

            log.info("Access token refreshed and saved to DB. Expires in {} seconds", expiresIn);
            initApi();
        } else {
            log.error("Failed to refresh token. Status: {} Response: {}", response.statusCode(), response.body());
            throw new IOException("Token refresh failed with status " + response.statusCode());
        }
    }

    public String postTweet(String text) throws Exception {
        ensureTokenValid();
        try {
            TweetCreateRequest req = new TweetCreateRequest();
            req.setText(text);
            TweetCreateResponse res = apiInstance.tweets().createTweet(req).execute();
            return res.getData().getId();
        } catch (ApiException e) {
            if (e.getCode() == 429) {
                log.warn("Rate limit hit. Sleeping for 60 seconds before retry...");
                Thread.sleep(60000); // or use x-rate-limit-reset from headers
                return postTweet(text); // retry once
            }
            throw e;
        }
    }


    public boolean isReady() {
        return isConfigured() && apiInstance != null;
    }
}
