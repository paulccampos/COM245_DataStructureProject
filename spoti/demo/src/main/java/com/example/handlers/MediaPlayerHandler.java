package com.example.handlers;

import java.util.ArrayList;
import java.util.List;

import com.example.App;
import com.example.services.MediaPlayerService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
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
    private Timeline updateTimeline;

    private MediaPlayerHandler() {
        // Private constructor for singleton
        // Set up a timeline to update the UI every second
        setupUpdateTimer();
    }

    private MediaPlayerService getMediaPlayerService() {
        try {
            MediaPlayerService service = App.getMediaPlayerServiceStatic();
            if (service == null) {
                System.err.println("Failed to get MediaPlayerService in MediaPlayerHandler");
            }
            return service;
        } catch (Exception e) {
            System.err.println("Error getting MediaPlayerService: " + e.getMessage());
            return null;
        }
    }

    private void setupUpdateTimer() {
        updateTimeline = new Timeline(
            new KeyFrame(Duration.seconds(1), event -> {
                // Update UI components every second
                updateMediaPlayer();
            })
        );
        updateTimeline.setCycleCount(Timeline.INDEFINITE);
        updateTimeline.play();
    }

    public static MediaPlayerHandler getInstance() {
        if (instance == null) {
            instance = new MediaPlayerHandler();
        }
        return instance;
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

        // Add click-to-seek functionality to progress bar
        if (progressBar != null) {
            progressBar.setOnMouseClicked(event -> {
                System.out.println("Progress bar clicked at position: " + event.getX() + " / " + progressBar.getWidth());
                MediaPlayerService service = getMediaPlayerService();
                if (service != null) {
                    System.out.println("MediaPlayerService found, checking MediaPlayer...");
                    if (service.getMediaPlayer() != null) {
                        System.out.println("MediaPlayer exists, status: " + service.getMediaPlayer().getStatus());
                        if (service.getMediaPlayer().getStatus() == javafx.scene.media.MediaPlayer.Status.READY ||
                            service.getMediaPlayer().getStatus() == javafx.scene.media.MediaPlayer.Status.PLAYING ||
                            service.getMediaPlayer().getStatus() == javafx.scene.media.MediaPlayer.Status.PAUSED) {
                            System.out.println("MediaPlayer is in seekable state, checking totalDuration...");
                            if (service.getMediaPlayer().getTotalDuration() != null) {
                                double clickPosition = event.getX() / progressBar.getWidth();
                                System.out.println("Seeking to position: " + (clickPosition * 100) + "%");
                                service.seekToPosition(clickPosition);
                            } else {
                                System.err.println("Cannot seek: MediaPlayer totalDuration is null");
                            }
                        } else {
                            System.err.println("Cannot seek: MediaPlayer status is " + service.getMediaPlayer().getStatus() + ", not READY/PLAYING/PAUSED");
                        }
                    } else {
                        System.err.println("Cannot seek: MediaPlayer is null");
                    }
                } else {
                    System.err.println("Cannot seek: MediaPlayerService is null");
                }
            });
        }

        // Set up duration label listener (only once during registration)
        MediaPlayerService service = getMediaPlayerService();
        if (durationLabel != null && service != null && service.getMediaPlayer() != null) {
            service.getMediaPlayer().currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                if (durationLabel != null && service.getMediaPlayer().totalDurationProperty().get() != null) {
                    durationLabel.setText(service.getCurrentTimeFormatted() + " / " + service.getTotalDurationFormatted());
                }
            });
            service.getMediaPlayer().totalDurationProperty().addListener((obs, oldDur, newDur) -> {
                if (durationLabel != null && newDur != null) {
                    durationLabel.setText(service.getCurrentTimeFormatted() + " / " + service.getTotalDurationFormatted());
                }
            });
        }

        updateMediaPlayer();
    }

    public void updateMediaPlayer() {
        MediaPlayerService service = getMediaPlayerService();
        if (mediaPlayerBar != null && service != null) {
            // Always keep the media player bar visible (like Spotify)
            mediaPlayerBar.setVisible(true);
            mediaPlayerBar.setManaged(true);

            if (service.getCurrentSong() != null) {
                // Song is playing - show song info and enable controls
                if (currentSongLabel != null) {
                    currentSongLabel.setText(service.getCurrentSong().getTitle() + " - " + service.getCurrentSong().getArtist());
                    currentSongLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                }
                if (playPauseButton != null) {
                    playPauseButton.setText(service.isPlaying() ? "⏸" : "▶");
                    playPauseButton.setDisable(false);
                }
                if (previousButton != null) {
                    previousButton.setDisable(false);
                }
                if (nextButton != null) {
                    nextButton.setDisable(false);
                }
                if (progressBar != null && service.getMediaPlayer() != null) {
                    // Bind progress bar to MediaPlayer's current time using the service method
                    progressBar.progressProperty().bind(
                        Bindings.createDoubleBinding(() -> service.getProgress(),
                            service.getMediaPlayer().currentTimeProperty(),
                            service.getMediaPlayer().totalDurationProperty()
                        )
                    );
                    progressBar.setDisable(false);
                }
                if (durationLabel != null) {
                    durationLabel.setText(service.getCurrentTimeFormatted() + " / " + service.getTotalDurationFormatted());
                    durationLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                }

            } else {
                // No song playing - show default state but keep controls visible
                if (currentSongLabel != null) {
                    currentSongLabel.setText("No song playing");
                    currentSongLabel.setStyle("-fx-text-fill: #888; -fx-font-weight: normal;");
                }
                if (playPauseButton != null) {
                    playPauseButton.setText("▶");
                    playPauseButton.setDisable(true);
                }
                if (previousButton != null) {
                    previousButton.setDisable(true);
                }
                if (nextButton != null) {
                    nextButton.setDisable(true);
                }
                if (progressBar != null) {
                    progressBar.progressProperty().unbind();
                    progressBar.setProgress(0.0);
                    progressBar.setDisable(true);
                }
                if (durationLabel != null) {
                    durationLabel.setText("0:00 / 0:00");
                    durationLabel.setStyle("-fx-text-fill: #888;");
                }
            }
        }

        // Notify all registered callbacks
        for (Runnable callback : updateCallbacks) {
            callback.run();
        }
    }



    public void addUpdateCallback(Runnable callback) {
        updateCallbacks.add(callback);
    }

    public void removeUpdateCallback(Runnable callback) {
        updateCallbacks.remove(callback);
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
