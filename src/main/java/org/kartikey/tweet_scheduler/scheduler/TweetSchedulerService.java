package org.kartikey.tweet_scheduler.scheduler;


import org.kartikey.tweet_scheduler.service.TweetService;
import org.kartikey.tweet_scheduler.service.TwitterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TweetSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(TweetSchedulerService.class);

    @Autowired
    private TweetService tweetService;

    @Autowired
    private TwitterService twitterService;

    // Post a tweet every hour at minute 0
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Kolkata")
    public void postHourlyTweet() {
        log.info("Starting scheduled tweet posting...");

        if (!twitterService.isReady()) {
            log.warn("Twitter service not ready. Skipping scheduled tweet.");
            return;
        }

        long unpostedCount = tweetService.getUnpostedTweetCount();
        log.info("Found {} unposted tweets", unpostedCount);

        if (unpostedCount == 0) {
            log.warn("No tweets available to post");
            return;
        }

        tweetService.postNextTweet().ifPresentOrElse(
                tweet -> log.info("Successfully posted scheduled tweet ID: {} - '{}'",
                        tweet.getId(), tweet.getText()),
                () -> log.error("Failed to post scheduled tweet")
        );
    }

    // Health check every 30 minutes
    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void healthCheck() {
        long unpostedCount = tweetService.getUnpostedTweetCount();
        log.info("Health check: {} unposted tweets remaining", unpostedCount);

        if (unpostedCount < 5) {
            log.warn("WARNING: Only {} tweets remaining. Consider adding more content.", unpostedCount);
        }
    }
}