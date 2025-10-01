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
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class librarycontroller {

    private void deletePlaylist(Document playlist) {
        String username = App.getCurrentUsername();
        mongoService.deletePlaylist(playlist.getString("title"), username);
        loadPlaylists(); // Refresh the list
    }

    private void editPlaylist(Document playlist) {
        String oldTitle = playlist.getString("title");
        String oldDesc = playlist.getString("description");

        TextInputDialog titleDialog = new TextInputDialog(oldTitle);
        titleDialog.setTitle("Edit Playlist");
        titleDialog.setHeaderText("Edit playlist title");
        titleDialog.setContentText("Title:");

        Optional<String> titleResult = titleDialog.showAndWait();
        if (titleResult.isPresent() && !titleResult.get().trim().isEmpty()) {
            TextInputDialog descDialog = new TextInputDialog(oldDesc);
            descDialog.setTitle("Edit Playlist");
            descDialog.setHeaderText("Edit playlist description");
            descDialog.setContentText("Description:");

            Optional<String> descResult = descDialog.showAndWait();
            if (descResult.isPresent()) {
                String username = App.getCurrentUsername();
                mongoService.updatePlaylist(oldTitle, titleResult.get().trim(), descResult.get().trim(), username);
                loadPlaylists(); // Refresh the library view
            }
        }
    }

    private MongoService mongoService;

    @FXML
    private VBox playlistsVBox;

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

    public librarycontroller() {
        mongoService = new MongoService();
    }

    @FXML
    private void initialize() {
        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> loadPlaylists());
        loadPlaylists();
        // Register UI components with MediaPlayerHandler
        MediaPlayerHandler.getInstance().registerUIComponents(
            mediaPlayerBar, currentSongLabel, durationLabel, progressBar,
            previousButton, playPauseButton, nextButton);
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

    private void loadPlaylists() {
        playlistsVBox.getChildren().clear();
        String username = App.getCurrentUsername();

        // Load liked playlists first
        List<String> likedPlaylistTitles = mongoService.getLikedPlaylistTitles(username);
        if (!likedPlaylistTitles.isEmpty()) {
            Label likedLabel = new Label("Liked Playlists");
            likedLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 10 0 5 0;");
            playlistsVBox.getChildren().add(likedLabel);

            for (String title : likedPlaylistTitles) {
                Document playlist = mongoService.getPlaylist(title, username);
                if (playlist == null) {
                    // Try admin playlist
                    playlist = mongoService.getPlaylist(title, "admin");
                }
                if (playlist != null) {
                    addPlaylistToVBox(playlist, true); // true for liked
                }
            }
        }

        // Separator
        if (!likedPlaylistTitles.isEmpty()) {
            Label separator = new Label("Your Playlists");
            separator.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 20 0 5 0;");
            playlistsVBox.getChildren().add(separator);
        }

        List<Document> playlists = mongoService.getPlaylistsForUser(username);
        // Filter out system/admin-created playlists to show only personal playlists in library
        playlists = playlists.stream()
            .filter(playlist -> {
                String title = playlist.getString("title");
                return title != null && !isSystemPlaylist(title);
            })
            .collect(java.util.stream.Collectors.toList());
        String sortBy = sortComboBox.getValue();

        // Default sorting: Personal playlists first, then system playlists (like Top Hits)
        if (sortBy == null || sortBy.isEmpty()) {
            playlists.sort((a, b) -> {
                String titleA = a.getString("title").trim();
                String titleB = b.getString("title").trim();

                // Check if playlist is a system playlist (starts with common system names)
                boolean isSystemA = isSystemPlaylist(titleA);
                boolean isSystemB = isSystemPlaylist(titleB);

                if (isSystemA && !isSystemB) {
                    return 1; // System playlists go after personal playlists
                } else if (!isSystemA && isSystemB) {
                    return -1; // Personal playlists go before system playlists
                } else {
                    // Both are same type, sort alphabetically
                    return titleA.compareTo(titleB);
                }
            });
        } else if ("Title".equals(sortBy)) {
            playlists.sort((a, b) -> a.getString("title").trim().compareTo(b.getString("title").trim()));
        } else if ("Date Created".equals(sortBy)) {
            // Assuming playlists have a "createdAt" field, sort by it
            playlists.sort((a, b) -> {
                long aTime = a.getLong("createdAt") != null ? a.getLong("createdAt") : 0;
                long bTime = b.getLong("createdAt") != null ? b.getLong("createdAt") : 0;
                return Long.compare(aTime, bTime);
            });
        }
        for (Document playlist : playlists) {
            addPlaylistToVBox(playlist, false); // false for personal
        }
    }

    private void addPlaylistToVBox(Document playlist, boolean isLiked) {
        String title = playlist.getString("title").trim();
        String desc = playlist.getString("description");
        if (desc != null) desc = desc.trim();
        HBox hbox = new HBox(10);
        ImageView iv = new ImageView(new Image(getClass().getResource("/com/example/images/vector-picture-icon.jpg").toExternalForm()));
        iv.setFitHeight(89);
        iv.setFitWidth(123);
        iv.setPreserveRatio(true);
        iv.setPickOnBounds(true);
        VBox textVBox = new VBox(5);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");
        textVBox.getChildren().addAll(titleLabel, descLabel);

        // Spacer to push button to the right
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Context menu for ... button
        ContextMenu contextMenu = new ContextMenu();
        if (!isLiked) {
            MenuItem editItem = new MenuItem("Edit Playlist");
            editItem.setOnAction(e -> editPlaylist(playlist));
            MenuItem deleteItem = new MenuItem("Delete Playlist");
            deleteItem.setOnAction(e -> deletePlaylist(playlist));
            contextMenu.getItems().addAll(editItem, deleteItem);
        } else {
            // For liked playlists, maybe add remove from liked
            MenuItem removeItem = new MenuItem("Remove from Liked");
            removeItem.setOnAction(e -> removeFromLiked(playlist));
            contextMenu.getItems().add(removeItem);
        }

        Button menuButton = new Button("...");
        menuButton.setOnMouseClicked((MouseEvent e) -> {
            if (!contextMenu.isShowing()) {
                contextMenu.show(menuButton, e.getScreenX(), e.getScreenY());
            } else {
                contextMenu.hide();
            }
        });

        hbox.getChildren().addAll(iv, textVBox, spacer, menuButton);

        // Make the playlist clickable
        hbox.setOnMouseClicked(event -> {
            try {
                App.setRootWithPlaylist("playlist", title, "library");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        playlistsVBox.getChildren().add(hbox);
    }

    private void removeFromLiked(Document playlist) {
        String username = App.getCurrentUsername();
        mongoService.removeFromLikedPlaylists(playlist.getString("title"), username);
        loadPlaylists(); // Refresh the list
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
    private void onClearQueue() {
        App.clearQueue();
    }

    /**
     * Helper method to identify system-generated playlists
     */
    private boolean isSystemPlaylist(String title) {
        if (title == null) return false;

        // Common system playlist names
        String[] systemPlaylists = {
            "Top Hits",
            "Recently Played",
            "Favorites",
            "Most Played"
        };

        for (String systemName : systemPlaylists) {
            if (title.equalsIgnoreCase(systemName)) {
                return true;
            }
        }

        return false;
    }
}
