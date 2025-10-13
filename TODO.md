# Task: Display Album Photo and Ensure MP3 Correspondence in All FXML Views

## Information Gathered

- Song model includes `imagePath` for album images and `file` for MP3 paths.
- Each song's MP3 file corresponds to its album based on the JSON data.
- Controllers (playlistcontroller, songscontroller, homecontroller, librarycontroller, searchcontroller, queuecontroller) already display album art in song boxes.
- MediaPlayerHandler updates the media player bar with the current song's image.
- User wants album photo to appear prominently when clicking or viewing a song in each FXML view (playlist, songs, home, library, search, queue).
- MP3 files are already correctly associated per song.

## Plan

1. **Update FXML Files**: Add an ImageView component to each FXML file to display the album image when a song is clicked or played.
   - [x] playlist.fxml: Added selectedSongImage ImageView.
   - [x] songs.fxml: Added selectedSongImage ImageView.
   - [x] home.fxml: Added selectedSongImage ImageView.
   - [x] library.fxml: Added selectedSongImage ImageView.
   - [x] search.fxml: Added selectedSongImage ImageView.
   - [x] queue.fxml: Added selectedSongImage ImageView.
2. **Update Controllers**: Modify controllers to set the album image in the new ImageView when a song is selected or played.
   - [x] playlistcontroller.java: Added selectedSongImage field and set image in playSong method.
   - [x] songscontroller.java: Added selectedSongImage field and set image in playSong method.
   - [x] homecontroller.java: Added selectedSongImage field and set image in playSong method.
   - [x] librarycontroller.java: Added selectedSongImage field (no playSong method, image set in loadQueue for queue).
   - [x] searchcontroller.java: Added selectedSongImage field and set image in playSelectedSong method.
   - [x] queuecontroller.java: Added selectedSongImage field and set image in loadQueue method.
3. **Ensure MP3 Correspondence**: Verify and confirm that MediaPlayerService loads the correct MP3 file (already implemented).
4. **Test Functionality**: Run the application and verify album image display and MP3 playback in each view.

## Dependent Files to Edit

- FXML: playlist.fxml, songs.fxml, home.fxml, library.fxml, search.fxml, queue.fxml
- Controllers: playlistcontroller.java, songscontroller.java, homecontroller.java, librarycontroller.java, searchcontroller.java, queuecontroller.java
- MediaPlayerService.java (minor check if needed)

## Followup Steps

- After edits, compile and run the application.
- Test clicking songs in each view to ensure album image appears and MP3 plays correctly.
- Confirm no errors in image loading or media playback.
