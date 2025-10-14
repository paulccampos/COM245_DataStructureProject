package com.example.controllers;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class homecontroller {

    private MongoService mongoService;

    @FXML
    private HBox madeForYouVBox;

    @FXML
    private HBox yourPlaylistsVBox;

    @FXML
    private HBox topPlayedVBox;

    @FXML
    private HBox recommendationsVBox;

    @FXML
    private HBox trySomethingNewVBox;

    @FXML
    private Label welcomeLabel;

    @FXML
    private ImageView selectedSongImage;


    public homecontroller() {
        mongoService = App.getMongoServiceStatic();
    }

    @FXML
    private void initialize() {
        mongoService.initializeTopHitsPlaylist();
        loadPlaylists();
        loadTopPlayedSongs();
        loadRecommendedSongs();
        loadTrySomethingNewSongs();
        welcomeLabel.setText("Welcome, " + App.getCurrentUsername() + "!");
    }

    private void loadPlaylists() {
        madeForYouVBox.getChildren().clear();
        yourPlaylistsVBox.getChildren().clear();

        List<Document> adminPlaylists = mongoService.getPlaylistsForUser("admin");
        for (Document playlist : adminPlaylists) {
            VBox playlistBox = createPlaylistBox(playlist);
            madeForYouVBox.getChildren().add(playlistBox);
        }

        String currentUsername = App.getCurrentUsername();
        if (currentUsername != null && !"admin".equals(currentUsername)) {
            List<Document> userPlaylists = mongoService.getPlaylistsForUser(currentUsername);
            for (Document playlist : userPlaylists) {
                VBox playlistBox = createPlaylistBox(playlist);
                yourPlaylistsVBox.getChildren().add(playlistBox);
            }
        }
    }

    private void loadTopPlayedSongs() {
        topPlayedVBox.getChildren().clear();
        List<Document> topSongs = mongoService.getMostPlayedSongs(10);
        for (Document song : topSongs) {
            VBox songBox = createSongBox(song);
            topPlayedVBox.getChildren().add(songBox);
        }
    }

    private void loadRecommendedSongs() {
        recommendationsVBox.getChildren().clear();
        String username = App.getCurrentUsername();
        if (username == null) return;

        List<String> topGenres = mongoService.getTopGenresForUser(username, 1);
        if (topGenres.isEmpty()) return;

        String topGenre = topGenres.get(0);
        List<Document> allSongs = mongoService.getAllSongs();

        List<Document> recommendedSongs = allSongs.stream()
            .filter(song -> {
                Object genreObj = song.get("genre");
                if (genreObj instanceof List) {
                    return ((List<?>) genreObj).contains(topGenre);
                } else if (genreObj instanceof String) {
                    return genreObj.equals(topGenre);
                }
                return false;
            })
            .sorted((s1, s2) -> Integer.compare(s2.getInteger("globalPlayCount", 0), s1.getInteger("globalPlayCount", 0)))
            .limit(10)
            .collect(Collectors.toList());

        for (Document song : recommendedSongs) {
            VBox songBox = createSongBox(song);
            recommendationsVBox.getChildren().add(songBox);
        }
    }

    private void loadTrySomethingNewSongs() {
        trySomethingNewVBox.getChildren().clear();
        String username = App.getCurrentUsername();
        if (username == null) return;

        List<Document> leastPlayed = mongoService.getLeastPlayedSongsForUser(username, 10);
        for (Document song : leastPlayed) {
            trySomethingNewVBox.getChildren().add(createSongBox(song));
        }
    }

    private VBox createPlaylistBox(Document playlist) {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.setPrefHeight(200);
        box.setPrefWidth(180);
        box.setSpacing(15);
        box.setStyle("-fx-background-radius: 15; -fx-padding: 20; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");

        ImageView iv = new ImageView();
        iv.setFitHeight(120);
        iv.setFitWidth(120);
        iv.setPreserveRatio(true);
        iv.setStyle("-fx-background-radius: 10;");
        String imagePath = playlist.getString("imagePath");
        if (imagePath == null) imagePath = "/com/example/images/default.png";
        java.net.URL url = getClass().getResource(imagePath);
        if (url != null) {
            iv.setImage(new Image(url.toExternalForm()));
        } else {
            iv.setImage(null);
        }

        Label label = new Label(playlist.getString("title"));
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -spawtify-text-color;");

        box.getChildren().addAll(iv, label);

        box.setOnMouseClicked(e -> {
            try {
                navigateToPlaylist(playlist.getString("title"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        return box;
    }

    private VBox createSongBox(Document song) {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.setPrefHeight(200);
        box.setPrefWidth(180);
        box.setSpacing(15);
        box.setStyle("-fx-background-radius: 15; -fx-padding: 20; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 0);");

        ImageView iv = new ImageView();
        iv.setFitHeight(120);
        iv.setFitWidth(120);
        iv.setPreserveRatio(true);
        iv.setStyle("-fx-background-radius: 10;");
        String imagePath = song.getString("imagePath");
        if (imagePath == null) imagePath = "/com/example/images/default.png";
        java.net.URL url = getClass().getResource(imagePath);
        if (url != null) {
            iv.setImage(new Image(url.toExternalForm()));
        } else {
            iv.setImage(null);
        }

        Label titleLabel = new Label(song.getString("title"));
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: -spawtify-text-color;");

        Label artistLabel = new Label(song.getString("artist"));
        artistLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -spawtify-subtle-text-color;");

        box.getChildren().addAll(iv, titleLabel, artistLabel);

        // Removed playSong on click to prevent accidental song play when opening/interacting with the app

        return box;
    }

    private void navigateToPlaylist(String playlistName) throws IOException {
        com.example.controllers.playlistcontroller.setPendingPlaylist(playlistName, "home");
        App.loadViewInMain("playlist");
    }

    private void playSong(Document song) {
        App.playSong(song);

        // Set the selected song image
        if (selectedSongImage != null) {
            String imagePath = song.getString("imagePath");
            if (imagePath == null) {
                imagePath = "/com/example/images/default.png";
            }
            try {
                java.net.URL url = getClass().getResource(imagePath);
                if (url != null) {
                    selectedSongImage.setImage(new Image(url.toExternalForm()));
                } else {
                    System.out.println("Image resource not found: " + imagePath);
                }
            } catch (Exception e) {
                System.err.println("Exception while loading image: " + imagePath);
                e.printStackTrace();
            }
        }
    }
}
