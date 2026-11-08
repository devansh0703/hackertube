<div align="center">
  <img src="assets/icons/HackerTube.png" width="112" alt="HackerTube">

# HackerTube for Android

**The YouTube videos Hacker News keeps telling each other to watch, in a native Android app.**

[Website](https://hackertube.org) · [Download](https://github.com/devansh0703/hackertube/releases/latest) · [Issues](https://github.com/devansh0703/hackertube/issues)
</div>

## About

[HackerTube](https://hackertube.org) collects the videos that Hacker News commenters have recommended to each other for nearly two decades: conference talks, lectures, documentaries, deep dives and hidden gems.

This Android app brings that catalogue to your phone. The complete HackerTube catalogue is bundled with the app, so discovery is based on Hacker News recommendations rather than YouTube's watch-time feed. Browse, search, playlists, Longs and related videos all stay inside HackerTube.

The bundled catalogue currently contains **6,454 videos**, organised by **150 topics in 16 areas**, with **27 curated playlists**.

## Download

Get the latest APK from [GitHub Releases](https://github.com/devansh0703/hackertube/releases/latest).

Requires Android 8.0 (API 26) or newer.

## Features

- **HackerTube-only discovery** — no YouTube home feed, shorts feed or unrelated recommendations.
- **Browse with filters** — year, content type, duration, hidden gems, evergreen status and several sort orders.
- **Longs** — every video over an hour, in one place.
- **Curated playlists** — hand-ordered collections, with album-style playback.
- **Mixed search** — videos, subjects and curated playlists in the same result list.
- **Liked and Watch later** — stored locally on your device.
- **Catalogue-only related videos and autoplay**, so playback never wanders outside HackerTube.
- **Background playback and downloads** from the underlying player.
- **Material 3 interface** with light and dark themes.

## Build from source

```bash
git clone https://github.com/devansh0703/hackertube.git
cd hackertube
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

For a signed release build, create a `keystore.properties` file in the project root as described in `app/build.gradle.kts`, then run:

```bash
./gradlew assembleRelease
```

## Catalogue and privacy

The processed catalogue lives in [`app/src/main/assets/hackertube/`](app/src/main/assets/hackertube/) and is loaded entirely from the device. See [`NOTICE.txt`](app/src/main/assets/hackertube/NOTICE.txt) for data attribution.

Likes, Watch later and watch history are stored on-device. The app does not create HackerTube accounts or send viewing activity to HackerTube.

HackerTube is an independent project. It is not affiliated with YouTube, Google, Hacker News or Y Combinator.

## Credits and licence

HackerTube for Android is built on [LibreTube](https://github.com/libre-tube/LibreTube), the GPLv3 Android client by Bnyro and contributors. The app remains free software under the [GNU General Public License v3 or later](https://www.gnu.org/licenses/gpl-3.0.en.html).

The HackerTube catalogue and website are independent projects; see [hackertube.org/about](https://hackertube.org/about) for their credits and data licence.
