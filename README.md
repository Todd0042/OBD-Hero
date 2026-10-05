# OBD-Hero

A modern, universal OBD-II diagnostic and maintenance assistant for Android, engineered for common US consumer vehicles (Ford, GM, Stellantis/Chrysler, Toyota, Honda, Nissan, Hyundai, Kia, Subaru, Mazda, VW, BMW).

Optimized for standard Android devices and foldables, including the **Google Pixel 9 Pro Fold**.

## Core Features
- **Universal DTC Diagnostics**: Read active (Mode 03), pending (Mode 07), and permanent (Mode 0A) DTCs with freeze-frame data.
- **Embedded Repair Guide**: Actionable DIY inspection steps, symptoms, and verified fixes built directly into the app (inspired by mx5-dash).
- **Instant VIN Decoding**: Automatic Mode 09 PID 02 retrieval with offline/cached NHTSA vPIC decoding (Year, Make, Model, Engine, Specs).
- **TPMS Management**: *(CANCELLED - Pending dedicated 315/433 MHz RF hardware acquisition)*
- **Battery Calibration (BMS Reset)**: Reset battery monitoring systems and historical charge profiles across major OEMs (Ford, GM, Toyota, VAG, BMW, Stellantis) after installing a new 12V battery.

- **Foldable Ergonomics**: Dual-pane master-detail view on unfolded inner screens and tabletop hands-free posture in the engine bay.

## Documentation & Roadmap
For full architecture specifications, protocol breakdowns, technical feasibility analyses, and the implementation roadmap, see:
- [PROJECT_PLAN.md](PROJECT_PLAN.md)
