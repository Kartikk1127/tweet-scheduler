package org.kartikey.tweet_scheduler.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.kartikey.tweet_scheduler.model.Tweet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);

    @Autowired
    private TweetService tweetService;

    public List<Tweet> importTweetsFromCsv(MultipartFile file) throws IOException, CsvException {
        List<Tweet> tweets = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {

            List<String[]> records = csvReader.readAll();

            // Skip header row if present
            boolean hasHeader = records.size() > 0 && !isNumeric(records.get(0)[0]);
            int startIndex = hasHeader ? 1 : 0;

            for (int i = startIndex; i < records.size(); i++) {
                String[] record = records.get(i);

                if (record.length >= 2) {
                    String text = record[1].trim();
                    Integer priority = record.length > 2 && isNumeric(record[2]) ?
                            Integer.parseInt(record[2]) : 0;

                    if (!text.isEmpty() && text.length() <= 280) {
                        tweets.add(new Tweet(text, priority));
                    } else {
                        log.warn("Skipping invalid tweet at row {}: too long or empty", i + 1);
                    }
                }
            }
        }

        log.info("Parsed {} tweets from CSV file", tweets.size());
        return tweetService.saveAllTweets(tweets);
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
