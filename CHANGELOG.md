# Changelog

## 1.2.0 — Floating mode

### Added
- Floating keyboard mode, with a Settings option and an R3 shortcut to switch modes.

## 1.1.0 — Variant input, D-pad tuning, and keyflow refinements

This release builds on the original 1.0.0 release with a set of controller-focused improvements that make variant input and repeated movement feel smoother and more predictable.

### Added
- Long-press character variants and symbol variant menus.
- A configurable hold delay for the variant menu, so variant selection can be tuned to each user's preference.
- A configurable D-pad repeat speed for faster or calmer movement when a direction is held.
- Support for typing a variant on release, improving the feel of variant selection.
- Gamepad B as a direct way to close the keyboard.

### Improved
- Input handling for accent and symbol variants feels more consistent and less disruptive during repeated key movement.

## 1.0.0 — First release

The first public version of Inlay: a controller-first keyboard for Android handhelds.

### What's in it

**Typing with a controller**
- A grid keyboard you move with the D-pad or left stick. Holding a direction repeats and speeds up; the stick has a calm dead zone so it never skips keys.
- Up and down remember your column, so moving through the rows feels natural.
- A types, X deletes, Y is space, Start is enter, B closes.
- X held down: deletes faster and faster, then whole words.
- LB / RB move the cursor. Hold RT and press them to jump whole words.
- RT: tap for shift, tap twice for caps lock. Hold RT for extra moves: a capital letter, undo (RT + X), word jumps. Small hints show what's possible while you hold it.
- LT switches between letters and symbols.
- Select: start selecting text. Press twice to select everything; press again to deselect.
- Double-tap Y (space) for a full stop (optional).
- Clipboard history with up to six recent copies. Anything marked as a password is skipped.
- Always types straight into the app's own text field; Android's full-screen typing box never appears.

**Look**
- OLED-friendly design: pure black, one accent colour, high-contrast letters picked automatically.
- 16 highlight colours plus a custom colour wheel. 8 key colour themes.
- Four highlight styles: Glow, Solid, Outline and Lift. Three ways for the highlight to move: Instant, Subtle and Fluid.
- Three key shapes, two layouts (Grid and Classic), three sizes (press the left stick).
- Controller icons in Xbox, PlayStation or Switch style, everywhere in the app.
- Choice of font: Atkinson Hyperlegible Next (made for clear letters), Inter, or the system font, with adjustable letter weight.

**Sound and vibration**
- Four sound packs: Soft (default), Soft Deep, Tactile and Chime. Every sound was made for this app.
- One action = one sound and one vibration pulse. Bigger actions feel weightier, never louder-by-repetition.
- Vibration starts every pulse with a short full-power kick, so even small motors are felt. Works in silent mode.
- Separate strengths for moving and pressing, a volume setting, and an option to play sounds in silent mode.

**Settings app**
- Fully usable with the controller: LB / RB switch sections, the D-pad changes values, Y shows a live keyboard preview.
- Uses the same sound pack as the keyboard, so the whole app sounds like one thing.
- Built-in button guide.

**Every device**
- Works on Android 10 and newer.
- Adapts to wide, 4:3, portrait and split-screen layouts.

### How it was made

Inlay grew over eleven rounds of design, testing and feedback on an AYN Odin 2 Portal, from a first rough grid to what you see here. Along the way:

- The D-pad and stick input was rebuilt so the keyboard never interferes with the rest of the system.
- The design was rebuilt around a written design guide (`DESIGN_GUIDE.md`) with shared spacing, corners, motion and contrast rules.
- The sounds went through several complete redesigns, ending with a recipe modelled on real soft button presses: a short low thud plus a crisp click, gone within 15 ms.
- Vibration was re-tuned for small handheld motors.
- The full-screen typing box some apps trigger was locked out, so you always see the app you're typing into.
