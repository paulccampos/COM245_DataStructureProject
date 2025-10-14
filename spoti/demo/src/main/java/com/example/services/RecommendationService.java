package com.example.services;

import java.util.List;

import com.example.MongoService;
import com.example.models.Song;
public class RecommendationService {
    private MongoService mongoService;

    public RecommendationService(MongoService mongoService) {
        this.mongoService = mongoService;
    }

    public List<Song> getRecommendations(Song currentSong) {
        java.util.List<Song> recommendations = new java.util.ArrayList<>();
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

        // Priority 3: Same genre AND artist
        for (Song song : allSongModels) {
            if (!addedTitles.contains(song.getTitle())) {
                java.util.List<String> songGenres = getStringList(song, "genre");
                String songArtist = song.getArtist();

                if (currentArtist != null && currentArtist.equals(songArtist) &&
                    hasCommonElements(currentGenres, songGenres)) {
                    recommendations.add(song);
                    addedTitles.add(song.getTitle());
                }
            }
        }

        // Priority 4: Same vibe AND artist
        for (Song song : allSongModels) {
            if (!addedTitles.contains(song.getTitle())) {
                java.util.List<String> songVibes = getStringList(song, "vibe");
                String songArtist = song.getArtist();

                if (currentArtist != null && currentArtist.equals(songArtist) &&
                    hasCommonElements(currentVibes, songVibes)) {
                    recommendations.add(song);
                    addedTitles.add(song.getTitle());
                }
            }
        }

        // Priority 5: Same vibe
        for (Song song : allSongModels) {
            if (!addedTitles.contains(song.getTitle())) {
                java.util.List<String> songVibes = getStringList(song, "vibe");

                if (hasCommonElements(currentVibes, songVibes)) {
                    recommendations.add(song);
                    addedTitles.add(song.getTitle());
                }
            }
        }

        // Priority 6: Same genre
        for (Song song : allSongModels) {
            if (!addedTitles.contains(song.getTitle())) {
                java.util.List<String> songGenres = getStringList(song, "genre");

                if (hasCommonElements(currentGenres, songGenres)) {
                    recommendations.add(song);
                    addedTitles.add(song.getTitle());
                }
            }
        }

        // Priority 7: Same artist
        for (Song song : allSongModels) {
            if (!addedTitles.contains(song.getTitle())) {
                String songArtist = song.getArtist();

                if (currentArtist != null && currentArtist.equals(songArtist)) {
                    recommendations.add(song);
                    addedTitles.add(song.getTitle());
                }
            }
        }

        recommendations.sort((a, b) -> Integer.compare(a.getGlobalPlayCount(), b.getGlobalPlayCount()));

        return recommendations;
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
}
