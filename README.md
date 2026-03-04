# Jukebox Playlist

A Minecraft Fabric mod that adds playlist functionality to Jukeboxes, allowing you to play multiple music discs in sequence, shuffle them, and repeat the entire collection.

## Features

- **Playlist Inventory**: Adds a 9-slot inventory to every Jukebox specifically for music discs.
- **Sequential Playback**: Automatically plays the next disc in the playlist when the current one finishes.
- **Playback Modes**:
  - **Shuffle**: Plays discs from the playlist in a random order.
  - **Repeat**: Loops the entire playlist once all discs have been played.

### Accessing the Playlist
To access the playlist settings of a Jukebox:
- **Open GUI**: Simply right-click any Jukebox to open the Playlist GUI.
- **Quick Add**: Right-click a Jukebox while holding a music disc to automatically add it to the playlist inventory.

### Using the Playlist
Once the GUI is open:
1. **Add Discs**: Place up to 9 music discs into the top row of slots.
2. **Toggle Playback**: Click the **▶/■** button to start or stop the playlist.
3. **Shuffle**: Click the **🔀** button to toggle random playback.
4. **Repeat**: Click the **🔁** button to toggle whether the playlist should restart after the last disc.

The Jukebox will automatically handle the transitions between songs.

## Requirements

- **Minecraft**: 1.21.1
- **Fabric Loader**: 0.18.4+
- **Fabric API**: 0.141.1+1.21.11+ Required

## Installation

1. Ensure you have [Fabric Loader](https://fabricmc.net/) installed.
2. Download the mod jar and place it in your `.minecraft/mods` folder.
3. Ensure [Fabric API](https://modrinth.com/mod/fabric-api) is also present in your mods folder.
4. Enjoy.
