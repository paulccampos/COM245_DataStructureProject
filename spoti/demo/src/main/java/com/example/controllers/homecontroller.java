package com.example.controllers;

import java.io.IOException;
import java.util.List;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class homecontroller {

    private MongoService mongoService;

    @FXML
    private Label playlist1Label;

    @FXML
    private Label playlist2Label;

    @FXML
    private Label playlist3Label;

    @FXML
    private javafx.scene.layout.HBox mediaPlayerBar;

    @FXML
    private javafx.scene.control.Label currentSongLabel;

    @FXML
    private javafx.scene.control.Button previousButton;

    @FXML
    private javafx.scene.control.Button playPauseButton;

    @FXML
    private javafx.scene.control.Button nextButton;

    @FXML
    private javafx.scene.control.Label durationLabel;

    @FXML
    private javafx.scene.control.ProgressBar progressBar;

    private Timeline progressTimeline;

    public homecontroller() {
        mongoService = new MongoService();
    }

    @FXML
    private void initialize() {
        mongoService.initializeTopHitsPlaylist();
        loadPlaylistNames();
        updateMediaPlayer();
    }

    private void loadPlaylistNames() {
        List<Document> playlists = mongoService.getPlaylists();
        if (playlists.size() > 0) {
            playlist1Label.setText(playlists.get(0).getString("title"));
        }
        if (playlists.size() > 1) {
            playlist2Label.setText(playlists.get(1).getString("title"));
        }
        if (playlists.size() > 2) {
            playlist3Label.setText(playlists.get(2).getString("title"));
        }
    }

    @FXML
    private void onPlaylist1() throws IOException {
        String playlistName = playlist1Label.getText();
        App.setRootWithPlaylist("playlist", playlistName, "home");
    }

    @FXML
    private void onPlaylist2() throws IOException {
        String playlistName = playlist2Label.getText();
        App.setRootWithPlaylist("playlist", playlistName, "home");
    }

    @FXML
    private void onPlaylist3() throws IOException {
        String playlistName = playlist3Label.getText();
        App.setRootWithPlaylist("playlist", playlistName, "home");
    }

    @FXML
    private void onHome() throws IOException {
        App.setRoot("home");
    }

    @FXML
    private void onSearch() throws IOException {
        App.setRoot("search");
    }

    @FXML
    private void onLibrary() throws IOException {
        App.setRoot("library");
    }

    @FXML
    private void onSongs() throws IOException {
        App.setRoot("songs");
    }

    public void updateMediaPlayer() {
        if (App.getCurrentSong() != null) {
            mediaPlayerBar.setVisible(true);
            currentSongLabel.setText(App.getCurrentSong().getString("title"));
            playPauseButton.setText(App.isPlaying() ? "Pause" : "Play");
            // Fix ClassCastException by safely getting duration as string
            Object durationObj = App.getCurrentSong().get("duration");
            String durationStr = "3:45";
            if (durationObj instanceof String) {
                durationStr = (String) durationObj;
            } else if (durationObj instanceof Integer) {
                int durInt = (Integer) durationObj;
                durationStr = String.format("%d:%02d", durInt / 60, durInt % 60);
            }
            durationLabel.setText("0:00 / " + durationStr);
            progressBar.setProgress(0); // Reset progress bar when new song is played

            // Start or stop the progress timeline based on playing status
            if (App.isPlaying()) {
                startProgressTimeline();
            } else {
                stopProgressTimeline();
            }
        } else {
            // If no current song, play next song in queue if available
            if (!App.getQueue().isEmpty()) {
                App.next();
                updateMediaPlayer();
            } else {
                mediaPlayerBar.setVisible(false);
                stopProgressTimeline();
            }
        }
    }

    private void startProgressTimeline() {
        if (progressTimeline != null) {
            progressTimeline.stop();
        }
        progressTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateProgress()));
        progressTimeline.setCycleCount(Timeline.INDEFINITE);
        progressTimeline.play();
    }

    private void stopProgressTimeline() {
        if (progressTimeline != null) {
            progressTimeline.stop();
        }
    }

    private void updateProgress() {
        if (App.getCurrentSong() != null) {
            String currentTimeStr = App.getCurrentTimeFormatted();
            String totalTimeStr = App.getTotalDurationFormatted();
            durationLabel.setText(currentTimeStr + " / " + totalTimeStr);

            // Parse times to calculate progress
            double currentSeconds = parseTimeToSeconds(currentTimeStr);
            double totalSeconds = parseTimeToSeconds(totalTimeStr);
            if (totalSeconds > 0) {
                double progress = currentSeconds / totalSeconds;
                progressBar.setProgress(progress);

                if (progress >= 1.0) {
                    // Song finished
                    if (!App.getQueue().isEmpty()) {
                        App.next();
                        updateMediaPlayer();
                    } else {
                        App.pause();
                        updateMediaPlayer();
                    }
                }
            }
        }
    }

    private double parseTimeToSeconds(String timeStr) {
        String[] parts = timeStr.split(":");
        if (parts.length == 2) {
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            return minutes * 60 + seconds;
        }
        return 0;
    }

    @FXML
    private void onPrevious() {
        App.previous();
        updateMediaPlayer();
    }

    @FXML
    private void onPlayPause() {
        App.togglePlayPause();
        updateMediaPlayer();
    }

    @FXML
    private void onNext() {
        App.next();
        updateMediaPlayer();
    }

    @FXML
    private void onViewQueue() throws IOException {
        App.setRoot("queue");
    }
}
