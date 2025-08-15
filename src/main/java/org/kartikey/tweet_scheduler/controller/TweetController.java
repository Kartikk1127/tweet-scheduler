package org.kartikey.tweet_scheduler.controller;

import org.kartikey.tweet_scheduler.model.Tweet;
import org.kartikey.tweet_scheduler.service.CsvImportService;
import org.kartikey.tweet_scheduler.service.TweetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tweets")
public class TweetController {

    @Autowired
    private TweetService tweetService;

    @Autowired
    private CsvImportService csvImportService;

    @GetMapping
    public List<Tweet> getAllTweets() {
        return tweetService.getAllTweets();
    }

    @GetMapping("/unposted")
    public List<Tweet> getUnpostedTweets() {
        return tweetService.getAllUnpostedTweets();
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        long total = tweetService.getAllTweets().size();
        long unposted = tweetService.getUnpostedTweetCount();

        return Map.of(
                "total", total,
                "posted", total - unposted,
                "unposted", unposted,
                "scheduled", tweetService.getScheduledTweets().size()
        );
    }

    @PostMapping
    public Tweet createTweet(@Valid @RequestBody Tweet tweet) {
        return tweetService.saveTweet(tweet);
    }

    @PostMapping("/import-csv")
    public ResponseEntity<Map<String, Object>> importCsv(@RequestParam("file") MultipartFile file) {
        try {
            List<Tweet> importedTweets = csvImportService.importTweetsFromCsv(file);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully imported " + importedTweets.size() + " tweets",
                    "imported", importedTweets.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to import CSV: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<Map<String, Object>> postTweet(@PathVariable Long id) {
        try {
            Tweet tweet = tweetService.getTweetById(id)
                    .orElseThrow(() -> new RuntimeException("Tweet not found"));

            Tweet postedTweet = tweetService.postTweet(tweet);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tweet posted successfully",
                    "twitterId", postedTweet.getTwitterId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to post tweet: " + e.getMessage()
            ));
        }
    }

//    @PostMapping("/post-next")
//    public ResponseEntity<Map<String, Object>> postNextTweet() {
//        return tweetService.postNextTweet()
//                .map(tweet -> ResponseEntity.ok(Map.of(
//                        "success", true,
//                        "message", "Tweet posted successfully",
//                        "tweetId", tweet.getId(),
//                        "twitterId", tweet.getTwitterId()
//                )))
//                .orElse(ResponseEntity.badRequest().body(Map.of(
//                        "success", false,
//                        "message", "No tweets available to post or posting failed"
//                )));
//    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTweet(@PathVariable Long id) {
        try {
            tweetService.deleteTweet(id);
            return ResponseEntity.ok(Map.of("message", "Tweet deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
