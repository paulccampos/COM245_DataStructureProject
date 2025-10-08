package com.example.controllers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;
import com.example.handlers.MediaPlayerHandler;
import com.example.services.MediaPlayerService;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class songscontroller {

    private MongoService mongoService;
    private MediaPlayerService mediaPlayerService;

    @FXML
    private VBox songsVBox;

    @FXML
    private ComboBox<String> sortComboBox;

    @FXML
    private ComboBox<String> genreComboBox;

    public songscontroller() {
        App app = new App();
        this.mongoService = app.getMongoService();
        this.mediaPlayerService = app.getMediaPlayerService();
    }

    @FXML
    private void initialize() {
        populateFilters();
        loadSongs();
        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> loadSongs());
        genreComboBox.valueProperty().addListener((obs, oldVal, newVal) -> loadSongs());
    }

    private void populateFilters() {
        sortComboBox.getItems().addAll("Title", "Artist");

        List<String> genres = mongoService.getAllSongs().stream()
            .flatMap(song -> {
                Object genreObj = song.get("genre");
                if (genreObj instanceof List<?> genreList) {
                    return genreList.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast);
                } else if (genreObj instanceof String genre) {
                    return java.util.stream.Stream.of(genre);
                }
                return java.util.stream.Stream.empty();
            })
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        genreComboBox.getItems().add("All Genres");
        genreComboBox.getItems().addAll(genres);
    }

    private void loadSongs() {
        List<Document> songs = mongoService.getAllSongs();

        String selectedGenre = genreComboBox.getValue();
        if (selectedGenre != null && !"All Genres".equals(selectedGenre)) {
            songs.removeIf(song -> {
                Object genreObj = song.get("genre");
                if (genreObj instanceof List<?> genreList) {
                    return genreList == null || !genreList.contains(selectedGenre);
                } else if (genreObj instanceof String) {
                    String songGenre = (String) genreObj;
                    return songGenre == null || !songGenre.equals(selectedGenre);
                }
                return true;
            });
        }

        String sortBy = sortComboBox.getValue();
        if ("Artist".equals(sortBy)) {
            quickSort(songs, 0, songs.size() - 1, "artist");
        } else if ("Title".equals(sortBy)) {
            quickSort(songs, 0, songs.size() - 1, "title");
        }
        songsVBox.getChildren().clear();

        HBox headerBox = new HBox(10);
        Label titleHeader = new Label("Title");
        titleHeader.setPrefWidth(200);
        Label artistHeader = new Label("Artist");
        artistHeader.setPrefWidth(150);
        Label albumHeader = new Label("Album");
        albumHeader.setPrefWidth(150);
        headerBox.getChildren().addAll(titleHeader, artistHeader, albumHeader);
        songsVBox.getChildren().add(headerBox);

        for (Document song : songs) {
            String title = song.getString("title");
            String artist = song.getString("artist");
            String album = song.getString("album");

            HBox songBox = new HBox(10);
            Label titleLabel = new Label(title);
            titleLabel.setPrefWidth(200);
            Label artistLabel = new Label(artist);
            artistLabel.setPrefWidth(150);
            Label albumLabel = new Label(album);
            albumLabel.setPrefWidth(150);

            javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            songBox.setOnMouseClicked(e -> playSong(song));

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

            songBox.getChildren().addAll(titleLabel, artistLabel, albumLabel, spacer, menuButton);
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
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
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
        com.example.models.Song songModel = new com.example.models.Song(song);
        mediaPlayerService.addManualToQueue(songModel);
    }

    private void playSong(Document song) {
        mediaPlayerService.playSong(new com.example.models.Song(song));
        MediaPlayerHandler.getInstance().updateMediaPlayer();
    }
}
