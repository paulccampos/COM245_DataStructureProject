package com.example.controllers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;
import com.example.models.Song;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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

public class playlistcontroller {

    private MongoService mongoService;
    private String playlistName;
    private String previousView;

    @FXML
    private Label playlistNameLabel;

    @FXML
    private Label topPlaylistNameLabel;

    @FXML
    private Label playlistDescriptionLabel;

    @FXML
    private Button addToLibraryButton;

    @FXML
    private VBox songsVBox;

    @FXML
    private ComboBox<String> sortComboBox;

    @FXML
    private Label currentSongLabel;

    @FXML
    private Button previousButton;

    @FXML
    private Button playPauseButton;

    @FXML
    private Button nextButton;

    @FXML
    private Button viewQueueButton;

    @FXML
    private Button clearQueueButton;

    @FXML
    private Label durationLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private HBox mediaPlayerBar;

    @FXML
    private Button shuffleButton;

    private static String pendingPlaylistName;
    private static String pendingPreviousView;

    public playlistcontroller() {
        // Use singleton services from App
        App app = new App();
        mongoService = app.getMongoService();
    }

    @FXML
    private void initialize() {
        // Check if there's pending playlist data and set it up
        if (pendingPlaylistName != null) {
            this.playlistName = pendingPlaylistName;
            this.previousView = pendingPreviousView;
            // Clear pending data
            pendingPlaylistName = null;
            pendingPreviousView = null;
        }

        // Set up the UI with the playlist name
        if (this.playlistName != null) {
            setPlaylistDetails();
            loadSongs();
        }

        // Set up sorting listener
        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> loadSongs());

        // Register UI components with MediaPlayerHandler
        com.example.handlers.MediaPlayerHandler.getInstance().registerUIComponents(
            mediaPlayerBar, currentSongLabel, durationLabel, progressBar,
            previousButton, playPauseButton, nextButton);
    }

    public static void setPendingPlaylist(String playlistName, String previousView) {
        pendingPlaylistName = playlistName;
        pendingPreviousView = previousView;
    }

    public void setPlaylistName(String playlistName) {
        // This method is kept for backward compatibility but the main logic is now in initialize()
        this.playlistName = playlistName;
        this.previousView = null; // Default to home if not set

        // Update UI elements
        setPlaylistDetails();

        // Load songs if UI is ready
        if (songsVBox != null) {
            loadSongs();
        }
    }

    private void setPlaylistDetails() {
        Document playlist = mongoService.getPlaylist(playlistName, App.getCurrentUsername());
        if (playlist != null) {
            String name = playlist.getString("title");
            String description = playlist.getString("description");
            if (playlistNameLabel != null) {
                playlistNameLabel.setText(name != null ? name : playlistName);
            }
            if (topPlaylistNameLabel != null) {
                topPlaylistNameLabel.setText(name != null ? name : playlistName);
            }
            if (playlistDescriptionLabel != null) {
                playlistDescriptionLabel.setText(description != null ? description : "");
            }
        } else {
            if (playlistNameLabel != null) {
                playlistNameLabel.setText(playlistName);
            }
            if (topPlaylistNameLabel != null) {
                topPlaylistNameLabel.setText(playlistName);
            }
            if (playlistDescriptionLabel != null) {
                playlistDescriptionLabel.setText("");
            }
        }
    }

    public void setPreviousView(String previousView) {
        this.previousView = previousView;
    }

    private void loadSongs() {
        String username = App.getCurrentUsername();
        List<Document> songs = mongoService.getSongsForPlaylist(playlistName, username);
        String sortBy = sortComboBox.getValue();
        if ("Artist".equals(sortBy)) {
            quickSort(songs, 0, songs.size() - 1, "artist");
        } else if ("Title".equals(sortBy)) {
            quickSort(songs, 0, songs.size() - 1, "title");
        } else if ("Duration".equals(sortBy)) {
            radixSort(songs);
        }
        songsVBox.getChildren().clear();

        // Add total duration label above header
        int totalDurationSeconds = 0;
        for (Document song : songs) {
            totalDurationSeconds += getDurationInSeconds(song);
        }
        int totalMinutes = totalDurationSeconds / 60;
        int totalSeconds = totalDurationSeconds % 60;
        Label totalDurationLabel = new Label("Total Playlist Duration: " + totalMinutes + ":" + String.format("%02d", totalSeconds));
        totalDurationLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 5 0;");
        songsVBox.getChildren().add(totalDurationLabel);

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
        actionsHeader.setPrefWidth(250);
        Label favoriteHeader = new Label("Favorite");
        favoriteHeader.setPrefWidth(80);
        headerBox.getChildren().addAll(titleHeader, artistHeader, albumHeader, durationHeader, favoriteHeader, actionsHeader);
        songsVBox.getChildren().add(headerBox);

        for (Document song : songs) {
            String title = song.getString("title");
            String artist = song.getString("artist");
            String album = song.getString("album");
            Object durationObj = song.get("duration");
            String duration;
            if (durationObj instanceof String) {
                duration = (String) durationObj;
            } else if (durationObj instanceof Integer) {
                int durInt = (Integer) durationObj;
                duration = String.format("%d:%02d", durInt / 60, durInt % 60);
            } else {
                duration = "3:45"; // Default duration
            }

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
            MenuItem deleteFromPlaylistItem = new MenuItem("Delete from Playlist");
            deleteFromPlaylistItem.setOnAction(e -> deleteSongFromPlaylist(song));
            contextMenu.getItems().addAll(addToPlaylistItem, addToQueueItem, deleteFromPlaylistItem);

            Button menuButton = new Button("...");
            menuButton.setOnMouseClicked((MouseEvent e) -> {
                if (!contextMenu.isShowing()) {
                    contextMenu.show(menuButton, e.getScreenX(), e.getScreenY());
                } else {
                    contextMenu.hide();
                }
            });

            // Small nudge to move button left from right edge
            javafx.scene.layout.Region postNudge = new javafx.scene.layout.Region();
            postNudge.setPrefWidth(20);

            Label favoriteLabel = new Label();
            favoriteLabel.setPrefWidth(80);
            // For demonstration, mark songs with title length even as favorite (replace with real logic)
            if (title.length() % 2 == 0) {
                favoriteLabel.setText("★");
            } else {
                favoriteLabel.setText("");
            }

            songBox.getChildren().addAll(titleLabel, artistLabel, albumLabel, durationLabel, favoriteLabel, spacer, menuButton, postNudge);
            songsVBox.getChildren().add(songBox);
        }
    }

    private void quickSort(List<Document> list, int low, int high, String key) {
        if (low < high) {
            // Partitioning logic
            String pivot = list.get(high).getString(key);
            int i = low - 1;
            for (int j = low; j < high; j++) {
                if (list.get(j).getString(key).compareTo(pivot) <= 0) {
                    i++;
                    Document temp = list.get(i);
                    list.set(i, list.get(j));
                    list.set(j, temp);
                }
            }
            Document temp = list.get(i + 1);
            list.set(i + 1, list.get(high));
            list.set(high, temp);
            int pi = i + 1;
            // Recursive calls
            quickSort(list, low, pi - 1, key);
            quickSort(list, pi + 1, high, key);
        }
    }

    private void radixSort(List<Document> list) {
        if (list.isEmpty()) return;
        // Find max duration in seconds
        int max = 0;
        for (Document song : list) {
            int dur = getDurationInSeconds(song);
            if (dur > max) max = dur;
        }
        // Perform counting sort for each digit
        for (int exp = 1; max / exp > 0; exp *= 10) {
            countingSort(list, exp);
        }
    }

    private void countingSort(List<Document> list, int exp) {
        int n = list.size();
        Document[] output = new Document[n];
        int[] count = new int[10];
        // Count occurrences
        for (Document song : list) {
            int dur = getDurationInSeconds(song);
            count[(dur / exp) % 10]++;
        }
        // Cumulative count
        for (int i = 1; i < 10; i++) {
            count[i] += count[i - 1];
        }
        // Build output array
        for (int i = n - 1; i >= 0; i--) {
            Document song = list.get(i);
            int dur = getDurationInSeconds(song);
            output[count[(dur / exp) % 10] - 1] = song;
            count[(dur / exp) % 10]--;
        }
        // Copy back to list
        for (int i = 0; i < n; i++) {
            list.set(i, output[i]);
        }
    }

    private int getDurationInSeconds(Document song) {
        Object durationObj = song.get("duration");
        if (durationObj instanceof String) {
            String durStr = (String) durationObj;
            String[] parts = durStr.split(":");
            if (parts.length == 2) {
                int min = Integer.parseInt(parts[0]);
                int sec = Integer.parseInt(parts[1]);
                return min * 60 + sec;
            }
        } else if (durationObj instanceof Integer) {
            return (Integer) durationObj;
        }
        return 225; // Default 3:45
    }

    private void playSong(Document song) {
        Song songObj = new Song(song);
        App.getMediaPlayerServiceStatic().playSong(songObj);
        com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
    }



    private void addSongToPlaylist(Document song) {
        String username = App.getCurrentUsername();
        List<Document> playlists = mongoService.getPlaylistsForUser(username);
        if (playlists.isEmpty()) {
            System.out.println("No playlists available. Create a playlist first.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>();
        dialog.setTitle("Select Playlist");
        dialog.setHeaderText("Choose a playlist to add the song to");
        dialog.setContentText("Playlist:");

        for (Document playlist : playlists) {
            dialog.getItems().add(playlist.getString("title"));
        }

        dialog.setSelectedItem(playlists.get(0).getString("title"));

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String selectedPlaylist = result.get();
            if (mongoService.isSongInPlaylist(selectedPlaylist, song)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Duplicate Song");
                alert.setHeaderText("Song Already in Playlist");
                alert.setContentText("The song '" + song.getString("title") + "' by " + song.getString("artist") + " is already in the playlist '" + selectedPlaylist + "'.");
                alert.showAndWait();
            } else {
                mongoService.addSongToPlaylist(selectedPlaylist, song, username);
                System.out.println("Song added to playlist: " + selectedPlaylist);
            }
        }
    }

    private void addSongToQueue(Document song) {
        Song songObj = new Song(song);
        App.getMediaPlayerServiceStatic().addManualToQueue(songObj);
    }

    private void deleteSongFromPlaylist(Document song) {
        String username = App.getCurrentUsername();
        mongoService.deleteSongFromPlaylist(playlistName, song, username);
        loadSongs(); // Refresh the list
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

    @FXML
    private void onBack() throws IOException {
        if (previousView != null) {
            App.setRoot(previousView);
        } else {
            App.setRoot("home");
        }
    }



    @FXML
    private void onPrevious() {
        App.getMediaPlayerServiceStatic().previous();
        com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
    }

    @FXML
    private void onPlayPause() {
        App.getMediaPlayerServiceStatic().togglePlayPause();
        com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
    }

    @FXML
    private void onNext() {
        App.getMediaPlayerServiceStatic().next();
        com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
    }

    @FXML
    private void onViewQueue() throws IOException {
        App.setCurrentPlaylistName(playlistName);
        App.setRoot("queue");
    }

    @FXML
    private void onClearQueue() {
        App.getMediaPlayerServiceStatic().clearQueue();
        com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
    }

    @FXML
    private void onShuffle() {
        String username = App.getCurrentUsername();
        List<Document> songs = mongoService.getSongsForPlaylist(playlistName, username);
        if (!songs.isEmpty()) {
            // Set shuffle mode
            App.getMediaPlayerServiceStatic().setShuffleMode(true);
            // Clear the existing queue before shuffling
            App.getMediaPlayerServiceStatic().clearQueue();

            // Shuffle the songs list
            Collections.shuffle(songs, new Random());

            // Play the first song in the shuffled list
            Document firstSong = songs.get(0);
            Song firstSongObj = new Song(firstSong);
            App.getMediaPlayerServiceStatic().playSong(firstSongObj);

            // Add the remaining songs to the queue in shuffled order
            for (int i = 1; i < songs.size(); i++) {
                Song songObj = new Song(songs.get(i));
                App.getMediaPlayerServiceStatic().addAutoToQueue(songObj);
            }

            // Update the UI
            com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
        }
    }

    @FXML
    private void onAddToLibrary() {
        String username = App.getCurrentUsername();
        if (username != null && !username.isEmpty()) {
            mongoService.addToLikedPlaylists(playlistName, username);
            addToLibraryButton.setText("Added to Library");
            addToLibraryButton.setDisable(true);
            System.out.println("Playlist '" + playlistName + "' added to liked playlists for user: " + username);
        } else {
            System.out.println("No user logged in.");
        }
    }
}
