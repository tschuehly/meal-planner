# Product Spec v4: Household Web Interface

## Goal

Add a shared household browser interface over the working product workflows.

The interface exposes the current planning state, profile, recipe backlog, plan review, and shopping list without adding separate Thomas and Cassandra roles.

Access is private through a household link or access code. After access is accepted, the browser remembers the household session.

## Added Product Unit

`Household web app`

The web app is a shared interface for the household. Thomas and Cassandra remain people in the household profile, not separate permission roles.

## Interface

Primary routes:

| Route | Purpose |
| --- | --- |
| `/` | Dashboard with current plan state and next action. |
| `/profile` | Household profile and person-specific preferences. |
| `/plan/new` | Weekly planning input. |
| `/plans/{planId}` | Plan review, revision, and acceptance. |
| `/recipes/backlog` | Saved recipe ideas. |
| `/shopping/current` | Current shopping list. |

## Product Behavior

The browser interface should support the same household workflows already proven in the command-line interface:

* save and edit household profile facts
* guide first-time profile setup when no profile exists
* generate a weekly lunch plan
* save and use backlog recipes
* revise one meal
* accept a plan
* generate and view a shopping list

The dashboard should show the next best action for the current household state.

## Minimal Persistence

Reuse the local single-household store from earlier versions.

The browser interface adds only private-link or access-code session state. It does not add named accounts, multi-device synchronization, or multi-household storage. If the same household opens two browser tabs, the product may refresh state on page load or manual reload.

## Supported Workflow

1. Open the dashboard.
2. Enter the private household link or access code if needed.
3. Confirm, create, or edit household basics.
4. Generate a weekly lunch plan.
5. Review and revise the plan.
6. Accept the plan.
7. Generate and open the shopping list.

## Acceptance Checks

* The dashboard shows the next action for the current household state.
* A private household link or access code can establish browser access.
* The profile screen saves household and person-specific constraints.
* Missing profile state leads to guided setup.
* The planning screen can generate a plan.
* The plan review screen can revise and accept a plan.
* The shopping screen shows a generated grouped list.
* The interface works on desktop and mobile.

## Exclusions

v4 does not include separate user accounts, grocery ordering, calendar integration, automatic cross-tab synchronization, or weekly feedback memory.
