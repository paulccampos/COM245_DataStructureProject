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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class playlistcontroller {

    private MongoService mongoService;
    private String playlistName;
    private String previousView;
    private boolean isCreator;
    private String createdBy;

    @FXML
    private Label playlistNameLabel;

    @FXML
    private Label topPlaylistNameLabel;

    @FXML
    private Label playlistDescriptionLabel;

    @FXML
    private VBox songsVBox;

    @FXML
    private VBox topSongsVBox;

    @FXML
    private ComboBox<String> sortComboBox;

    @FXML
    private Button shuffleButton;

    @FXML
    private TextField searchTextField;

    @FXML
    private Button optionsButton;

    private static String pendingPlaylistName;
    private static String pendingPreviousView;

    public playlistcontroller() {
        mongoService = App.getMongoServiceStatic();
    }

    @FXML
    private void initialize() {
        // Check if there's pending playlist data and set it up
        if (pendingPlaylistName != null) {
            this.playlistName = pendingPlaylistName;
            this.previousView = pendingPreviousView;
            pendingPlaylistName = null;
            pendingPreviousView = null;
        }

        if (this.playlistName != null) {
            setPlaylistDetails();
            loadSongs();
        }

        sortComboBox.getItems().addAll("Title", "Artist", "Duration");
        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (searchTextField != null) {
                filterSongs(searchTextField.getText());
            } else {
                loadSongs();
            }
        });

        if (searchTextField != null) {
            searchTextField.textProperty().addListener((obs, oldVal, newVal) -> filterSongs(newVal));
        }
    }

    public static void setPendingPlaylist(String playlistName, String previousView) {
        pendingPlaylistName = playlistName;
        pendingPreviousView = previousView;
    }

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
        this.previousView = null;

        setPlaylistDetails();

        if (songsVBox != null) {
            loadSongs();
        }
    }

    private void setPlaylistDetails() {
        Document playlist = mongoService.getPlaylist(playlistName, App.getCurrentUsername());
        if (playlist != null && optionsButton != null) {
            createdBy = playlist.getString("createdBy");
            String currentUsername = App.getCurrentUsername();
            isCreator = currentUsername.equals(createdBy);
            boolean isLiked = mongoService.getLikedPlaylistTitles(currentUsername).contains(playlistName);

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

            // Load top 4 most played songs
            loadTopSongs();

            ContextMenu contextMenu = new ContextMenu();
            if (isCreator && !createdBy.equals("admin")) {  // Hide delete for admin-created playlists
                MenuItem deleteItem = new MenuItem("Delete Playlist");
                deleteItem.setOnAction(e -> onDeletePlaylist());
                contextMenu.getItems().add(deleteItem);
                MenuItem editNameItem = new MenuItem("Edit name");
                editNameItem.setOnAction(e -> onEditPlaylistName());
                contextMenu.getItems().add(editNameItem);
                MenuItem editDescItem = new MenuItem("Edit description");
                editDescItem.setOnAction(e -> onEditPlaylistDescription());
                contextMenu.getItems().add(editDescItem);
            } else {
                if (isLiked) {
                    MenuItem removeItem = new MenuItem("Remove from Library");
                    removeItem.setOnAction(e -> onRemoveFromLibrary());
                    contextMenu.getItems().add(removeItem);
                } else {
                    MenuItem addItem = new MenuItem("Add to Library");
                    addItem.setOnAction(e -> onAddToLibrary());
                    contextMenu.getItems().add(addItem);
                }
            }

            optionsButton.setOnMouseClicked(event -> contextMenu.show(optionsButton, event.getScreenX(), event.getScreenY()));
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
        filterSongs("");
    }

    private void filterSongs(String filter) {
        String username = App.getCurrentUsername();
        List<Document> songs = mongoService.getSongsForPlaylist(playlistName, username);

        if (filter != null && !filter.trim().isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            songs = songs.stream()
                .filter(s -> s.getString("title").toLowerCase().contains(lowerFilter) ||
                             s.getString("artist").toLowerCase().contains(lowerFilter) ||
                             s.getString("album").toLowerCase().contains(lowerFilter))
                .collect(java.util.stream.Collectors.toList());
        }

        String sortBy = sortComboBox.getValue();
        if ("Artist".equals(sortBy)) {
            quickSort(songs, 0, songs.size() - 1, "artist");
        } else if ("Title".equals(sortBy)) {
            quickSort(songs, 0, songs.size() - 1, "title");
        } else if ("Duration".equals(sortBy)) {
            radixSort(songs);
        }
        songsVBox.getChildren().clear();
        songsVBox.setStyle("-fx-background-color: #0b52bf;");

        int totalDurationSeconds = 0;
        for (Document song : songs) {
            totalDurationSeconds += getDurationInSeconds(song);
        }
        int totalMinutes = totalDurationSeconds / 60;
        int totalSeconds = totalDurationSeconds % 60;
        Label totalDurationLabel = new Label("Total Playlist Duration: " + totalMinutes + ":" + String.format("%02d", totalSeconds));
        totalDurationLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 5 0; -fx-text-fill: -spawtify-subtle-text-color;");
        songsVBox.getChildren().add(totalDurationLabel);

        HBox headerBox = new HBox(20);
        Label imageHeader = new Label();
        imageHeader.setPrefWidth(40);
        Label titleHeader = new Label("Title");
        titleHeader.setPrefWidth(200);
        titleHeader.setStyle("-fx-text-fill: white;");
        Label artistHeader = new Label("Artist");
        artistHeader.setPrefWidth(150);
        artistHeader.setStyle("-fx-text-fill: white;");
        Label albumHeader = new Label("Album");
        albumHeader.setPrefWidth(150);
        albumHeader.setStyle("-fx-text-fill: white;");
        headerBox.getChildren().addAll(imageHeader, titleHeader, artistHeader, albumHeader);
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

            HBox songBox = new HBox(20);
            songBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            ImageView albumArt = new ImageView();
            albumArt.setFitHeight(40);
            albumArt.setFitWidth(40);
            String imagePath = song.getString("imagePath");
            if (imagePath == null) {
                imagePath = "/com/example/images/default.png";
            }
            try {
                java.net.URL url = getClass().getResource(imagePath);
                if (url != null) {
                    albumArt.setImage(new Image(url.toExternalForm()));
                } else {
                    System.out.println("Image resource not found: " + imagePath);
                }
            } catch (Exception e) {
                System.err.println("Exception while loading image: " + imagePath);
                e.printStackTrace();
            }

            Label titleLabel = new Label(title);
            titleLabel.setPrefWidth(200);
            titleLabel.setStyle("-fx-text-fill: white;");
            Label artistLabel = new Label(artist);
            artistLabel.setPrefWidth(150);
            artistLabel.setStyle("-fx-text-fill: white;");
            Label albumLabel = new Label(album);
            albumLabel.setPrefWidth(150);
            albumLabel.setStyle("-fx-text-fill: white;");

            songBox.setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    playSong(song);
                }
            });

            ContextMenu contextMenu = new ContextMenu();
            MenuItem addToPlaylistItem = new MenuItem("Add to Playlist");
            addToPlaylistItem.setOnAction(e -> addSongToPlaylist(song));
            MenuItem addToQueueItem = new MenuItem("Add to Queue");
            addToQueueItem.setOnAction(e -> addSongToQueue(song));
            contextMenu.getItems().addAll(addToPlaylistItem, addToQueueItem);
            if (isCreator) {
                MenuItem deleteFromPlaylistItem = new MenuItem("Delete from Playlist");
                deleteFromPlaylistItem.setOnAction(e -> deleteSongFromPlaylist(song));
                contextMenu.getItems().add(deleteFromPlaylistItem);
            }

            songBox.setOnContextMenuRequested(e -> contextMenu.show(songBox, e.getScreenX(), e.getScreenY()));

            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            Button menuButton = new Button("...");
            menuButton.getStyleClass().add("playlist-button");
            menuButton.setOnMouseClicked((MouseEvent e) -> {
                if (!contextMenu.isShowing()) {
                    contextMenu.show(menuButton, e.getScreenX(), e.getScreenY());
                } else {
                    contextMenu.hide();
                }
            });

            songBox.getChildren().addAll(albumArt, titleLabel, artistLabel, albumLabel, spacer, menuButton);
            songsVBox.getChildren().add(songBox);
        }
    }

    private void quickSort(List<Document> list, int low, int high, String key) {
        if (low < high) {
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
            quickSort(list, low, pi - 1, key);
            quickSort(list, pi + 1, high, key);
        }
    }

    private void radixSort(List<Document> list) {
        if (list.isEmpty()) return;
        int max = 0;
        for (Document song : list) {
            int dur = getDurationInSeconds(song);
            if (dur > max) max = dur;
        }
        for (int exp = 1; max / exp > 0; exp *= 10) {
            countingSort(list, exp);
        }
    }

    private void countingSort(List<Document> list, int exp) {
        int n = list.size();
        Document[] output = new Document[n];
        int[] count = new int[10];
        for (Document song : list) {
            int dur = getDurationInSeconds(song);
            count[(dur / exp) % 10]++;
        }
        // Cumulative count
        for (int i = 1; i < 10; i++) {
            count[i] += count[i - 1];
        }
        for (int i = n - 1; i >= 0; i--) {
            Document song = list.get(i);
            int dur = getDurationInSeconds(song);
            output[count[(dur / exp) % 10] - 1] = song;
            count[(dur / exp) % 10]--;
        }
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
        if (searchTextField != null) {
            filterSongs(searchTextField.getText());
        } else {
            loadSongs();
        }
    }

    @FXML
    private void onBack() throws IOException {
        App.loadViewInMain(previousView != null ? previousView : "home");
    }

    @FXML
    private void onShuffle() {
        String username = App.getCurrentUsername();
        List<Document> songs = mongoService.getSongsForPlaylist(playlistName, username);
        if (!songs.isEmpty()) {
            // Set shuffle mode
            App.getMediaPlayerServiceStatic().setShuffleMode(true);
            App.getMediaPlayerServiceStatic().clearQueue();

            Collections.shuffle(songs, new Random());

            Document firstSong = songs.get(0);
            Song firstSongObj = new Song(firstSong);
            App.getMediaPlayerServiceStatic().playSong(firstSongObj);

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
        mongoService.addToLikedPlaylists(playlistName, username);
        setPlaylistDetails();
        System.out.println("Playlist '" + playlistName + "' added to liked playlists for user: " + username);
    }

    @FXML
    private void onDeletePlaylist() {
        String currentUsername = App.getCurrentUsername();
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to permanently delete this playlist?", ButtonType.YES, ButtonType.NO);
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                mongoService.deletePlaylist(playlistName, currentUsername);
                try {
                    App.loadViewInMain("library");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void onRemoveFromLibrary() {
        String currentUsername = App.getCurrentUsername();
        mongoService.removeFromLikedPlaylists(playlistName, currentUsername);
        setPlaylistDetails();
        System.out.println("Playlist '" + playlistName + "' removed from library for user: " + currentUsername);
    }

    private void onEditPlaylistName() {
        String currentUsername = App.getCurrentUsername();
        Document playlist = mongoService.getPlaylist(playlistName, currentUsername);
        if (playlist == null) return;

        TextInputDialog dialog = new TextInputDialog(playlistName);
        dialog.setTitle("Edit Playlist Name");
        dialog.setHeaderText("Enter a new name for your playlist.");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.trim().isEmpty() && !newName.equals(playlistName)) {
                mongoService.updatePlaylist(playlistName, newName.trim(), playlist.getString("description"), currentUsername);
                this.playlistName = newName.trim();
                setPlaylistDetails();
            }
        });
    }

    private void onEditPlaylistDescription() {
        String currentUsername = App.getCurrentUsername();
        Document playlist = mongoService.getPlaylist(playlistName, currentUsername);
        if (playlist == null) return;

        TextInputDialog dialog = new TextInputDialog(playlist.getString("description"));
        dialog.setTitle("Edit Playlist Description");
        dialog.setHeaderText("Enter a new description for your playlist.");
        dialog.setContentText("Description:");

        dialog.showAndWait().ifPresent(newDescription -> {
            if (!newDescription.equals(playlist.getString("description"))) {
                mongoService.updatePlaylist(playlistName, playlistName, newDescription.trim(), currentUsername);
                setPlaylistDetails();
            }
        });
    }

    private void loadTopSongs() {
        if (topSongsVBox == null) return;

        topSongsVBox.getChildren().clear();

        String username = App.getCurrentUsername();
        List<Document> songs = mongoService.getSongsForPlaylist(playlistName, username);

        // Sort songs by play count (assuming playCount field exists, else use a default)
        songs.sort((a, b) -> {
            int playCountA = a.getInteger("playCount", 0);
            int playCountB = b.getInteger("playCount", 0);
            return Integer.compare(playCountB, playCountA); // Descending order
        });

        // Create a GridPane for the album art like in library
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setAlignment(javafx.geometry.Pos.CENTER);

        int count = Math.min(4, songs.size());
        for (int i = 0; i < count; i++) {
            final int index = i;
            ImageView iv = new ImageView();
            iv.setFitWidth(60);
            iv.setFitHeight(60);
            String path = songs.get(i).getString("imagePath");
            if (path != null && path.startsWith("/com/example/images/Albums")) {
                path = path.replace("/com/example/images/Albums", "/Albums");
            }
            if (path == null) {
                path = "/com/example/images/default.png";
            }
            try {
                java.net.URL url = getClass().getResource(path);
                if (url != null) {
                    Image img = new Image(url.toExternalForm());
                    iv.setImage(img);
                } else {
                    System.out.println("Image resource not found: " + path);
                }
            } catch (Exception e) {
                System.err.println("Exception while loading image: " + path);
                e.printStackTrace();
            }
            iv.setOnMouseClicked(e -> playSong(songs.get(index)));
            grid.add(iv, i % 2, i / 2);
        }

        topSongsVBox.getChildren().add(grid);
    }

}
