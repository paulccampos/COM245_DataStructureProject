package com.example.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class librarycontroller {

    private MongoService mongoService;

    @FXML
    private VBox libraryGrid;

    @FXML
    private ComboBox<String> sortComboBox;

    @FXML
    private TextField searchTextField;

    public librarycontroller() {
        mongoService = App.getMongoServiceStatic();
    }

    @FXML
    private void initialize() {
        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> loadPlaylists());
        loadPlaylists();
        if (searchTextField != null) {
            searchTextField.textProperty().addListener((obs, oldVal, newVal) -> filterPlaylists(newVal));
        }
    }

    private void loadPlaylists() {
        filterPlaylists(searchTextField.getText());
    }

    private void filterPlaylists(String filter) {
        libraryGrid.getChildren().clear();
        String username = App.getCurrentUsername();

        List<Document> allPlaylists = mongoService.getPlaylistsForUser(username);
        // Add liked playlists
        List<String> likedPlaylistTitles = mongoService.getLikedPlaylistTitles(username);
        for (String title : likedPlaylistTitles) {
            Document playlist = mongoService.getPlaylist(title, username);
            if (playlist == null) {
                playlist = mongoService.getPlaylist(title, "admin");
            }
            if (playlist != null) {
                allPlaylists.add(0, playlist); // Add to front
            }
        }

        // Filter by search text
        if (filter != null && !filter.trim().isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            allPlaylists = allPlaylists.stream()
                .filter(p -> p.getString("title").toLowerCase().contains(lowerFilter))
                .collect(Collectors.toList());
        }

        // Sort
        String sortBy = sortComboBox != null ? sortComboBox.getValue() : null;
        if ("Title".equals(sortBy)) {
            allPlaylists.sort((a, b) -> a.getString("title").trim().compareToIgnoreCase(b.getString("title").trim()));
        }

        // Create grid rows
        HBox currentRow = new HBox(25);
        int itemsPerRow = 4; // Adjust as needed
        for (int i = 0; i < allPlaylists.size(); i++) {
            if (i > 0 && i % itemsPerRow == 0) {
                libraryGrid.getChildren().add(currentRow);
                currentRow = new HBox(25);
            }
            VBox item = createLibraryItem(allPlaylists.get(i));
            currentRow.getChildren().add(item);
        }
        if (!currentRow.getChildren().isEmpty()) {
            libraryGrid.getChildren().add(currentRow);
        }
    }

    private VBox createLibraryItem(Document playlist) {
        String title = playlist.getString("title").trim();
        String desc = playlist.getString("description");
        if (desc == null) desc = "Playlist";

        VBox item = new VBox(12);
        item.setStyle("-fx-cursor: hand; -fx-alignment: center;");

        // Cover image
        ImageView cover = new ImageView();
        try {
            cover.setImage(new Image(getClass().getResource("/com/example/images/vector-picture-icon.jpg").toExternalForm()));
        } catch (Exception e) {
            // Placeholder
            cover.setImage(null);
        }
        cover.setFitWidth(180);
        cover.setFitHeight(180);
        cover.setPreserveRatio(true);

        // Title
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: 500; -fx-font-size: 15px;");

        // Subtitle
        Label subtitleLabel = new Label(desc);
        subtitleLabel.setStyle("-fx-font-size: 13px;");

        item.getChildren().addAll(cover, titleLabel, subtitleLabel);

        // Click to open playlist
        item.setOnMouseClicked(event -> {
            try {
                playlistcontroller.setPendingPlaylist(title, "library");
                App.loadViewInMain("playlist");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return item;
    }

    @FXML
    private void onCreatePlaylist() {
        TextInputDialog titleDialog = new TextInputDialog();
        titleDialog.setTitle("Create Playlist");
        titleDialog.setHeaderText("Enter playlist title");
        titleDialog.setContentText("Title:");

        Optional<String> titleResult = titleDialog.showAndWait();
        if (titleResult.isPresent() && !titleResult.get().trim().isEmpty()) {
            TextInputDialog descDialog = new TextInputDialog();
            descDialog.setTitle("Create Playlist");
            descDialog.setHeaderText("Enter playlist description");
            descDialog.setContentText("Description:");

            Optional<String> descResult = descDialog.showAndWait();
            if (descResult.isPresent()) {
                String username = App.getCurrentUsername();
                mongoService.createPlaylist(titleResult.get().trim(), descResult.get().trim(), username);
                loadPlaylists(); // Refresh the library view
            }
        }
    }
}
