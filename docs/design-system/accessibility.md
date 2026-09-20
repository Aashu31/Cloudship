# CloudShip — Permanent Design System: Accessibility & Inclusive Design

**Document Version:** 1.0.0  
**Status:** Permanent Design Law  

---

## 1. Compliance Standard

CloudShip targets full **WCAG 2.1 Level AA** compliance across all user interfaces:
1. **Color Contrast**: All normal text must maintain at least `4.5:1` contrast against its surface; large text (`>= 18px bold` or `>= 24px`) must maintain at least `3.0:1`.
2. **Color-Independence**: No system state, warning, or error is communicated solely through color.
3. **Keyboard Navigability**: Every interactive capability is reachable and operable via keyboard alone.

---

## 2. Keyboard Navigation Contracts

| Key Binding | Target / Action |
|---|---|
| `Tab` / `Shift+Tab` | Advances / reverses focus across interactive primitives |
| `⌘K` or `Ctrl+K` | Opens the central Command Palette from any page |
| `/` | Focuses the primary search input (when not inside a form) |
| `Escape` | Dismisses any active modal, side drawer, dropdown, or command palette |
| `ArrowDown` / `ArrowUp` | Navigates items inside the Command Palette or dropdown list |
| `Enter` | Executes selected command or submits active form |

---

## 3. Focus Indicator Specification

Focused interactive elements receive an unmistakable high-contrast focus ring:
```css
:focus-visible {
  outline: 2px solid var(--accent-highlight);
  outline-offset: 2px;
  box-shadow: 0 0 0 4px rgba(2, 132, 199, 0.25);
}
```
Default browser focus outlines are never suppressed with `outline: none` unless a custom `:focus-visible` ring is provided.

---

## 4. Semantic HTML & Screen Reader Support

- **Landmarks**: `<header class="navbar">`, `<nav aria-label="...">`, `<main class="workspace">`, `<aside aria-label="...">`.
- **ARIA Attributes**:
  - Dialogs: `role="dialog"`, `aria-modal="true"`, `aria-labelledby="..."`
  - Status Indicators: `aria-live="polite"`, `role="status"`
  - Expandable Panels: `aria-expanded="true/false"`, `aria-controls="..."`
- **Text Alternatives**: All icon buttons include an `aria-label` attribute (e.g. `aria-label="Close project drawer"`).
