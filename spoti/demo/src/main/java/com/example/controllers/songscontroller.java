package com.example.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;
import com.example.handlers.MediaPlayerHandler;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class songscontroller {

    private MongoService mongoService;

    @FXML
    private VBox songsVBox;

    @FXML
    private ComboBox<String> sortComboBox;

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
    private Button viewQueueButton;

    @FXML
    private javafx.scene.control.Label durationLabel;

    @FXML
    private ProgressBar progressBar;

    public songscontroller() {
        mongoService = new MongoService();
    }

    @FXML
    private void initialize() {
        loadSongs();
        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> loadSongs());
        // Register UI components with MediaPlayerHandler
        MediaPlayerHandler.getInstance().registerUIComponents(
            mediaPlayerBar, currentSongLabel, durationLabel, progressBar,
            previousButton, playPauseButton, nextButton);
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
    private void onViewQueue() throws IOException {
        App.setRoot("queue");
    }

    @FXML
    private void onPrevious() {
        App.previous();
    }

    @FXML
    private void onPlayPause() {
        App.togglePlayPause();
    }

    @FXML
    private void onNext() {
        App.next();
    }

    private void loadSongs() {
        List<Document> songs = mongoService.getAllSongs();
        String sortBy = sortComboBox.getValue();
        if ("Artist".equals(sortBy)) {
            songs.sort((a, b) -> a.getString("artist").compareTo(b.getString("artist")));
        } else if ("Title".equals(sortBy)) {
            songs.sort((a, b) -> a.getString("title").compareTo(b.getString("title")));
        }
        songsVBox.getChildren().clear();

        // Add header row
        HBox headerBox = new HBox(10);
        Label titleHeader = new Label("Title");
        titleHeader.setPrefWidth(200);
        Label artistHeader = new Label("Artist");
        artistHeader.setPrefWidth(150);
        Label albumHeader = new Label("Album");
        albumHeader.setPrefWidth(150);
        Label durationHeader = new Label("Duration");
        durationHeader.setPrefWidth(100);
        Label actionsHeader = new Label("Actions");
        actionsHeader.setPrefWidth(200);
        headerBox.getChildren().addAll(titleHeader, artistHeader, albumHeader, durationHeader, actionsHeader);
        songsVBox.getChildren().add(headerBox);

        for (Document song : songs) {
            String title = song.getString("title");
            String artist = song.getString("artist");
            String album = song.getString("album");
            Object durationObj = song.get("duration");
            String duration = durationObj != null ? durationObj.toString() : "3:45"; // Default duration

            HBox songBox = new HBox(10);
            Label titleLabel = new Label(title);
            titleLabel.setPrefWidth(200);
            Label artistLabel = new Label(artist);
            artistLabel.setPrefWidth(150);
            Label albumLabel = new Label(album);
            albumLabel.setPrefWidth(150);
            Label durationLabel = new Label(duration);
            durationLabel.setPrefWidth(100);

            // Spacer to push buttons to the right
            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            // Make song row clickable for playback
            songBox.setOnMouseClicked(e -> playSong(song));

            // Context menu for ... button
            ContextMenu contextMenu = new ContextMenu();
            MenuItem addToPlaylistItem = new MenuItem("Add to Playlist");
            addToPlaylistItem.setOnAction(e -> addSongToPlaylist(song));
            MenuItem addToQueueItem = new MenuItem("Add to Queue");
            addToQueueItem.setOnAction(e -> addSongToQueue(song));
            contextMenu.getItems().addAll(addToPlaylistItem, addToQueueItem);

            Button menuButton = new Button("...");
            menuButton.setOnMouseClicked((MouseEvent e) -> {
                if (!contextMenu.isShowing()) {
                    contextMenu.show(menuButton, e.getScreenX(), e.getScreenY());
                } else {
                    contextMenu.hide();
                }
            });

            songBox.getChildren().addAll(titleLabel, artistLabel, albumLabel, durationLabel, spacer, menuButton);
            songsVBox.getChildren().add(songBox);
        }
    }

    private void addSongToPlaylist(Document song) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>();
        dialog.setTitle("Select Playlist");
        dialog.setHeaderText("Choose a playlist to add the song to");
        dialog.setContentText("Playlist:");

        List<Document> playlists = mongoService.getPlaylists();
        for (Document playlist : playlists) {
            dialog.getItems().add(playlist.getString("title"));
        }

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            mongoService.addSongToPlaylist(result.get(), song);
            // Optionally, refresh or show message
        }
    }

    private void addSongToQueue(Document song) {
        App.addToQueue(song);
    }

    private void playSong(Document song) {
        App.playSong(song);
    }
}
