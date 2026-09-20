# CloudShip — Permanent Design System: Motion & Animation

**Document Version:** 1.0.0  
**Status:** Permanent Design Law  

---

## 1. Functional Motion Mandate

Motion in CloudShip is strictly **functional**, not decorative:
- It guides spatial continuity (e.g. an item opening in a side drawer).
- It provides feedback for user interactions (e.g. button click state, toast entrance).
- It communicates live state transitions (e.g. deployment moving from `BUILD` to `DEPLOY`).

---

## 2. Duration & Easing Standards

| Interaction Type | Duration | Easing Curve | Purpose |
|---|---|---|---|
| **Micro-interaction** | `120ms – 160ms` | `cubic-bezier(0.2, 0, 0, 1)` | Button press, hover transitions, focus rings |
| **Panel / Drawer Slide** | `180ms – 240ms` | `cubic-bezier(0.16, 1, 0.3, 1)` | Slide-over drawer, modal entrance, toast entry |
| **State Pulse / Telemetry**| `1800ms` (Loop) | `ease-in-out` | Health indicator breathing dot (subtle) |

---

## 3. Strict Motion Laws

1. **Zero Layout Shifts**: Transitions must never alter layout geometry (`width`, `height`, `margin`, `top`, `left`). All transitions operate exclusively on composite properties: `opacity` and `transform`.
2. **No Infinite Background Distractions**: Continuously moving starry backgrounds, looping particle grids, or rotating 3D spheres are prohibited.
3. **Accessibility (Reduced Motion)**:
   ```css
   @media (prefers-reduced-motion: reduce) {
     *, *::before, *::after {
       animation-duration: 0.01ms !important;
       animation-iteration-count: 1 !important;
       transition-duration: 0.01ms !important;
       scroll-behavior: auto !important;
     }
   }
   ```
   Users requesting reduced motion receive instantaneous state changes with zero transitions.
