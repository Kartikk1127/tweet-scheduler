package org.kartikey.tweet_scheduler.util;

import org.kartikey.tweet_scheduler.model.TweetEntry;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvLoader {

    public List<TweetEntry> loadTweets(String fileName) {
        List<TweetEntry> tweets = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(fileName), StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) { // Skip headers
                    firstLine = false;
                    continue;
                }
                String[] cols = line.split(",", 2);
                if (cols.length == 2) {
                    tweets.add(new TweetEntry(Integer.parseInt(cols[0].trim()), cols[1].trim()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tweets;
    }
}
