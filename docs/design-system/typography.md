# CloudShip — Permanent Design System: Typography

**Document Version:** 1.0.0  
**Status:** Permanent Design Law  

---

## 1. Type Families & Philosophy

CloudShip prioritizes extreme typographic clarity, optical balance, and numerical precision.

### 1.1 Primary Interface Font (Sans-Serif)
Used for all UI copy, navigation, buttons, forms, and headers:
```css
font-family: "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
```

### 1.2 Monospace Engine Font
Used for code, commit SHAs, versions, IP addresses, ports, URLs, and JSON keys:
```css
font-family: "JetBrains Mono", "SFMono-Regular", Menlo, Monaco, Consolas, "Liberation Mono", monospace;
```

### 1.3 Tabular Numerics for Metrics
All numerical figures (deployment counts, CPU percentages, durations, timestamps) must use tabular lining figures to ensure vertical alignment across data rows:
```css
font-variant-numeric: tabular-nums;
font-feature-settings: "tnum";
```

---

## 2. Typographic Scale & Hierarchy

| Role | Size (rem / px) | Weight | Line Height | Tracking | Token |
|---|---|---|---|---|---|
| **Display / Page Title** | `1.75rem` (28px) | `700` (Bold) | `1.2` | `-0.03em` | `--font-title-lg` |
| **Section Header** | `1.25rem` (20px) | `600` (SemiBold) | `1.3` | `-0.02em` | `--font-title-md` |
| **Card / Panel Title** | `1.00rem` (16px) | `600` (SemiBold) | `1.4` | `-0.01em` | `--font-title-sm` |
| **Body (Default)** | `0.875rem` (14px) | `400` (Regular) | `1.5` | `normal` | `--font-body` |
| **Body Medium** | `0.875rem` (14px) | `500` (Medium) | `1.5` | `normal` | `--font-body-medium` |
| **Metadata / Caption** | `0.75rem` (12px) | `500` (Medium) | `1.4` | `+0.01em` | `--font-caption` |
| **Metric Large** | `2.25rem` (36px) | `700` (Bold) | `1.1` | `-0.03em` | `--font-metric-lg` |
| **Code / Micro-mono** | `0.8125rem` (13px)| `400` / `500` | `1.4` | `normal` | `--font-mono` |

---

## 3. Formatting Rules

1. **Avoid Excessive Uppercase**: Do not transform entire sentences into uppercase. Uppercase is strictly confined to compact metadata badges (e.g. `UP`, `DOWN`, `v1.0.0`, `PROD`).
2. **Scannable Metric Units**: Always place measurement units in supporting secondary text rather than embedding within the number:
   ```html
   <!-- Correct -->
   <div class="metric-value">12<span class="metric-unit">ms</span></div>
   <!-- Incorrect -->
   <div class="metric-value">12 ms latency rate recorded</div>
   ```
3. **Hierarchy Independent of Color**: A user with monochromatic vision or reduced contrast must be able to distinguish page title, section header, and metadata solely through size, weight, and vertical rhythm.
