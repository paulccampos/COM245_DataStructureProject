package com.example.models;

import java.util.List;

import org.bson.Document;
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
                this.types.add(Type.AUTO);
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
        types.add(Type.AUTO);
    }

    public void addSong(Song song, Type type) {
        if (type == Type.MANUAL) {
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
            int insertIndex = types.size();
            for (int i = 0; i < types.size(); i++) {
                if (types.get(i) == Type.MANUAL) {
                    insertIndex = i + 1;
                }
            }
            songs.add(insertIndex, song);
            types.add(insertIndex, type);
            sortAutoSongs();
        }
    }

    public void addSongAtFront(Song song) {
        songs.add(0, song);
        types.add(0, Type.MANUAL);
    }

    public void addSongWithPriority(Song song) {
        if (song.isManualAddition()) {
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

    private void sortAutoSongs() {
        int autoStart = 0;
        for (int i = 0; i < types.size(); i++) {
            if (types.get(i) == Type.MANUAL) {
                autoStart = i + 1;
            }
        }
        for (int i = autoStart; i < songs.size() - 1; i++) {
            for (int j = i + 1; j < songs.size(); j++) {
                if (songs.get(i).getGlobalPlayCount() < songs.get(j).getGlobalPlayCount()) {
                    Song tempSong = songs.get(i);
                    songs.set(i, songs.get(j));
                    songs.set(j, tempSong);
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
