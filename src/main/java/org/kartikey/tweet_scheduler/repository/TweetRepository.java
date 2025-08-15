package org.kartikey.tweet_scheduler.repository;


import org.kartikey.tweet_scheduler.model.Tweet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TweetRepository extends JpaRepository<Tweet, Long> {

    // Find the next tweet to post (not posted, highest priority, oldest first)
    @Query("SELECT t FROM Tweet t WHERE t.posted = false AND " +
            "(t.scheduledFor IS NULL OR t.scheduledFor <= :now) " +
            "ORDER BY t.priority DESC, t.createdAt ASC")
    Optional<Tweet> findNextTweetToPost(LocalDateTime now);

    // Find all unposted tweets
    List<Tweet> findByPostedFalseOrderByPriorityDescCreatedAtAsc();

    // Find tweets posted in a time range
    List<Tweet> findByPostedTrueAndPostedAtBetween(LocalDateTime start, LocalDateTime end);

    // Count unposted tweets
    long countByPostedFalse();

    // Find tweets by priority
    List<Tweet> findByPriorityOrderByCreatedAtAsc(Integer priority);

    // Find scheduled tweets
    @Query("SELECT t FROM Tweet t WHERE t.posted = false AND t.scheduledFor > :now " +
            "ORDER BY t.scheduledFor ASC")
    List<Tweet> findScheduledTweets(LocalDateTime now);
}