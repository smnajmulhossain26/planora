# Planora 📋🔒🧮
**Offline-First Smart To-Do + Scientific Calculator + App Locker** (Java + XML + Firebase + Room)

> ⚠️ **সৎ কথা আগে:** এই প্রজেক্টের কোড আমি লিখেছি এবং XML ফাইলগুলোর সিনট্যাক্স যাচাই করেছি, কিন্তু এখানে Android Studio/ইন্টারনেট ছিল না, তাই **কম্পাইল, APK বিল্ড ও ফোনে টেস্ট করা হয়নি।** Android Studio-তে প্রথমবার Build করলে ছোটখাটো এরর আসতে পারে — Build আউটপুট কপি করে আমাকে দিলে ঠিক করে দেব। APK আপনাকে নিজের Android Studio থেকে বানাতে হবে (ধাপ ৭)।


## 🆕 v1.2 – এই ভার্সনে কী ঠিক/নতুন হয়েছে
- **🔒 App Locker এখন সত্যিই লক করে।** আগের পদ্ধতিতে ব্যাকগ্রাউন্ড সার্ভিস থেকে একটি Activity খুলে লক স্ক্রিন দেখানো হতো, কিন্তু Android 10+ ব্যাকগ্রাউন্ড থেকে Activity খুলতে দেয় না — তাই লক স্ক্রিন আসছিল না। এখন সার্ভিস নিজেই **লক করা অ্যাপের উপর ফুল-স্ক্রিন পাসওয়ার্ড ওভারলে** বসায় (Display over other apps অনুমতি দিয়ে — এতে Android-এর কোনো বাধা নেই)।
  - লক করা অ্যাপ যতবার খুলবেন, ততবার পাসওয়ার্ড চাইবে।
  - সঠিক পাসওয়ার্ড দিলে অ্যাপ খুলবে এবং আপনি অ্যাপ থেকে বের না হওয়া (Home / অন্য অ্যাপ / স্ক্রিন অফ) পর্যন্ত খোলা থাকবে। বের হয়ে আবার ঢুকলে আবার লক।
  - ভুল পাসওয়ার্ডে "Wrong password" দেখায়। Back চাপলে Home-এ চলে যায়।
  - স্ক্রিন অফ হলে সব অ্যাপ আবার লক হয়ে যায়।
  - ডিটেকশনে সময়-ভিত্তিক ইভেন্ট ফিল্টার যোগ হয়েছে যাতে আনলকের সাথে সাথে আবার লক না আসে।
  - অ্যাপ বন্ধ/কিল হলেও Planora খুললে সার্ভিস নিজে থেকে চালু হয়ে যায়।
- **🧮 নতুন Calculator:** আসল ফোন-ক্যালকুলেটরের মতো ডার্ক ডিজাইন, বড় গোল কী, কমলা অপারেটর, সবুজ `=`, **লাইভ রেজাল্ট প্রিভিউ**, `±`, `Ans`, History, ভাইব্রেশন-ফিডব্যাক, ৪ সারি Scientific কী (sin cos tan asin acos atan ln log √ x² ^ ! π e ( ) DEG/RAD)।
- **👤 রেজিস্ট্রেশন:** Create Account চাপলেই অ্যাকাউন্ট তৈরি হয়ে **সাথে সাথে লগইন** হয়ে যায় (আর লগইন পেজে ফিরতে হয় না)। **Email Verification আর লাগে না** — Email ও Password দিয়ে সরাসরি লগইন করা যায়। Google লগইন আগের মতোই।
- **🎨 নতুন রং ও ডিজাইন:** ইন্ডিগো→ভায়োলেট গ্র্যাডিয়েন্ট, নরম ল্যাভেন্ডার ব্যাকগ্রাউন্ড (লাইট) ও ডিপ নেভি (ডার্ক), বর্ডারওয়ালা গোল কার্ড, নতুন বটম নেভিগেশন, নতুন লক স্ক্রিন।

## 🆕 v1.1-এ যা বদলেছিল
- **নিচের নেভিগেশন এখন ৫টি:** Home · Planner · Calculator · App Lock · Profile। (Planner-এর ভেতরে উপরে Tasks / Calendar / Statistics ট্যাব আছে। Calculator ও App Locker আর Profile-এর ভেতরে নেই।)
- **লগইনের পর প্রথমবার Permission স্ক্রিন:** আগে Notification (Android-এর অফিশিয়াল ডায়ালগ), তারপর Exact Alarm। বাকিগুলোর অবস্থাও দেখায়। Continue চাপলে অ্যাপে ঢোকে।
  - **ফাইল পারমিশন নিয়ে কথা:** Planora ছবি/ফাইল বাছার সময় Android-এর নিজস্ব Photo Picker / File Picker ব্যবহার করে, তাই ফাইল পারমিশন *লাগেই না* — স্ক্রিনে সেটা “No permission needed ✓” দেখায়। এতে আপনার বাকি ফাইল নিরাপদ থাকে।
- **App Lock চালু করার সময় ধাপে ধাপে গাইড:** Enable সুইচ চাপলে অনুমতি না থাকলে একটি গাইড শিট খোলে — Usage Access ও Display-over-other-apps কীভাবে দিতে হবে বাংলায় ১-২-৩-৪ ধাপে দেখায়, প্রতিটির পাশে ✓/✗ স্ট্যাটাস। দুটো দেওয়া হয়ে গেলে লক নিজে থেকে অন হয়।
  - Android 13+ এ APK থেকে ইনস্টল করলে অনুমতির সুইচ ধূসর হতে পারে — গাইডে “Allow restricted settings” (⋮ মেনু) ধাপটিও আছে।
- **ডিজাইন:** গ্র্যাডিয়েন্ট হেডার (Home, Login, Register, App Lock), ভাসমান প্রগ্রেস কার্ড, রঙিন স্ট্যাট কার্ড, গোল কোণাওয়ালা কার্ড, রঙ-কোডেড Calculator কী, নতুন Permission কার্ড, ডার্ক মোডের জন্য আলাদা রং।
- **আপনার `google-services.json` ইতিমধ্যে `app/` ফোল্ডারে বসানো আছে** (প্রজেক্ট: `planora23`, প্যাকেজ: `com.planora.app`)।

> ⚠️ **Google Login নিয়ে সতর্কতা:** আপনার `google-services.json`-এ শুধু **একটি SHA-1** (`37:FA:C7:7B:...:1F:07`) আছে। অন্য কম্পিউটার/Keystore থেকে Build করলে (বা Release APK বানালে) তার SHA-1 Firebase → Project settings → Your apps → **Add fingerprint**-এ যোগ করুন, নইলে Google Login `code 10` দেবে। Firebase Console-এ **Authentication → Google** চালু আছে কিনা নিশ্চিত করুন।
> 🔒 এই ZIP-এ আপনার Firebase কনফিগ আছে — ZIP পাবলিকলি শেয়ার করবেন না।

---

## ১. প্রজেক্টে যা যা আছে
| ফিচার | অবস্থা |
|---|---|
| Welcome + Onboarding (৪ পেজ) | ✅ |
| Email/Password রেজিস্ট্রেশন | ✅ (তৈরি হওয়ার সাথে সাথে অটো-লগইন; ভেরিফিকেশন লাগে না) |
| Google Sign-In (Firebase Auth) | ✅ |
| Forgot Password (Firebase Reset Link) | ✅ |
| Offline-first Task (Room) + Firestore Sync (WorkManager) | ✅ (Duplicate হয় না: Task ID = Firestore Doc ID) |
| Conflict policy | ✅ Last-write-wins (`updatedAt`), Delete = soft delete (tombstone) |
| Category (Default ৭টি + নিজের তৈরি/Edit/Delete) | ✅ |
| Priority (High/Medium/Low, আলাদা রং + আইকন) | ✅ |
| Reminder (একাধিক, Custom সহ) + AlarmManager + Notification | ✅ |
| Reboot-এর পর Alarm পুনরায় Schedule | ✅ |
| Repeat Task (Day/Week/Month/Year/Custom N days) | ✅ (Complete করলে পরের Task তৈরি হয়) |
| Dashboard, Calendar (Day/Week/Month + ডট ইন্ডিকেটর), Search, Filter, History, Statistics + Bar Chart | ✅ |
| Permission Center | ✅ |
| Dark / Light / System Theme | ✅ |
| Profile (ছবি Photo Picker দিয়ে, নাম Edit, Logout, Delete Account + Re-auth) | ✅ |
| Sync Status (Online/Offline/Syncing/Pending/Failed + Retry) | ✅ |
| **Scientific Calculator** (sin cos tan asin acos atan ln log √ x² ^ ! π e % DEG/RAD, History) | ✅ |
| **App Locker** (পাসওয়ার্ড, অ্যাপ লিস্ট থেকে সিলেক্ট, লক স্ক্রিন, Forgot Password → Gmail-এ ৬ সংখ্যার কোড) | ✅ |

### যা সরল করা হয়েছে / এখনো নেই (সৎ তালিকা)
- `UserProfileEntity`, `AttachmentEntity`, `AppSettingsEntity` Room টেবিল আলাদা করে বানাইনি। প্রোফাইল Firebase Auth-এ, সেটিংস SharedPreferences-এ।
- Attachment শুধু ফোনে (URI) সংরক্ষিত হয়, Cloud-এ আপলোড হয় না।
- Language Settings: শুধু English (বাটন আছে, বহুভাষা পরে)।
- DataStore-এর বদলে SharedPreferences ব্যবহার হয়েছে।
- Room ডাটাবেসের পুরো এনক্রিপশন নেই। App Locker-এর পাসওয়ার্ড/সেটিংস Android Keystore-ভিত্তিক `EncryptedSharedPreferences`-এ থাকে (পাসওয়ার্ড শুধু salted hash হিসেবে)।
- Firestore-এর নিজস্ব offline cache বন্ধ রাখা হয়েছে — Room-ই একমাত্র Source of Truth।

---

## ২. Android Studio-তে প্রজেক্ট খোলা
1. **Android Studio (Hedgehog/Iguana বা নতুন)** ইনস্টল করুন। JDK 17 (Studio-র সাথেই আসে)।
2. `Planora.zip` Extract করুন।
3. Android Studio → **File → Open** → `Planora` ফোল্ডার সিলেক্ট করুন (ভেতরের `app` নয়, বাইরের `Planora`)।
4. Gradle Sync হতে দিন (ইন্টারনেট লাগবে)। **এই সময় `google-services.json` না থাকায় এরর আসবে — এটা স্বাভাবিক, ধাপ ৩-এ ঠিক হবে।**
5. যদি "Gradle wrapper" নিয়ে সমস্যা হয়: Studio-র নিচের Terminal-এ `gradle wrapper --gradle-version 8.2` চালান, অথবা Settings → Build Tools → Gradle থেকে "Gradle JDK" 17 দিন।

---

## ৩. Firebase সেটআপ (সবচেয়ে গুরুত্বপূর্ণ)
### ৩.১ প্রজেক্ট তৈরি
1. https://console.firebase.google.com → **Add project** → নাম `Planora`।
2. **Project settings (⚙️) → Your apps → Android আইকন**।
3. **Package name:** `com.planora.app` (অন্য নাম চাইলে ধাপ ৮ দেখুন)।
4. **SHA-1 দিন** (Google Login-এর জন্য বাধ্যতামূলক)। SHA-1 পাওয়ার উপায়:
   - Android Studio-র ডানদিকে **Gradle → Planora → app → Tasks → android → signingReport** ডাবল ক্লিক → `SHA1:` কপি করুন, **অথবা**
   - Terminal: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`
   - ⚠️ পরে Release (সাইন করা) APK বানালে তার keystore-এর SHA-1-ও Firebase-এ যোগ করতে হবে।

### ৩.২ Authentication চালু করুন
**Build → Authentication → Get started → Sign-in method**
- **Email/Password** → Enable
- **Google** → Enable → Support email দিন → Save

### ৩.৩ `google-services.json` বসান *(এই ভার্সনে আপনার ফাইলটি আগে থেকেই `app/`-এ আছে — শুধু নতুন Firebase প্রজেক্ট নিলে নিচের ধাপ লাগবে)*
1. Firebase-এ **google-services.json** ডাউনলোড করুন। (⚠️ Google চালু ও SHA-1 দেওয়ার **পরে** ডাউনলোড করুন, নইলে ভেতরে Web Client ID থাকবে না এবং `default_web_client_id` এরর দেবে।)
2. ফাইলটি রাখুন: **`Planora/app/google-services.json`** (`app` ফোল্ডারের ভেতরে)।
3. Android Studio-তে **File → Sync Project with Gradle Files**।

### ৩.৪ Firestore Database
**Build → Firestore Database → Create database** → Production mode → লোকেশন বেছে নিন।
তারপর **Rules** ট্যাবে `firestore.rules` ফাইলের পুরো লেখা পেস্ট করে **Publish** করুন। (এতে একজন ইউজার শুধু নিজের ডাটা দেখতে/বদলাতে পারে।)

### ৩.৫ Storage (প্রোফাইল ছবির জন্য)
**Build → Storage → Get started** → **Rules** ট্যাবে `storage.rules` পেস্ট করে Publish।
> Firebase এখন Storage-এর জন্য Blaze প্ল্যান চাইতে পারে। না চাইলে প্রোফাইল ছবি আপলোড কাজ করবে না, বাকি সব করবে।

### ৩.৬ Cloud Functions (শুধু App Locker-এর "Forgot Password" কোডের জন্য)
Gmail-এ ৬ সংখ্যার কোড পাঠাতে হলে সার্ভার-সাইড কোড দরকার। এজন্য **Blaze (pay-as-you-go) প্ল্যান** লাগবে (ছোট ব্যবহারে প্রায় বিনামূল্যে, তবে কার্ড দিতে হয়)। এটা না চাইলে বাকি অ্যাপ ঠিকই চলবে, শুধু Locker-এর Forgot Password কোড আসবে না।

**ধাপ ক — Gmail App Password বানান** (যে Gmail থেকে কোড *পাঠানো* হবে):
1. ওই Gmail অ্যাকাউন্টে **2-Step Verification** চালু করুন।
2. https://myaccount.google.com/apppasswords → নাম `Planora` → ১৬ অক্ষরের পাসওয়ার্ড কপি করুন।

**ধাপ খ — Firebase CLI:**
```bash
# Node.js 20 ইনস্টল থাকতে হবে (https://nodejs.org)
npm install -g firebase-tools
firebase login
cd Planora
firebase use --add            # আপনার Planora প্রজেক্ট সিলেক্ট করুন
cd functions && npm install && cd ..
firebase functions:secrets:set GMAIL_USER           # যেমন: yourname@gmail.com
firebase functions:secrets:set GMAIL_APP_PASSWORD   # ধাপ ক-এর ১৬ অক্ষরের পাসওয়ার্ড
firebase deploy --only functions,firestore:rules,storage
```
> কোড ইউজারের **Planora-তে লগইন করা ইমেইলে** (Gmail/অন্য) যাবে। কোড ১০ মিনিট বৈধ, ৫ বার ভুল করলে বাতিল, ১ মিনিটে ১টির বেশি পাঠানো যায় না। কোড তৈরি/যাচাই সম্পূর্ণ সার্ভারে — অ্যাপে নয়।

---

## ৪. ফোনে চালানো
1. ফোনে **Developer options → USB debugging** চালু করে PC-তে লাগান (অথবা Emulator ব্যবহার করুন — **Google Play** আইকনওয়ালা Emulator নিন, নইলে Google Login চলবে না)।
2. Studio-র উপরে ডিভাইস সিলেক্ট করে ▶ **Run** চাপুন।

## ৫. App Locker কীভাবে ব্যবহার করবেন
1. নিচের **App Lock** ট্যাবে চাপুন → প্রথমবার **পাসওয়ার্ড তৈরি** করুন (কমপক্ষে ৪ অক্ষর)।
2. **Enable App Lock** সুইচ চাপুন। অনুমতি না থাকলে **গাইড শিট** খুলবে:
   - **ধাপ ১ – Usage Access:** “Open settings” → তালিকা থেকে **Planora** → “Permit usage access” অন → ব্যাক।
   - **ধাপ ২ – Display over other apps:** “Open settings” → **Planora** → “Allow display over other apps” অন → ব্যাক।
   - **ধাপ ৩ – (সুপারিশ) Battery/Autostart:** Planora-কে “Unrestricted” দিন; Redmi/Xiaomi/Oppo/Vivo/Samsung-এ Autostart চালু করুন।
   - সুইচ ধূসর হলে: App info → ⋮ → **Allow restricted settings**।
3. দুটো অনুমতি পেলে লক নিজে থেকেই **ON** হয়।
4. তালিকা থেকে যে অ্যাপগুলো লক করতে চান সেগুলোর সুইচ অন করুন।
5. লক করা অ্যাপ খুললেই পাসওয়ার্ড স্ক্রিন আসবে। পাসওয়ার্ড ভুললে **Forgot password?** → **Send code** → ইমেইলের ৬ সংখ্যার কোড + নতুন পাসওয়ার্ড।
6. অ্যাপ থেকে বেরিয়ে আবার ঢুকলে App Lock ট্যাবে আবার পাসওয়ার্ড চাইবে (নিরাপত্তার জন্য)।

**সীমাবদ্ধতা (Android-এর নিয়ম):**
- Xiaomi/Redmi, Oppo, Vivo, Samsung-এ ব্যাকগ্রাউন্ড সার্ভিস মেরে ফেলতে পারে। Settings → Apps → Planora → **Autostart চালু** ও **Battery → No restrictions** দিন।
- ব্যবহারকারী Planora ফোর্স-স্টপ বা আনইনস্টল করলে লক কাজ করবে না।
- Google Play-তে প্রকাশ করলে Usage Access/Overlay ব্যবহারের জন্য Play Policy ঘোষণা দিতে হবে।

## ৬. Reminder ঠিকমতো কাজ করানো
Profile → **Permission Center** থেকে Notification, Exact Alarm, Battery অবস্থা দেখুন। কোনো Permission না দিলেও অ্যাপ ক্র্যাশ করে না; শুধু সংশ্লিষ্ট ফিচারের কারণ দেখায়।

## ৭. APK বানানো
- **Debug APK (টেস্টের জন্য):** Build → Build Bundle(s)/APK(s) → **Build APK(s)** → ফাইল: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK:** Build → **Generate Signed Bundle / APK** → APK → নতুন Keystore তৈরি করুন (**Keystore ও পাসওয়ার্ড হারাবেন না**) → release। তারপর ওই keystore-এর SHA-1 Firebase-এ যোগ করে নতুন `google-services.json` বসান।

---

## ৮. কোথায় কী পরিবর্তন করবেন
| কী বদলাতে চান | কোথায় |
|---|---|
| অ্যাপের নাম | `app/src/main/res/values/strings.xml` → `app_name` |
| প্রধান রং | `app/src/main/res/values/colors.xml` (ও `values-night/colors.xml`) |
| Package name (`com.planora.app`) | ১) `app/build.gradle` → `namespace` ও `applicationId` ২) Studio-তে `java/com/planora/app` ফোল্ডারে Right-click → Refactor → Rename/Move ৩) Firebase-এ নতুন package দিয়ে নতুন `google-services.json` |
| Firebase কনফিগ | `app/google-services.json` |
| Firestore / Storage নিয়ম | `firestore.rules`, `storage.rules` |
| OTP ইমেইলের লেখা, মেয়াদ, চেষ্টার সংখ্যা | `functions/index.js` (উপরের `OTP_TTL_MS`, `MAX_ATTEMPTS`) |
| Functions Region বদলালে | `functions/index.js`-এ `region` যোগ করুন এবং `LockerResetActivity.java`-এ `FirebaseFunctions.getInstance("your-region")` |
| Default Category | `data/Repo.java` → `seedDefaults` |
| Reminder প্রিসেট (5/15/30/60 মিনিট) | `ui/AddTaskActivity.java` → `PRESETS` |
| Locker পোলিং গতি | `locker/LockService.java` → `postDelayed(this, 400)` |
| Privacy Policy লেখা | `ui/SettingsActivity.java` |
| সব XML লেআউট | `app/src/main/res/layout/` |

## ৯. ফোল্ডার কাঠামো
```
Planora/
├─ app/src/main/
│  ├─ AndroidManifest.xml
│  ├─ java/com/planora/app/
│  │  ├─ data/      Room Entity/DAO/DB, Repo, SyncWorker, SyncStatus
│  │  ├─ reminder/  Alarms, AlarmReceiver, BootReceiver
│  │  ├─ ui/        Splash, Onboarding, Login, Register, Main, Fragments, AddTask ...
│  │  ├─ calc/      Calc (parser), CalculatorFragment
│  │  ├─ locker/    LockService, LockerFragment (গাইড সহ), LockScreenActivity, LockPrefs ...
│  │  └─ util/      Util, Prefs
│  └─ res/layout, drawable, values, menu   (সব XML)
├─ functions/       Cloud Functions (OTP)
├─ firestore.rules  storage.rules  firebase.json
├─ README.md   TESTING.md
```

## ১০. সাধারণ সমস্যা
| সমস্যা | সমাধান |
|---|---|
| `default_web_client_id` পাওয়া যাচ্ছে না | Google Sign-in চালু + SHA-1 দিয়ে **নতুন** `google-services.json` ডাউনলোড করে `app/`-এ বসান, Sync করুন |
| Google লগইনে `code 10` / `12500` | SHA-1 ভুল/অনুপস্থিত, বা Emulator-এ Google Play নেই |
| `File google-services.json is missing` | ফাইলটি `app/` ফোল্ডারে বসেনি |
| ভেরিফিকেশন মেইল আসছে না | Spam দেখুন; Firebase Console → Authentication → Templates |
| Reminder দেরিতে আসছে | Permission Center → Exact alarm চালু, Battery অপ্টিমাইজেশন শিথিল |
| Locker কাজ করছে না | ১) App Lock ট্যাবে দুটো অনুমতিই “Allowed ✓” কিনা ২) Enable সুইচ অন কিনা ৩) অন্তত একটি অ্যাপের সুইচ অন কিনা ৪) Settings → Apps → Planora → Battery = No restrictions, Autostart চালু ৫) নোটিফিকেশনে “Planora App Locker is active” দেখা যাচ্ছে কিনা (না দেখালে সুইচ বন্ধ করে আবার অন করুন) |
| OTP পাঠানো ব্যর্থ | Blaze প্ল্যান, `firebase deploy --only functions` সফল হয়েছে কিনা, Secrets সেট আছে কিনা — `firebase functions:log` দেখুন |

সাহায্যের জন্য: এরর টেক্সট আমাকে পাঠান, ঠিক করে দেব। 🙂
