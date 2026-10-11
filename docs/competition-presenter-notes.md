# Presenter notes — competition demo day

Two audiences, two branches. **Coworkers** see `safe-test` (full dummy): a
magic show with a moral. **Head of Security** sees `unsafe`: a budget argument
with props. This file is only the stage layer. The safety gates live in
`docs/competition-runbook.md` and `docs/competition-live-runbook.md`; nothing
here relaxes them.

## For the head of Security (`unsafe`)

### The arc — 20 minutes, then Q&A

1. **Cold open (0:00–1:00).** No agenda, no intro. Plug the Flipper into the
   sacrificial laptop while people sit down and run the key reveal. The lab
   WiFi password is on the projector before anyone has checked their phone.
   *Then* say good morning.
2. **One-line framing.** Hold up the Flipper: "This costs less than the pizza
   budget, and everything you see today ships in the official app catalog."
3. **Act 1 — your laptop betrays you (D1).** Replay the cold-open moment
   ("that was 30 seconds"), then the credential prompt. A briefed organizer
   types the test account; the room sees the username come back and hears the
   password was discarded. "A real phish would have taken it."
4. **Act 2 — your doors are already open (D3).** Clone the lab badge and hand
   the clone to the head of Security. Let them open the reader themselves. A
   door opening in their own hand beats any slide.
5. **Act 3 — your network is a suggestion (D5).** Deauth: they watch the lab
   client drop and rejoin. Beacon spam for the laugh. Portal: they type the
   test credential into the LabGuest page, then watch you delete the capture
   on stage.
6. **The close (2 min).** Compress the blue beats into one message: "Every
   attack you saw has a control, and the control is cheaper than the
   incident." Then the ask.

### Participation is the anti-sleep drug

Three moments where their hands are on the gear: typing the test credential,
opening the door with the clone, connecting to the portal. People remember
what they did, not what they watched.

### Stagecraft that lands

- **Silence is a tool.** When the key appears, stop talking. Count to five.
  Let them read it.
- **Narrate the typing.** "It types faster than you can — this is a keyboard,
  not a Windows exploit. That is why device control is the fix."
- **Name the clock.** "27 seconds from plugging in to a WiFi key." Numbers get
  quoted in the follow-up mail; adjectives do not.
- **Never apologize, never debug on stage.** If a beat fails: "the demo gods
  tax us," play the rehearsal video of that beat, keep the story moving.

### Every red beat ends with a control and a price

| They saw | The control | Rough cost |
| --- | --- | --- |
| BadUSB key reveal + credential prompt | Block unknown HID (device-control policy / Intune) | one policy, near zero |
| Badge clone | Migrate 125 kHz to MIFARE DESFire EV3 | per-door hardware + cards |
| Deauth + rogue portal | WPA3/802.1X, rogue-AP and deauth detection, staff brief | config + monitoring time |

Fill in the real numbers before demo day. This table is the point of the
show: it converts fear into budget.

### Demo insurance (T-minus)

- **T-1 week.** Full rehearsal on the actual laptop, AP, and cards. Both
  key-reveal variants tested; the working one stays on the SD. Portal path
  TODO(verify) closed. Authorization record signed.
- **T-1 day.** Second full rehearsal, recorded on video (the fallback).
  Charge everything. Label two SD cards. Disable Windows Update and
  notifications on the laptop. Print the authorization record.
- **T-1 hour.** Validators run (`PACK LIVE OK`, `PACK OK`). Projector tested
  with the real cable. Phone on DND, hotspot off. Restore checklist staged
  with a named witness.

### Questions to expect

- "Is this legal?" → the signed authorization record; every target is ours.
- "Could this hit us for real?" → yes, where the control is missing. Point at
  the table row.
- "Why haven't we seen this before?" → nobody looked. That is the finding,
  and it is the cheapest one you will fund this year.

### The ask

One page left behind: three gaps, three fixes, the costs, and one sentence —
"I can pilot the first control in two weeks."

## For coworkers (`safe-test`)

Same stations, different frame: a magic show with a moral.

- Let them press the button: rickroll payload on the volunteer laptop, the
  badge clone in their hand.
- The badge clone lands hardest with non-technical folks — it is a photocopy
  of a door key.
- Close with three habits, not three controls: lock your PC when you stand
  up, never plug in a found USB device, report a weird login portal instead
  of typing into it.
- Fifteen minutes, no more. The rickroll is the applause line; end there.
