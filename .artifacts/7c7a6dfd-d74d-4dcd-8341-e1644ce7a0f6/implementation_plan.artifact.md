# Implementation Plan - Force Live API Data Fetching for Phone Specs

Remove offline mock data fallbacks in `PhoneSpecsActivity.kt` and enforce fetching live data directly from the remote API source (`PhoneSpecsService`), displaying loading states and error messages if requests fail.

## User Review Required

> [!IMPORTANT]
> - **Removal of Mock Fallback**: `PhoneSpecsActivity` will no longer fall back to static mock phones when the API fails or returns empty. Instead, it will fetch directly from the live API and show user-friendly error messages or empty states if network requests fail.
> - **Live API Integration**: Ensures all data shown in the Phone Specs list and detail dialogs comes directly from `https://api-mobilespecs.azharimm.dev/`.

## Open Questions

- None.

## Proposed Changes

### Phone Specs Activity
#### [MODIFY] [PhoneSpecsActivity.kt](file:///C:/Users/Lesego%20Mogara/Downloads/New%20folder/TheRepairmate-master/app/src/main/java/com/example/therepairmate/ui/PhoneSpecsActivity.kt)
- Remove `getMockPhones()` and `showMockDetail()`.
- Update `loadLatest()` and `search()` to strictly query `PhoneSpecsService.instance.getLatestPhones()` and `searchPhones(query)`, showing `b.tvEmpty` with error messages if the live API call fails.
- Update `showPhoneDetail()` to strictly query `PhoneSpecsService.instance.getPhoneDetail(phone.slug)` without falling back to mock details.

## Verification Plan

### Automated Tests
- Run `:app:testDebugUnitTest`.

### Manual Verification
- Deploy app, open Phone Specs, and verify live data fetching from the API source.
