package org.kartikey.tweet_scheduler.twitter;

import com.twitter.clientlib.ApiException;
import com.twitter.clientlib.TwitterCredentialsOAuth2;
import com.twitter.clientlib.api.TwitterApi;
import com.twitter.clientlib.model.TweetCreateRequest;
import com.twitter.clientlib.model.TweetCreateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TwitterService {

    private static final Logger log = LoggerFactory.getLogger(TwitterService.class);

    @Value("${TWITTER_OAUTH2_CLIENT_ID}")
    private String clientId;

    @Value("${TWITTER_OAUTH2_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${TWITTER_OAUTH2_ACCESS_TOKEN}")
    private String accessToken;

    @Value("${TWITTER_OAUTH2_REFRESH_TOKEN}")
    private String refreshToken;

    private TwitterApi apiInstance;
    private AtomicLong tokenExpiryTime = new AtomicLong(System.currentTimeMillis() + (1000L * 60 * 60)); // 1 hour default

    public TwitterService() {
//        initApi();
    }

    private void initApi() {
        TwitterCredentialsOAuth2 creds = new TwitterCredentialsOAuth2(
                clientId, clientSecret, accessToken, refreshToken
        );
        this.apiInstance = new TwitterApi(creds);
    }

    public synchronized void ensureTokenValid() throws IOException, InterruptedException {
        long now = System.currentTimeMillis();
        if (now >= tokenExpiryTime.get()) {
            refreshAccessToken();
        } else if (apiInstance == null) {
            log.warn("API instance was null, reinitializing...");
            initApi();
        }
    }


    private void refreshAccessToken() throws IOException, InterruptedException {
        log.info("Refreshing Twitter access token...");

        String form = "grant_type=refresh_token" +
                "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8) +
                "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twitter.com/2/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        log.info("Token refresh HTTP status: {}", response.statusCode());
        log.debug("Token refresh raw response: {}", response.body());

        if (response.statusCode() == 200) {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var json = mapper.readTree(response.body());

            accessToken = json.get("access_token").asText();
            if (json.has("refresh_token")) {
                refreshToken = json.get("refresh_token").asText();
            }
            int expiresIn = json.get("expires_in").asInt();

            tokenExpiryTime.set(System.currentTimeMillis() + (expiresIn * 1000L));

            log.info("Access token refreshed successfully. Expires in {} seconds", expiresIn);

            // Reinitialize API with new credentials
            initApi();
        } else {
            log.error("Failed to refresh token. Status: {} Response: {}", response.statusCode(), response.body());
            throw new IOException("Token refresh failed with status " + response.statusCode());
        }
    }


    public void postTweet(String text) {
        try {
            log.info("Attempting to post tweet: {}", text);
            ensureTokenValid();
            TweetCreateRequest req = new TweetCreateRequest();
            req.setText(text);
            TweetCreateResponse res = apiInstance.tweets().createTweet(req).execute();
            log.info("Tweet posted successfully. Response: {}", res);
        } catch (ApiException e) {
            log.error("Twitter API error while posting tweet. Status: {}, Body: {}",
                    e.getCode(), e.getResponseBody(), e);
        } catch (IOException | InterruptedException e) {
            log.error("Error posting tweet", e);
        }
    }

}
