package com.example.handlers;

import java.util.ArrayList;
import java.util.List;

import com.example.App;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

public class MediaPlayerHandler {

    private static MediaPlayerHandler instance;

    private HBox mediaPlayerBar;
    private Label currentSongLabel;
    private Label durationLabel;
    private ProgressBar progressBar;
    private Button previousButton;
    private Button playPauseButton;
    private Button nextButton;

    private List<Runnable> updateCallbacks = new ArrayList<>();
    private Timeline progressTimeline;
    private long songStartTime;
    private long songDurationMillis;

    private MediaPlayerHandler() {
        // Private constructor for singleton
        initializeTimeline();
    }

    public static MediaPlayerHandler getInstance() {
        if (instance == null) {
            instance = new MediaPlayerHandler();
        }
        return instance;
    }

    private void initializeTimeline() {
        progressTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateProgress()));
        progressTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    public void registerUIComponents(HBox mediaPlayerBar, Label currentSongLabel, Label durationLabel,
                                   ProgressBar progressBar, Button previousButton, Button playPauseButton, Button nextButton) {
        this.mediaPlayerBar = mediaPlayerBar;
        this.currentSongLabel = currentSongLabel;
        this.durationLabel = durationLabel;
        this.progressBar = progressBar;
        this.previousButton = previousButton;
        this.playPauseButton = playPauseButton;
        this.nextButton = nextButton;

        // Set up button actions
        if (previousButton != null) {
            previousButton.setOnAction(e -> {
                App.previous();
                updateMediaPlayer();
            });
        }
        if (playPauseButton != null) {
            playPauseButton.setOnAction(e -> {
                App.togglePlayPause();
                updateMediaPlayer();
            });
        }
        if (nextButton != null) {
            nextButton.setOnAction(e -> {
                App.next();
                updateMediaPlayer();
            });
        }

        updateMediaPlayer();
    }

    public void updateMediaPlayer() {
        if (mediaPlayerBar != null) {
            if (App.getCurrentSong() != null) {
                mediaPlayerBar.setVisible(true);
                if (currentSongLabel != null) {
                    currentSongLabel.setText(App.getCurrentSong().getString("title") + " - " + App.getCurrentSong().getString("artist"));
                }
                if (durationLabel != null) {
                    durationLabel.setText(App.getCurrentTimeFormatted() + " / " + App.getTotalDurationFormatted());
                }
                if (playPauseButton != null) {
                    playPauseButton.setText(App.isPlaying() ? "⏸" : "▶");
                }
                if (progressBar != null) {
                    // Calculate progress based on current time
                    if (App.isPlaying()) {
                        long currentTime = System.currentTimeMillis() - songStartTime;
                        double progress = (double) currentTime / songDurationMillis;
                        progressBar.setProgress(Math.min(progress, 1.0));
                    } else {
                        progressBar.setProgress(0.0);
                    }
                }

                // Start or stop timeline based on playing state
                if (App.isPlaying()) {
                    if (progressTimeline.getStatus() != Timeline.Status.RUNNING) {
                        songStartTime = System.currentTimeMillis();
                        // Get song duration in milliseconds
                        String durationStr = App.getTotalDurationFormatted();
                        songDurationMillis = parseDurationToMillis(durationStr);
                        progressTimeline.play();
                    }
                } else {
                    progressTimeline.pause();
                }
            } else {
                mediaPlayerBar.setVisible(false);
                progressTimeline.pause();
            }
        }

        // Notify all registered callbacks
        for (Runnable callback : updateCallbacks) {
            callback.run();
        }
    }

    private void updateProgress() {
        if (App.getCurrentSong() != null && App.isPlaying()) {
            long currentTime = System.currentTimeMillis() - songStartTime;
            if (currentTime >= songDurationMillis) {
                // Song ended
                if (!App.getQueue().isEmpty()) {
                    // Play next song
                    App.next();
                    updateMediaPlayer();
                } else {
                    // No more songs, pause
                    App.pause();
                    updateMediaPlayer();
                }
            } else {
                // Update progress bar
                if (progressBar != null) {
                    double progress = (double) currentTime / songDurationMillis;
                    progressBar.setProgress(Math.min(progress, 1.0));
                }
                // Update duration label
                if (durationLabel != null) {
                    durationLabel.setText(App.getCurrentTimeFormatted() + " / " + App.getTotalDurationFormatted());
                }
            }
        }
    }

    private long parseDurationToMillis(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) {
            return 180000; // Default 3 minutes
        }
        try {
            String[] parts = durationStr.split(":");
            if (parts.length == 2) {
                int minutes = Integer.parseInt(parts[0]);
                int seconds = Integer.parseInt(parts[1]);
                return (minutes * 60 + seconds) * 1000L;
            }
        } catch (NumberFormatException e) {
            // Ignore and use default
        }
        return 180000; // Default 3 minutes
    }

    public void addUpdateCallback(Runnable callback) {
        updateCallbacks.add(callback);
    }

    public void removeUpdateCallback(Runnable callback) {
        updateCallbacks.remove(callback);
    }
}
