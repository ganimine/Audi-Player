# Android Media Player

A modern Android media player designed for seamless audio and video playback, with a clean interface, searchable media library, configurable filters, playlists, and a persistent bottom mini-player.

> **Project status:** This README describes the intended features and implementation requirements. Update the platform, SDK, and build details to match the actual source code before publishing.

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Playback Interface](#playback-interface)
- [Media Library and Filters](#media-library-and-filters)
- [Search](#search)
- [User Experience](#user-experience)
- [Privacy and Permissions](#privacy-and-permissions)
- [Requirements](#requirements)
- [Build and Run](#build-and-run)
- [Suggested Project Structure](#suggested-project-structure)
- [Testing Checklist](#testing-checklist)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

## Overview

The app provides a single place to browse and play supported audio and video files stored on an Android device. Its interface should keep playback controls easy to reach without duplicating the currently playing UI across the top and bottom bars.

The library supports choosing which media types to display and filtering files by size. Once filters are applied, the filter panel closes automatically while the selected filters remain active.

## Key Features

- Audio playback and video playback using the project's supported playback engine and codecs.
- Media library for supported local files.
- Search for media by available metadata, such as title, artist, album, or filename.
- Content-type filters for audio, video, or both.
- Minimum and maximum file-size filters.
- Quick size presets and a custom size range.
- Persistent filter preferences, with a clear/reset option.
- Bottom mini-player for the current item.
- Full-screen or expanded playback interface.
- Playlist and playback-state support, where implemented.
- Light/dark themes and custom skins, where implemented.
- Graceful handling of unavailable files, denied permissions, and empty results.

Features should only be described as available in a release after they have been implemented and tested.

## Playback Interface

### Top app bar

The top app bar should be reserved for navigation, search, and page-specific actions. It should **not** display a duplicate currently playing mini-player.

### Bottom mini-player

When media is loaded and the user is browsing the library, display a single mini-player in the bottom area. It can show:

- Artwork or a media thumbnail.
- Track or file title.
- Artist or other available media information.
- Play/pause control.
- A tap action to open the full player.

Avoid rendering the same mini-player in both the top and bottom bars. Keep a single source of truth for the current media item and playback state.

### Full player

The full player should prioritize the media content, playback progress, and controls. Video full-screen mode should use the available display area and avoid unnecessary UI overlays.

Applying library filters must not restart, pause, or reset the currently playing item or its playback position.

## Media Library and Filters

The filter workflow should be temporary and easy to reopen.

1. Open the media library's filter control.
2. Choose **Audio**, **Video**, or **Audio and video**.
3. Choose a size option:
   - All sizes
   - A quick preset (Small, Medium, or Large)
   - A custom minimum and maximum size
4. Select **Apply Filters**.
5. The filter panel closes and the library displays matching files.

### Filter behavior

- Support KB, MB, and GB for display; compare file sizes internally in bytes.
- Validate that the minimum size does not exceed the maximum.
- Do not apply an arbitrary size limit unless the user selects one.
- Keep filters active when the user navigates around the library.
- Show a subtle indicator when filters are active.
- Allow users to reopen the panel and clear or reset filters.
- Preserve the current search query where possible when filters change.
- Show a helpful empty state when no files match.
- Do not reopen the filter panel automatically every time the user opens the library or player.
- Do not close the player or alter playback state when the panel is dismissed.

## Search

Keep search accessible from the top app bar. Search should work with the currently selected media type and active size filters. Where possible, search indexed metadata instead of rescanning all storage on each keystroke or filter change.

Search should handle missing metadata and inaccessible or deleted files without crashing.

## User Experience

- Responsive layouts for different Android screen sizes and orientations.
- Clear spacing and touch targets.
- Smooth but restrained panel transitions.
- Light and dark theme support, if provided by the app.
- Compatibility with Android system bars, display cutouts, and edge-to-edge layouts.
- No overlapping bottom navigation and mini-player controls.
- Accessible labels for icons and controls.
- Clear loading, error, permission-denied, and empty states.

## Privacy and Permissions

Request only the Android permissions needed for the features in use. Explain why media access is required and handle permission denial gracefully. Follow current Android storage and media-access rules for the target SDK.

If the app later adds network streaming, analytics, cloud synchronization, or background location, document those behaviors separately and request only the permissions and consent that are genuinely necessary.

Do not log private media content or sensitive user data unnecessarily.

## Requirements

The exact requirements depend on the existing project. For a typical Android development setup, use:

- Android Studio or a compatible IDE.
- A compatible JDK and Android SDK for the project's Gradle and Android Gradle Plugin versions.
- Gradle wrapper included with the project.
- An Android device or emulator for testing.

If the project uses Flutter, Kotlin, Java, or another framework, follow that framework's setup instructions and the versions declared in the repository. Do not change SDK or dependency versions without checking compatibility.

## Build and Run

### Open the project

1. Clone or download the repository.
2. Open the project root in the appropriate IDE.
3. Allow the build system to sync dependencies.
4. Resolve any SDK or JDK requirements shown by the project.

### Build with Gradle

For a typical Gradle-based Android project, run the following from the repository root.

On Linux/macOS:

```bash
./gradlew assembleDebug
```

On Windows:

```bat
gradlew.bat assembleDebug
```

The debug APK is commonly generated under:

```text
app/build/outputs/apk/debug/
```

The actual output path may differ if the project has multiple modules or custom build variants.

### Install on a connected Android device

Linux/macOS:

```bash
./gradlew installDebug
```

Windows:

```bat
gradlew.bat installDebug
```

These commands apply only if the repository is a Gradle-based Android project with the corresponding tasks. For Flutter or another framework, use its documented build and installation commands instead.

## Suggested Project Structure

Adapt this structure to the existing repository rather than duplicating or replacing established architecture.

```text
project-root/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java-or-kotlin/
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   ├── test/
│   │   └── androidTest/
│   └── build.gradle
├── gradle/
├── gradlew
├── gradlew.bat
├── settings.gradle
├── build.gradle
└── README.md
```

Recommended logical components:

- `MediaLibrary`: indexed media items and library queries.
- `MediaFilter`: content-type and size-range filtering.
- `MediaSearch`: search across available metadata.
- `PlaybackController`: one authoritative playback state.
- `MiniPlayer`: the single bottom mini-player component.
- `FullPlayer`: expanded audio/video playback UI.
- `FilterPanel`: temporary filter interface that dismisses after applying.
- `SettingsRepository`: persistent filter and appearance preferences.

## Testing Checklist

Before releasing a build, verify the following:

- [ ] The currently playing mini-player appears only in the bottom bar.
- [ ] The top bar contains navigation, search, and relevant page actions without a duplicate mini-player.
- [ ] Users can select audio, video, or both.
- [ ] Minimum and maximum file-size filters work correctly.
- [ ] KB, MB, and GB values are converted correctly.
- [ ] Invalid size ranges are rejected with a clear message.
- [ ] Applying filters automatically closes the filter panel.
- [ ] Filters remain active until changed or reset.
- [ ] The filter panel does not reopen automatically during ordinary navigation.
- [ ] Search works with the active filters.
- [ ] Empty results display a helpful message and a way to reset filters.
- [ ] Applying filters does not pause, restart, or seek the current playback.
- [ ] Full-screen video and device rotation behave correctly.
- [ ] Permission denial, deleted files, and unavailable media are handled gracefully.
- [ ] Existing playlists, themes, and playback features continue to work.
- [ ] The project builds successfully and automated tests pass.

## Roadmap

Potential future improvements include:

- Advanced codec support through a suitable playback engine.
- Queue management, playlists, and playback history.
- Subtitle and audio-track selection for video.
- External Bluetooth or cast-device playback, if required.
- Custom skins and media-art backgrounds.
- Better media indexing and library refresh controls.
- Accessibility improvements and performance/battery testing.

Prioritize stable playback, correct filtering, and a consistent interface before adding advanced features.

## Contributing

1. Create a branch for your change.
2. Keep changes focused and consistent with the existing architecture.
3. Add or update tests for modified behavior.
4. Run the relevant build and test commands.
5. Describe the change and any known limitations in the pull request.

## License

Add the license applicable to this project before distributing it. Do not assume the project is open source or assign a license without the owner's decision.
