# The Repairmate 📱🔧

**The Repairmate** is a modern, high-performance Android application built for QuickFix Tech Solutions. It provides a seamless, end-to-end device repair management experience for both customers and repair technicians.

---

## 🌟 Key Features

### 👤 1. Authentication & Role-Based Access
* **Secure Sign-In/Sign-Up**: Email/password authentication and Google Credential Manager integration.
* **Role-Based Routing**: Automatically routes authenticated users to either the **Customer Dashboard** or the **Admin Console** based on Firestore user roles.

### 📱 2. Interactive Customer Dashboard
* **Live Status Banner**: Real-time tracking of active repairs with stage progress indicators (1–6).
* **Quick Actions**: One-tap access to book repairs, view repair history, call support, open WhatsApp, or email us.
* **Service Estimator**: Browse repair services (Screen, Battery, Charging, Software, Water Damage, etc.) with preliminary ZAR price estimates.

### 🗓️ 3. Smart Repair Booking Flow
* **Device Brand & Model Autocomplete**: Comprehensive model suggestions covering major manufacturers (Apple iPhone, Samsung Galaxy, Huawei, Xiaomi, Google Pixel, OnePlus).
* **Time Slot Reservation**: Weekday drop-off scheduling (09:00–17:00, Mon–Fri) backed by Firestore transactions to prevent double-booking.

### 📊 4. Real-Time Tracking & Admin Console
* **Live Status Timelines**: Reactive Firestore `Flow` listeners providing instant updates as repairs progress through diagnostics, repair, testing, and pickup.
* **Admin Analytics & Workload**: KPI metrics cards (Active, Unassigned, Ready for Pickup, Completed), estimated active financial pipeline, and technician workload balancing.

### ✨ 5. Liquid Glass UI & Advanced Micro-Animations
* **Liquid Glassmorphism**: Frosted translucent cards, luminous multi-stop gradient borders, and obsidian space-mesh backgrounds.
* **Tactile Animations**: Elastic spring-touch feedback (`pressEffect`), smooth scroll-to-top helpers, status pulses, error shakes, numeric count-ups, and staggered entrance transitions.

---

## 👥 Technician & Agent Roster

Our expert technical team consists of:
* **Dlamini Thabiso**
* **Lesego Mogara**
* **Muvhuso Ndanduleni**
* **Sibusiso Sindane**

---

## 📞 Support & Contact Details

* **Phone & WhatsApp**: `+27661614699`
* **Email**: `support@quickfix.example`
* **Store Hours**: Mon–Fri · 09:00–17:00
* **API Spec**: Integrated with MobileAPI (`https://mobileapi.dev/v1/`)

---

## 🛠️ Tech Stack & Architecture

* **Language**: Kotlin 1.9.24
* **UI Toolkit**: Material 3, ConstraintLayout, XML & Liquid Glass Custom Drawables
* **Concurrency**: Kotlin Coroutines & Flow
* **Backend & Database**: Firebase Authentication & Cloud Firestore (Real-time sync)
* **Architecture**: MVVM / Repository pattern with reactive state management

---

## 🚀 Getting Started

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/therepairmate.git
   ```
2. **Open in Android Studio**:
   - Open Android Studio and select **Open**.
   - Navigate to the `TheRepairmate-master` folder.
3. **Configure Firebase**:
   - Add your `google-services.json` file to the `app/` directory.
4. **Build & Run**:
   - Sync Gradle and click **Run 'app'** on an emulator or physical device.
  
   ## Our app pictures
   <img width="720" height="1600" alt="Screenshot_20261106_170637_com example therepairmate" src="https://github.com/user-attachments/assets/d71d0ff1-b339-494c-8b9c-0dbb3c3cf05b" />
<img width="720" height="1600" alt="Screenshot_20261106_170618_com example therepairmate" src="https://github.com/user-attachments/assets/c1c2d737-f2fc-40f6-a5f7-eab9082bfd6d" />
<img width="720" height="1600" alt="Screenshot_20261106_170609_com example therepairmate" src="https://github.com/user-attachments/assets/cbf4a61a-265d-4306-ac3a-b1c258638847" />
<img width="720" height="1600" alt="Screenshot_20261106_170559_com example therepairmate" src="https://github.com/user-attachments/assets/6dff12ce-d7ab-41a8-a5d3-fc314cf4b96e" />
<img width="720" height="1600" alt="Screenshot_20261106_170903_com example therepairmate" src="https://github.com/user-attachments/assets/bf408070-40fb-41ba-b828-aca06ec4e14e" />
<img width="720" height="1600" alt="Screenshot_20261106_170852_com example therepairmate" src="https://github.com/user-attachments/assets/93bf87ca-2e18-4660-9fcb-1af4e136fc1d" />
<img width="720" height="1600" alt="Screenshot_20261106_170842_com example therepairmate" src="https://github.com/user-attachments/assets/02de1e61-d407-4614-a0f8-bded11d60285" />
<img width="720" height="1600" alt="Screenshot_20261106_170835_com example therepairmate" src="https://github.com/user-attachments/assets/d92f3b6f-d41b-4437-bb64-b7571c42a239" />
<img width="720" height="1600" alt="Screenshot_20261106_170647_com example therepairmate" src="https://github.com/user-attachments/assets/1f77a0dc-257b-477f-993d-9a8ed9c74350" />

