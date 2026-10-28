# Walkthrough - Optional Device Photo Upload for Repair Booking

Implemented an optional photo attachment feature allowing users to submit pictures of their laptops or phones when booking a repair.

## Changes

### 1. Data Model & Repository (`Models.kt`, `Repo.kt`)
- Added `imageUrl` field to the [Repair](file:///C:/Users/Lesego_Mogara/Downloads/New%20folder/TheRepairmate-master/app/src/main/java/com/example/therepairmate/data/Models.kt) data class.
- Updated `Repo.book` in [Repo.kt](file:///C:/Users/Lesego_Mogara/Downloads/New%20folder/TheRepairmate-master/app/src/main/java/com/example/therepairmate/data/Repo.kt) to accept and persist `imageUrl` in Firestore.

### 2. Booking Layout (`activity_book.xml`)
- Added an optional **"Attach photo of device (optional)"** button (`btnAddPhoto`) and an `ImageView` (`ivDevicePhoto`) thumbnail preview container in [activity_book.xml](file:///C:/Users/Lesego_Mogara/Downloads/New%20folder/TheRepairmate-master/app/src/main/res/layout/activity_book.xml).

### 3. Booking Activity (`BookRepairActivity.kt`)
- Integrated `ActivityResultContracts.GetContent()` in [BookRepairActivity.kt](file:///C:/Users/Lesego_Mogara/Downloads/New%20folder/TheRepairmate-master/app/src/main/java/com/example/therepairmate/ui/BookRepairActivity.kt) to allow picking device images from the gallery.
- Displayed the selected image as a thumbnail preview and passed the image URI to `Repo.book`.

## Verification Results

### Automated Tests
- Gradle build (`app:assembleDebug`) completed successfully without errors.
