# Planora 📋🔒🧮
**Smart Offline-First Task Planner, App Locker & Productivity Suite**

`Planora` is a modern Android task management and productivity application. It helps you schedule daily tasks, get alarm-based notification reminders, protect your privacy with a background app locker, and boost your productivity.

---

## 📱 App Screenshots

| Home  | Profile & Permission | App Locker Engine | Calculator |
|:---:|:---:|:---:|:---:|
| ![Home Screen](https://files.catbox.moe/zqdj78.jpg?text=Planora+Home) | ![Profile Screen](https://files.catbox.moe/iga6ev.jpg?text=Profile+Setiang+Permission) | ![App Locker](https://files.catbox.moe/4ppgm7.jpg?text=App+Locker+Screen) | ![Calculator](https://files.catbox.moe/6xyihq.jpg?text=Scientific+Calculator) |

---

## 🌟 Key Features

### 📋 1. Task Management & Planner
* **Task creation and filtering:** Quickly and easily create, edit, complete, and filter tasks.
* **Categories:** Create custom categories such as Study, Office, Personal, Shopping, Health, and Family, and tag them with colors.
* **Priority levels:** Set High, Medium, or Low priority, shown visually with color-coded stripes.
* **Repeat tasks:** Automatically repeat tasks daily, weekly, monthly, yearly, or at a custom interval of days.
* **Search and filter:** Filter and search tasks by date, category, or priority.

### 🔔 2. Smart Reminder & Snooze System
* **Exact alarms:** Reminders are delivered at the scheduled time using Android's `AlarmManager` and `SCHEDULE_EXACT_ALARM`.
* **Smart repeating alarm mechanism:**
  * **1st trigger (at the scheduled time):** 5 seconds of alarm sound and vibration.
  * **2nd trigger (after 1 minute):** 5 seconds of alarm sound and vibration.
  * **Subsequent triggers (every 5 minutes):** 10 seconds of sound and vibration, until the user takes an action.
* **Interactive notification and reminder screen:** Tapping the notification opens the `TaskReminderActivity` screen, from which you can directly:
  1. **Mark Complete:** Marks the task as done and stops the alarm.
  2. **Snooze:** Choose 5 minutes, 15 minutes, 30 minutes, or 1 hour to be reminded again later.
  3. **Dismiss:** Immediately stops the alarm and the notification.
* **Auto-restart rescheduling:** Alarms are rescheduled through `BootReceiver` even after a phone restart or a time change.

### 🔒 3. Background App Locker Engine
* **Full-screen password gate:** As soon as a locked app is opened, a full-screen security overlay window appears using `TYPE_APPLICATION_OVERLAY`.
* **Persistent background service:** A Foreground Service (`LockService` with `START_STICKY`) keeps monitoring running in the background at all times.
* **Recent Apps clear protection:** Even if Planora is swiped away from Recent Apps, the service automatically keeps running through a recovery mechanism and a 15-minute Watchdog alarm.
* **Encrypted security:** The lock password and data are kept safe using `EncryptedSharedPreferences` backed by Android's KeyStore.
* **Password recovery:** A forgotten password can be reset through a 6-digit OTP verification code sent by email.

### 📅 4. Interactive Calendar View
* The calendar view shows how many tasks fall on each day through dot indicators, so you can review your schedule day by day.

### 📊 5. Visual Statistics & Analytics
* View graphical bar charts (`BarChartView`) of completed and pending tasks, along with productivity stats.

### 🧮 6. Built-in Scientific Calculator
* Standard and scientific calculator (Sin, Cos, Tan, Log, Ln, Square Root, Exponents, Parentheses, Degree/Radian, History).

### 🌙 7. Dark / Light Theme
* Supports System Default, Light, and Dark modes.

---

## ☁️ Firebase Connectivity & Cloud Sync

Planora is an **Offline-First** application. It works 100% offline and syncs data with Firebase in the background as soon as an internet connection is available.

1. **Firebase Authentication:**
   * Sign-up and auto-login with Email/Password.
   * Google One-Tap / Google Sign-In support.
   * Password recovery through an email reset link.
2. **Firebase Firestore Database:**
   * Tasks, categories, and user profiles are synced to the cloud.
   * Conflict-free background sync between offline and online data is performed by a `WorkManager` background worker.
3. **Firebase Storage:**
   * User profile picture upload and storage management.
4. **Firebase Cloud Functions:**
   * Email OTP generation for App Locker password verification.

---

## 📁 Directory & Folder Structure

```
Planora/
├── app/
│   ├── build.gradle                   # App-level build and dependency configuration
│   ├── google-services.json           # Firebase service configuration file
│   └── src/main/
│       ├── AndroidManifest.xml        # App permissions, services, receivers, and activity declarations
│       ├── java/com/planora/app/
│       │   ├── App.java               # Application class, notification channel creation
│       │   ├── data/                  # Data layer (Room DB, DAO, Firestore, SyncWorker)
│       │   │   ├── AppDb.java         # Room database
│       │   │   ├── TaskEntity.java    # Task table structure
│       │   │   ├── TaskDao.java       # Task data access object
│       │   │   ├── Repo.java          # Central repository layer
│       │   │   ├── Cloud.java         # Firebase cloud connector
│       │   │   └── SyncWorker.java    # Background sync worker
│       │   ├── locker/                # App locker engine
│       │   │   ├── LockService.java   # Foreground app locker background service
│       │   │   ├── LockPrefs.java     # Keystore-encrypted preferences
│       │   │   ├── LockerFragment.java# App locker user interface and guide
│       │   │   └── LockScreenActivity.java
│       │   ├── reminder/              # Smart alarm and notification engine
│       │   │   ├── Alarms.java        # AlarmManager scheduler
│       │   │   ├── AlarmReceiver.java # Alarm trigger receiver
│       │   │   ├── ReminderSoundPlayer.java # Sound and vibration duration player
│       │   │   └── BootReceiver.java  # Reboot auto-scheduler receiver
│       │   ├── ui/                    # User interface layer (Activities & Fragments)
│       │   │   ├── MainActivity.java  # Main controller activity
│       │   │   ├── HomeFragment.java  # Dashboard and daily tasks
│       │   │   ├── PlannerFragment.java # Task planner
│       │   │   ├── TaskReminderActivity.java # Interactive reminder and snooze screen
│       │   │   ├── AddTaskActivity.java # Task creation and editing
│       │   │   ├── CalendarFragment.java # Calendar interface
│       │   │   ├── StatsFragment.java # Statistics and bar chart
│       │   │   └── ProfileFragment.java # User profile and settings
│       │   ├── calc/                  # Scientific calculator
│       │   └── util/                  # Utility classes (Prefs, Util)
│       └── res/                       # Layouts, drawables, themes, and resource files
│           ├── layout/                # XML UI layouts
│           ├── drawable/              # Icons and background shapes
│           └── values/                # Color, string, and theme configuration
└── build.gradle                       # Project-level build file
```

---

## 💡 Benefits & Advantages of Using Planora

* 🎯 **Increased productivity:** Scheduling daily and upcoming tasks on time multiplies your efficiency.
* ⏰ **Timely alarms and snooze:** Progressive 5-second/10-second alarm sounds and snooze options help prevent forgotten tasks.
* 🔐 **Privacy and focus protection:** Locking social media or distracting apps with the App Locker helps you stay focused while studying or working.
* 📡 **Seamless offline and online freedom:** The app works fully without internet and syncs in the background the moment a connection is available.
* 📱 **All-in-one multitasking:** Task manager, app locker, calendar, statistics, and calculator are all available in a single lightweight app.

---

## 🛠 Tech Stack

* **Language:** Java
* **UI Architecture:** Android Material Design Components, ViewBinding, XML Layouts
* **Database (Local):** Android Room Database (Offline Source of Truth)
* **Cloud Database:** Firebase Firestore
* **Authentication:** Firebase Auth (Email/Password & Google Sign-In)
* **Background Tasks:** Android WorkManager, AlarmManager, Foreground Service
* **Security:** Android KeyStore, EncryptedSharedPreferences
* **Image Loading:** Glide Library

---

## 📄 License & Privacy
The Planora application ensures users' privacy and security. No sensitive information or passwords are stored without local KeyStore encryption.
