---
name: Boris
description: A quiet Android reader with optional Amber or bunker identity.
colors:
  zinc-900: "#18181B"
  zinc-800: "#27272A"
  zinc-700: "#3F3F46"
  zinc-500: "#71717A"
  zinc-400: "#A1A1AA"
  zinc-200: "#E4E4E7"
  paper: "#FFFFFF"
  gray-900: "#111827"
  gray-700: "#374151"
  gray-500: "#6B7280"
  gray-200: "#E5E7EB"
  gray-100: "#F5F5F5"
  indigo-500: "#6366F1"
  indigo-600: "#4F46E5"
  highlight-mine: "#FDE047"
  sky-400: "#38BDF8"
  blue-500: "#3B82F6"
  on-primary: "#FFFFFF"
  on-highlight: "#000000"
typography:
  display:
    fontFamily: "Source Serif 4, Georgia, serif"
    fontSize: "40sp"
    fontWeight: 700
    lineHeight: 1.2
  headline:
    fontFamily: "Source Serif 4, Georgia, serif"
    fontSize: "36sp"
    fontWeight: 700
    lineHeight: 1.22
  title:
    fontFamily: "sans-serif"
    fontSize: "16sp"
    fontWeight: 500
    lineHeight: 1.5
  body:
    fontFamily: "Source Serif 4, Georgia, serif"
    fontSize: "21sp"
    fontWeight: 400
    lineHeight: 1.71
  label:
    fontFamily: "sans-serif"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: 1.43
rounded:
  sm: "6dp"
  md: "8dp"
spacing:
  xs: "8dp"
  sm: "12dp"
  md: "16dp"
  lg: "24dp"
  xl: "32dp"
components:
  button-primary:
    backgroundColor: "{colors.indigo-500}"
    textColor: "{colors.on-primary}"
    rounded: "{rounded.md}"
    padding: "14dp 20dp"
    height: "52dp"
  button-secondary:
    backgroundColor: "{colors.zinc-800}"
    textColor: "{colors.zinc-200}"
    rounded: "{rounded.md}"
    padding: "14dp 20dp"
    height: "52dp"
  field-outlined:
    backgroundColor: "{colors.zinc-900}"
    textColor: "{colors.zinc-200}"
    rounded: "{rounded.md}"
  mark-highlight:
    backgroundColor: "{colors.highlight-mine}"
    textColor: "{colors.on-highlight}"
---

# Design System: Boris

## Overview

**Creative North Star: "The Quiet Article"**

Chrome is the webapp login: zinc, indigo, yellow marks, short sans type. The article is a book: Source Serif 4, justified body, little else on the page. Identity is optional and must never look more important than reading.

Dark theme is first-class (zinc-900). Light theme is paper and gray-900 with indigo-600 actions. Material 3 supplies structure; brand shows up through these tokens, not through extra decoration.

**Key Characteristics:**
- Zinc surfaces, indigo filled actions, 8dp corners
- Yellow marks only on Home copy (`#FDE047` on black text)
- Source Serif for reading; sans for buttons, labels, Home greeting
- Flat tonal surfaces, no drop-shadow vocabulary
- Reading column max 720dp

## Colors

One indigo accent for actions, one yellow for editorial marks, zinc/paper for everything else.

### Primary
- **Indigo 500** (`#6366F1`): filled buttons in dark theme (Amber, Read, bunker Connect).
- **Indigo 600** (`#4F46E5`): filled buttons in light theme.

### Secondary
- **Highlight mine** (`#FDE047`): Home copy marks and notice-card tint. Text on the mark is black.

### Neutral
- **Zinc 900 / 800 / 700** (`#18181B` / `#27272A` / `#3F3F46`): dark background, elevated fill, outline.
- **Zinc 200 / 400 / 500** (`#E4E4E7` / `#A1A1AA` / `#71717A`): dark text, secondary text, muted footer.
- **Paper / Gray 900** (`#FFFFFF` / `#111827`): light background and text.
- **Sky 400 / Blue 500** (`#38BDF8` / `#3B82F6`): secondary/link roles in the Material scheme, not a second brand accent on Home.

### Named Rules
**The Two Loud Colors Rule.** Indigo is for actions. Yellow is for marks. Do not add a third accent.

## Typography

**Display Font:** Source Serif 4 (serif fallback)
**Body Font:** Source Serif 4 on the reader; system sans on Home chrome
**Label Font:** system sans-serif

**Character:** The article is literary; the shell is the webapp. Do not put serif on buttons or the Home greeting.

### Hierarchy
- **Display** (Bold, 40sp / 48sp): reserved, rarely used.
- **Headline** (Bold/SemiBold, 36sp–24sp): article title (`headlineLarge`), Home greeting uses sans SemiBold 24sp instead.
- **Title** (sans Medium, 16sp / 24sp): top app bar, button labels.
- **Body** (serif Regular, 21sp / 36sp, justified): article. Home description is sans 16sp / 26sp, centered.
- **Label** (sans Medium, 14sp / 12sp): chrome, reading time, footer.

### Named Rules
**The Face Split Rule.** Serif reads. Sans steers.

## Layout

Home is a centered column, max 420dp, 24dp horizontal padding, 24dp vertical rhythm, IME and system-bar insets applied. Reader is a centered column, max 720dp, 20dp horizontal padding. Compact phone is the ship target; no tablet navigation rail yet. Touch targets stay at least 48dp.

## Elevation & Depth

Flat. Depth is tonal (surface vs surfaceVariant) and 1dp outlines (`#3F3F46` dark, `#E5E7EB` light). No shadow vocabulary.

### Named Rules
**The Flat-By-Default Rule.** Do not add drop shadows to Home or the reader.

## Shapes

8dp rounded rectangles for buttons, fields, bunker panel, and notice cards. 6dp is acceptable on inner bunker controls. No pill buttons, no 16dp+ squircle tiles.

## Components

### Buttons
- **Shape:** 8dp
- **Primary:** indigo fill, white label, sans Medium, min height 52dp, optional 18dp icon
- **Secondary:** zinc-800 fill, 1dp zinc-700 border, on-background label (Signer, Cancel)
- **Text:** Sign out, compact links

### Inputs / Fields
- **Style:** outlined, 8dp, single line. Bunker field is monospace. URL field is sans.
- **Home URL placeholder:** the default article URL, not a bunker string.

### Navigation
- Two screens: Home and Reader. Reader top app bar: back, title, share, open original. System Back pops. No bottom bar.

### Signature: Home login
Matches the webapp login: greeting, marked sentence, Amber then Signer, nstart.me footer. Signer expands to bunker field + Connect / Cancel. Amber-missing install links appear only after Amber is tapped.

### Signature: Article
Selectable Source Serif markdown, reading time in sans small, images open the gallery.

## Do's and Don'ts

### Do:
- **Do** keep Home copy identical to the webapp, including the two yellow marks.
- **Do** use Amber / Signer / Read as the Android button labels.
- **Do** theme through Material color roles in `BorisTheme`.
- **Do** honor IME, status bar, and gesture inset padding.

### Don't:
- **Don't** put an `nsec` field anywhere.
- **Don't** gate reading on login.
- **Don't** invent bookmarks, highlights, or social chrome on Android.
- **Don't** use Inter-as-brand, purple gradients, glassmorphism, or nested cards.
- **Don't** show the three Amber install links until the user asks to connect Amber.
