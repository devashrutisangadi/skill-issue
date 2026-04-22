# Skill Issue - New York Times Wordle Clone

An Android word-guessing game inspired by Wordle, built with Java and XML views, with customizable categories loaded from `words.json`.

## Highlights
- Dynamic game board based on the selected target phrase length.
- Phrase support (including entries with spaces) with fixed-space tile handling.
- Custom in-app keyboard with live key coloring (gray/yellow/green).
- Auto-centered horizontal board scrolling for long phrases while typing.
- Intro-first UX: users see short instructions on launch, then start gameplay.
- Category-driven content from a local JSON asset (`app/src/main/assets/words.json`).

## Tech Stack
- Android (Java, XML)
- Gradle (Kotlin DSL)
- Gson for JSON parsing
- Min SDK 24, Target SDK 36

## Project Structure
- `app/src/main/java/com/example/customwordle/MainActivity.java` - core game logic and UI state handling
- `app/src/main/java/com/example/customwordle/WordLoader.java` - loads category data from JSON
- `app/src/main/assets/words.json` - category and phrase source data
- `app/src/main/res/layout/activity_main.xml` - main screen layout
- `app/src/main/res/anim/shake.xml` - input feedback animation

## How It Works
1. User selects a category and taps **START**.
2. App picks a random playable entry from that category.
3. Grid size and attempts are calculated dynamically from the target entry.
4. User enters guesses using the custom keyboard.
5. Tiles and keys are colored based on guess accuracy.

## Run Locally
### Prerequisites
- Android Studio (latest stable recommended)
- Android SDK installed
- JDK 11+

### Steps
1. Clone this repository.
2. Open the project in Android Studio.
3. Sync Gradle.
4. Run on emulator/device:
   - From Android Studio, click **Run**
   - Or from terminal:

```bash
./gradlew :app:assembleDebug
```

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Customization
- Add/edit categories and phrases in:
  - `app/src/main/assets/words.json`
- Update branding/app name in:
  - `app/src/main/res/values/strings.xml`

## Roadmap
- Add difficulty presets and timed mode.
- Add persistent stats (win streak, guess distribution).
- Add animations for tile reveal per letter.

