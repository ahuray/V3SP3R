# NFC and RFID badge audit

Use this station on badges and test cards the organizers issued for the lab.

The stock NFC app, including on Unleashed firmware, detects magic/Gen1a cards. A hit on that check is the finding to write on the score sheet. A default MIFARE Classic key dictionary ships with the firmware at `/ext/nfc/assets/mf_classic_dict.nfc`.

TODO: add company-specific keys as a user dictionary on the lab SD card. Keep that dictionary out of this repository.

NEVER capture, store, or share real credentials or coworker badge data outside the lab. This folder is documentation only; do not add badge dumps here. Wipe station SD cards at the end of the session.
