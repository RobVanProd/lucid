# LUCID

**The Cognitive Firewall**

*We are not building a productivity app. We are building Digital Silence.*

---

## Philosophy: Inversion of Control

Current operating systems are **Push-based**. Notifications push at you. Apps scream for badges. Feeds scroll infinitely. The user is the consumer.

LUCID is **Pull-based**. Nothing enters your consciousness unless you have explicitly aligned it with your current Intent.

---

## Architecture

### 1. The Zero Interface (The Blank Canvas)

When you unlock your device, you do not see a grid of apps. You see:
- A single, pulsing cursor
- A question: **"What is the intention?"**

If you type "Learn about Renaissance Architecture," LUCID spins up a temporary, ephemeral interface dedicated solely to that. It pulls data, strips away the ads, the recommendations, the clickbait, and presents a pure, synthesized workspace.

When you are done, the interface **dissolves**.

### 2. The Reality Filter (DOM Manipulation)

LUCID reads the DOM before it renders. It extracts the content you need and renders only that. Elegantly. The noise never touches your retina.

**Removed automatically:**
- Advertisements
- Pop-ups and modals
- Cookie consent banners
- Newsletter sign-ups
- Social media embeds
- Recommendation engines
- Infinite scroll triggers
- Autoplay videos

### 3. The Communication Synthesizer

The average person is overwhelmed by fragmentation: WhatsApp, Slack, Email, SMS.

LUCID aggregates all inbound comms into a single **"Briefing."**
- It doesn't buzz you. It waits.
- When you decide to check, it says: *"3 urgent matters from family. 4 newsletters (summarized). 12 low-priority alerts (archived)."*

### 4. The Mode Switch

A physical interaction using the phone's gyroscope or swipe gesture:

- **Mode A: EXPLORE** - The internet as it is. Raw.
- **Mode B: LUCID** - The filter is up. E-ink aesthetic. Monochrome. The dopamine loops are severed.

---

## Building the APK

### Option 1: Using Docker (Recommended)

The easiest way to build - no SDK setup required:

```bash
# Build the Docker image and APK
docker build -t lucid-builder .

# Extract the APK
mkdir -p output
docker run --rm -v $(pwd)/output:/output lucid-builder

# APK will be at output/lucid-debug.apk
```

### Option 2: Using the Build Script

```bash
# Set up Android SDK first, then:
./build-apk.sh debug    # or 'release'
```

### Option 3: Manual Build

**Prerequisites:**
- JDK 17 or higher
- Android SDK with:
  - Platform SDK 34
  - Build Tools 34.0.0

```bash
# Set Android SDK path
export ANDROID_HOME=/path/to/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties

# Build
./gradlew assembleDebug

# APK location
ls app/build/outputs/apk/debug/app-debug.apk
```

### Install on Device

```bash
# Enable USB debugging on your Android phone, then:
adb install app/build/outputs/apk/debug/app-debug.apk

# Or set LUCID as your launcher:
# Settings > Apps > Default apps > Home app > LUCID
```

---

## Project Structure

```
lucid/
├── app/
│   └── src/main/
│       ├── java/com/lucid/app/
│       │   ├── data/           # Data models and preferences
│       │   ├── filter/         # Reality Filter engine
│       │   └── ui/             # Compose UI components
│       │       ├── components/ # Reusable components
│       │       ├── screens/    # Main screens
│       │       └── theme/      # E-ink theme
│       └── res/                # Resources
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Why This Changes The World

If you give a human back 2 hours of deep, focused thought per day, you don't just improve their productivity. You lower their cortisol. You improve their relationships. You return their agency.

We are not building a tool. We are building **Digital Silence**.

And in that silence, people can finally hear themselves think again.

---

## License

MIT
