# BadUSB demo payloads

These `.txt` files are Ducky Script 1.0. Each non-empty line is one command. `REM` is a comment. `DELAY` is a pause in milliseconds. `STRING` types the rest of the line literally. `GUI r` is the Windows key plus R (the Run dialog). `ENTER` confirms it.

Keep every `DELAY` between 500 and 1500 ms so the script can keep up with a live machine without stalling the round.

## Push to the Flipper

Copy the `.txt` files to `/ext/badusb/` on the Flipper SD card.

- In V3SP3R, use the file browser to upload each file to that folder.
- Or pull the SD card and copy the files into the `badusb` directory at the card root. On the Flipper that directory is `/ext/badusb`.

## Run

On the Flipper, open the BadUSB app, select the file, and run it. The Flipper must be plugged into the test PC over USB so the PC sees it as a keyboard.

## Safety

- Run these only on a machine the organizer assigned to the station. This pack is for your own test machines.
- Ship demo payloads only. The three files here open Calculator, type a lock-your-PC reminder in Notepad, or open a browser rickroll.
- Do not add payloads that destroy data, download a second stage, or open a shell. `validate_pack.py` rejects those patterns and rejects any line whose first command is outside classic Ducky Script 1.0.

## Add a payload

1. Add `badusb/<name>.txt`. Start with `REM` lines that name the pack, say what the script demos, and say it is for your own test machine only.
2. Use delays of 500–1500 ms. One command per line.
3. Add a `file` entry in `manifest.json` with `"path": "badusb/<name>.txt"` and `"target": "/ext/badusb/<name>.txt"`.
4. From this pack directory, run `python3 validate_pack.py`.
