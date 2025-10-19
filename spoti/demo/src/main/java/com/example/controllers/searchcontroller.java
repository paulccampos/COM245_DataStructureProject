package com.example.controllers;

import java.util.List;
import java.util.Optional;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class searchcontroller {

    @FXML
    private TextField searchTextField;

    @FXML
    private ListView<Document> resultsListView;

    @FXML
    private ImageView selectedSongImage;

    private MongoService mongoService;

    @FXML
    private void initialize() {
        mongoService = new MongoService();

        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            performSearch(newValue);
        });

        resultsListView.setOnMouseClicked(event -> {
            Document selected = resultsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                playSelectedSong(selected);
            }
        });

        resultsListView.setCellFactory(param -> new ListCell<Document>() {
            private ImageView imageView = new ImageView();
            private Text title = new Text();
            private Text artist = new Text();
            private VBox vBox = new VBox(title, artist);
            private HBox hBox = new HBox(10, imageView, vBox);

            @Override
            protected void updateItem(Document item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    title.setText(item.getString("title"));
                    title.setStyle("-fx-text-fill: white;");
                    artist.setText(item.getString("artist"));
                    artist.setStyle("-fx-text-fill: white;");
                    String imagePath = item.getString("imagePath");
                    if (imagePath == null) {
                        imagePath = "/com/example/images/default.png";
                    }
                    try {
                        java.net.URL url = getClass().getResource(imagePath);
                        if (url != null) {
                            imageView.setImage(new Image(url.toExternalForm()));
                        } else {
                            System.out.println("Image resource not found: " + imagePath);
                        }
                    } catch (Exception e) {
                        System.err.println("Exception while loading image: " + imagePath);
                        e.printStackTrace();
                    }
                    imageView.setFitHeight(50);
                    imageView.setFitWidth(50);

                    // Add context menu and button
                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem addToPlaylistItem = new MenuItem("Add to Playlist");
                    addToPlaylistItem.setOnAction(e -> addSongToPlaylist(item));
                    MenuItem addToQueueItem = new MenuItem("Add to Queue");
                    addToQueueItem.setOnAction(e -> addSongToQueue(item));
                    contextMenu.getItems().addAll(addToPlaylistItem, addToQueueItem);

                    Button menuButton = new Button("...");
                    menuButton.getStyleClass().add("playlist-button");
                    menuButton.setOnMouseClicked((MouseEvent e) -> {
                        if (!contextMenu.isShowing()) {
                            contextMenu.show(menuButton, e.getScreenX(), e.getScreenY());
                        } else {
                            contextMenu.hide();
                        }
                    });

                    hBox.getChildren().clear();
                    hBox.getChildren().addAll(imageView, vBox);
                    hBox.getChildren().add(menuButton);
                    HBox.setHgrow(vBox, javafx.scene.layout.Priority.ALWAYS);
                    setGraphic(hBox);
                }
            }
        });
    }

    private void performSearch(String query) {
        if (query == null || query.isEmpty()) {
            resultsListView.setItems(FXCollections.observableArrayList());
            return;
        }
        List<Document> results = mongoService.searchSongs(query);
        ObservableList<Document> items = FXCollections.observableArrayList(results);
        resultsListView.setItems(items);
    }

    private void playSelectedSong(Document selected) {
        App.playSong(selected);

        // Set the selected song image
        if (selectedSongImage != null) {
            String imagePath = selected.getString("imagePath");
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
        com.example.models.Song songModel = new com.example.models.Song(song);
        App.getMediaPlayerServiceStatic().addManualToQueue(songModel);
    }
}
