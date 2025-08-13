package org.kartikey.tweet_scheduler.controller;

import org.kartikey.tweet_scheduler.twitter.TwitterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestTweetController {

    private final TwitterService twitterService;

    public TestTweetController(TwitterService twitterService) {
        this.twitterService = twitterService;
    }

    /**
     * Test endpoint to post a tweet instantly.
     * Usage: GET /test/tweet?msg=Hello%20World
     */
    @GetMapping("/tweet")
    public String postTestTweet(@RequestParam(defaultValue = "Test tweet from Spring Boot app!") String msg) {
        try {
            twitterService.postTweet(msg);
            return "✅ Tweet sent successfully! Check your Twitter timeline.";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Failed to send tweet: " + e.getMessage();
        }
    }
}