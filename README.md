# Call Forwarding by Nilay

A lightweight personal Android utility for one-tap conditional call forwarding (**Missed, Canceled/Busy, and Unreachable**) on Jio using verified standard GSM MMI codes.

## Features
- **One-Tap 2x1 Home Screen Widget** (Rich Emerald Green when ON, cool pastel slate when OFF)
- **Quick Settings Tile** with lock-screen protection
- **3 Receptionist Presets** with Contact Picker and Copy/Paste WhatsApp sync for a second phone
- **Verified Jio MMI Codes**:
  - Activate All 3 Conditional Rules: `**004*<10-digit-number>#`
  - Cancel All Forwarding (Night OFF): `##002#`
  - Live Verify: `*#61#` (Missed), `*#67#` (Canceled/Busy), `*#62#` (Unreachable)
- **Simple Morning & Night Reminders** with direct **Turn OFF Now** / **Turn ON Now** notification buttons
- **Dual-SIM Lock** to avoid SIM-picker interruptions

## Build APK via GitHub Actions
1. Create a new GitHub repository (Public or Private).
2. Upload all files from this folder (including `.github/workflows/build-apk.yml`).
3. Open the **Actions** tab → click **Build Call Forwarding by Nilay APK** → wait ~2–3 minutes.
4. Download **CallForwardingByNilay-APK**, unzip `CallForwardingByNilay.apk`, and install on your Android phone.
