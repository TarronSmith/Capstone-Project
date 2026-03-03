## 2/3 – 2/10
- Set up LibGDX project structure

### Core Models
- Implemented Item model
- Implemented Customer model
- Implemented Shop model
- Implemented DecisionLogic (customer purchase behavior)
Core models should be finished now. My next goal is to build a simulation engine to help with debugging

### Simulation Engine
- Built MarketPlan (fixed customer distribution)
- Built CustomerSpawner (visual and logical customer flow)
- Built RivalAI (inventory strategy based on demand)
- Built RoundManager (BUY -> SELL -> RESULTS phase control)

### UI Integration
- Connected simulation to LibGDX via FirstScreen
- Implemented player buy controls
- Implemented rival auto stocking
- Displayed inventory, demand, and revenue statistics

### Milestone Completion
- Demo is running the way I want. Stats are matching initial values. AI shows simple adaptablity
- Went back and cleaned up and refractored some of the code

## 2/10 – 2/17

### Multi-Item System
- Reworked the earlier demo implementation that supported only two items.
- Implemented an ItemCatalog system that defines the complete set of items available in the market. Inventory and purchasing logic now reference catalog items rather than hardcoded item types.
Updated MarketEngine to support:
- dynamic catalog item selection
- multi-item inventory handling
- vendor cost lookup through the catalog

### Demand Tracking
- Extended RoundManager to track per-item market activity.
Added structures to record:
- desired items during the Sell Phase
- sold items during the Sell Phase
These statistics persist between rounds and provide information for analytics and AI decisions.

### System Cleanup
- Removed early demo logic tied to fixed item assumptions.
- Refactored several simulation classes so the market can scale to any number of items without changing game logic.

## 2/17 - 2/24

## Rival AI Upgrade
- Improved RivalAI stocking behavior using demand-based heuristics.
The AI now:
- Reads demand data from the previous round
- Buys at least one unit of each demanded item (exploration)
- Spends remaining cash using a greedy scoring approach

Scoring heuristic:
score = demandShare * (price / vendorCost)
This prioritizes items that are both profitable and frequently requested by customers.

## UI Improvements
Updated the HUD to support:
- dynamic catalog display
- per-item demand statistics
- per-item sales statistics
Removed UI elements tied to the earlier two-item prototype.

## Code Refactoring
Performed a cleanup across the codebase

- Added responsibility comments to simulation classes
- Reorganized package structure for clarity
- Removed unused demo references
- Standardized naming conventions

## Milestone Completion
The simulation now supports a fully dynamic item system and improved AI stocking behavior.

## 2/24 - 3/3
### Round-Based Game Loop
Implemented a finite round system for gameplay.

RoundManager now tracks:
- current round number
- maximum number of rounds
- game outcome conditions

Each round follows the phase order:
BUY → SELL → RESULTS
Revenue from each Sell Phase is transferred into the shop’s cash balance during the Results phase.

### Game Over System
Implemented a GameOverScreen that activates when the final round completes.
The system determines the result using final wallet values:
- WIN – player has more money than riva
- LOSS – rival has more money
- TIE – both wallets equal
A restart option resets the session state.

### HUD Updates
Updated the HUD to display:
- current round (Round X / Max)
- player and rival cash
- updated statistics after each round

### Engine Integration
Updated interactions between:
- MarketEngine
- RoundManager
- HUD rendering
to ensure correct phase transitions and accurate revenue accounting.

### Milestone Completion
The game now has a complete playable loop with:
- multi-round gameplay
- better AI competition
- win/loss conditions
- updated HUD
