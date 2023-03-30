# Forkyz Changelog

## Version 41

- Support background shapes in IPuz spec (both IPuz and JPZ import).
- Jump to random clue via shake (interaction settings) or voice ("jump").
- Remove Notre Temps Geant Force 2 downloader (not a regular puzzle).

## Version 40

- Add "ask Chat GPT for help" (configure in interaction settings).
- Welcome users to new versions, with link to release notes.
- For cells not attached to clues, toggle between selecting the cell and all detached cells.
- Add Nathan Curtis and Luckystreak XWords to online sources page.

## Version 39

- Improved accessibility: clue/box announcements via TalkBack or text-to-speech.
- Support more puzzle styling (colours, dots/dashes, blocks).
- Support more IPuz style features (plus some relaxations like HTML colours).
- Support JPZ "clue" cells, move citations to completion message.
- Support dashed bars in RCI Jeux imports, plus more French puzzles (thanks slock83).
- Fix cleanup bug for completed puzzles.
- Display intro on first run, add completion data to puzzle info.
- Changelog in release notes in help.

## Version 38

- New De Telegraaf and De Standaard downloaders.
- New play screen for acrostic puzzles.
- Streamline board rendering (please report bugs).
- Wrap long board views in clue list and notes.
- New Keesing XML parser.
- Additional features BrainsOnly: circled boxes, rebus square, puzzle info.
- Additional features IPuz: clue labels, any crossword kind, acrostic, "smart" cell indexing (default 0-based now).
- Additional features JPZ: support Alex Boisvert's acrostic extensions.
- Group cells with no attached clue for easier entering.
- Remake app icon with Android 13 themed icon support.
- Fix scratch mode in main board view and other bugs.

## Version 37

- Add voice commands to puzzle activities.
- Downloaders setup now aware of expected availability time of puzzle sources.
- Target Android 13.
- Minor UI tweaks.

## Version 36

- Fix King Digital downloaders.

## Version 35

- Warn if no connection when downloading.
- Add legacy-like theme option.
- Allow auto/background downloaders to be a sub-selection of enabled downloaders.

## Version 34

- Support USA Today Sunday downloads (thx th0mcat).

## Version 33

- "Pinned" solution word in Hamburger Abendblatt puzzles.
- Move "show all words" clue list option to clue list menu.

## Version 32

- Switch to Material 3 Theme.
- Enable dynamic (Material You) colours in settings.
- Revamp settings screen into sub-screens.

## Version 31

Remove "failed" notification when downloading an existing puzzle.

## Version 30

Optional PuzzleMe mobile-style clues list showing all words

## Version 29

- Daily downloads from WSJ
- Washington Post Sunday download (from Martin Herbach)

## Version 28

- Fix crash in landscape mode.

## Version 27

- Slightly finer-grained control of notifications in settings.
- Notify about failed downloads.

## Version 26

- Add configurable timeout to puzzle download.
- Parallelise downloads.
- Support rebus cells in AcrossLite (.puz) files.
- Support puzzle intro messages (in "info") and completion messages (on complete dialog).

## Version 25

Bugfix release.

## Version 24

- New daily download: Hamburger Abendblatt (German).
- New daily download: Le Parisien (French).
- New scraper: Przekroj Magazine (Polish), with pictures!
- Attempt to handle native keyboard properly (can now input diacritics and emojis).
- Allow custom download source in any supported format, not just .puz.
- New supported import formats: Raetselzentrale Schwedenratsel JSON, RCI Jeux Mots Fleches JSON, Guardian HTML, Przekroj HTML and JSON.
- Refresh online puzzle sources list.
- Correct charset for Across Lite (.puz) version 2.0 and up.
- Add share URL. Visible when sharing clues, and can be opened from share menu.

## Version 23

- Add share clue/board feature.
- Support notes on all clues, cells with numbers in different positions, and entering multiple characters into a single cell.
- Enable automatic clue text size scaling for older devices, remove clue text size menu option.

## Version 22

- Experimental sources are now scrapers, visible in download dialog.
- Add Private Eye as downloadable source.
- Support split clues in Guardian puzzles.
- Add insert special characters feature.
- Refresh online sources list.
- Remove Washington Post source: AmuseLabs now obfuscate their data, i assume to prevent third-party downloads.

## Version 21

- Allow customisation of automatic downloads, and support older devices.
- Bring back the native keyboard (your mileage may vary).
- Restore wrap to next list behaviour for next clue movement strategy.

## Version 20

- Move to puzzle "zones" instead of only across/down clues, and allow non-standard clue numbers. Supports e.g. spirals and rows gardens in IPuz and JPZ.
- Haptic feedback option on keyboard.

## Version 19

- Support coloured cells in JPZ and IPuz
- Support bars in JPZ as well as IPuz
- Support HTML in clue/title/etc text
- Player notes for whole puzzle as well as just clue
- Limited support for additional clue lists (not just across/down)
- Allow cells that would typically be numbered to not be numbered


## Version 18

- Remove LA Times and Washington Post Classic downloads.
- Allow import of multiple files simultaneously.

## Version 17

- Fix crashes when first box is blank, and when crossword directories have been deleted.

## Version 16

- Initial support for bar puzzles in IPuz format (tested on Square Chase puzzles only).
- Add "flag clue" feature.
- Add hide button option for keyboard (good for gesture navigation).
- Display word count in clue list when option set.
- Recode parallel movement strategy.

## Version 15

- Repair download for Android 12
- Notes page: long press board while anagram source selected now copies board to anagram solution.
- Corrected snap-to-clue behaviour
- Fixed oddities in clue tabs


## Version 14

- Bug fixes and library updates.

## Version 13

- Support for QWERTZ, Colemak and Dvorak keyboard layouts.
- Custom daily .puz source added to downloads.

## Version 12

- Fix Newsday downloads.

## Version 11

- Add change clue and direction buttons to keyboard.
- Show puzzle author on browse screen.

## Version 10

- Updated list of online puzzle sources.
- Add show errors under cursor option.
- Amuse Labs and King Digital downloads supported: circled boxes now work for LA Times puzzles, bring back Washington Post, LAT Sundays, Joseph, Sheffer, and Premier crossword downloaders.
- Circled boxes re-enabled in JPZ puzzles.
- Improved error highlighting in night colors.

## Version 9

- Bug fix: set source and support URL properly for Guardian downloads.

## Version 8

- Import of IPuz files
- Use IPuz as backend file format instead of Across Lite
- Remove HTTP/download system actions in favour of "open with"
- Prevent multiple simultaneous browse activities
- Avoid reloading puzzle list in browse activity on delete/move, and losing scroll position
- Friendlier import file names

## Version 7

- Import files from storage.
- Updated help pages, including notes help.
- List of external sources where puzzles can be downloaded.
- New default keyboard mode only hides with back button.
- Adjustment of zoom/snap behaviour on play board.

## Version 6

- Cache meta data to speed up browsing (after first view).
- Clean up zoom/scroll behaviour.
- Add menu option link to support puzzle sources.
- Improve arrow key navigation (inc. bug fixes).
- Fix crash in browsing when last puzzle was deleted.

## Version 5

- Compatible with Android 11.
- Updated downloadable puzzles.
- Theme and UI adjustments.
- Notes/anagram solving screen.
- Remove Google integration.
- Remove Crashlytics.

