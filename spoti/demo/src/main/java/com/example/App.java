package com.example;

import java.io.IOException;

import com.example.controllers.playlistcontroller;
import com.example.services.MediaPlayerService;
import com.example.services.UserService;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX App
 * Handles application lifecycle and scene navigation.
 */
public class App extends Application {

    private static Scene scene;
    private static String currentPlaylistName;
    private static org.bson.Document currentSong;
    private static java.util.List<org.bson.Document> queue = new java.util.ArrayList<>();

    // Singleton service instances
    private static MongoService mongoService;
    private static MediaPlayerService mediaPlayerService;
    private static UserService userService;
    private static com.example.models.User currentUser;

    // Constructor for dependency injection
    public App() {
        // Initialize singleton services if not already done
        if (mongoService == null) {
            mongoService = new MongoService();
            mediaPlayerService = new MediaPlayerService(mongoService);
            userService = new UserService(mongoService);
        }
    }

    // Constructor with dependency injection
    public App(MongoService mongoService, MediaPlayerService mediaPlayerService, UserService userService) {
        App.mongoService = mongoService;
        App.mediaPlayerService = mediaPlayerService;
        App.userService = userService;
    }

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("login"), 900, 600);
        // Apply the luxurious CSS stylesheet
        scene.getStylesheets().add(App.class.getResource("styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    public static void setRootWithPlaylist(String fxml, String playlistName, String previousView) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("fxml/" + fxml + ".fxml"));
        Parent root = fxmlLoader.load();

        // Get the controller and set the playlist name
        if (fxmlLoader.getController() instanceof playlistcontroller) {
            playlistcontroller controller = (playlistcontroller) fxmlLoader.getController();
            controller.setPlaylistName(playlistName);
            controller.setPreviousView(previousView);
        }

        scene.setRoot(root);
    }

    public static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("fxml/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }





    public static void main(String[] args) {
        launch();
    }

    // Service getters for dependency injection
    public MongoService getMongoService() {
        return mongoService;
    }

    public MediaPlayerService getMediaPlayerService() {
        return mediaPlayerService;
    }

    public UserService getUserService() {
        return userService;
    }

    // Static service getters for MediaPlayerHandler
    public static MediaPlayerService getMediaPlayerServiceStatic() {
        try {
            // Initialize singleton services if not already done
            if (mediaPlayerService == null) {
                mongoService = new MongoService();
                mediaPlayerService = new MediaPlayerService(mongoService);
                userService = new UserService(mongoService);
            }
            return mediaPlayerService;
        } catch (Exception e) {
            System.err.println("Error getting MediaPlayerService: " + e.getMessage());
            return null;
        }
    }

    // Static methods for managing current playlist and queue
    public static void setCurrentPlaylistName(String playlistName) {
        currentPlaylistName = playlistName;
    }

    public static String getCurrentPlaylistName() {
        return currentPlaylistName;
    }

    public static void setCurrentSong(org.bson.Document song) {
        currentSong = song;
    }

    public static org.bson.Document getCurrentSong() {
        return currentSong;
    }

    public static void setQueue(java.util.List<org.bson.Document> queueList) {
        queue = queueList;
    }

    public static java.util.List<org.bson.Document> getQueue() {
        return queue;
    }

    // Static methods for media player operations (delegating to mediaPlayerService)
    public static void playSong(org.bson.Document song) {
        try {
            // Use singleton mediaPlayerService
            if (mediaPlayerService == null) {
                getMediaPlayerServiceStatic(); // Initialize if needed
            }
            // Convert Document to Song object
            com.example.models.Song songObj = new com.example.models.Song(song);
            mediaPlayerService.playSong(songObj);
            // Update UI across all controllers
            com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
        } catch (Exception e) {
            System.err.println("Error in playSong(): " + e.getMessage());
        }
    }

    public static void addToQueue(org.bson.Document song) {
        queue.add(song);
    }

    public static void clearQueue() {
        queue.clear();
    }

    public static void previous() {
        try {
            // Use singleton mediaPlayerService
            if (mediaPlayerService == null) {
                getMediaPlayerServiceStatic(); // Initialize if needed
            }
            mediaPlayerService.previous();
            // Update UI across all controllers
            com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
        } catch (Exception e) {
            System.err.println("Error in previous(): " + e.getMessage());
        }
    }

    public static void togglePlayPause() {
        try {
            // Use singleton mediaPlayerService
            if (mediaPlayerService == null) {
                getMediaPlayerServiceStatic(); // Initialize if needed
            }
            mediaPlayerService.togglePlayPause();
            // Update UI across all controllers
            com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
        } catch (Exception e) {
            System.err.println("Error in togglePlayPause(): " + e.getMessage());
        }
    }

    public static void next() {
        try {
            // Use singleton mediaPlayerService
            if (mediaPlayerService == null) {
                getMediaPlayerServiceStatic(); // Initialize if needed
            }
            mediaPlayerService.next();
            // Update UI across all controllers
            com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
        } catch (Exception e) {
            System.err.println("Error in next(): " + e.getMessage());
        }
    }

    // User session management methods
    public static void setCurrentUser(org.bson.Document user) {
        try {
            currentUser = new com.example.models.User(user);
        } catch (Exception e) {
            System.err.println("Error setting current user: " + e.getMessage());
        }
    }

    public static org.bson.Document getCurrentUser() {
        try {
            return currentUser != null ? currentUser.getDocument() : null;
        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
            return null;
        }
    }

    public static String getCurrentUsername() {
        try {
            return currentUser != null ? currentUser.getUsername() : null;
        } catch (Exception e) {
            System.err.println("Error getting current username: " + e.getMessage());
            return null;
        }
    }

    public static boolean isUserLoggedIn() {
        try {
            return currentUser != null;
        } catch (Exception e) {
            System.err.println("Error checking user login status: " + e.getMessage());
            return false;
        }
    }

    public static boolean isAdminUser() {
        try {
            return currentUser != null && "admin".equals(currentUser.getStatus());
        } catch (Exception e) {
            System.err.println("Error checking admin status: " + e.getMessage());
            return false;
        }
    }

    public static MongoService getMongoServiceStatic() {
        return mongoService;
    }

    public static UserService getUserServiceStatic() {
        return userService;
    }

}
