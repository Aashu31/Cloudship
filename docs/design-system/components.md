# CloudShip — Permanent Design System: Component Primitives Specification

**Document Version:** 1.0.0  
**Status:** Permanent Design Law  

---

## 1. Overview & Primitives Inventory

CloudShip components are built from **19 fundamental primitives**. Every new feature must be composed from these primitives without inventing isolated one-off components.

```text
Primitives:
├── Control:       Button, Input, Select, Dropdown, Tabs
├── Display:       Badge, Status, Metric, Card, Table, Timeline
├── Feedback:      Toast, Modal, Drawer, Tooltip, Skeleton
└── Navigation:    Panel, Navigation, Command Palette
```

---

## 2. Core Primitives Detailed

### 2.1 Button System
Communicates action hierarchy without visual shouting:
- **Primary (`.btn-primary`)**: One dominant action per screen (e.g. `Deploy Workload`, `Create Project`).
  - Styling: `--accent-primary` background, soft white text, subtle hover lift.
- **Secondary (`.btn-secondary`)**: Standard supporting actions (`Refresh`, `Cancel`, `Inspect`).
  - Styling: `--color-surface-elevated` background, subtle border, `--text-primary`.
- **Danger (`.btn-danger`)**: Destructive operations (`Delete Project`, `Abort Deployment`).
  - Styling: Restrained crimson border and tint; never solid screaming red until confirmed.
- **Ghost (`.btn-ghost`)**: Low-priority or icon-only actions (drawer close, copy link).

### 2.2 Input & Form Controls
- Structural hairline borders, dark background (`--color-canvas`), clear focus-visible outline in `--accent-highlight`.
- Validation errors appear directly beneath the specific field in clear muted red text (`0.8rem`).

### 2.3 Status Indicators & Badges
- Features a dual indicator: an illuminated **status dot** and an explicit **text badge**.
- Must never communicate state via color alone.
- Statuses: `CONNECTED` (green), `DISCONNECTED` (red), `DEGRADED` (amber), `PROBING` (cyan).

### 2.4 Metric Cards
- Emphasizes the number using tabular numerals.
- Accompanied by a concise uppercase category label and a contextual subtext explaining normal bounds.

### 2.5 Table Primitive
- Used for structured comparison (e.g. projects, deployments, pods).
- Sticky subtle header row, row hover highlights, monospace identifiers, and clear action buttons.

### 2.6 Side Drawer (Slide-Over Panel)
- Preferred over disruptive full-screen modals for editing, inspecting logs, or viewing project details.
- Slides from the right viewport edge (180ms ease-out). Dismissible via `Esc` key, backdrop click, or close button.

### 2.7 Command Palette (⌘K / Ctrl+K)
- Centered overlay modal with immediate focus on search input.
- Provides keyboard-driven navigation (`ArrowDown`, `ArrowUp`, `Enter`, `Esc`).
- Categorized action list: Navigation, Quick Actions, Diagnostics, Documentation links.

### 2.8 Toast Notification
- Fixed to bottom-right (or mobile top-right).
- Non-blocking, automatic dismissal after 4000ms, with explicit type tint and message.

### 2.9 Deployment Pipeline Timeline
- Represents the continuous delivery progression:
  `SOURCE` → `BUILD` → `TEST` → `CONTAINER` → `REGISTRY` → `DEPLOY` → `HEALTH CHECK` → `LIVE`
- Visually clean node-and-connector rail with active stage pulsing marker.
