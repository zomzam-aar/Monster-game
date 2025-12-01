# CatchTheBall.java — Detailed Code Walkthrough

This document explains how `CatchTheBall.java` works. It is written as an academic-friendly walkthrough appropriate for submission to a supervisor or professor.

---

## Project Summary

`CatchTheBall.java` is a self-contained Java Swing application implementing a small 2D arcade game. Shapes fall from the top of the screen and the player moves a paddle (the "monster") horizontally to catch them. The game includes images, animations, sound effects, and simple game state (score, level, lives, high score).

The implementation favors clarity and direct Swing usage rather than architectural separation — all logic, rendering and UI components are combined in a single class `CatchTheBall`.

---

## How to run

1. Place the required resource files in the same folder as the source (some resources are optional; absence will fall back to vector drawing or silence):
   - Images: `BackGround.png`, `StartBackGround.png`, `fireball.png`, `heart.png`, `game_over.png`, `monster.gif`
   - Sounds (WAV): `start.wav`, `game.wav`, `eating1.wav`, `eating2.wav`, `miss.wav`, `lose-heart.wav`, `gameover.wav`, plus optional `roar-echo.wav` and `monster-growl.wav` for voice/ambient sounds.
2. Compile and run:

```bash
javac CatchTheBall.java
java CatchTheBall
```

---

## High-level structure

- `public class CatchTheBall extends JPanel implements ActionListener` — the main game class.
- The game runs on a Swing `Timer` (`timer`) that triggers `actionPerformed` at roughly 20 ms intervals (about 50 FPS).
- UI and rendering: `paintComponent(Graphics g)` performs all drawing using `Graphics2D`.
- Sounds use the Java Sound API (`javax.sound.sampled.Clip`).
- Images loaded via `ImageIO.read(File)` and `ImageIcon`.

---

## Core fields and their roles

- Display constants: `static final int WIDTH = 720; static final int HEIGHT = 424;`
- Game variables: `score`, `level`, `lives`, `highScore`.
- Falling shape: `shapeX`, `shapeY`, `shapeSize`, `shapeSpeed`, `rotation`, `scale`.
- Player (paddle/monster): `playerX`, `playerY`, `playerWidth`, `playerHeight`, `mouseTargetX`.
- Flags: `inWelcomeScreen`, `gameRunning`, `inGameOver`.
- Resources: `BufferedImage backgroundImage, shapeImage, heartImage, gameOverImage` and `Image monsterImage`.
- Audio: `Clip startMusicClip, gameMusicClip` and `List<Clip> activeClips` for short sounds.
- Timers: `Timer timer` (game loop), `welcomeVoiceTimer`, `gameVoiceTimer`.

---

## Initialization and resource loading

- The constructor sets the preferred size, registers mouse motion events for paddle movement, creates the game loop `Timer`, loads resources if files exist, creates the welcome screen UI components, and starts the start-screen music.

- Mouse movement handlers update `mouseTargetX` so the paddle follows the cursor with smoothing applied in the game loop.

---

## UI screens and navigation

1. **Welcome Screen (`createWelcomeScreen`)**
   - Uses a `null` (absolute) layout and places a `JLabel` and `JTextField` for entering player name and a `Start Game` button.
   - Plays the welcome voice once and starts a `Timer` (`welcomeVoiceTimer`) to replay it periodically while the welcome screen is active.

2. **Game Screen (`startGame`)**
   - Removes the welcome components, creates in-game buttons (`New Game`, `Main Menu`, `Exit`) positioned at the top-right.
   - Calls `resetShape()` to spawn the first falling item and initializes moving-stars background.
   - Starts the gameplay timer and loops the game music.
   - Optionally starts an in-game repeating ambient voice (`gameVoiceTimer`).

3. **Game Over Screen (`createGameOverScreen`)**
   - If `gameOverImage` exists it is drawn centered (250×250). Otherwise a textual "Game Over" label is shown.
   - Displays score and high score and presents `Restart`, `Main Menu`, and `Exit` buttons.

4. **Menu & restart behaviors**
   - `goToMainMenu()` resets core state and returns to the welcome screen.
   - `restartGame()` resets gameplay variables and starts a fresh game session.

---

## Game mechanics and rules

- `resetShape()` increments `spawnCount`.
  - Every 15th spawn produces a life-heart (`shapeIsHeart = true`, `shapeSize = 30`). Catching it grants `lives++` and shows a "+1 Life" overlay.
  - Every 10th spawn produces a larger scoring shape (`shapeSize = 50`) that gives 2 points when caught.
  - Otherwise shapes use a standard `shapeSize = 35`.

- `actionPerformed(ActionEvent e)` runs every tick and performs:
  - Movement of the falling shape: `shapeY += shapeSpeed`.
  - Smooth paddle movement using `mouseSmoothing` toward `mouseTargetX`.
  - Collision detection (shape overlaps paddle) — on catch either grants life or score and shows overlays and plays sounds.
  - If shape falls past the bottom of the window, plays a lose sound, decrements lives, and if `lives <= 0` calls `gameOver()` immediately.
  - Increments star positions for the animated background.

---

## Rendering details (paintComponent)

- If `inWelcomeScreen` the method draws the start background image (if present) and returns.
- Otherwise, rendering steps:
  1. Draw background (image or gradient) and a translucent overlay based on `bgColor` for temporary flash effects.
  2. Render stars as small white ovals.
  3. Draw the current falling shape:
     - If `shapeIsHeart`, draw the `heartImage` (or approximate with polygon).
     - Else if `shapeImage` exists, draw it rotated about its center with scaling.
     - Otherwise draw a vector circle/square/star depending on `shapeType`.
  4. Draw the player's paddle as a `monsterImage` GIF if available, otherwise a cyan rounded rectangle.
  5. HUD: player name, score, level, high score.
  6. Lives: draw `heartImage` icons if present, otherwise pink ovals.
  7. Draw catch overlay text near the catch location (e.g., "2x" or "+1 Life").

---

## Audio handling

- Short non-looping effects use `playSound(String)` which creates a `Clip`, opens it with an `AudioInputStream`, tracks it in `activeClips`, and starts playback. On STOP the clip is closed and removed from `activeClips`.

- Looping background music uses persistent `Clip` instances (`startMusicClip`, `gameMusicClip`) and calls `loop(Clip.LOOP_CONTINUOUSLY)`.

- Timers (`welcomeVoiceTimer`, `gameVoiceTimer`) are used to repeat voice/ambient clips at intervals (e.g., welcome voice every 15s, in-game voice every 10s).

- `stopAllShortClips()` is used when switching screens to stop/close any active non-looping clips and clear the list.

---

## Design trade-offs & limitations

- The app uses a single class for all responsibilities (UI, game logic, rendering, audio). For a larger project, separating concerns is recommended.
- Audio handling uses synchronous Clip creation on the EDT which can briefly block the UI when opening large audio files.
- The periodic timers for voice playback may overlap sounds if the clip duration exceeds the interval; preventing overlap would require tracking per-clip state and checking `isRunning()` before starting a new instance.
- Layout uses absolute positioning (`null` layout), which is simple but not responsive to different resolutions.

---

## Potential improvements (academic suggestions)

- Refactor into smaller classes: `GameState`, `Renderer`, `AudioManager`, `UIManager`.
- Move audio file loading and clip creation off the EDT to avoid small freezes.
- Use a layout manager or responsive UI to handle different window sizes.
- Use JavaFX Media or an external library for MP3 playback and more robust audio control.
- Add unit tests for game rules (scoring, spawn logic) by isolating logic from rendering.

---

## Short executive summary (one paragraph)

`CatchTheBall.java` is a compact Swing-based arcade game combining drawing, animation, sound, and simple input handling in a single class. A Swing `Timer` runs the game loop while `paintComponent` performs custom rendering. The code demonstrates basic game programming concepts—collision detection, spawn logic, score/lives management, sprite rendering, and audio playback—while remaining intentionally straightforward and easy to extend.

---

If you'd like, I can also:
- Produce a one-page PDF from this Markdown suitable for printing.
- Add inline comments to the Java source file highlighting key blocks.
- Split the code into smaller classes and update the project structure for submission.

