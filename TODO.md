# TODO: Prevent song from playing on app open

## Overview

Ensure that no song plays automatically when the application opens, even if there was a previous session.

## Steps

- [x] Add clearCurrentSong() method in MediaPlayerService to stop and clear any current song.
- [x] Call clearCurrentSong() in MainController.initialize() to prevent auto-play on app start.
- [ ] Test the application to ensure no song plays on open.
