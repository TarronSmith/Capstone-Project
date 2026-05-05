# MarketRival
**Adaptive Market Competition Simulator**

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

## Updated Development Timeline

### Feb 3 – Feb 10
- Core models (Item, Shop, Customer)
- Initial simulation logic
- Basic decision system

---

### Feb 10 – Feb 17
- Multi-item system via ItemCatalog
- Dynamic inventory handling
- Demand tracking system

---

### Feb 17 – Feb 24
- Rival AI upgrade (demand-driven stocking)
- Refactoring for scalability
- Improved HUD (still debug-based)

---

### Feb 24 – Mar 3
- Full round system (BUY / SELL / RESULTS)
- Game over conditions
- Round tracking and win/loss logic

---

### Mar – Present (Major Progress Phase)

#### UI Overhaul
- Replaced debug HUD with:
  - Buy menu UI
  - Inventory panel
  - Round summary tab
- Added clickable buttons and mouse input

#### Visual Simulation Upgrade
- Implemented customer movement paths
- Added entrance/exit logic for shops
- Built static tile map with full vegetation placement

#### Gameplay Polish
- Added spending, revenue, and profit tracking
- Fixed UI alignment and text rendering
- Implemented text wrapping for dynamic UI content
- Removed debug overlays

---

## Current State of the Project

The game is now:
- Fully playable
- Visually structured
- UI-driven (not debug-driven)
- Using a complete round-based loop

Remaining work is focused on:
- additional UI polish
- balancing
- feature expansion

---

## Future Improvements
- Enhanced AI strategies
- Sound effects and feedback
- UI animations and transitions
- Expanded map environments
- Save/load system
