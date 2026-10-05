# OBD-Hero Core Philosophy & Quality Rule

## Universal First-Class Standards: "The App", Not a Compromise
Every vehicle manufacturer supported by OBD-Hero must be treated with equal depth, rigor, and technical seriousness. There are no "second-class" brands or token implementations.

### 1. Exhaustive Data Depth
- **No Placeholder Datasets**: Whenever a vehicle make, platform profile, or diagnostic category is added, exhaustively ingest and bundle complete real-world datasets (master codes, proprietary DIDs, specific ECU headers, and factory inspection guides).
- **Honest Transparency**: If specific dealer-proprietary service bulletin data is genuinely unavailable publicly, be 100% upfront and transparent (*"Data unavailable publicly. Contact an official repair center for details"*). Never invent fake diagnostic procedures or hide limitations behind generic fluff.

### 2. Multi-Computer CAN Integrity
- Understand the real-world physical and electronic architecture of each car (Gateway routing, CAN-FD, MS-CAN vs HS-CAN, Security Gateways like SGW/VIP, and module-specific sleep/wake behaviors like BMW FRM).
- Query all computers on the vehicle (ECM, TCM, ABS, BCM/TPMS, SRS, Airbag), never just the engine broadcast.

### 3. Professional Workshop & Enthusiast Utility
- Deliver actionable, dealer-grade utility: component pinout tests, exact wiring check procedures, factory failure modes, and fluid temperatures needed for mechanical maintenance (e.g. transmission fluid service levels).
