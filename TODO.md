# TODO: Update Playlist ScrollPane Background, Remove Columns, Set Text Colors, and Adjust Layout

## Steps to Complete:
- [x] Edit `playlist.fxml` to set the ScrollPane background to blue (#0b52bf) to match the theme.
- [x] Edit `playlistcontroller.java` to remove the "File" column from the header and each song row in the `filterSongs` method.
- [x] Edit `playlistcontroller.java` to remove the "Favorite" column from the header and each song row.
- [x] Edit `playlistcontroller.java` to remove the "Duration" column from the header and each song row.
- [x] Edit `playlistcontroller.java` to remove the "Actions" column from the header and each song row.
- [x] Edit `playlistcontroller.java` to set text color to white for header labels (Title, Artist, Album).
- [x] Edit `playlistcontroller.java` to set text color to white for all labels in the song rows (title, artist, album).
- [x] Edit `playlistcontroller.java` to increase spacing between columns in header and song rows.
- [x] Edit `playlistcontroller.java` to fix context menu to use setOnContextMenuRequested instead of setContextMenu.
- [x] Edit `userprofile.css` to make logout button blue background with red hover.
- [x] Edit `userprofile.css` to make logout button semi-transparent like back button.
- [x] Edit `styles.css` to add playlist-button style for semi-transparent blue buttons with hover effect.
- [x] Edit `styles.css` to add text-field style for white border and white text.
- [x] Edit `playlist.fxml` to apply playlist-button styleClass to shuffle and options buttons.
- [ ] Verify the changes by running the application (optional, as user can test).

## Notes:
- Changed ScrollPane style from transparent to #0b52bf.
- Removed fileHeader, favoriteHeader, durationHeader, actionsHeader from headerBox.
- Removed fileLabel, favoriteLabel, durationLabel, menuButton from each songBox.
- Added -fx-text-fill: white; to header labels and song row labels.
- Increased HBox spacing from 10 to 20 for more space between columns.
- Logout button now has blue background (#0b52bf) with red hover (#ff6b6b).
- Logout button is semi-transparent (rgba(11, 82, 191, 0.7)).
- Playlist buttons (shuffle, options) are semi-transparent blue with hover effect.
- Search TextField has white border and white text.
