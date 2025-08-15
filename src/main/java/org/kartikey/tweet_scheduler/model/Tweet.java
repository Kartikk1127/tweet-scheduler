package org.kartikey.tweet_scheduler.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "tweets")
public class Tweet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 280)
    @NotBlank(message = "Tweet text cannot be empty")
    @Size(max = 280, message = "Tweet cannot exceed 280 characters")
    private String text;

    @Column(name = "is_posted", nullable = false)
    private boolean posted = false;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor;

    @Column(name = "twitter_id")
    private String twitterId; // Twitter's tweet ID after posting

    @Column(name = "priority")
    private Integer priority = 0; // Higher priority tweets post first

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public Tweet() {}

    public Tweet(String text) {
        this.text = text;
    }

    public Tweet(String text, Integer priority) {
        this.text = text;
        this.priority = priority;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isPosted() { return posted; }
    public void setPosted(boolean posted) { this.posted = posted; }

    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getScheduledFor() { return scheduledFor; }
    public void setScheduledFor(LocalDateTime scheduledFor) { this.scheduledFor = scheduledFor; }

    public String getTwitterId() { return twitterId; }
    public void setTwitterId(String twitterId) { this.twitterId = twitterId; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    @Override
    public String toString() {
        return "Tweet{id=" + id + ", text='" + text + "', posted=" + posted + "}";
    }
}