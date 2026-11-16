# Accidental Gardener Data

The plant database and growing guides for **The Accidental Gardener** app.

- **Plant wiki:** https://accidental-gardener.github.io/Accidental-Gardener-Data/
- **plants.json** is the database the app downloads. **version.json** tells phones when there's a new version.
- **tools/gen_plants.py** builds plants.json. Edit it and run `python3 tools/gen_plants.py plants.json`, then raise `VERSION`.
- **worker/** is the request service on Cloudflare that turns "please add this plant" requests into GitHub Issues.

## For admins

Requests from the app and the wiki arrive as Issues labelled `plant-request`, `guide-fix` or `feedback`.
Only people with write access to this repo can change the database.
