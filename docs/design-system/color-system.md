# CloudShip — Permanent Design System: Color System & Tokens

**Document Version:** 1.0.0  
**Status:** Permanent Design Law  

---

## 1. Palette Philosophy

CloudShip operates on a **dark-first, high-contrast, low-fatigue** color architecture. The canvas evokes deep space and aerospace telemetry screens: near-black base surfaces paired with crisp structural divisions and strategic accents.

Color is never used as ornament; color is an information signal.

---

## 2. Canvas & Surface Hierarchy

| Token Name | Hex Value | Role & Usage | Contrast vs Text |
|---|---|---|---|
| `--color-canvas` | `#080B10` | Global background canvas; deepest baseline level | 15.2:1 (AAA) |
| `--color-surface-base` | `#0D131F` | Default card and panel background | 13.8:1 (AAA) |
| `--color-surface-elevated`| `#141C2B` | Modals, drawers, command palette, dropdowns | 11.5:1 (AAA) |
| `--color-surface-hover` | `#1B2538` | Hovered rows, interactive item backgrounds | 9.8:1 (AAA) |
| `--color-surface-active`| `#233047` | Selected items, active navigation tabs | 8.2:1 (AAA) |

---

## 3. Structural Hairline Borders

Borders define spatial discipline without creating heavy cognitive containers:

| Token Name | Value | Usage |
|---|---|---|
| `--border-subtle` | `rgba(255, 255, 255, 0.07)` | Standard card dividers, table borders, panel separators |
| `--border-medium` | `rgba(255, 255, 255, 0.14)` | Inputs, active card borders, interactive states |
| `--border-accent` | `rgba(2, 132, 199, 0.50)` | Focused inputs, primary selected states |

---

## 4. Typography Contrast Tiers

| Token Name | Hex Value | Purpose |
|---|---|---|
| `--text-primary` | `#F8FAFC` | Headings, active values, primary titles (pure soft white) |
| `--text-secondary` | `#94A3B8` | Body text, labels, secondary descriptions (cool muted slate) |
| `--text-muted` | `#64748B` | Timestamps, metadata, hints, disabled labels |
| `--text-accent` | `#38BDF8` | Links, active tab text, command hotkeys |

---

## 5. Strategic Accent: Electric Horizon

CloudShip features **ONE** dominant accent color: **Electric Cyan-Blue**.
It is applied with strict surgical precision:
- Primary button actions
- Active navigation and selected tabs
- Focused form controls (`:focus-visible`)
- Real-time deployment pipeline progress
- Command palette selection highlight

| Token Name | Hex Value | Usage |
|---|---|---|
| `--accent-primary` | `#0284C7` | Base interactive accent |
| `--accent-primary-hover`| `#0369A1` | Hovered primary buttons |
| `--accent-highlight` | `#38BDF8` | Glowing markers, focus indicators |
| `--accent-subtle-bg` | `rgba(2, 132, 199, 0.12)` | Active tab pills, badge backgrounds |

---

## 6. Semantic Status System

Status colors communicate health, states, and risk. They are **never** used for decoration or general branding:

| State | Background Tint | Border Tint | Text Color | Meaning |
|---|---|---|---|---|
| **SUCCESS** | `rgba(16, 185, 129, 0.10)` | `rgba(16, 185, 129, 0.30)` | `#34D399` | Service UP, deployment successful, probe healthy |
| **WARNING** | `rgba(245, 158, 11, 0.10)` | `rgba(245, 158, 11, 0.30)` | `#FBBF24` | Degradation, pending trigger, rollback in progress |
| **DANGER / ERROR** | `rgba(239, 68, 68, 0.10)` | `rgba(239, 68, 68, 0.30)` | `#F87171` | Probe failed, deployment halted, crash-loop detected |
| **INFO** | `rgba(2, 132, 199, 0.10)` | `rgba(2, 132, 199, 0.30)` | `#38BDF8` | Informational status, build step running |

> [!NOTE]
> **Accessibility Mandate**: Color must never be the sole indicator of state. Every status element must combine a semantic icon/glyph, shape, or explicit text label alongside the color tint.
