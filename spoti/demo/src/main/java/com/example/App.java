package com.example;

import java.io.IOException;

import java.io.File;
import com.example.controllers.playlistcontroller;
import com.example.services.MediaPlayerService;
import com.example.controllers.MainController;
import com.example.services.UserService;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static Scene scene;
    private static String currentPlaylistName;
    private static org.bson.Document currentSong;
    private static java.util.List<org.bson.Document> queue = new java.util.ArrayList<>();
    private static MainController mainController;

    private static MongoService mongoService;
    private static MediaPlayerService mediaPlayerService;
    private static UserService userService;
    private static com.example.models.User currentUser;

    public App() {
        if (mongoService == null) {
            mongoService = new MongoService();
            mediaPlayerService = new MediaPlayerService(mongoService);
            userService = new UserService(mongoService);
        }
    }

    public App(MongoService mongoService, MediaPlayerService mediaPlayerService, UserService userService) {
        App.mongoService = mongoService;
        App.mediaPlayerService = mediaPlayerService;
        App.userService = userService;
    }

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("login"), 900, 600);
        scene.getStylesheets().add(App.class.getResource("/com/example/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("fxml/" + fxml + ".fxml"));
        Parent root = loader.load();
        
        if ("main".equals(fxml)) {
            mainController = loader.getController();
        }
        scene.setRoot(root);
    }

    public static void setRootWithPlaylist(String fxml, String playlistName, String previousView) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("fxml/" + fxml + ".fxml"));
        Parent root = fxmlLoader.load();

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

    public static void loadViewInMain(String fxmlName) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("fxml/" + fxmlName + ".fxml"));
        Parent view = loader.load();

        if (mainController != null) {
            mainController.setView(view);
        }
    }




    public static void main(String[] args) {
        launch();
    }

    public MongoService getMongoService() {
        return mongoService;
    }

    public MediaPlayerService getMediaPlayerService() {
        return mediaPlayerService;
    }

    public UserService getUserService() {
        return userService;
    }

    public static MediaPlayerService getMediaPlayerServiceStatic() {
        try {
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

    public static void playSong(org.bson.Document song) {
        try {
            if (mediaPlayerService == null) {
                getMediaPlayerServiceStatic();
            }
            com.example.models.Song songObj = new com.example.models.Song(song);
            mediaPlayerService.playSong(songObj);
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
            if (mediaPlayerService == null) {
                getMediaPlayerServiceStatic();
            }
            mediaPlayerService.previous();
            com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
        } catch (Exception e) {
            System.err.println("Error in previous(): " + e.getMessage());
        }
    }

    public static void togglePlayPause() {
        try {
            if (mediaPlayerService == null) {
                getMediaPlayerServiceStatic();
            }
            mediaPlayerService.togglePlayPause();
            com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
        } catch (Exception e) {
            System.err.println("Error in togglePlayPause(): " + e.getMessage());
        }
    }

    public static void next() {
        try {
            if (mediaPlayerService == null) {
                getMediaPlayerServiceStatic();
            }
            mediaPlayerService.next();
            com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
        } catch (Exception e) {
            System.err.println("Error in next(): " + e.getMessage());
        }
    }

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

    public static void logout() throws IOException {
        if (mediaPlayerService != null) {
            mediaPlayerService.pause();
            mediaPlayerService.clearQueue();
        }

        currentUser = null;

        try {
            File file = new File("user_credentials.dat");
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            System.err.println("Error clearing credentials: " + e.getMessage());
        }

        setRoot("login");
    }
}
