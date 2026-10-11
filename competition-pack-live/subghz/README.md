# Live Sub-GHz notes (LAB USE ONLY)

This directory holds no transmit files on purpose. Live Sub-GHz material is
created on the bench, from lab-owned hardware, during station D2
(`docs/competition-live-runbook.md`):

1. **Capture.** Read the lab's own fixed-code remote in the stock Sub-GHz app
   and save the `.sub` under `/ext/subghz/`.
2. **Replay.** Transmit that capture at the lab's own receiver only, on the
   legal ISM frequency written on the bench card.
3. **Rolling-code failure beat.** Capture the company-owned fob once, press
   the real fob so the receiver advances, replay the stale capture, and show
   that nothing happens.

The dummy placeholder in the safe pack
(`competition-pack/subghz/demo_remote_TODO.sub`) is never transmitted.

## Optional bruteforce segment (only if the authorization record names it)

Pre-generated bruteforce files come from the public repository
[tobiabocchi/flipperzero-bruteforce](https://github.com/tobiabocchi/flipperzero-bruteforce).
They are not vendored in this repo: download them on the computer, review
them, and copy only the file for the lab receiver's protocol onto the lab SD.

Hard rules for that segment:

- Own gate/receiver only, on the legal band from the bench card.
- Transmit only for the airtime written on the authorization record, then
  stop.
- Warn the room about airtime before the transmission starts.
- If the authorization record does not name this beat, it does not run.
