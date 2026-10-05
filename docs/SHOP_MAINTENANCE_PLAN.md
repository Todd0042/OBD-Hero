# OBD-Hero Workshop Edition: Shop Maintenance & Service Tools Specification

## Executive Summary
This specification defines the architecture, protocol command routines, and fallback manual workflows for **Shop Maintenance & Service Tools** in OBD-Hero. Tailored specifically for technicians performing high-velocity shop services:
1. **Oil Life & Maintenance Reminder Reset** (Engine Oil, Filters, Spark, Transmission)
2. **TPMS Relearn & Sensor Registration** (Tire Rotation & New Sensor Relearn)
3. **12V Battery Registration & BMS Adaptation** (BMW, Ford, GM, Hyundai/Kia, VAG)
4. **Electronic Parking Brake (EPB) Service Mode** (Caliper Retraction for Rear Brake Service)

---

## 1. Oil Life & Maintenance Reminder Reset

### The Problem
Automakers employ convoluted, inconsistent manual reset procedures (e.g. key-on-engine-off, pump gas pedal 3 times within 5 seconds, hold trip meter button while cycling ignition, or diving through 6 layers of infotainment submenus).

### OBD-Hero Dual-Mode Solution
1. **Direct OBD-II / UDS Bi-Directional Routine Execution**: Sends the manufacturer-specific reset frame directly to the instrument cluster (IPC), body control module (BCM), or engine control module (ECM).
2. **Instant Workshop Fallback Card**: If the vehicle uses an isolated cluster or active Security Gateway (SGW) preventing unauthorized writes without autoauth, OBD-Hero immediately displays the exact step-by-step physical pedal/button procedure for that exact decoded VIN/year/make/model.

### Manufacturer Protocol Routines (Bi-Directional)
| Automaker | Target ECU Header | Protocol / Service Command | Expected ECU Response |
| :--- | :--- | :--- | :--- |
| **Ford / Lincoln** | PCM (`7E0`) or BCM (`726`) | Mode `31 01 02 01` (RoutineControl: Start Oil Life Reset) | `71 01 02 01` (Positive Response) |
| **GM (Global A/B)** | IPC (`760`) or BCM (`720`) | Mode `2F 11 52 03 64` (IOControl: Set Oil Life to 100%) | `6F 11 52 03` |
| **Honda / Acura** | Gauge Cluster (`7A0`) | Mode `22 20 01` / Routine `31 01 A0 01` (Maintenance Minder Reset) | Positive confirmation frame |
| **Toyota / Lexus** | Combination Meter (`7C0`) | Mode `31 01 10 01` or Mode `2F 01` | Reset maintenance counter |
| **BMW** | Kombi / Instrument Cluster (`60`) | UDS Service `31 01 00 01` (CBS Reset: Engine Oil) | `71 01 00 01` |
| **Chrysler / FCA** | IPC (`760`) / ECM (`7E0`) | Routine `31 01 03 01` | Reset Oil Degradation Counter |

---

## 2. TPMS Relearn & Tire Service

### Target Workflows
- **Tire Rotation Relearn**: After cross-rotating tires, update corner positions (FL, FR, RL, RR) without requiring an expensive standalone TPMS tool.
- **Stationary Relearn Trigger**: Command the BCM / TPMS receiver module to enter "Learn Mode" (vehicle honks horn or flashes hazards to indicate wheel #1 is ready).
- **Manual Corner Trigger Walkthrough**: Exact order of tire sensor pinging (Left Front -> Right Front -> Right Rear -> Left Rear).

---

## 3. 12V Battery Registration & BMS Sensor Calibration

### Why It Matters
Modern vehicles equipped with AGM/EFB batteries and Intelligent Battery Sensors (IBS) adjust alternator charging voltage based on the calculated internal resistance and age of the battery. Installing a new battery without resetting the BMS causes overcharging, thermal runaway, and drastically shortened battery lifespan.

### Implementations
- **BMW**: Existing FRM-safe sleep walkthrough + UDS `31 01 01 00` to DME/DDE.
- **Ford**: BCM BMS Reset (`31 01 02 02` on header `726`).
- **GM**: Current Sensor Zeroing on BCM.
- **Hyundai / Kia**: IBS Sensor Calibration routine.
- **Audi / VW**: Gateway `19` Adaptation Channel 004 / 005 (Battery serial & capacity registration).

---

## 4. Electronic Parking Brake (EPB) Service Mode

### Target Workflows
- **Enter Pad Replacement Mode**: Caliper electric servo motors wind back fully into the piston housing, allowing the caliper piston to be pushed straight back without damaging internal gears.
- **Exit Pad Replacement Mode**: Automatically ratchets motors forward until pad contact resistance is measured, re-calibrating clamp clearance.

---

## 5. UI Architecture Integration
A dedicated **Service & Maintenance Tab** (`AppTab.SERVICE`) or quick-access panel on the Vehicle Profile screen, featuring:
- High-contrast, gloved-hand-friendly action cards with immediate status feedback.
- Safety interlocks (Engine Off, Ignition ON, Park Brake Engaged).
- Audit log of completed service resets for shop work orders and diagnostic health reports.
