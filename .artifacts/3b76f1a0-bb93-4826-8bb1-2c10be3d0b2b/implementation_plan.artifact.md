# Implementation Plan - Add Phone Brand & Device Model Options

Add comprehensive device model and brand options (Apple, Samsung, Huawei, Xiaomi, Google, OnePlus, etc.) to the booking form (`BookRepairActivity` & `activity_book.xml`) so users can easily select or search their device model.

## User Review Required

> [!IMPORTANT]
> We propose upgrading the device model input field in `activity_book.xml` from a plain `TextInputEditText` to a Material `AutoCompleteTextView` populated with a comprehensive list of popular models across all major phone brands (Apple, Samsung, Huawei, Xiaomi, Google, OnePlus, etc.), while still allowing custom entries.

## Open Questions

- None.

## Proposed Changes

### UI & Layout
#### [MODIFY] [activity_book.xml](file:///C:/Users/Lesego%20Mogara/Downloads/New%20folder/TheRepairmate-master/app/src/main/res/layout/activity_book.xml)
- Replace `TextInputEditText` with `AutoCompleteTextView` for `etModel` to support suggestions/autocomplete.

### Logic & Data
#### [MODIFY] [Logic.kt](file:///C:/Users/Lesego%20Mogara/Downloads/New%20folder/TheRepairmate-master/app/src/main/java/com/example/therepairmate/data/Logic.kt)
- Add a list of popular phone brands and models (`EstimateService.PHONE_MODELS`) covering Apple, Samsung, Huawei, Xiaomi, Google, and OnePlus.

#### [MODIFY] [BookRepairActivity.kt](file:///C:/Users/Lesego%20Mogara/Downloads/New%20folder/TheRepairmate-master/app/src/main/java/com/example/therepairmate/ui/BookRepairActivity.kt)
- Attach an `ArrayAdapter` with `PHONE_MODELS` to `etModel` (`AutoCompleteTextView`).

## Verification Plan

### Automated Tests
- Gradle build `app:assembleDebug` to verify compilation.

### Manual Verification
- Open "Book a repair", tap on the device model field, and verify suggestions appear for Apple, Samsung, Huawei, Xiaomi, Google, and OnePlus.
