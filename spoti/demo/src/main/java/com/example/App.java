package com.example;

import java.io.IOException;

import com.example.controllers.playlistcontroller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    private static MongoService mongoService = new MongoService();
    private static org.bson.Document currentSong;
    private static java.util.List<org.bson.Document> queue;
    private static boolean isPlaying = false;
    private static java.util.List<Object> mediaPlayerControllers = new java.util.ArrayList<>();
    private static String currentPlaylistName;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("home"), 900, 600);
        stage.setScene(scene);
        stage.show();

        // Initialize Top Hits playlist
        mongoService.initializeTopHitsPlaylist();
        mongoService.displaySongsInTerminal(); // ✅ prints all songs individually
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    public static void setRootWithPlaylist(String fxml, String playlistName, String previousView) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("fxml/" + fxml + ".fxml"));
        Parent root = fxmlLoader.load();
        playlistcontroller controller = fxmlLoader.getController();
        controller.setPlaylistName(playlistName);
        controller.setPreviousView(previousView);
        scene.setRoot(root);
    }

    public static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("fxml/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static MongoService getMongoService() {
        return mongoService;
    }

    // Media player methods

    public static void togglePlayPause() {
        if (isPlaying) {
            pause();
        } else {
            resume();
        }
    }

    public static void next() {
        if (queue != null && !queue.isEmpty()) {
            org.bson.Document nextSong = queue.remove(0);
            mongoService.removeFromQueue(nextSong);
            currentSong = nextSong;
            isPlaying = true;
            playbackStartTime = System.currentTimeMillis();
            pausedTime = 0;
            System.out.println("Next: " + currentSong.getString("title"));
            // Update media player UI
            com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
        }
    }

    public static void previous() {
        // For simplicity, replay current or implement history
        if (currentSong != null) {
            System.out.println("Previous: " + currentSong.getString("title"));
        }
    }

    public static void addToQueue(org.bson.Document song) {
        if (queue == null) {
            queue = new java.util.ArrayList<>();
        }
        queue.add(song);
        mongoService.addToQueue(song);
        System.out.println("Added to queue: " + song.getString("title"));
    }

    public static java.util.List<org.bson.Document> getQueue() {
        if (queue == null) {
            queue = mongoService.getQueue();
        }
        return queue;
    }

    public static org.bson.Document getCurrentSong() {
        return currentSong;
    }

    public static boolean isPlaying() {
        return isPlaying;
    }

    public static String getCurrentPlaylistName() {
        return currentPlaylistName;
    }

    public static void setCurrentPlaylistName(String playlistName) {
        currentPlaylistName = playlistName;
    }

    // Add missing methods to fix compilation errors
    private static long playbackStartTime = 0;
    private static long pausedTime = 0;

    public static String getCurrentTimeFormatted() {
        if (!isPlaying) {
            return formatTime(pausedTime);
        }
        long elapsed = System.currentTimeMillis() - playbackStartTime + pausedTime;
        // Cap elapsed at total duration to prevent timer going past
        long totalMillis = parseTimeToMillis(getTotalDurationFormatted());
        if (elapsed > totalMillis) {
            elapsed = totalMillis;
        }
        return formatTime(elapsed);
    }

    private static long parseTimeToMillis(String timeStr) {
        String[] parts = timeStr.split(":");
        if (parts.length == 2) {
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            return (minutes * 60L + seconds) * 1000;
        }
        return 0;
    }

    public static String getTotalDurationFormatted() {
        if (currentSong != null) {
            Object durationObj = currentSong.get("duration");
            if (durationObj instanceof String) {
                return (String) durationObj;
            } else if (durationObj instanceof Integer) {
                int durInt = (Integer) durationObj;
                return String.format("%d:%02d", durInt / 60, durInt % 60);
            }
        }
        return "3:45"; // default duration
    }

    private static String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public static void playSong(org.bson.Document song) {
        currentSong = song;
        isPlaying = true;
        playbackStartTime = System.currentTimeMillis();
        pausedTime = 0;
        System.out.println("Playing: " + song.getString("title") + " by " + song.getString("artist"));
        try {
            scene.setRoot(scene.getRoot());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Update media player UI
        com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
    }

    public static void pause() {
        isPlaying = false;
        pausedTime += System.currentTimeMillis() - playbackStartTime;
        System.out.println("Paused");
        // Update media player UI
        com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
    }

    public static void resume() {
        if (currentSong != null) {
            isPlaying = true;
            playbackStartTime = System.currentTimeMillis();
            System.out.println("Resumed: " + currentSong.getString("title"));
            // Update media player UI
            com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
