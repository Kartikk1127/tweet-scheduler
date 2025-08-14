package org.kartikey.tweet_scheduler.scheduler;

import org.kartikey.tweet_scheduler.model.TweetEntry;
import org.kartikey.tweet_scheduler.twitter.TwitterService;
import org.kartikey.tweet_scheduler.util.CsvLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TweetSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(TweetSchedulerService.class);

    private final CsvLoader csvLoader;
    private final TwitterService twitterService;
    private final Random random = new Random();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final AtomicInteger tweetIndex = new AtomicInteger(0);

    public TweetSchedulerService(CsvLoader csvLoader, TwitterService twitterService) {
        this.csvLoader = csvLoader;
        this.twitterService = twitterService;
    }


    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Kolkata")
    public void postHourlyTweet() {
        Path path = Paths.get("tweets.csv");
        try {
            List<TweetEntry> tweets = csvLoader.loadTweets("tweets.csv");

            if (tweets.isEmpty()) {
                log.warn("No tweets found in CSV.");
                return;
            }

            TweetEntry tweet = tweets.get(0); // first in order
            twitterService.postTweet(tweet.getText());
            log.info("Posted tweet #{}: {}", tweet.getId(), tweet.getText());

            // Remove first tweet and rewrite CSV
            List<TweetEntry> remainingTweets = tweets.subList(1, tweets.size());
            writeTweetsToCsv(path, remainingTweets);

        } catch (Exception e) {
            log.error("Error posting tweet: {}", e.getMessage(), e);
        }
    }

    private void writeTweetsToCsv(Path path, List<TweetEntry> tweets) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (TweetEntry tweet : tweets) {
                writer.write(tweet.getId() + "," + tweet.getText());
                writer.newLine();
            }
        }
    }

}
