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
