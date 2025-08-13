package org.kartikey.tweet_scheduler.scheduler;

import org.kartikey.tweet_scheduler.model.TweetEntry;
import org.kartikey.tweet_scheduler.twitter.TwitterService;
import org.kartikey.tweet_scheduler.util.CsvLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class TweetSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(TweetSchedulerService.class);

    private final CsvLoader csvLoader;
    private final TwitterService twitterService;
    private final Random random = new Random();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public TweetSchedulerService(CsvLoader csvLoader, TwitterService twitterService) {
        this.csvLoader = csvLoader;
        this.twitterService = twitterService;
    }

    // Runs at midnight to plan the next day's tweets
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    public void scheduleDailyTweets() {
        List<TweetEntry> tweets = csvLoader.loadTweets("tweets.csv");
        Collections.shuffle(tweets);
        int count = Math.min(15, tweets.size());
        List<TweetEntry> todaysTweets = tweets.subList(0, count);

        for (TweetEntry tweet : todaysTweets) {
            LocalTime randomTime = LocalTime.of(8 + random.nextInt(16), random.nextInt(60));
            LocalDateTime postDateTime = LocalDate.now().atTime(randomTime);

            long delay = Duration.between(LocalDateTime.now(), postDateTime).toMillis();
            if (delay > 0) {
                executor.schedule(() -> twitterService.postTweet(tweet.getText()), delay, TimeUnit.MILLISECONDS);
                log.info("Scheduled tweet #{} at {}", tweet.getId(), postDateTime);
            }
        }
    }
}
