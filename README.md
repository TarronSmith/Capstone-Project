# MarketRival
**Adaptive Market Competition Simulator**

## Project Overview
MarketRival is a 2D, top-down simulation game in which the player operates a shop competing against an AI-controlled rival in a finite market. Customers with different purchasing behaviors choose where to shop based on price, quality, popularity, and social influence. The player’s objective is to generate more profit than the rival and ultimately force them into bankruptcy through better strategic decisions.

The project emphasizes adaptive AI decision-making, agent-based simulation, and clearly defined, deterministic game rules rather than real-time action gameplay.

---

## Project Goals

### Primary Goals
- Implement a complete market simulation with discrete Buy Phases and Sell Phases
- Model heterogeneous customer behavior using agent-based decision logic
- Design a rival AI that adapts to market conditions using heuristic strategies

### Secondary Goals
- Provide player-facing analytics to support strategic decisions
- Maintain a clean, modular architecture
- Demonstrate good software engineering practices (version control, documentation)

---

## Game Rules & Mechanics

### Phase Structure
Each turn consists of two phases:

#### Buy Phase
- The player and rival AI purchase inventory using available funds
- Market statistics from previous rounds are visible
- Purchased items become available for sale during the upcoming Sell Phase

#### Sell Phase
- Customers enter the market and choose a shop to visit
- Each customer evaluates the available inventory
- Customers attempt to purchase at most one item
- A sale occurs only if the item is in stock and affordable
- Sales update revenue, profit, and market demand statistics

A shop is considered bankrupt when its wallet reaches zero or below.

---

## Game Loop
Each round of the simulation follows this process:
### Buy Phase:
- Player and rival purchase inventory

### Customer Spawn
- Customers are generated according to the market distribution

### Shop Selection
- Customers evaluate both shops and choose where to visit

### Sell Phase
- Customers attempt to purchase items

### Results
- Sales, revenue, and demand statistics are recorded for the next round

---

## AI Design

### Customer AI
Customers are modeled as agents with behavioral profiles that influence their purchasing decisions.

Each customer has a CustomerProfile containing parameters such as:
- priceSensitivity : preference for lower prices
- qualitySensitivity : preference for higher quality goods
- prestigeBias : preference for premium products
- loyalty : tendency to return to previously visited shops
- stockoutAversion : tendency to avoid shops that previously failed to satisfy them

Customers evaluate items using a scoring function implemented in DecisionLogic. The item with the highest score that the customer can afford is selected.
Customers also maintain limited memory of previous rounds, which influences future behavior.

### Rival AI
The rival shop uses heuristic decision logic to determine how to stock inventory each round.
The AI considers factors such as:
- available funds
- recent demand patterns
- inventory composition
- expected profitability

---

## Technology Stack
- **Language:** Java  
- **Framework:** LibGDX  
- **Game Style:** 2D pixel art, top-down simulation  
- **Sprite Resolution:** 16×16 pixels (scaled at runtime)  
- **AI Techniques:** Utility-based heuristics, agent-based modeling  
- **Build System:** Gradle  
- **IDE:** Eclipse  
- **Version Control:** Git and GitHub  
- **Assets:** Open-license pixel art from itch.io  

---

## Project Architecture
### Simulation Engine
MarketEngine:
- Controls the main simulation loop
- Manages phase transitions
- Spawns customers
- Processes purchases and market statistics

### Round Management
RoundManager:
- Tracks the current game phase
- Maintains turn statistics
= Controls phase progression

### Customer System
- Customer : represents an individual agent
- CustomerProfile : defines behavioral parameters
- DecisionLogic : determines shop and item choices

### Shop System
- Shop : manages inventory, sales, and revenue
- Item : represents products with price and quality attributes

### Customer Spawning & Movement
CustomerSpawner:
- Generates customers according to the market distribution
- Handles visual movement toward shops during the Sell Phase

Rendering Layer
- FirstScreen : main game screen and input handling
- HudRenderer : displays game statistics
- CustomerListRenderer : displays customer activity logs

---

## Project Timeline
Development Strategy:
**Rules -> Simulation -> AI -> UI -> Polish**, ensuring a working system early and reducing scope risk.

### Feb 2 – Feb 16: Core Rules & Data Models
- Define core domain classes (`Shop`, `Item`, `Inventory`, `Customer`, `MarketState`)
- Implement deterministic game rules (wallet updates, inventory constraints, bankruptcy)
- Validate correctness using console output and test simulations

### Feb 17 – Mar 2: Buy/Sell Phase Simulation Engine
- Implement the turn loop: **Buy Phase -> Sell Phase -> End-of-Turn Updates**
- Implement purchase resolution (stock checks, affordability, profit calculation)
- Track per-turn metrics (sales, revenue, profit, market share)

### Mar 3 – Mar 16: Customer Agent Behavior
- Implement customer types (Followers, Prestige Buyers, Value Buyers)
- Implement weighted scoring logic for shop and item selection
- Verify market dynamics through repeated simulation runs

### **Mar 17: Midterm Checkpoint**
- Fully functional market simulation (logic-complete)
- Buy and Sell Phases operational
- Customer behavior implemented and testable
- Console-driven or minimal UI demonstration available

### Mar 18 – Apr 6: Rival AI (Utility-Based Heuristics)
- Implement rival shop decision-making (pricing, inventory purchasing)
- Implement utility scoring (expected profit, market share, bankruptcy risk)
- Add constraints to ensure the AI is challenging but beatable

### Apr 7 – Apr 20: UI Integration & Visuals
- Connect simulation engine to LibGDX UI
- Display core statistics (wallets, inventory, turn number, profits)
- Integrate 16×16 pixel art assets and basic animations

### Apr 21 – May 5: Analytics, Balancing, and Final Polish
- Add player-facing analytics (item performance, demand indicators)
- Balance AI difficulty and economic parameters
- Bug fixes, documentation updates, and final submission preparation
