# VibeTerminal - Technical Architecture

## Project Structure

```
VibeTerminal/
├── app/                          # Android app (Kotlin + Compose)
│   ├── src/main/
│   │   ├── java/com/vibeterminal/
│   │   │   ├── ui/              # UI Components
│   │   │   │   ├── terminal/    # Terminal view
│   │   │   │   ├── ime/         # Japanese IME bridge
│   │   │   │   └── translation/ # Translation overlay
│   │   │   ├── core/            # Core logic
│   │   │   │   ├── terminal/    # Termux engine wrapper
│   │   │   │   ├── translator/  # Translation engine
│   │   │   │   └── assistant/   # CLI assistant
│   │   │   └── data/            # Data layer
│   │   │       ├── patterns/    # Local translation patterns
│   │   │       └── cache/       # Translation cache
│   │   └── res/                 # Resources
│   └── build.gradle.kts
├── termux-engine/               # Termux integration (Git submodule)
├── translations/                # Translation pattern database
│   ├── git.json
│   ├── npm.json
│   ├── docker.json
│   └── common.json
├── docs/                        # Documentation
└── scripts/                     # Build & dev scripts
```

## Core Components

### 1. Terminal Engine (Termux-based)
- Embedded Termux terminal emulator
- proot Linux environment
- pkg package manager access

### 2. Japanese IME Bridge
- InputConnection override
- Real-time composing text display
- Compatible with all Android IMEs

### 3. Translation Engine
- Local pattern matching (offline, fast)
- LLM API fallback (online, accurate)
- Smart caching system

### 4. Touch-Optimized UI
- Minimum 44dp touch targets
- Gesture controls
- Z Fold6 adaptive layout

## Technology Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Terminal**: Termux Terminal Emulator (embedded)
- **Build**: Gradle with Kotlin DSL
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)

## Development Phases

### Phase 1: Foundation (Current)
- [x] Project structure
- [ ] Termux engine integration
- [ ] Basic terminal view
- [ ] Japanese IME bridge prototype

### Phase 2: Core Features
- [ ] Translation engine
- [ ] Touch optimization
- [ ] Z Fold6 layout

### Phase 3: Polish
- [ ] UI/UX refinement
- [ ] Performance optimization
- [ ] Testing

## Build Instructions

```bash
# Clone with submodules
git clone --recursive https://github.com/yourusername/VibeTerminal

# Build
cd VibeTerminal/app
./gradlew assembleDebug

# Install
adb install app/build/outputs/apk/debug/app-debug.apk
```
