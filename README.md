# MarketRival
**Adaptive Market Competition Simulator**

## Project Overview
MarketRival is a 2D, top-down simulation game in which the player operates a shop competing against an AI-controlled rival in a finite market. Customers with different purchasing behaviors choose where to shop based on price, quality, popularity, and social influence. The player’s objective is to generate more profit than the rival and ultimately force them into bankruptcy through better strategic decisions.

The project emphasizes adaptive AI decision-making, agent-based simulation, and clearly defined, deterministic game rules rather than real-time action gameplay.

---

## Project Goals

### Primary Goals
- Implement a complete market simulation with discrete **Buy Phases** and **Sell Phases**
- Model heterogeneous customer behavior using multiple purchasing strategies
- Design a rival AI that adapts to market conditions using utility-based heuristics
- Fully complete, test, and document the project within a single semester

### Secondary Goals
- Provide player-facing analytics to support strategic decisions
- Maintain a clean, modular architecture
- Demonstrate good software engineering practices (version control, documentation)

---

## Game Rules & Mechanics

### Phase Structure
Each turn consists of two phases:

#### Buy Phase
- Occurs before each Sell Phase
- Player and rival AI purchase inventory using available funds
- Market analytics from previous Sell Phases are available
- Purchased inventory becomes available in the upcoming Sell Phase

#### Sell Phase
- Customers visit shops and attempt to purchase items
- Each customer may purchase at most one item
- A sale occurs only if the item is in stock and affordable
- Revenue, costs, and net profit are calculated
- Profit or loss is applied to each shop’s wallet

A shop is considered bankrupt when its wallet reaches zero or below.

---

## AI Design

### Customer AI
Customers are modeled as agents with distinct purchasing strategies:
- **Followers:** Prefer shops with high recent popularity
- **Prestige Buyers:** Influenced by purchases made by wealthy customers
- **Value Buyers:** Evaluate price-to-quality ratios and reject low-quality items

Each customer uses a weighted scoring function to select where to shop.

### Rival Shop AI
The rival shop uses **utility-based heuristic AI**:
- Chooses from a fixed set of actions (pricing, inventory, quality investment)
- Scores actions based on expected profit, market share, and financial risk
- Adapts strategy over time based on observed outcomes
- Operates under constraints to remain challenging but beatable

No machine learning or external APIs are used.

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
- **Core Module:** Game logic, simulation engine, AI, and rules
- **Desktop (LWJGL3) Module:** Desktop launcher only
- **Simulation Engine:** Controls phase progression and game state updates
- **Agent Modules:** Customer agents and rival AI
- **Execution Layer:** Applies decisions deterministically during each phase
- **Assets Directory:** Stores all visual and data assets

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