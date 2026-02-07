# Pencil Prompt Pack: Voice Input Components

> Run after 12-commands-screen.md to add voice input UI to the Commands page.

---

## Prompt

Add voice input components to the HomeTusk Commands page, enabling users to record audio commands via microphone.

---

## 1. Voice Mic Button

### Position
- Right side of textarea wrapper (inside)
- Vertically centered
- 8px gap from textarea edge

### Button Styles

**Default state:**
- Size: 40x40px
- Background: transparent
- Border: none
- Color: #888888 (mic icon)
- Cursor: pointer

**Hover state:**
- Background: rgba(196, 90, 59, 0.1)
- Color: #C45A3B
- Border-radius: 8px

**Recording state:**
- Background: #C45A3B
- Color: white
- Border-radius: 50%
- Pulsing animation (scale 1.0 → 1.1, opacity 1 → 0.8)

**Disabled state:**
- Opacity: 0.5
- Cursor: not-allowed

### Icon
- Microphone icon: 20x20px
- Stroke width: 2px

---

## 2. Voice Recording Status Bar

### Position
- Below textarea, above actions row
- Full width of composer card
- Margin-top: 12px

### Container
- Background: #F9F7F4
- Border-radius: 8px
- Padding: 12px 16px
- Flex layout, align center, gap 12px

### Status Variants

**Recording:**
- Left: Pulsing red dot (8x8px, #D84315, animation pulse)
- Center: "Recording..." - 13px, weight 500, #1A1A1A
- Right: Timer "0:05" - 13px, #888888, monospace (IBM Plex Mono)
- Far right: Cancel button (X icon, 32x32px, ghost)

**Uploading:**
- Left: Spinner (16x16px, #C45A3B, rotating)
- Center: "Uploading audio..." - 13px, #1A1A1A
- Right: Cancel button

**Transcribing:**
- Left: Spinner (16x16px, #5B8FC4)
- Center: "Transcribing..." - 13px, #1A1A1A
- Right: Cancel button

**Ready (transcript received):**
- Left: Checkmark icon (16x16px, #6B8E5E)
- Center: "Transcript ready" - 13px, #6B8E5E
- Transcript text appears in textarea above

### Pulsing Dot Animation
```css
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.2); }
}
```

---

## 3. Voice Error Message

### Position
- Below textarea (replaces status bar on error)
- Full width of composer card

### Container
- Background: #FFEBE6
- Border: 1px solid rgba(216, 67, 21, 0.2)
- Border-radius: 8px
- Padding: 16px
- Flex layout, gap 12px

### Layout

**Left column:**
- Warning icon: 20x20px, #D84315

**Center column (flex: 1):**
- Title: Error type - 14px, weight 600, #D84315
- Message: User-friendly text - 13px, #1A1A1A, margin-top 4px

**Bottom row (actions):**
- Flex layout, gap 8px, margin-top 12px
- "Try again" button: Primary small (height 32px)
- "Type instead" button: Ghost small

### Error Messages

| Error Type | Title | Message |
|------------|-------|---------|
| permission_denied | Microphone access needed | Please allow microphone access in your browser settings. |
| not_supported | Browser not supported | Voice input requires Chrome, Firefox, or Edge. |
| recording_failed | Recording failed | Something went wrong. Want to try again? |
| no_audio_data | No audio detected | We didn't catch any audio. Check your microphone. |
| upload_failed | Upload failed | Couldn't upload the recording. Check your connection. |
| transcription_failed | Transcription failed | We couldn't understand the audio. Try speaking clearly. |
| timeout | Request timed out | The transcription took too long. Try a shorter message. |
| rate_limited | Too many requests | You can try again in {countdown}. |
| network_error | Network error | Check your connection and try again. |

### Rate Limit Variant
- Message includes countdown: "You can try again in 30s"
- "Try again" button disabled until countdown reaches 0
- Countdown updates every second

---

## 4. Updated Composer Card Layout

### With Voice Button

```
┌─────────────────────────────────────────────────────┐
│  ┌───────────────────────────────────────────┬───┐  │
│  │ Type a command...                         │ 🎤│  │
│  │                                           │   │  │
│  └───────────────────────────────────────────┴───┘  │
│                                                     │
│  [? Help]                          [Send Command]   │
└─────────────────────────────────────────────────────┘
```

### With Recording Status

```
┌─────────────────────────────────────────────────────┐
│  ┌───────────────────────────────────────────┬───┐  │
│  │                                           │ 🔴│  │
│  │                                           │   │  │
│  └───────────────────────────────────────────┴───┘  │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │ 🔴 Recording...                    0:05  ✕ │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  [? Help]                          [Send Command]   │
└─────────────────────────────────────────────────────┘
```

### With Error

```
┌─────────────────────────────────────────────────────┐
│  ┌───────────────────────────────────────────┬───┐  │
│  │ Type a command...                         │ 🎤│  │
│  └───────────────────────────────────────────┴───┘  │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │ ⚠️ Microphone access needed                 │   │
│  │ Please allow microphone access in browser.  │   │
│  │                                             │   │
│  │ [Try again]  [Type instead]                 │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  [? Help]                          [Send Command]   │
└─────────────────────────────────────────────────────┘
```

---

## 5. Mobile Adaptations

### Mic Button
- Same size (40x40px)
- Position: right side of textarea

### Recording Status
- Same layout, slightly smaller padding (10px 12px)
- Timer font size: 12px

### Error Message
- Stack layout on very small screens
- Actions wrap to new line if needed

---

## Screen Variants

| Frame Name | Description |
|------------|-------------|
| Commands - Voice Idle | Mic button visible, default state |
| Commands - Voice Hover | Mic button hover state |
| Commands - Voice Recording | Recording status bar, pulsing mic |
| Commands - Voice Uploading | Upload spinner in status |
| Commands - Voice Transcribing | Transcribe spinner in status |
| Commands - Voice Ready | Checkmark, transcript in textarea |
| Commands - Voice Error Permission | Permission denied error |
| Commands - Voice Error Rate Limit | Rate limit with countdown |
| Commands - Voice Mobile Recording | Mobile layout recording |

---

## Expected Output

9 frames showing voice input states integrated with Commands page.

---

## Implementation Reference

```
clients/web/src/components/commands/
├── VoiceMicButton.tsx/.css
├── VoiceRecordingStatus.tsx/.css
├── VoiceErrorMessage.tsx/.css
└── CommandInput.tsx (integration)
```
