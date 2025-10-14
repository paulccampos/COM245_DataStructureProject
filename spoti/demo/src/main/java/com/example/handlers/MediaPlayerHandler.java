package com.example.handlers;

import java.util.ArrayList;
import java.util.List;

import com.example.App;
import com.example.controllers.queuecontroller;
import com.example.services.MediaPlayerService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

public class MediaPlayerHandler {

    private static MediaPlayerHandler instance;

    private HBox mediaPlayerBar;
    private Label currentSongLabel;
    private Label currentArtistLabel;
    private Label currentTimeLabel;
    private Label durationLabel;
    private ProgressBar progressBar;
    private Button previousButton;
    private Button playPauseButton;
    private Button nextButton;
    private ImageView currentSongImage;

    private queuecontroller queueController;

    private List<Runnable> updateCallbacks = new ArrayList<>();
    private Timeline updateTimeline;

    private MediaPlayerHandler() {
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



    public void registerUIComponents(HBox mediaPlayerBar, Label currentSongLabel, Label currentArtistLabel, Label currentTimeLabel, Label durationLabel,
                                   ProgressBar progressBar, Button previousButton, Button playPauseButton, Button nextButton, ImageView currentSongImage) {
        this.mediaPlayerBar = mediaPlayerBar;
        this.currentSongLabel = currentSongLabel;
        this.currentArtistLabel = currentArtistLabel;
        this.currentTimeLabel = currentTimeLabel;
        this.durationLabel = durationLabel;
        this.progressBar = progressBar;
        this.previousButton = previousButton;
        this.playPauseButton = playPauseButton;
        this.nextButton = nextButton;
        this.currentSongImage = currentSongImage;

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

        updateMediaPlayer();
    }

    public void updateMediaPlayer() {
        MediaPlayerService service = getMediaPlayerService();
        if (mediaPlayerBar != null && service != null) {
            mediaPlayerBar.setVisible(true);
            mediaPlayerBar.setManaged(true);

            if (service.getCurrentSong() != null) {
                if (currentSongLabel != null) {
                    currentSongLabel.setText(service.getCurrentSong().getTitle());
                }
                if (currentArtistLabel != null) {
                    currentArtistLabel.setText(service.getCurrentSong().getArtist());
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
                    progressBar.progressProperty().bind(
                        Bindings.createDoubleBinding(() -> service.getProgress(),
                            service.getMediaPlayer().currentTimeProperty(),
                            service.getMediaPlayer().totalDurationProperty()
                        )
                    );
                    progressBar.setDisable(false);
                }
                if (currentTimeLabel != null) {
                    currentTimeLabel.setText(service.getCurrentTimeFormatted());
                }
                if (durationLabel != null) {
                    durationLabel.setText(service.getTotalDurationFormatted());
                }
                if (currentSongImage != null) {
                    String imagePath = service.getCurrentSong().getImagePath();
                    if (imagePath != null && imagePath.startsWith("/com/example/images/Albums")) {
                        imagePath = imagePath.replace("/com/example/images/Albums", "/Albums");
                    }
            if (imagePath == null) {
                        imagePath = "/com/example/images/default.png";
                    }
                    try {
                        java.net.URL url = getClass().getResource(imagePath);
                        if (url != null) {
                            currentSongImage.setImage(new javafx.scene.image.Image(url.toExternalForm()));
                        } else {
                            System.out.println("Image resource not found: " + imagePath);
                        }
                    } catch (Exception e) {
                        System.err.println("Exception while loading image: " + imagePath);
                        e.printStackTrace();
                    }
                }

            } else {
                if (currentSongLabel != null) {
                    currentSongLabel.setText("No song playing");
                }
                if (currentArtistLabel != null) {
                    currentArtistLabel.setText("");
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
                if (currentTimeLabel != null) {
                    currentTimeLabel.setText("0:00");
                }
                if (durationLabel != null) {
                    durationLabel.setText("0:00");
                }
                if (currentSongImage != null) {
                    currentSongImage.setImage(null);
                }
            }
        }

        for (Runnable callback : updateCallbacks) {
            callback.run();
        }

        if (queueController != null) {
            queueController.refreshQueue();
        }
    }



    public void addUpdateCallback(Runnable callback) {
        updateCallbacks.add(callback);
    }

    public void removeUpdateCallback(Runnable callback) {
        updateCallbacks.remove(callback);
    }

    public void registerQueueController(queuecontroller controller) {
        this.queueController = controller;
    }

}
