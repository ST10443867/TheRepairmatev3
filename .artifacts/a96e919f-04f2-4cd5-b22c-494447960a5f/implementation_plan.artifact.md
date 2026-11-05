# Implementation Plan: 50 Phone Fallback, Interactive Admin Dashboard/Charts, Filter/Sort Repairs & Admin Branding

## User Review Required

> [!IMPORTANT]
> - **50 Offline Phone Specs**: Expanded mock phone database to 50 popular models across major brands.
> - **Interactive Admin Dashboard & Charts**: Clicking KPI cards (Active, Unassigned, Ready for pickup, Completed), status bars, or technician workload bars will filter and display the corresponding repairs.
> - **Admin Filter & Sort**: Added status filtering and sorting (Newest/Oldest/Ticket ID) options in the admin repair management page.
> - **Admin Branding**: Added QuickFix logo in top-left header and a semi-transparent watermark in the background of the admin dashboard.

## Proposed Changes

### 1. Phone Specs Offline Fallback (`PhoneSpecsActivity.kt`)
- Expand `getMockPhones()` to contain 50 popular phone models with thumbnails and GSM-style specs.

### 2. Admin Dashboard Layout & Branding (`activity_admin.xml`, `AdminDashboardActivity.kt`)
- Add QuickFix logo (`@drawable/logo`) in top-left header of `activity_admin.xml`.
- Add transparent background watermark text "QUICKFIX TECH SOLUTIONS".
- Make KPI cards (`cardActive`, `cardUnassigned`, `cardReady`, `cardCompleted`), status status bars, and technician workload bars clickable with press effects and click listeners passing filter intents to `RepairsActivity`.

### 3. Repair Management Filter & Sort (`activity_repairs.xml`, `RepairsActivity.kt`)
- Add filter spinner/chips and sort spinner in `activity_repairs.xml` for admin view.
- Implement filtering by status/technician and sorting by date/ticket ID in `RepairsActivity.kt`.

## Verification Plan

### Automated Tests
- Run Gradle build (`app:assembleDebug`) to verify compilation.

### Manual Verification
- Deploy app and test 50 offline phones, interactive admin KPI/charts, repair filter/sort, and admin branding.
