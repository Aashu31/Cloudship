# CloudShip — Operations Dashboard Frontend

## Overview
This directory contains the user interface for the CloudShip platform. It provides operators and developers with real-time deployment status, health telemetry badges, manual trigger options, failure simulation drills, and incident post-mortem audit views.

## Architecture & Technology Choice
- **Design Paradigm**: Lightweight, dependency-free, responsive web client.
- **Technologies**:
  - HTML5 (Semantic document layout)
  - CSS3 (Flexbox & Grid responsive layout, custom variables)
  - Vanilla JavaScript (ES6+ Fetch API, async/await, DOM updates)

## Target Structure (Phase 1 / Phase 8 Implementation)
```text
frontend/
├── index.html                  # Main operational dashboard view
├── css/
│   ├── variables.css           # Color tokens, spacing, dark mode palette
│   └── style.css               # Component and layout styling
├── js/
│   ├── api.js                  # REST API client for backend communication
│   ├── ui.js                   # DOM rendering and status badge updates
│   └── app.js                  # Application entry point and polling loops
└── assets/                     # Static icons and logos
```

## Phase Status
- **Current State**: Phase 0 Scaffold.
- **Next Action**: Implement initial static UI shell in Phase 1 / Phase 8.
