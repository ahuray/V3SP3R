# Evil portal demo page (LAB USE ONLY)

`index.html` is the captive-portal page for the live Wi-Fi station
(docs/competition-live-runbook.md, station D5). It is served by the ESP32-S2
devboard running WiFi Marauder firmware, on the isolated lab network only.

## Placement on the Flipper SD card

Copy `index.html` onto the lab Flipper's SD card at the path your portal build
expects:

- Evil-portal FAPs conventionally read `/ext/apps_data/evil_portal/index.html`
  (the live manifest targets that path).
- Some Marauder/FAP builds expect a different directory or let you pick the
  HTML file from the SD browser.

TODO(verify): confirm the exact path and the POST endpoint (`/login` here)
against the Marauder build flashed on the lab devboard before demo day. Any
POST to the portal is captured; the form action is relative on purpose.

## Hard rules for this page

- No external resources: no scripts, fonts, images, or links off this file.
- No real branding: `LabGuest WiFi` is a fictional lab network. Do not restyle
  it to imitate a real company, ISP, or venue login page.
- Test credentials only: the audience signs in with organizer-issued test
  accounts, never real ones.
- Captured entries stay on the lab SD card and are deleted on stage during the
  restore checklist.
