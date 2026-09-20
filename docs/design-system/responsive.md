# CloudShip — Permanent Design System: Responsive Layout & Mobile Architecture

**Document Version:** 1.0.0  
**Status:** Permanent Design Law  

---

## 1. Responsive Philosophy: Mobile-First Engineering

CloudShip is engineered **mobile-first**, then scaled to tablet, laptop, and multi-monitor workstations:
- On mobile, an engineer in an on-call situation must be able to inspect service health, check deployment progress, review incident alerts, and execute rollbacks cleanly.
- Complex desktop layouts do not simply scale down; they transform intentionally into stacked cards, bottom sheets, and collapsible sections.

---

## 2. Breakpoint Grid

| Breakpoint | Viewport Range | Device Profile | Layout Behavior |
|---|---|---|---|
| **xs** | `320px – 430px` | Modern smartphones | Single-column stack, drawer becomes full-width sheet, touch targets >= 44px |
| **sm** | `431px – 768px` | Large phones & small tablets | 2-column metric cards, responsive tables with horizontal priority columns |
| **md** | `769px – 1024px`| Tablets & compact laptops | Split panels, fixed header with visible command badges |
| **lg** | `1025px – 1360px`| Standard desktop | Full workspace grid, asymmetric columns (60/40 or 70/30), open drawers |
| **xl** | `> 1360px` | Ultra-wide monitors | Centered 1360px shell with generous negative space |

---

## 3. Responsive Component Transformations

### 3.1 Tables on Mobile
- Tables prioritize high-signal columns: Name, Status, and Action button.
- Secondary columns (Creation date, full URL) collapse or move into the Project Details Drawer.
- Zero accidental horizontal scroll on the main viewport.

### 3.2 Drawers and Overlays
- On viewports `< 768px`, slide-over side drawers automatically take `100vw` or convert into an upward-sliding bottom sheet with a clear touch-friendly close target.

### 3.3 Touch Targets
- Every interactive element (buttons, tab pills, table action icons) maintains a minimum clickable/tappable bounding box of `44px x 44px` on touch screens.
