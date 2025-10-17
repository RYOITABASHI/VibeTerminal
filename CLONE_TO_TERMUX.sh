#!/data/data/com.termux/files/usr/bin/bash
# VibeTerminal - Clone to Termux Script
# Run this script in Termux to set up the project

set -e

echo "🚀 VibeTerminal - Termux Setup"
echo "================================"
echo ""

# Check if running in Termux
if [[ ! "$PREFIX" =~ "com.termux" ]]; then
    echo "❌ Error: This script must be run in Termux"
    echo "   Current PREFIX: $PREFIX"
    exit 1
fi

echo "✅ Running in Termux"
echo ""

# UserLAnd repository path
USERLAND_REPO="/data/data/com.termux/files/home/storage/shared/VibeTerminal"
TERMUX_REPO="$HOME/VibeTerminal"

echo "📂 Checking UserLAnd repository..."

# Check if shared storage is accessible
if [ ! -d "$HOME/storage/shared" ]; then
    echo "⚠️  Shared storage not set up"
    echo "   Setting up storage access..."
    termux-setup-storage
    echo "   Please grant storage permission and re-run this script"
    exit 1
fi

# Option 1: Clone from shared storage (if accessible)
if [ -d "$USERLAND_REPO/.git" ]; then
    echo "✅ Found UserLAnd repo in shared storage"
    echo "   Cloning from: $USERLAND_REPO"
    git clone "$USERLAND_REPO" "$TERMUX_REPO"
else
    # Option 2: Clone via SSH (UserLAnd sshd must be running)
    echo "⚠️  UserLAnd repo not found in shared storage"
    echo ""
    echo "Alternative: Clone via SSH"
    echo "1. Make sure UserLAnd sshd is running:"
    echo "   ~/CCUI/fix_sshd.sh start"
    echo ""
    echo "2. Run this command:"
    echo "   git clone ssh://userland@localhost:2222/home/userland/VibeTerminal ~/VibeTerminal"
    echo ""
    exit 1
fi

cd "$TERMUX_REPO"

echo ""
echo "✅ Repository cloned successfully"
echo ""
echo "📦 Next steps:"
echo "1. Install required packages:"
echo "   pkg install openjdk-17 gradle kotlin"
echo ""
echo "2. Set up Android SDK (see TERMUX_SETUP.md)"
echo ""
echo "3. Build the project:"
echo "   ./gradlew assembleDebug"
echo ""
echo "🎉 Setup complete! Happy coding!"
