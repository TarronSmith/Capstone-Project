# MarketRival

**Market Competition Simulator**

---

## Project Overview

MarketRival is a 2D, top-down simulation game where the player runs a shop competing against an AI-controlled rival across multiple rounds. Customers with different behaviors evaluate shops based on price, availability, and demand trends.

The objective is to outperform the rival by making better purchasing decisions, managing inventory efficiently, and responding to evolving market demand.

The project emphasizes:

* Agent-based simulation
* Adaptive, rule-based AI behavior driven by previous-round demand
* Explicit economic rules with controlled randomness in customer behavior
* Structured UI-driven gameplay rather than console-driven or debug-driven interaction

---

## Current Features (As Implemented)

### Core Gameplay Loop

Each round follows:

**BUY → SELL → RESULTS**

* BUY Phase:

  * Player purchases inventory through the user interface
  * Rival AI stocks inventory based on demand heuristics

* SELL Phase:

  * Customers spawn and physically move across the map
  * Customers choose shops and attempt purchases
  * Inventory and affordability determine successful sales

* RESULTS Phase:

  * Revenue, profit, and demand statistics are finalized
  * Data is stored for subsequent-round decisions

---

### Player UI (Fully Implemented)

#### Order Menu

* Displays all available items
* Shows:

  * Buy price (vendor cost)
  * Sell price
* Includes clickable **BUY buttons**

#### Inventory Menu

* Displays player-owned items and quantities

#### Round Summary Tab (Top Center UI)

* Displays:

  * Current round
  * Current wallet
  * Current phase (BUY / SELL / RESULTS)
* Expandable panel showing:

  * Top desired item from the previous round
  * Top sold item
  * Total spent
  * Revenue
  * Profit
  * Items sold with text wrapping

---

### Visual Simulation

#### Tile-Based Map

* Static, hand-placed environment
* Includes:

  * Two shops: player and rival
  * Road system
  * Distributed vegetation for visual density

#### Customer Movement

Customers:

* Spawn off-screen
* Move along predefined waypoint routes
* Enter shops through designated entrance points
* Visit the second shop if a purchase cannot be completed at the first
* Exit the map after interacting with the shops
* Use local-separation steering to reduce visual overlap

#### Sprite System

* Pixel-art characters using sprite sheets
* Nearest-neighbor scaling for crisp visuals

---

### AI Systems

#### Customer AI

Customers score affordable products using:

* Price
* Quality
* Individual price and quality preferences

Each customer:

* Chooses a shop based on available products, previous visits, loyalty, and prior stockouts
* Attempts to purchase one item
* Contributes to demand tracking

---

#### Rival AI

The rival shop:

* Analyzes demand from the previous round
* Attempts to purchase at least one unit of demanded items when affordable
* Allocates its remaining budget using a scoring heuristic

The scoring formula is:

`score = demandShare * (price / vendorCost)`

This prioritizes:

* High-demand items
* Items with stronger revenue potential relative to their vendor cost

The rival uses a hand-designed scoring heuristic rather than a trained machine-learning model.

---

### Market Analytics

Tracked per round:

* Items desired
* Items sold
* Revenue
* Profit
* Inventory flow

These directly influence:

* Rival AI decisions
* Player strategy

---

## Architecture Overview

### Simulation Engine

* `MarketEngine`

  * Controls the game loop and coordinates phase transitions
  * Processes customer interactions
  * Manages active simulation state
  * Coordinates the major simulation subsystems

### Round Management

* `RoundManager`

  * Tracks the current phase (BUY / SELL / RESULTS)
  * Tracks the current round and maximum number of rounds
  * Stores per-round analytics
  * Manages player and rival cash
  * Determines win, loss, and tie outcomes

### Customer System

* `CustomerSpawner`

  * Handles customer spawning
  * Controls waypoint-based customer movement
  * Applies local-separation behavior

* `DecisionLogic`

  * Scores available products
  * Determines shop selection
  * Determines purchase selection

* `CustomerProfile`

  * Stores behavioral weights such as price sensitivity, quality preference, loyalty, and stockout aversion

### Shop System

* `Shop`

  * Manages inventory
  * Processes customer purchases
  * Tracks revenue and units sold

* `ItemCatalog`

  * Defines available items
  * Stores vendor acquisition costs

* `Item`

  * Represents product names, prices, and quality values

### UI Layer

* `BuyMenuUI`
* `RoundSummaryTabUI`
* Legacy debug HUD removed from the final gameplay presentation

### Rendering

* `FirstScreen`

  * Runs the main rendering loop
  * Handles player input
  * Coordinates the user interface and visual simulation

---

## Technology Stack

* Language: Java
* Framework: LibGDX
* Rendering: SpriteBatch (2D)
* Build System: Gradle
* IDE: Eclipse
* Version Control: Git and GitHub
* Assets: Open-license pixel art

---

## Project Timeline

Development strategy:

**Rules → Simulation → AI → UI → Polish**

---

### 2/3 – 2/10: Project Setup and Core Models

* Set up the LibGDX project structure
* Created the main Java package structure
* Implemented core domain models:

  * `Item`
  * `Customer`
  * `Shop`
  * `DecisionLogic`
* Built the initial customer purchase behavior
* Began validating game rules through simulation output

---

### 2/10 – 2/17: Simulation Engine and Gameplay Loop

* Built simulation engine components:

  * `MarketPlan`
  * `CustomerSpawner`
  * `MarketEngine`
  * `RoundManager`
  * `RivalAI`
* Connected the simulation to LibGDX through `FirstScreen`
* Added keyboard-based buying controls
* Displayed inventory, demand, and revenue statistics
* Implemented the first working loop:

  * BUY
  * SELL
  * RESULTS
* Achieved the first playable demonstration

---

### 2/17 – 2/24: Multi-Item System and Demand Tracking

* Replaced the fixed-item system with `ItemCatalog`
* Updated the engine to support:

  * Dynamic item selection
  * Scalable inventory management
* Added demand tracking for:

  * Items desired during the SELL phase
  * Items sold during the SELL phase
* Refactored the simulation to remove hard-coded product assumptions and support additional items

---

### 2/24 – 3/3: Rival AI and Round System

* Improved `RivalAI` with demand-based decision logic

The rival AI now:

* Reads demand from the previous round
* Attempts to buy at least one unit of demanded items
* Spends its remaining cash using heuristic scoring

Heuristic:

`score = demandShare * (price / vendorCost)`

* Implemented a finite round system
* Updated `RoundManager` to track:

  * Current round
  * Maximum number of rounds
  * Game outcome
* Added `GameOverScreen`
* Implemented win, loss, and tie conditions
* Updated the HUD with round and cash information

---

### 3/3 – 3/10: Customer Movement and Shop Flow

* Expanded `CustomerSpawner` into a complete movement system

Customers now:

* Spawn from either side of the map
* Walk along the road using predefined waypoints
* Enter shops through designated entrances
* Disappear inside shops
* Wait briefly to simulate shopping
* Visit the second shop when necessary
* Exit the map after completing their interactions

Additional improvements:

* Improved spacing between customers
* Added local-separation movement
* Aligned customer movement visually with the road

---

### 3/10 – 3/17: Tile Map and World Rendering

* Created the `TileMap` system
* Added:

  * Grass tiles
  * Road tiles
  * Entrance paths
  * Vegetation layer
* Replaced tile-built shops with sprite-based shops
* Added separate player and rival shop sprites
* Updated the rendering pipeline in `FirstScreen`
* Ensured consistent pixel scaling

---

### 3/17 – 3/24: Buy Menu and Inventory UI

* Created `BuyMenuUI`
* Added:

  * Order panel
  * Inventory panel
  * Clickable BUY buttons
* Integrated the `pxfntfree9-8x10` pixel font
* Updated item displays to include:

  * Shortened item names
  * Vendor cost
  * Selling price
* Fixed interface alignment and formatting

---

### 3/24 – 3/31: Round Summary UI and Analytics

* Created `RoundSummaryTabUI`
* Added a top-center tab displaying:

  * Current phase
  * Round number
  * Player wallet

The expandable panel displays:

* Top desired item

* Top sold item

* Amount spent

* Revenue

* Profit

* Items sold

* Updated `RoundManager` to track:

  * Previous-round spending
  * Revenue
  * Profit
  * Demand statistics

* Implemented text wrapping for long lines

* Fixed unstable phase display behavior

---

### 3/31 – 4/7: Debug Cleanup and Visual Polish

* Removed debug HUD clutter
* Finalized the primary gameplay screen:

  * Map
  * Shops
  * Customers
  * UI panels
* Expanded vegetation placement
* Eliminated:

  * Overlapping sprites
  * Blocked paths
  * Empty regions
* Improved overall scene composition

---

### 4/7 – 4/14: UI Integration and System Stability

* Finalized interaction between:

  * `RoundManager`
  * `BuyMenuUI`
  * `RoundSummaryTabUI`
* Fixed phase-transition instability:

  * Removed rapid switching between phases
  * Stabilized the SELL-to-RESULTS transition
* Ensured consistent phase display
* Verified correct tracking of:

  * Spending
  * Revenue
  * Profit
  * Demand statistics
* Completed functional integration of all major systems

---

### 4/14 – 4/21: UI Stabilization and Menu Completion

* Finalized the `BuyMenuUI` layout and interaction
* Added clickable BUY buttons for each item
* Fixed an inventory-display mismatch
* Standardized money formatting:

  * Decimal precision
  * Consistent interface formatting
* Integrated an updated font with:

  * Decimal support
  * Dollar-symbol support
* Fixed text alignment and spacing
* Corrected interface positioning and layering

---

### 4/21 – 4/28: Round Summary System Finalization

* Completed `RoundSummaryTabUI` functionality
* Added an expandable round-summary panel
* Finalized the display of:

  * Top desired item
  * Top sold item
  * Spending
  * Revenue
  * Profit
  * Items sold
* Implemented text wrapping for item lists
* Fixed overflow problems in the interface panel
* Improved formatting and alignment of summary data

---

### 4/28 – 5/5: Map Finalization and Debug Removal

* Finalized the static `TileMap` layout
* Reworked vegetation placement:

  * Removed overlapping sprites
  * Prevented placement on roads
  * Avoided shop overlap
* Filled empty map regions for visual completeness
* Ensured consistent spacing across the map
* Fixed rendering problems:

  * Removed duplicate and ghost sprites
  * Corrected draw layering
* Removed debug text from `FirstScreen`
* Cleaned the rendering pipeline to show only gameplay elements

---

## Current State

* Fully playable round-based simulation
* Complete UI system with menus and a round-summary tab
* Demand-responsive, rule-based rival AI
* Customer movement and shop interaction
* Fully populated static map
* Clean presentation without a debug-interface dependency

---

## Future Improvements

* Enhanced rival strategies
* Sound effects and player feedback
* UI animations and transitions
* Expanded map environments
* Save and load system
