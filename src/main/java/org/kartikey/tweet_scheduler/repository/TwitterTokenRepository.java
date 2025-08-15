package org.kartikey.tweet_scheduler.repository;

import org.kartikey.tweet_scheduler.model.TwitterToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TwitterTokenRepository extends JpaRepository<TwitterToken, Long> {
}
