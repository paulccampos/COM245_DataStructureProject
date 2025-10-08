package com.example.controllers;

import java.io.IOException;
import java.util.List;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;
import com.example.handlers.MediaPlayerHandler;
import com.example.models.Song;
import com.example.services.MediaPlayerService;
import com.example.services.UserService;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class homecontroller {

    private MongoService mongoService;
    private MediaPlayerService mediaPlayerService;

    @FXML
    private HBox playlistsVBox;

    @FXML
    private Label welcomeLabel;


    public homecontroller() {
        mongoService = App.getMongoServiceStatic();
        mediaPlayerService = App.getMediaPlayerServiceStatic();
    }

    @FXML
    private void initialize() {
        mongoService.initializeTopHitsPlaylist();
        loadPlaylists();
        welcomeLabel.setText("Welcome, " + App.getCurrentUsername() + "!");
    }

    private void loadPlaylists() {
        String currentUser = App.getCurrentUsername();
        List<Document> playlists;
        if ("admin".equals(currentUser)) {
            // Admin sees only admin-created playlists
            playlists = mongoService.getPlaylistsForUser("admin");
        } else {
            playlists = mongoService.getAllPlaylists();
        }
        playlistsVBox.getChildren().clear();
        for (Document playlist : playlists) {
            VBox playlistBox = createPlaylistBox(playlist);
            playlistsVBox.getChildren().add(playlistBox);
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
        if (imagePath == null) imagePath = "/com/example/images/vector-picture-icon.jpg";
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

    private void onAddPlaylist() throws IOException {
        App.setRoot("library");
    }

    private void navigateToPlaylist(String playlistName) throws IOException {
        // Set the pending playlist data before navigating
        com.example.controllers.playlistcontroller.setPendingPlaylist(playlistName, "home");
        App.loadViewInMain("playlist");
    }
}
