# Sub-GHz

`demo_remote_TODO.sub` is a Princeton-shaped template at 433.92 MHz. The key is the placeholder `DE AD BE EF`. It will not open anything. Copy it to `/ext/subghz/demo_remote_TODO.sub` if you want the template on the card, then capture a real button from a remote you own into a separate file on the SD card.

Leave the placeholder key in this repository. The validator requires a `Key:` line containing `DE AD BE EF`, so a real capture cannot replace this file and still pass.

## Capture your own remote

1. Flipper → Sub-GHz → Read.
2. Press one button on a remote you own.
3. Save. The capture is stored under `/ext/subghz/` on the Flipper SD card.

Never capture third-party signals. That includes a neighbor's outlet, a gate you do not own, and a vehicle that is not yours.

## Region

Country/region: ____ — transmit only on ISM bands legal there; Unleashed firmware unlocks extended frequencies, stay on your regional band.

## Rolling codes

Most modern gates and cars use rolling codes. Replay the capture you took from your own remote: the receiver does not open, because that code was already used. When the replay demo fails, that failure is the defense talking point.
