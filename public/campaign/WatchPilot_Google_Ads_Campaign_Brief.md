# WatchPilot (v5.0.0) — Google Ads App Campaign Kit

**Package ID:** `com.dpjha.moviebuddy`  
**Platform:** Android (Phone, Foldable & Tablet Adaptive)  
**Campaign Type:** Google Ads App Campaign (`App installs` -> `App engagement` / in-app actions)

---

## 1. Google Ads App Campaign Copy (Verified Character Limits)

### Headlines (Max 30 characters each — paste up to all 5)
| # | Headline Text | Length | Focus |
|---|---|---|---|
| 1 | `WatchPilot: Movie & TV Tracker` | 30 / 30 | Brand + primary keyword |
| 2 | `Never Lose Episode Progress` | 27 / 30 | Core pain point |
| 3 | `Find Where Movies Stream Now` | 28 / 30 | High-intent OTT lookup |
| 4 | `Trailers, Watchlist & Sync` | 26 / 30 | Feature stack |
| 5 | `Built for Android Phone & Tab` | 29 / 30 | Phone + adaptive tablet appeal |

### Descriptions (Max 90 characters each — paste up to all 5)
| # | Description Text | Length |
|---|---|---|
| 1 | `Track movies, seasons & episodes. Spot Netflix, Prime & Hotstar links fast. Free app!` | 85 / 90 |
| 2 | `Watch HD trailers, save watchlists & sync Cinema DNA free across Android phone & tablet.` | 88 / 90 |
| 3 | `Rate what you watch, pick personalized favorites & organize custom cinema lists easily.` | 87 / 90 |
| 4 | `Check instant streaming availability by region, track next episodes & cloud-sync free.` | 86 / 90 |
| 5 | `No lost progress. Adaptive tablet two-pane view, cast bios, trailers & regional guides.` | 87 / 90 |

---

## 2. Visual & Video Assets Included

### Images
1. **`watchpilot_uac_landscape_1200x628.jpg` (`1200×628 px`, 1.91:1)** — Required Google Ads landscape image for Display, Gmail, Discover & Play Store companion slots.
2. **`watchpilot_uac_square_1200x1200.jpg` (`1200×1200 px`, 1:1)** — Required square creative for Discovery, Native & Display grids.
3. **`watchpilot_uac_portrait_1200x1500.jpg` (`1200×1500 px`, 4:5)** — High-CTR portrait creative for mobile feeds.
4. **`watchpilot_uac_story_1080x1920.jpg` (`1080×1920 px`, 9:16)** — Full-screen vertical interstitial / mobile story placement.
5. **`watchpilot_uac_tablet_1920x1080.jpg` (`1920×1080 px`, 16:9)** — Adaptive tablet showcase banner & YouTube custom thumbnail.
6. **`watchpilot_play_feature_1024x500.png` (`1024×500 px`)** — Play Console Feature Graphic updated for WatchPilot v5.0 Phone & Tablet.

### Videos (15s H.264 MP4 + Stereo AAC Audio — Ready for YouTube Upload)
> **Important for Google Ads App Campaigns:** Google Ads pulls videos from **YouTube URLs** (you can set them to **Unlisted** or **Public** on your brand YouTube channel, then paste the 3 YouTube URLs directly into Google Ads!).
1. **`watchpilot_promo_landscape_16x9_1920x1080.mp4`** (`1920×1080`, 15s) — YouTube In-Stream & Desktop Display.
2. **`watchpilot_promo_vertical_9x16_1080x1920.mp4`** (`1080×1920`, 15s) — YouTube Shorts, AdMob Vertical Interstitial & Reels.
3. **`watchpilot_promo_square_1x1_1080x1080.mp4`** (`1080×1080`, 15s) — Discovery / Performance Max / Social Square feeds.

---

## 3. Recommended Google Ads Launch Blueprint

### Step 1: Link Firebase + Play Console before Spending
1. In **Google Ads → Tools → Data Manager / Linked Accounts**, link **Google Play (`com.dpjha.moviebuddy`)** and **Firebase (`movieverse-dd226`)**.
2. Import **Firebase `first_open`** (automatic once Analytics SDK is active) so Google Ads counts verified Android installs cleanly without extra attribution SDK cost.
3. Verify your **`app-ads.txt`** at your Play Console Developer Website root (already bundled in repo at `/app-ads.txt`).

### Step 2: Campaign Structure & Bidding
- **Campaign Subtype:** **App installs** (`Get new users for your app`).
- **Start Bidding (Weeks 1–2):** **Target CPI (tCPI)** or **Install Volume** with a realistic daily budget `>= 10x–20x` your target CPI so the ML exits learning quickly (e.g., if target CPI is `₹12 / $0.25`, start around `₹250–₹500 / day` or `$10–$15 / day` in Tier-1/Tier-2 mixes; **avoid mixing Tier-1 US + Tier-3 India in the same ad group**, or 95% of impressions will shift to the cheapest CPI region!).
- **Recommended Split:**
  - **Campaign A (Growth Markets — India / LATAM / SE Asia):** Low CPI, fast install volume & review velocity (`tCPI` optimized).
  - **Campaign B (High-ARPU AdMob Markets — US / UK / CA / AU):** Separate budget so US/UK eCPM traffic isn't starved by lower-CPI geos.
  - **Device/Form-Factor Note:** Explicitly include **Android Tablets** (`Android 7.0+`, `minSdk 24`) in targeting — your new adaptive tablet UI converts tablet users cleanly into high-retention viewers!

### Step 3: Keyword & Audience Signals (for YouTube / Search / Play Store ASO)
- **Primary Play Store & Ad Themes:** `movie watchlist app`, `tv show episode tracker`, `where to watch movies streaming`, `movie trailer app`, `series progress tracker`, `cinema tracker android`, `tablet movie organizer`.
- **Negative Exclusions (YouTube / Display):** Exclude kids/nursery-rhyme channels (`Made for Kids` placements) to maximize adult cinema enthusiast retention and keep AdMob eCPM healthy.

---

## 4. Video Voiceover / Ad Script (15s Spot)
- **0:00–0:04:** *"Meet WatchPilot — your personal cinema and series pilot."*
- **0:04–0:08:** *"Track every episode you've watched, watch HD trailers, and instantly see where to stream on Netflix, Prime & more."*
- **0:08–0:12:** *"Designed natively for both Android phones and tablets, with automatic Google Cloud Sync."*
- **0:12–0:15:** *"WatchPilot v5. Download free on Google Play today."*
