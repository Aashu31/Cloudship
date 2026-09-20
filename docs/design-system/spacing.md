# CloudShip — Permanent Design System: Spacing & Layout Geometry

**Document Version:** 1.0.0  
**Status:** Permanent Design Law  

---

## 1. Modular Spacing Scale (4px Base)

All margins, paddings, gaps, and structural offsets derive strictly from a **4px modular grid**. Arbitrary values (e.g. `13px`, `27px`, `38px`) are prohibited.

| Token | Value (rem / px) | Typical Application |
|---|---|---|
| `--space-1` | `0.25rem` (4px) | Micro gaps between icon and label, inline badges |
| `--space-2` | `0.50rem` (8px) | Compact form field padding, button inner padding |
| `--space-3` | `0.75rem` (12px) | Standard table cell padding, card inner elements gap |
| `--space-4` | `1.00rem` (16px) | Standard input padding, list item separation |
| `--space-6` | `1.50rem` (24px) | Card inner padding, section grid gap |
| `--space-8` | `2.00rem` (32px) | Major panel separation, header vertical padding |
| `--space-12`| `3.00rem` (48px) | Workspace section breaks |
| `--space-16`| `4.00rem` (64px) | Empty state breathing room, hero spacing |

---

## 2. Corner Radii Tokens

CloudShip rejects overly bulbous or completely pill-shaped cards. Radii are disciplined and structural:

| Token | Value | Role |
|---|---|---|
| `--radius-sm` | `4px` | Small buttons, status badges, code pills |
| `--radius-md` | `8px` | Inputs, dropdown menus, standard cards, buttons |
| `--radius-lg` | `12px`| Large workspace panels, modals, drawers |
| `--radius-full`| `9999px`| Status indicator circular dots, round avatars |

---

## 3. Layout Dimensions & Viewport Boundaries

- **Shell Maximum Width**: `1360px` (centered with auto margins).
- **Desktop Sidebar / Rail (Future Scope)**: `240px` fixed or collapsible to `64px`.
- **Drawer Width**:
  - Desktop: `520px` max-width.
  - Mobile: `100vw` (fills width, acts as slide-over bottom sheet).
- **Command Palette Width**: `640px` max-width, elevated with high z-index (`1000`).
- **Minimum Touch Target**: `44px` on all mobile viewports (`@media (max-width: 768px)`).
