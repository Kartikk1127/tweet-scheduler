package org.kartikey.tweet_scheduler.service;

import org.kartikey.tweet_scheduler.model.Tweet;
import org.kartikey.tweet_scheduler.repository.TweetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TweetService {

    private static final Logger log = LoggerFactory.getLogger(TweetService.class);

    @Autowired
    private TweetRepository tweetRepository;

    @Autowired
    private TwitterService twitterService;

    public Tweet saveTweet(Tweet tweet) {
        log.debug("Saving tweet: {}", tweet.getText());
        return tweetRepository.save(tweet);
    }

    public List<Tweet> saveAllTweets(List<Tweet> tweets) {
        log.info("Saving {} tweets to database", tweets.size());
        return tweetRepository.saveAll(tweets);
    }

    public Optional<Tweet> getNextTweetToPost() {
        return tweetRepository.findNextTweetToPost(LocalDateTime.now());
    }

    public List<Tweet> getAllUnpostedTweets() {
        return tweetRepository.findByPostedFalseOrderByPriorityDescCreatedAtAsc();
    }

    public long getUnpostedTweetCount() {
        return tweetRepository.countByPostedFalse();
    }

    public List<Tweet> getScheduledTweets() {
        return tweetRepository.findScheduledTweets(LocalDateTime.now());
    }

    public Tweet postTweet(Tweet tweet) throws Exception {
        if (tweet.isPosted()) {
            throw new IllegalStateException("Tweet has already been posted");
        }

        String twitterId = twitterService.postTweet(tweet.getText());

        tweet.setPosted(true);
        tweet.setPostedAt(LocalDateTime.now());
        tweet.setTwitterId(twitterId);

        Tweet savedTweet = tweetRepository.save(tweet);
        log.info("Successfully posted and saved tweet ID: {} (Twitter ID: {})",
                savedTweet.getId(), twitterId);

        return savedTweet;
    }

    public Optional<Tweet> postNextTweet() {
        Optional<Tweet> nextTweet = getNextTweetToPost();

        if (nextTweet.isEmpty()) {
            log.info("No tweets available to post");
            return Optional.empty();
        }

        try {
            Tweet postedTweet = postTweet(nextTweet.get());
            return Optional.of(postedTweet);
        } catch (Exception e) {
            log.error("Failed to post tweet ID: {}", nextTweet.get().getId(), e);
            return Optional.empty();
        }
    }

    public void deleteTweet(Long id) {
        tweetRepository.deleteById(id);
        log.info("Deleted tweet ID: {}", id);
    }

    public Optional<Tweet> getTweetById(Long id) {
        return tweetRepository.findById(id);
    }

    public List<Tweet> getAllTweets() {
        return tweetRepository.findAll();
    }
}
