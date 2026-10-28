# Implementation Plan: Optional Device Photo Upload for Repair Booking

Add an optional device photo attachment option (laptops or phones) to the repair booking screen, store the image reference in the data model, and persist it in Firestore.

## User Review Required

> [!IMPORTANT]
> - **Optional Photo Picker**: Users can attach a photo of their device from their gallery when booking a repair. It is completely optional.
> - **Thumbnail Preview**: Shows a preview of the attached photo.
> - **Persistence**: Stores the image URI/URL in the `Repair` record in Firestore.

## Proposed Changes

### 1. Data Model Update (`Models.kt`, `Repo.kt`)
#### [MODIFY] [Models.kt](file:///C:/Users/Lesego_Mogara/Downloads/New%20folder/TheRepairmate-master/app/src/main/java/com/example/therepairmate/data/Models.kt)
- Add `var imageUrl: String = ""` to `Repair` data class.

#### [MODIFY] [Repo.kt](file:///C:/Users/Lesego_Mogara/Downloads/New%20folder/TheRepairmate-master/app/src/main/java/com/example/therepairmate/data/Repo.kt)
- Update `book(...)` method to accept and store `imageUrl`.

### 2. Booking Layout (`activity_book.xml`)
#### [MODIFY] [activity_book.xml](file:///C:/Users/Lesego_Mogara/Downloads/New%20folder/TheRepairmate-master/app/src/main/res/layout/activity_book.xml)
- Add a button `btnAddPhoto` ("Attach photo of device (optional)").
- Add an `ImageView` (`ivDevicePhoto`) for thumbnail preview.

### 3. Booking Activity (`BookRepairActivity.kt`)
#### [MODIFY] [BookRepairActivity.kt](file:///C:/Users/Lesego_Mogara/Downloads/New%20folder/TheRepairmate-master/app/src/main/java/com/example/therepairmate/ui/BookRepairActivity.kt)
- Add image picker launcher (`ActivityResultContracts.GetContent()`).
- Handle photo selection, display thumbnail preview, and pass `photoUri` to `Repo.book`.

## Verification Plan

### Automated Tests
- Run Gradle build (`app:assembleDebug`) to verify compilation.

### Manual Verification
- Deploy app and test booking a repair with and without attaching an optional device photo.
