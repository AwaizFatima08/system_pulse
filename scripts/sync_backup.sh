#!/bin/bash
set -e

PROJECT_DIR="/mnt/storage/projects/system_pulse"
GDRIVE_FOLDER_ID="1-MZY3Q1gA9C_EtAxxu-byE7-zUV0Wr42"

echo "=== 1. Git Status & Remote Sync ==="
cd "$PROJECT_DIR"
if [ -d ".git" ]; then
    git add .
    if ! git diff --cached --quiet; then
        COMMIT_MSG="${1:-Auto-update System Pulse project}"
        git commit -m "$COMMIT_MSG"
    fi
    git push -u origin main || echo "Git push warning; check network / credentials."
fi

echo "=== 2. Google Drive Backup Sync via rclone ==="
rclone sync "$PROJECT_DIR" gdrive: \
    --drive-root-folder-id "$GDRIVE_FOLDER_ID" \
    --exclude ".gradle/**" \
    --exclude "build/**" \
    --exclude "app/build/**" \
    --exclude ".kotlin/**" \
    --exclude "local.properties" \
    --exclude "*.apk" \
    --exclude "*.aab" \
    --exclude ".idea/**" \
    -v

echo "=== Backup & GitHub Sync Complete! ==="
