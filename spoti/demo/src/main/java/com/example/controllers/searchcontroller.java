package com.example.controllers;

import java.util.List;

import org.bson.Document;

import com.example.App;
import com.example.MongoService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
                    artist.setText(item.getString("artist"));
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
}