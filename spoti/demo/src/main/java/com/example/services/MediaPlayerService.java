package com.example.services;

import java.util.List;

import com.example.MongoService;
import com.example.models.Queue;
import com.example.models.Song;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

/**
 * Service class for handling media player logic.
 * Manages playback, queue, and recommendations.
 */
public class MediaPlayerService {
    private MongoService mongoService;
    private Queue queue;
    private Song currentSong;
    private boolean isPlaying;
    private MediaPlayer mediaPlayer;
    private long playbackStartTime;
    private long pausedTime;
    private boolean isShuffleMode = false;
    private java.util.ArrayList<Song> playHistory = new java.util.ArrayList<>();

    public MediaPlayerService(MongoService mongoService) {
        this.mongoService = mongoService;
        this.queue = new Queue(mongoService.getQueue());
        this.isPlaying = false;
    }

    public void playSong(Song song) {
        currentSong = song;
        isPlaying = true;
        playbackStartTime = System.currentTimeMillis();
        pausedTime = 0;
        System.out.println("Playing: " + song.getTitle() + " by " + song.getArtist());

        if (song != null) {
            playHistory.removeIf(s -> s.getTitle().equals(song.getTitle()) && s.getArtist().equals(song.getArtist()));
            playHistory.add(0, song); // Add to the beginning of the list
            if (playHistory.size() > 10) {
                playHistory.remove(10);
            }
        }

        // Increment play count
        String currentUser = com.example.App.getCurrentUsername();
        if (currentUser != null) {
            mongoService.updateSongPlayCounts(song.getTitle(), song.getArtist(), currentUser, 1);
            song.incrementUserPlay(currentUser);
        }
        mongoService.updateSongProperty(song.getTitle(), song.getArtist(), "globalPlayCount", song.getGlobalPlayCount() + 1);
        song.setGlobalPlayCount(song.getGlobalPlayCount() + 1);
        System.out.println("Play count updated for " + song.getTitle() + " to " + song.getGlobalPlayCount());

        try {
            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer.getStatus() != javafx.scene.media.MediaPlayer.Status.DISPOSED &&
                        mediaPlayer.getStatus() != javafx.scene.media.MediaPlayer.Status.STOPPED) {
                        System.out.println("Stopping previous MediaPlayer with status: " + mediaPlayer.getStatus());
                        mediaPlayer.stop();
                    }
                } catch (Exception e) {
                    System.err.println("Error stopping previous MediaPlayer: " + e.getMessage());
                } finally {
                    try {
                        mediaPlayer.dispose();
                        System.out.println("Previous MediaPlayer disposed successfully");
                    } catch (Exception e) {
                        System.err.println("Error disposing previous MediaPlayer: " + e.getMessage());
                    }
                }
            }
            String filePath = song.getFile();
            if (filePath != null && !filePath.isEmpty()) {
                try {
                    java.net.URL resourceUrl = getClass().getResource(filePath);
                    if (resourceUrl != null) {
                        Media media = new Media(resourceUrl.toExternalForm());
                        mediaPlayer = new MediaPlayer(media);
                        mediaPlayer.setOnReady(() -> {
                            System.out.println("Media ready, duration: " + mediaPlayer.getTotalDuration());
                            mediaPlayer.play();
                            notifyMediaPlayerHandler();
                        });
                        mediaPlayer.setOnError(() -> {
                            System.err.println("MediaPlayer error: " + mediaPlayer.getError());
                        });
                        mediaPlayer.setOnEndOfMedia(() -> {
                            System.out.println("Song ended, playing next");
                            next();
                        });
                    } else {
                        System.err.println("Resource not found: " + filePath);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading media file: " + filePath + " - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        notifyMediaPlayerHandler();
    }

    public void pause() {
        isPlaying = false;
        pausedTime += System.currentTimeMillis() - playbackStartTime;
        System.out.println("Paused");
        if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
        }
        notifyMediaPlayerHandler();
    }

    public void resume() {
        if (currentSong != null) {
            isPlaying = true;
            playbackStartTime = System.currentTimeMillis();
            System.out.println("Resumed: " + currentSong.getTitle());
            if (mediaPlayer != null && (mediaPlayer.getStatus() == MediaPlayer.Status.READY || mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED)) {
                mediaPlayer.play();
            }
            notifyMediaPlayerHandler();
        }
    }

    public void togglePlayPause() {
        if (isPlaying) {
            pause();
        } else {
            if (currentSong != null) {
                resume();
            } else if (queue != null && !queue.isEmpty()) {
                Song nextSong = queue.removeSong(0);
                mongoService.removeFromQueue(convertSongToDocument(nextSong));
                playSong(nextSong);
            }
        }
    }

    public void next() {
        if (queue != null && !queue.isEmpty()) {
            Song nextSong = queue.removeSong(0);
            mongoService.removeFromQueue(convertSongToDocument(nextSong));
            playSong(nextSong);
        } else if (currentSong != null) {
            // Run addRecommendations in a background thread to avoid blocking the UI
            new Thread(() -> {
                addRecommendations(currentSong);
                javafx.application.Platform.runLater(() -> {
                    if (queue != null && !queue.isEmpty()) {
                        Song nextSong = queue.removeSong(0);
                        mongoService.removeFromQueue(convertSongToDocument(nextSong));
                        playSong(nextSong);
                    }
                });
            }).start();
        }
    }

    public void previous() {
        if (currentSong != null) {
            System.out.println("Previous: " + currentSong.getTitle());
            playSong(currentSong);
        }
    }

    public void addToQueue(Song song) {
        addManualToQueue(song);
    }

    public void addManualToQueue(Song song) {
        queue.addSong(song, Queue.Type.MANUAL);
        mongoService.addToQueue(convertSongToDocument(song));
        System.out.println("Added to queue (manual): " + song.getTitle());
        notifyMediaPlayerHandler();
    }

    public void addAutoToQueue(Song song) {
        queue.addSong(song, Queue.Type.AUTO);
        mongoService.addToQueue(convertSongToDocument(song));
        System.out.println("Added to queue (auto): " + song.getTitle());
    }

    public void setShuffleMode(boolean mode) {
        this.isShuffleMode = mode;
    }

    public boolean isShuffleActive() {
        return isShuffleMode;
    }

    public void clearQueue() {
        queue.clear();
        mongoService.clearQueue();
        System.out.println("Queue cleared");
        notifyMediaPlayerHandler();
    }

    public void clearCurrentSong() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        currentSong = null;
        isPlaying = false;
        playbackStartTime = 0;
        pausedTime = 0;
        System.out.println("Current song cleared");
        notifyMediaPlayerHandler();
    }

    public void removeSongFromQueue(Song song) {
        queue.getSongs().remove(song);
        mongoService.removeFromQueue(convertSongToDocument(song));
    }

    public void moveUp(int index) {
        if (index > 0 && index < queue.size()) {
            Song song = queue.removeSong(index);
            queue.getSongs().add(index - 1, song);
            notifyMediaPlayerHandler();
        }
    }

    public void moveDown(int index) {
        if (index >= 0 && index < queue.size() - 1) {
            Song song = queue.removeSong(index);
            queue.getSongs().add(index + 1, song);
            notifyMediaPlayerHandler();
        }
    }

    private void addRecommendations(Song currentSong) {
        List<Song> recommendations = new java.util.ArrayList<>();
        java.util.Set<String> addedTitles = new java.util.HashSet<>();
        addedTitles.add(currentSong.getTitle());

        List<org.bson.Document> allSongs = mongoService.getAllSongs();
        List<Song> allSongModels = allSongs.stream().map(Song::new).collect(java.util.stream.Collectors.toList());

        java.util.List<String> currentGenres = getStringList(currentSong, "genre");
        java.util.List<String> currentVibes = getStringList(currentSong, "vibe");
        String currentArtist = currentSong.getArtist();

        // Priority 1: Same vibe AND genre AND artist
        for (Song song : allSongModels) {
            if (!addedTitles.contains(song.getTitle())) {
                java.util.List<String> songGenres = getStringList(song, "genre");
                java.util.List<String> songVibes = getStringList(song, "vibe");
                String songArtist = song.getArtist();

                if (currentArtist != null && currentArtist.equals(songArtist) &&
                    hasCommonElements(currentGenres, songGenres) &&
                    hasCommonElements(currentVibes, songVibes)) {
                    recommendations.add(song);
                    addedTitles.add(song.getTitle());
                }
            }
        }

        // Priority 2: Same vibe AND genre
        if (recommendations.size() < 20) {
            for (Song song : allSongModels) {
                if (!addedTitles.contains(song.getTitle())) {
                    java.util.List<String> songGenres = getStringList(song, "genre");
                    java.util.List<String> songVibes = getStringList(song, "vibe");

                    if (hasCommonElements(currentGenres, songGenres) &&
                        hasCommonElements(currentVibes, songVibes)) {
                        recommendations.add(song);
                        addedTitles.add(song.getTitle());
                    }
                }
            }
        }

        recommendations.sort((a, b) -> Integer.compare(a.getGlobalPlayCount(), b.getGlobalPlayCount()));

        for (Song rec : recommendations) {
            addAutoToQueue(rec);
        }
        System.out.println("Added " + recommendations.size() + " recommendations to queue.");
    }

    private java.util.List<String> getStringList(Song song, String fieldName) {
        Object fieldValue = null;
        if ("genre".equals(fieldName)) fieldValue = song.getGenre();
        else if ("vibe".equals(fieldName)) fieldValue = song.getVibe();

        if (fieldValue instanceof java.util.List) {
            return (java.util.List<String>) fieldValue;
        } else if (fieldValue instanceof String) {
            return java.util.Arrays.asList((String) fieldValue);
        }
        return new java.util.ArrayList<>();
    }

    private boolean hasCommonElements(java.util.List<String> list1, java.util.List<String> list2) {
        if (list1 == null || list2 == null) return false;
        for (String item1 : list1) {
            for (String item2 : list2) {
                if (item1 != null && item1.equalsIgnoreCase(item2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private org.bson.Document convertSongToDocument(Song song) {
        return new org.bson.Document("title", song.getTitle())
                .append("artist", song.getArtist())
                .append("album", song.getAlbum())
                .append("duration", song.getDuration())
                .append("file", song.getFile())
                .append("globalPlayCount", song.getGlobalPlayCount())
                .append("genre", song.getGenre())
                .append("vibe", song.getVibe());
    }

    // Getters
    public Song getCurrentSong() { return currentSong; }
    public boolean isPlaying() { return isPlaying; }
    public Queue getQueue() { return queue; }
    public MediaPlayer getMediaPlayer() { return mediaPlayer; }
    public List<Song> getPlayHistory() {
        return new java.util.ArrayList<>(this.playHistory);
    }

    public String getCurrentTimeFormatted() {
        if (mediaPlayer != null && mediaPlayer.getCurrentTime() != null) {
            long millis = (long) mediaPlayer.getCurrentTime().toMillis();
            long totalSeconds = millis / 1000;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
        return "0:00";
    }

    public String getTotalDurationFormatted() {
        if (currentSong != null) {
            return currentSong.getDuration();
        }
        return "3:45";
    }

    public double getProgress() {
        if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null && mediaPlayer.getCurrentTime() != null) {
            Duration current = mediaPlayer.getCurrentTime();
            Duration total = mediaPlayer.getTotalDuration();
            if (total.toMillis() > 0) {
                return current.toMillis() / total.toMillis();
            }
        }
        return 0.0;
    }

    public void seek(double progress) {
        if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
            Duration totalDuration = mediaPlayer.getTotalDuration();
            Duration seekTime = totalDuration.multiply(progress);
            mediaPlayer.seek(seekTime);
            long millis = (long) seekTime.toMillis();
            long totalSeconds = millis / 1000;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            System.out.println("Seeking to: " + String.format("%d:%02d", minutes, seconds));
        }
    }

    public void seekToPosition(double positionRatio) {
        System.out.println("Attempting to seek to position: " + (positionRatio * 100) + "%");

        if (mediaPlayer == null) {
            System.err.println("Cannot seek: MediaPlayer is null");
            return;
        }

        if (mediaPlayer.getStatus() != MediaPlayer.Status.READY &&
            mediaPlayer.getStatus() != MediaPlayer.Status.PLAYING &&
            mediaPlayer.getStatus() != MediaPlayer.Status.PAUSED) {
            System.err.println("Cannot seek: MediaPlayer status is " + mediaPlayer.getStatus() + ", not READY/PLAYING/PAUSED");
            return;
        }

        if (mediaPlayer.getTotalDuration() == null) {
            System.err.println("Cannot seek: MediaPlayer totalDuration is null");
            return;
        }

        try {
            Duration totalDuration = mediaPlayer.getTotalDuration();
            Duration seekTime = totalDuration.multiply(positionRatio);
            mediaPlayer.seek(seekTime);
            long millis = (long) seekTime.toMillis();
            long totalSeconds = millis / 1000;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            System.out.println("Successfully seeking to position: " + String.format("%d:%02d", minutes, seconds) +
                             " (MediaPlayer status: " + mediaPlayer.getStatus() + ")");
        } catch (Exception e) {
            System.err.println("Error seeking to position: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void notifyMediaPlayerHandler() {
        try {
            com.example.handlers.MediaPlayerHandler.getInstance().updateMediaPlayer();
        } catch (Exception e) {
            System.err.println("Error notifying MediaPlayerHandler: " + e.getMessage());
        }
    }
}
