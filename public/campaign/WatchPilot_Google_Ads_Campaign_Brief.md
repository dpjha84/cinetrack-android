# WatchPilot (v5.1.0 • Build 1006) — Google Ads App Campaign Kit

**Package ID:** `com.dpjha.moviebuddy`  
**Platform:** Android (Phone, Foldable & Tablet Adaptive)  
**Monetization:** Freemium (AdMob + Rewarded Pro Trial Passes) + WatchPilot Pro (`pro_monthly`, `pro_annual`, `pro_lifetime`)  
**Campaign Types:**
1. **Phase 1 (Launch):** Google Ads App Campaign — `App installs` (`first_open`)
2. **Phase 2 (Scale Paid Conversion):** Google Ads App Campaign — `App installs (In-app actions)` / `tCPA` / `tROAS` optimizing on `pro_plan_unlocked` and `begin_checkout`

---

## 1. Google Ads App Campaign Copy (Verified Character Limits)

### Headlines (Max 30 characters each — paste up to all 5)
| # | Headline Text | Length | Focus |
|---|---|---|---|
| 1 | `WatchPilot: Movie & TV Tracker` | 30 / 30 | Brand + primary keyword |
| 2 | `Snap Reels to Your Watchlist` | 28 / 30 | AI Screenshot Vision Scanner |
| 3 | `AI Picks for Movie Night Fast` | 29 / 30 | Tonight's AI Concierge |
| 4 | `Audit OTT Apps & Save Money` | 27 / 30 | OTT Money-Saver Radar |
| 5 | `Find Where Movies Stream Now` | 28 / 30 | High-intent regional OTT lookup |

### Descriptions (Max 90 characters each — paste up to all 5)
| # | Description Text | Length |
|---|---|---|
| 1 | `Track shows & movies offline, import IMDb/Letterboxd CSVs & spot Netflix or Prime links.` | 88 / 90 |
| 2 | `Snap any Reel or TikTok screenshot with AI to add movies straight into your watchlist.` | 86 / 90 |
| 3 | `Get Tonight's AI Concierge picks, spoiler-free recaps & swipe to match in live rooms.` | 85 / 90 |
| 4 | `Audit paid streaming apps against your watchlist, pause idle plans & cut monthly spend.` | 87 / 90 |
| 5 | `Adaptive Android phone & tablet UI with cloud sync, ratings, trailers & Go Pro ad-free.` | 87 / 90 |

---

## 2. Visual & Video Assets Included

### Images
1. **`watchpilot_uac_landscape_1200x628.jpg` (`1200×628 px`, 1.91:1)** — Required Google Ads landscape image for Display, Gmail, Discover & Play Store companion slots.
2. **`watchpilot_uac_square_1200x1200.jpg` (`1200×1200 px`, 1:1)** — Required square creative for Discovery, Native & Display grids.
3. **`watchpilot_uac_portrait_1200x1500.jpg` (`1200×1500 px`, 4:5)** — High-CTR portrait creative for mobile feeds.
4. **`watchpilot_uac_story_1080x1920.jpg` (`1080×1920 px`, 9:16)** — Full-screen vertical interstitial / mobile story placement.
5. **`watchpilot_uac_tablet_1920x1080.jpg` (`1920×1080 px`, 16:9)** — Adaptive tablet showcase banner & YouTube custom thumbnail.
6. **`watchpilot_play_feature_1024x500.png` (`1024×500 px`)** — Play Console Feature Graphic (`1024×500`).

### Videos (15s H.264 MP4 + Stereo AAC Audio — Upload to YouTube first)
> **Important for Google Ads App Campaigns:** Google Ads pulls videos from **YouTube URLs** (set to **Unlisted** or **Public** on your channel, then paste the 3 URLs into your App Campaign asset group).
1. **`watchpilot_promo_landscape_16x9_1920x1080.mp4`** (`1920×1080`, 15s) — YouTube In-Stream & Desktop Display.
2. **`watchpilot_promo_vertical_9x16_1080x1920.mp4`** (`1080×1920`, 15s) — YouTube Shorts, AdMob Vertical Interstitial & Reels.
3. **`watchpilot_promo_square_1x1_1080x1080.mp4`** (`1080×1080`, 15s) — Discovery / Performance Max / Social Square feeds.

---

## 3. Firebase + Play Store + Google Ads Conversion Funnel

All key funnel events are instrumented in `AnalyticsTracker.kt`:
- **Install Baseline:** `first_open` (automatic Firebase Analytics event).
- **Activation (Mid-Funnel Micro-Conversions):** `watchlist_added`, `ai_concierge_generated`, `snap_scan_completed`, `match_room_created`, `match_found`.
- **Paid Revenue Funnel (Primary tCPA / tROAS Conversions):**
  - `paywall_viewed` (includes `trigger_reason`: `banner`, `profile`, `ott_money_saver`, `snap_scan`, `ai_concierge`, `ai_recap`, `match_room`)
  - `begin_checkout` (fires when tapping Monthly, Annual, or Lifetime purchase CTA)
  - `pro_plan_unlocked` (fires after verified Play Billing unlock with `plan_type` & `source = google_play`)
  - Plus **Google Play ↔ Firebase linked `in_app_purchase` / `subscription`** auto-revenue events once Play Console is linked to Firebase (`movieverse-dd226`).

### Step-by-Step Launch Blueprint
1. **Link Accounts Before Spending:**
   - **Firebase (`movieverse-dd226`) ↔ Google Play (`com.dpjha.moviebuddy`)** for verified `in_app_purchase` revenue attribution.
   - **Firebase ↔ Google Ads** to import `first_open`, `watchlist_added`, `begin_checkout`, `pro_plan_unlocked`, and `in_app_purchase` as conversions (set `pro_plan_unlocked` / `in_app_purchase` as Primary once volume >= 10 conversions/week).
   - **Google Play Developer Website:** Host `public/privacy.html`, `public/terms.html`, `public/account-deletion.html`, and root `/app-ads.txt` (`pub-9351754143985661`).
2. **Campaign Segmentation (Never Mix Tier-1 & Low-CPI Geos in One Ad Group):**
   - **Campaign A — High-ARPU Pro Buyer Markets (`US, UK, CA, AU, DE`):** Optimize toward `first_open` -> `begin_checkout` -> `pro_plan_unlocked` / `in_app_purchase`. Strongest conversion rates on **Annual (`pro_annual`)** + **OTT Money-Saver Radar**.
   - **Campaign B — High-Velocity Growth Markets (`IN, BR, MX, ID, PH`):** Low-CPI `first_open` / `watchlist_added` targeting localized pricing & AdMob + rewarded trial engagement.
   - **Include Android Tablets (`minSdk 24+`):** Dedicated 2-pane adaptive tablet layout delivers higher session length and lower uninstall rates.
3. **YouTube & Display Exclusions:**
   - Exclude kids/nursery-rhyme channels (`Made for Kids`) so ads reach adult movie/series viewers and keep AdMob & Pro ARPU high.

---

## 4. Updated 15s Promo Script (Pro + AI Value Prop)
- **0:00–0:03:** *"Stop forgetting shows and paying for streaming apps you never open."*
- **0:03–0:07:** *"Snap any Reel screenshot to add titles automatically, and check where to stream in your region."*
- **0:07–0:11:** *"Get AI movie-night picks, swipe to match with friends, and audit your OTT spend with WatchPilot Pro."*
- **0:11–0:15:** *"Download WatchPilot on Google Play today."*
