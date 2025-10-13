package com.example.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
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

    @FXML
    private ImageView selectedSongImage;

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
        List<String> likedPlaylistTitles = mongoService.getLikedPlaylistTitles(username);
        for (String title : likedPlaylistTitles) {
            Document playlist = mongoService.getPlaylist(title, username);
            if (playlist == null) {
                playlist = mongoService.getPlaylist(title, "admin");
            }
            if (playlist != null) {
                allPlaylists.add(0, playlist);
            }
        }

        if (filter != null && !filter.trim().isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            allPlaylists = allPlaylists.stream()
                .filter(p -> p.getString("title").toLowerCase().contains(lowerFilter))
                .collect(Collectors.toList());
        }

        String sortBy = sortComboBox != null ? sortComboBox.getValue() : null;
        if ("Title".equals(sortBy)) {
            allPlaylists.sort((a, b) -> a.getString("title").trim().compareToIgnoreCase(b.getString("title").trim()));
        }

        HBox currentRow = new HBox(25);
        int itemsPerRow = 4;
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
        String createdBy = playlist.getString("createdBy");
        String currentUsername = App.getCurrentUsername();
        if (desc == null) desc = "Playlist";

        VBox item = new VBox(12);
        item.setAlignment(Pos.CENTER);
        item.setStyle("-fx-cursor: hand;");

        // Create a GridPane for the album art
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setAlignment(Pos.CENTER);

        List<Document> songs = mongoService.getSongsForPlaylist(title, createdBy);
        if (songs.isEmpty()) {
            ImageView cover = new ImageView();
            try {
                cover.setImage(new Image(getClass().getResource("/com/example/images/vector-picture-icon.jpg").toExternalForm()));
            } catch (Exception e) {
                cover.setImage(null);
            }
            cover.setFitWidth(180);
            cover.setFitHeight(180);
            cover.setPreserveRatio(true);
            grid.add(cover, 0, 0, 2, 2);
        } else {
            int num = Math.min(4, songs.size());
            for (int i = 0; i < num; i++) {
                ImageView iv = new ImageView();
                iv.setFitWidth(90);
                iv.setFitHeight(90);
                String path = songs.get(i).getString("imagePath");
                if (path == null) {
                    path = "/com/example/images/vector-picture-icon.jpg";
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
                grid.add(iv, i % 2, i / 2);
            }
        }

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: 500; -fx-font-size: 15px;");

        Label subtitleLabel = new Label(desc);
        subtitleLabel.setStyle("-fx-font-size: 13px;");

        VBox textContainer = new VBox(5, titleLabel, subtitleLabel);
        textContainer.setAlignment(Pos.CENTER);

        VBox clickContainer = new VBox(grid, textContainer);
        clickContainer.setAlignment(Pos.CENTER);
        clickContainer.setSpacing(12);
        clickContainer.setOnMouseClicked(event -> {
            try {
                playlistcontroller.setPendingPlaylist(title, "library");
                App.loadViewInMain("playlist");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        item.getChildren().add(clickContainer);

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
                loadPlaylists();
            }
        }
    }
}
