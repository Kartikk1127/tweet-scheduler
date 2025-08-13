package org.kartikey.tweet_scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TweetSchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TweetSchedulerApplication.class, args);
	}

}
