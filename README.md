# MarketRival
**Market Competition Simulator**

---

## Project Overview
MarketRival is a 2D, top-down simulation game where the player runs a shop competing against an AI-controlled rival across multiple rounds. Customers with different behaviors evaluate shops based on price, availability, and demand trends.

The objective is to outperform the rival by making better purchasing decisions, managing inventory efficiently, and responding to evolving market demand.

The project emphasizes:
- Agent-based simulation
- Adaptive AI behavior
- Deterministic economic rules
- Structured UI-driven gameplay (not console/debug driven)

---

## Current Features (As Implemented)

### Core Gameplay Loop
Each round follows:

**BUY → SELL → RESULTS**

- BUY Phase:
  - Player purchases inventory via UI (buttons/menu)
  - Rival AI stocks inventory based on demand heuristics

- SELL Phase:
  - Customers spawn and physically move across the map
  - Customers choose shops and attempt purchases
  - Inventory and affordability determine successful sales

- RESULTS Phase:
  - Revenue, profit, and demand statistics are finalized
  - Data is stored for next round decisions

---

### Player UI (Fully Implemented)

#### Order Menu
- Displays all available items
- Shows:
  - Buy price (vendor cost)
  - Sell price
- Includes clickable **BUY buttons**

#### Inventory Menu
- Displays player-owned items and quantities

#### Round Summary Tab (Top Center UI)
- Displays:
  - Current round
  - Current wallet
  - Current phase (BUY / SELL / RESULTS)
- Expandable panel showing:
  - Top desired item (last round)
  - Top sold item
  - Total spent
  - Revenue
  - Profit
  - Items sold (with wrapping text)

---

### Visual Simulation

#### Tile-Based Map
- Static, hand-placed environment (non-random)
- Includes:
  - Two shops (player + rival)
  - Road system
  - Distributed vegetation for visual density

#### Customer Movement
- Customers:
  - Spawn off-screen
  - Move along road
  - Enter shops via entrance points
  - Exit after interaction

#### Sprite System
- Pixel-art characters (sprite sheet)
- Nearest-neighbor scaling for crisp visuals

---

### AI Systems

#### Customer AI
Customers evaluate items using scoring logic based on:
- affordability
- availability
- item attractiveness

Each customer:
- chooses a shop
- attempts to purchase one item
- contributes to demand tracking

---

#### Rival AI
The rival shop:
- analyzes previous round demand
- guarantees minimum stock for demanded items
- allocates remaining budget using a heuristic:

This prioritizes:
- high-demand items
- high-profit-margin items

---

### Market Analytics

Tracked per round:
- items desired
- items sold
- revenue
- profit
- inventory flow

These directly influence:
- AI decisions
- player strategy

---

## Architecture Overview

### Simulation Engine
- `MarketEngine`
  - controls game loop and phase transitions
  - processes customer interactions
  - manages game state

### Round Management
- `RoundManager`
  - tracks phase (BUY / SELL / RESULTS)
  - tracks round number
  - stores per-round analytics

### Customer System
- `CustomerSpawner`
  - handles spawning and movement
- `DecisionLogic`
  - determines purchases

### Shop System
- `Shop`
  - inventory, revenue, spending
- `ItemCatalog`
  - defines available items
- `Item`
  - pricing and attributes

### UI Layer
- `BuyMenuUI`
- `RoundSummaryTabUI`
- (legacy debug HUD removed)

### Rendering
- `FirstScreen`
  - main render loop
  - input handling
  - UI coordination

---

## Technology Stack
- Language: Java  
- Framework: LibGDX  
- Rendering: SpriteBatch (2D)  
- Build: Gradle  
- IDE: Eclipse  
- Version Control: Git / GitHub  
- Assets: Open-license pixel art  

---

## Project Timeline

## Project Timeline

Development strategy:
Rules → Simulation → AI → UI → Polish

---

### 2/3 – 2/10: Project Setup and Core Models
- Set up the LibGDX project structure
- Created main Java package structure
- Implemented core domain models:
  - `Item`
  - `Customer`
  - `Shop`
  - `DecisionLogic`
- Built initial customer purchase behavior
- Began validating game rules through simulation output

---

### 2/10 – 2/17: Simulation Engine and Gameplay Loop
- Built simulation engine components:
  - `MarketPlan`
  - `CustomerSpawner`
  - `MarketEngine`
  - `RoundManager`
  - `RivalAI`
- Connected simulation to LibGDX via `FirstScreen`
- Added keyboard-based buy controls
- Displayed inventory, demand, and revenue stats
- Implemented first working loop:
  BUY → SELL → RESULTS
- Achieved first playable demo

---

### 2/17 – 2/24: Multi-Item System and Demand Tracking
- Replaced fixed-item system with `ItemCatalog`
- Updated engine to support:
  - dynamic item selection
  - scalable inventory system
- Added demand tracking:
  - desired items (Sell phase)
  - sold items (Sell phase)
- Refactored simulation to scale cleanly

---

### 2/24 – 3/3: Rival AI + Round System
- Improved `RivalAI` with demand-based logic

Rival AI now:
- reads previous round demand
- buys at least one of demanded items
- spends remaining cash using scoring

Heuristic:
score = demandShare * (price / vendorCost)

- Implemented finite round system
- `RoundManager` now tracks:
  - current round
  - max rounds
  - game outcome
- Added `GameOverScreen`
- Implemented win / loss / tie conditions
- Updated HUD with round + cash display

---

### 3/3 – 3/10: Customer Movement and Shop Flow
- Expanded `CustomerSpawner` into full movement system

Customers now:
- spawn from either side
- walk along the road
- enter shops via entrances
- disappear inside shops
- wait briefly (shopping time)
- exit map afterward

- Added logic to visit second shop if needed
- Improved spacing between customers
- Aligned movement with road visually

---

### 3/10 – 3/17: Tile Map and World Rendering
- Created `TileMap` system
- Added:
  - grass tiles
  - road tiles
  - entrance paths
  - vegetation layer
- Replaced tile-based shops with sprite-based shops
- Added player and rival shop sprites
- Updated rendering pipeline in `FirstScreen`
- Ensured consistent pixel scaling

---

### 3/17 – 3/24: Buy Menu and Inventory UI
- Created `BuyMenuUI`
- Added:
  - order panel
  - inventory panel
  - clickable BUY buttons
- Integrated pixel font (pxfntfree9-8x10)
- Updated item display:
  - shortened names
  - buy price
  - sell price
- Fixed UI alignment and formatting

---

### 3/24 – 3/31: Round Summary UI and Analytics
- Created `RoundSummaryTabUI`
- Added top-center tab displaying:
  - phase (BUY / SELL / RESULTS)
  - round number
  - player wallet

Expandable panel shows:
- top desired item
- top sold item
- amount spent
- revenue
- profit
- items sold

- Updated `RoundManager` to track:
  - last round spending
  - revenue
  - profit
  - demand stats
- Implemented text wrapping for long lines
- Fixed unstable phase switching display

---

### 3/31 – 4/7: Debug Cleanup and Visual Polish
- Removed debug HUD clutter
- Finalized main gameplay screen:
  - map
  - shops
  - customers
  - UI panels
- Expanded vegetation placement
- Eliminated:
  - overlapping sprites
  - blocked paths
  - empty regions
- Improved overall scene composition

---

### 4/7 – 4/14: UI Integration and System Stability
- Finalized interaction between:
  - `RoundManager`
  - `BuyMenuUI`
  - `RoundSummaryTabUI`
- Fixed phase transition instability:
  - removed rapid switching between phases
  - stabilized SELL → RESULTS flow
- Ensured consistent phase display (BUY / SELL / RESULTS)
- Verified correct tracking of:
  - spending
  - revenue
  - profit
  - demand stats
- Completed functional integration of all major systems

---

### 4/14 – 4/21: UI Stabilization and Menu Completion
- Finalized `BuyMenuUI` layout and interaction
- Added clickable BUY buttons per item
- Fixed inventory display mismatch
- Standardized money formatting:
  - decimal precision
  - consistent UI formatting
- Integrated updated font with:
  - decimal support
  - dollar symbol support
- Fixed text alignment and spacing issues
- Corrected UI positioning and layering

---

### 4/21 – 4/28: Round Summary System Finalization
- Completed `RoundSummaryTabUI` functionality
- Added expandable round summary panel
- Finalized display of:
  - top desired item
  - top sold item
  - spending
  - revenue
  - profit
  - items sold
- Implemented text wrapping for item lists
- Fixed overflow issues in UI panel
- Improved formatting and alignment of summary data

---

### 4/28 – 5/5: Map Finalization and Debug Removal
- Finalized static `TileMap` layout
- Reworked vegetation placement:
  - removed overlapping sprites
  - prevented placement on roads
  - avoided shop overlap
- Filled all empty map regions for visual completeness
- Ensured consistent spacing across entire map
- Fixed rendering issues:
  - removed duplicate/ghost sprites
  - resolved incorrect draw layering
- Removed all debug text from `FirstScreen`
- Cleaned rendering pipeline to show only gameplay elements

---

## Current State
- Fully playable round-based simulation
- Complete UI system (menus + summary tab)
- Adaptive rival AI
- Customer movement and shop interaction
- Fully populated static map
- Clean presentation with no debug dependency
---

## Future Improvements
- Enhanced AI strategies
- Sound effects and feedback
- UI animations and transitions
- Expanded map environments
- Save/load system
