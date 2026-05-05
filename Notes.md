## 2/3 – 2/10
- Set up LibGDX project structure

### Core Models
- Implemented Item model
- Implemented Customer model
- Implemented Shop model
- Implemented DecisionLogic (customer purchase behavior)

Core models should be finished now. Next goal is to build a simulation engine to help with debugging.

### Simulation Engine
- Built MarketPlan (fixed customer distribution)
- Built CustomerSpawner (visual and logical customer flow)
- Built RivalAI (inventory strategy based on demand)
- Built RoundManager (BUY → SELL → RESULTS phase control)

### UI Integration
- Connected simulation to LibGDX via FirstScreen
- Implemented player buy controls
- Implemented rival auto stocking
- Displayed inventory, demand, and revenue statistics

### Milestone Completion
- Demo is running as expected
- Stats are matching initial values
- AI shows simple adaptability
- Cleaned up and refactored some code

---

## 2/10 – 2/17

### Multi-Item System
- Reworked demo that originally supported only two items
- Implemented ItemCatalog to define full market item set
- Updated inventory and purchasing logic to reference catalog items

Updated MarketEngine to support:
- dynamic catalog item selection
- multi-item inventory handling
- vendor cost lookup through catalog

### Demand Tracking
- Extended RoundManager to track per-item activity

Added:
- desired items during Sell Phase
- sold items during Sell Phase

These persist between rounds and feed into AI + analytics.

### System Cleanup
- Removed hardcoded item assumptions
- Refactored simulation classes for scalability

---

## 2/17 – 2/24

### Rival AI Upgrade
- Improved RivalAI stocking behavior using demand heuristics

AI now:
- reads previous round demand
- buys at least one unit of demanded items
- spends remaining cash greedily

Scoring heuristic:
score = demandShare * (price / vendorCost)

This prioritizes:
- high demand
- high profit margin

### UI Improvements
- Updated HUD to support:
  - dynamic catalog display
  - per-item demand stats
  - per-item sales stats
- Removed early prototype UI assumptions

### Code Refactoring
- Added responsibility comments
- Cleaned package structure
- Removed unused demo logic
- Standardized naming

### Milestone Completion
- Fully dynamic item system working
- AI responding to demand correctly

---

## 2/24 – 3/3

### Round-Based Game Loop
- Implemented finite round system

RoundManager now tracks:
- current round
- max rounds
- game outcome conditions

Each round:
BUY → SELL → RESULTS

Revenue applied during RESULTS phase.

### Game Over System
- Implemented GameOverScreen

End conditions:
- WIN (player > rival)
- LOSS (rival > player)
- TIE

Restart resets session.

### HUD Updates
- Added:
  - round display
  - player/rival cash
  - updated round stats

### Engine Integration
- Synced:
  - MarketEngine
  - RoundManager
  - HUD rendering

Ensured:
- correct phase transitions
- accurate revenue handling

### Milestone Completion
- Full playable loop achieved

---

## 3/3 – 3/10

### Customer Movement System
- Expanded CustomerSpawner into full movement system

Customers now:
- spawn from either side
- walk along road
- enter shops via entrances
- disappear inside shop
- wait briefly (simulate shopping)
- exit map

### Behavior Improvements
- Added logic for visiting second shop if needed
- Improved spacing between customers
- Aligned movement visually to road

---

## 3/10 – 3/17

### Tile Map System
- Created TileMap class
- Added:
  - grass tiles
  - road tiles
  - entrance paths
  - vegetation layer

### Rendering Changes
- Replaced tile-built shops with sprite-based shops
- Added separate player + rival shop sprites
- Updated FirstScreen rendering order

### Visual Consistency
- Fixed scaling issues
- Ensured pixel-art consistency

---

## 3/17 – 3/24

### Buy Menu UI
- Created BuyMenuUI

Added:
- order panel
- inventory panel
- BUY buttons

### UI Rendering
- Integrated pixel font (pxfntfree9-8x10)
- Implemented text rendering system

### Formatting Fixes
- Adjusted:
  - item naming (shortened)
  - cost display
  - spacing/alignment
- Fixed button positioning issues

---

## 3/24 – 3/31

### Round Summary UI
- Created RoundSummaryTabUI

Top tab displays:
- phase
- round
- wallet

Expandable panel shows:
- top desired
- top sold
- spent
- revenue
- profit
- items sold

### Data Tracking
- Extended RoundManager:
  - spending tracking
  - revenue tracking
  - profit calculation
  - demand persistence

### UI Fixes
- Implemented text wrapping
- Fixed overflow issues
- Stabilized phase display

---

## 3/31 – 4/7

### Debug Cleanup
- Removed debug HUD clutter
- Focused screen on actual gameplay UI

### Map Polish
- Expanded vegetation placement
- Eliminated:
  - overlapping sprites
  - blocked entrances
  - empty map regions

### Visual Improvements
- Balanced top vs bottom map density
- Improved overall scene readability

---

## 4/7 – 4/14

### System Stability
- Fixed phase transition issues:
  - eliminated rapid BUY/SELL switching
- Stabilized SELL → RESULTS transition

### Integration
- Finalized interaction between:
  - RoundManager
  - BuyMenuUI
  - RoundSummaryTabUI

### Validation
- Verified correctness of:
  - spending
  - revenue
  - profit
  - demand tracking

---

## 4/14 – 4/21

### UI Stabilization
- Finalized BuyMenuUI layout and interaction
- Fixed inventory mismatch bug

### Money Formatting
- Added:
  - decimal support
  - dollar symbol
- Fixed sprite font spacing issues

### UI Layout Fixes
- Anchored menus correctly
- Fixed layering (panels vs buttons)
- Corrected text alignment

---

## 4/21 – 4/28

### Round Summary Finalization
- Completed RoundSummaryTabUI behavior

Added:
- full analytics display
- correct formatting of stats
- proper wrapping of items sold list

### Bug Fixes
- Fixed overflow issues
- Fixed incorrect stat values
- Cleaned up formatting inconsistencies

---

## 4/28 – 5/5

### Map Finalization
- Completed static TileMap layout

### Vegetation Placement
- Removed:
  - plant overlap
  - road overlap
  - shop overlap
- Evenly distributed vegetation across map
- Filled all empty areas

### Rendering Fixes
- Removed ghost/double-rendered sprites
- Fixed persistent draw issues

### Debug Removal
- Removed all debug text from FirstScreen

### Final State
- Game is now:
  - fully playable
  - visually complete
  - UI-driven (no debug dependency)
