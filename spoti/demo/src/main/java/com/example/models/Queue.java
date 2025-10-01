package com.example.models;

import java.util.List;

import org.bson.Document;

/**
 * Model class representing a Playback Queue.
 * Encapsulates queue data and provides methods for managing the queue.
 */
public class Queue {
    public enum Type { MANUAL, AUTO }

    private List<Song> songs;
    private List<Type> types;

    public Queue(List<Document> songDocs) {
        this.songs = new java.util.ArrayList<>();
        this.types = new java.util.ArrayList<>();
        if (songDocs != null) {
            for (Document songDoc : songDocs) {
                this.songs.add(new Song(songDoc));
                this.types.add(Type.AUTO); // Assume loaded are auto
            }
        }
    }

    // Getters
    public List<Song> getSongs() { return songs; }

    // Setters
    public void setSongs(List<Song> songs) { this.songs = songs; }

    // Queue operations
    public void addSong(Song song) {
        songs.add(song);
        types.add(Type.AUTO); // Default
    }

    public void addSong(Song song, Type type) {
        if (type == Type.MANUAL) {
            // Find last MANUAL index
            int lastManual = -1;
            for (int i = types.size() - 1; i >= 0; i--) {
                if (types.get(i) == Type.MANUAL) {
                    lastManual = i;
                    break;
                }
            }
            int insertIndex = lastManual + 1;
            songs.add(insertIndex, song);
            types.add(insertIndex, type);
        } else {
            // AUTO, insert at end of MANUALs (or end if no MANUALs)
            int insertIndex = types.size();
            for (int i = 0; i < types.size(); i++) {
                if (types.get(i) == Type.MANUAL) {
                    insertIndex = i + 1;
                }
            }
            songs.add(insertIndex, song);
            types.add(insertIndex, type);
            // Sort the AUTO section by globalPlayCount descending
            sortAutoSongs();
        }
    }

    public void addSongAtFront(Song song) {
        songs.add(0, song);
        types.add(0, Type.MANUAL); // Assume
    }

    public void addSongWithPriority(Song song) {
        // If song is manually added, insert after last MANUAL
        if (song.isManualAddition()) {
            // Find last MANUAL index
            int lastManual = -1;
            for (int i = types.size() - 1; i >= 0; i--) {
                if (types.get(i) == Type.MANUAL) {
                    lastManual = i;
                    break;
                }
            }
            int insertIndex = lastManual + 1;
            songs.add(insertIndex, song);
            types.add(insertIndex, Type.MANUAL);
        } else {
            // For non-manual songs, add at the end
            songs.add(song);
            types.add(Type.AUTO);
        }
    }

    public Song removeSong(int index) {
        if (index >= 0 && index < songs.size()) {
            types.remove(index);
            return songs.remove(index);
        }
        return null;
    }

    public void clear() {
        songs.clear();
        types.clear();
    }

    public Type getType(int index) {
        if (index >= 0 && index < types.size()) {
            return types.get(index);
        }
        return null;
    }

    public boolean isEmpty() {
        return songs.isEmpty();
    }

    public int size() {
        return songs.size();
    }

    public Song get(int index) {
        if (index >= 0 && index < songs.size()) {
            return songs.get(index);
        }
        return null;
    }

    // Sort AUTO songs by globalPlayCount descending, preserving MANUAL positions
    private void sortAutoSongs() {
        // Find the start of AUTO section (after last MANUAL)
        int autoStart = 0;
        for (int i = 0; i < types.size(); i++) {
            if (types.get(i) == Type.MANUAL) {
                autoStart = i + 1;
            }
        }
        // Sort from autoStart to end by globalPlayCount descending
        for (int i = autoStart; i < songs.size() - 1; i++) {
            for (int j = i + 1; j < songs.size(); j++) {
                if (songs.get(i).getGlobalPlayCount() < songs.get(j).getGlobalPlayCount()) {
                    // Swap songs
                    Song tempSong = songs.get(i);
                    songs.set(i, songs.get(j));
                    songs.set(j, tempSong);
                    // Swap types (should both be AUTO)
                    Type tempType = types.get(i);
                    types.set(i, types.get(j));
                    types.set(j, tempType);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Queue{" +
                "songCount=" + songs.size() +
                '}';
    }
}
