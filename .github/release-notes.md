2.0.7
## Public home visits and spawn accuracy

- Visiting another player's public home now pays the teleport fee to the home owner instead of deleting the money. The amount is `economy.cost.visit-public` (default 10; falls back to `teleport` if unset).
- If the owner is offline, earnings are stored and paid with a chat summary the next time they join.
- `/vhome` with no arguments opens a GUI of every public home, using player heads via the Mojang Skin API.
- `/spawn` now teleports to the exact stored coordinates (no 2-block safe-search offset). Regular home teleports also prefer the original block first.

## Validation

- Automated tests covering public-home payouts, offline earnings, the `/vhome` browser, and exact spawn teleport.
