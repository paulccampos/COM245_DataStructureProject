package com.example.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;
import com.example.handlers.MediaPlayerHandler;
import com.example.services.MediaPlayerService;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MainController {

    @FXML
    private BorderPane mainPane;

    @FXML
    private HBox mediaPlayerBar;

    @FXML
    private Label currentSongLabel;

    @FXML
    private Label currentArtistLabel;

    @FXML
    private Label currentTimeLabel;

    @FXML
    private Button previousButton;

    @FXML
    private Button playPauseButton;

    @FXML
    private Button nextButton;

    @FXML
    private Label durationLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button fullscreenButton;

    @FXML
    private ImageView currentSongImage;

    @FXML
    private VBox userPlaylistsVBox;

    private MediaPlayerService mediaPlayerService;
    private MongoService mongoService;
    private String currentViewName;

    @FXML
    private void initialize() {
        this.mediaPlayerService = App.getMediaPlayerServiceStatic();
        this.mongoService = App.getMongoServiceStatic();

        // Clear any current song to prevent auto-play on app open
        mediaPlayerService.clearCurrentSong();

        // Register UI components with MediaPlayerHandler
        MediaPlayerHandler.getInstance().registerUIComponents(
            mediaPlayerBar, currentSongLabel, currentArtistLabel, currentTimeLabel, durationLabel, progressBar,
            previousButton, playPauseButton, nextButton, currentSongImage);

        loadUserPlaylists();

        // Load the initial home view
        loadView("home");
    }

    private void loadView(String fxmlName) {
        this.currentViewName = fxmlName;
        try {
            // Note: We assume the FXML files are now in a 'views' subfolder
            // e.g., /com/example/fxml/views/home.fxml
            Parent view = FXMLLoader.load(getClass().getResource("/com/example/fxml/" + fxmlName + ".fxml"));
            mainPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setView(Parent view) {
        mainPane.setCenter(view);
    }

    private void loadUserPlaylists() {
        if (userPlaylistsVBox == null) return;

        userPlaylistsVBox.getChildren().clear();
        String username = App.getCurrentUsername();
        if (username != null) {
            // Get user's own playlists
            List<Document> userPlaylists = mongoService.getPlaylistsForUser(username);
            // Get liked playlist titles
            List<String> likedPlaylistTitles = mongoService.getLikedPlaylistTitles(username);

            // Combine and deduplicate playlists
            List<Document> allPlaylistsToShow = new ArrayList<>();
            java.util.Set<String> addedPlaylistTitles = new java.util.HashSet<>(); // To track unique titles

            // Add user's own playlists first
            for (Document playlist : userPlaylists) {
                String title = playlist.getString("title");
                if (addedPlaylistTitles.add(title)) { // Add if not already present
                    allPlaylistsToShow.add(playlist);
                }
            }

            // Add liked playlists (if not already added as user's own)
            for (String likedTitle : likedPlaylistTitles) {
                if (!addedPlaylistTitles.contains(likedTitle)) { // Only add if not already added
                    Document likedPlaylist = mongoService.getPlaylist(likedTitle, username); // Try user's own
                    if (likedPlaylist == null) {
                        likedPlaylist = mongoService.getPlaylist(likedTitle, "admin"); // Try admin's
                    }
                    if (likedPlaylist != null) {
                        allPlaylistsToShow.add(likedPlaylist);
                        addedPlaylistTitles.add(likedTitle); // Mark as added
                    }
                }
            }

            for (Document playlist : allPlaylistsToShow) {
                String title = playlist.getString("title");
                Hyperlink link = new Hyperlink(title);
                link.setOnAction(e -> {
                    playlistcontroller.setPendingPlaylist(title, "home");
                    loadView("playlist"); // Load playlist view in the center
                });
                userPlaylistsVBox.getChildren().add(link);
            }
        }
    }

    @FXML
    private void onHome() {
        loadView("home");
    }

    @FXML
    private void onSearch() {
        loadView("search");
    }

    @FXML
    private void onLibrary() {
        loadView("library");
    }

    @FXML
    private void onSongs() {
        loadView("songs");
    }

    @FXML
    private void onProfile() {
        loadView("userprofile");
    }

    @FXML
    private void onPrevious() {
        mediaPlayerService.previous();
    }

    @FXML
    private void onPlayPause() {
        mediaPlayerService.togglePlayPause();
    }

    @FXML
    private void onNext() {
        mediaPlayerService.next();
    }

    @FXML
    private void onViewQueue() throws IOException {
        queuecontroller.setPreviousView(this.currentViewName);
        loadView("queue");
    }
}