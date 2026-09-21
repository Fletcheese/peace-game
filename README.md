# PEACE

An Android arcade game built with Kotlin and Jetpack Compose. Guide your ship through drifting, rotating gates while dodging enemies — survive as long as possible and rack up score and multiplier.

## Gameplay

- Control a glowing ship that follows your touch (or an optional on-screen joystick) around an animated space background.
- Fly through **gates**: pairs of endpoints that drift, rotate, and accelerate over time. Passing through them scores points; touching a lethal gate triggers an explosion.
- Avoid **enemies** that spawn and home in on the player.
- Score and best-run multiplier are tracked, with a local top-3 high score list.
- A one-time tutorial can be toggled on/off from the home screen.

## Mod Profiles

The game includes a mod system (`ModManager`) that lets you create and switch between profiles adjusting:

- **Enemy**: color, max speed, acceleration, size, spawn rate/count
- **Player**: max speed, acceleration, size
- **Gate**: color, max speed, acceleration, length, end-zone size, explosion radius, spawn rate

Profiles are persisted locally via `SharedPreferences` and can be edited from the settings UI.

## Project Structure

```
app/src/main/java/com/fletcheese/peace/
├── MainActivity.kt              # App entry point, home screen, navigation, settings UI
├── InteractiveCircleScreen.kt   # Main game loop/screen
├── GamePlayer.kt                # Player ship state and rendering
├── Enemy.kt                     # Enemy behavior and rendering
├── Gate.kt                      # Gate movement, rotation, and collision logic
├── Explosion.kt                 # Explosion effect
├── Shard.kt                     # Debris/particle effects
├── SpaceBackground.kt           # Animated background
├── ModManager.kt                # Mod profile persistence (SharedPreferences + JSON)
├── ScoreManager.kt              # High scores, settings persistence
├── MusicManager.kt              # Background music playback
├── SoundManager.kt              # Sound effect playback
└── ui/                          # Additional UI components
```

## Tech Stack

- Kotlin
- Jetpack Compose (Material 3)
- Navigation Compose
- kotlinx.serialization (JSON) for mod profile storage

## Requirements

- Android Studio (or a compatible Gradle/JDK setup)
- Android SDK: `minSdk` 24, `targetSdk`/`compileSdk` 35

## Building

```bash
./gradlew assembleDebug
```

For a signed release build, see the release signing setup used by this project (App Bundle / Google Play).

```bash
./gradlew bundleRelease
```

## License

No license specified.
