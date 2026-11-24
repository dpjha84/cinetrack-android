# WatchPilot (v5.1.0 • Build 1006) — Google Play Store Listing Pack

**Package ID:** `com.dpjha.moviebuddy`  
**Version:** `5.1.0` (`versionCode: 1006`)  
**App Category:** Entertainment  
**Pricing Model:** Free with In-App Purchases (`pro_monthly`, `pro_annual`, `pro_lifetime`) & Ads (Ad-Free on Pro)

---

## 1. Play Store Main Listing Copy (Policy-Verified)

### App Name (Max 30 characters — Play Policy Compliant)
`WatchPilot` *(10 / 30 chars — no emojis, no ALL CAPS, no promotional buzzwords)*

### Short Description (Max 80 characters)
`Track movies & shows, scan Reels to watchlist, audit OTT spend & match tonight.` *(79 / 80 chars)*

### Full Description (Max 4000 characters)
```
WatchPilot helps you track what you watch, find where titles stream in your country, and decide what to watch in seconds — without losing progress or paying for streaming apps you barely use.

KEY FEATURES FOR EVERY CINEPHILE
• Track Movies & TV Episodes: Log watchlist status (Plan to Watch, Watching, Completed, On Hold, Dropped), star ratings, reviews, and season-by-season episode progress with quick "+1 Ep" updates.
• Where to Stream by Region: See instant streaming, rent, and buy availability across Netflix, Prime Video, Disney+, Max, Apple TV+, JioCinema, Hotstar, Hulu, Crunchyroll, and more — localized to your country.
• Offline Watchlist & Search Cache: View your saved library, ratings, episode progress, and search history even without an internet connection.
• Instant CSV Import & Cloud Sync: Import existing libraries from IMDb, Letterboxd, Moviebase, Trakt, or CSV files and sync safely across Android phones and tablets via Google Sign-In.
• Trailers, Cast Bios & Personalization: Watch trailers, browse cast & crew filmographies, pick favorite genres and stars, and share Cinema Wrapped cards.

UPGRADE TO WATCHPILOT PRO
• OTT Money-Saver Radar & "On My Apps" Filter: Audit your paid streaming apps against your active watchlist to spot idle subscriptions you can pause and filter your entire library to what streams on your owned apps.
• Unlimited AI Screenshot Vision Scanner: Pick a screenshot of any Reel, TikTok, or Top-10 list and automatically match titles into your watchlist.
• Unlimited Tonight's AI Concierge & Spoiler-Free Recaps: Get 3 mood- and time-based picks grounded in your ratings, plus Watch-Fit scores and spoiler-free "Previously On..." recaps.
• Unlimited Live Swipe-to-Match Rooms: Host 6-digit rooms with a partner or friends to swipe and match tonight's movie in under 60 seconds.
• Streaming & New-Season Alerts + Cinema DNA Insights: Get alerts when tracked shows drop a new season or arrive on your apps, unlock Era & Language DNA breakdowns, unlimited custom collections, CSV/JSON backups, and a 100% ad-free experience.

Notes:
• WatchPilot is a tracking and discovery companion and does not stream copyrighted movies or TV shows.
• This product uses the TMDB API but is not endorsed or certified by TMDB. Streaming availability data is powered by JustWatch.
• Monthly & Annual Pro subscriptions auto-renew via Google Play unless canceled at least 24 hours before renewal. A one-time Lifetime unlock is also available.
```

### Release Notes / What's New (`v5.1.0` — Max 500 characters)
```
WatchPilot 5.1.0 brings unified WatchPilot Pro & Light/Dark theme polish:
• Quick "+ Watchlist" pill right on Home carousels & section grids for untracked titles
• Swipeable My Library tabs with customizable collections & On My Apps filtering
• Offline Room caching for watchlist & search history
• Tonight's AI Concierge, Screenshot-to-Watchlist Vision, Live Swipe-to-Match rooms & OTT Money-Saver Radar
```

---

## 2. Google Play Billing SKUs (Must Match Play Console Exact IDs)

| SKU ID in Code (`MonetizationManager.kt`) | Play Console Product Type | Recommended Default USD Price | Offering |
|---|---|---|---|
| `pro_monthly` | Subscription (Monthly base plan) | `$1.99 / mo` | Flexible Pro access + ad removal |
| `pro_annual` | Subscription (Annual base plan, optional 7-day free trial offer) | `$11.99 / yr` | Best Value (~50% savings) |
| `pro_lifetime` | One-Time In-App Product (Managed Product) | `$24.99` | Lifetime Founders Unlock |

> **Important Play Console Note:** In `EditionBillingProvider.kt`, the app queries `pro_monthly` & `pro_annual` as `BillingClient.ProductType.SUBS` and `pro_lifetime` as `BillingClient.ProductType.INAPP`. All 3 product IDs **must be created and set to Active** in Google Play Console before publishing.

---

## 3. Google Play Data Safety & App Content Checklist

| Section | Declaration Summary |
|---|---|
| **App Access** | Provide a demo/reviewer note explaining Guest Mode works immediately and Google Sign-In enables Cloud Sync & Match Rooms. |
| **Ads Declaration** | **Yes, my app contains ads** (Google AdMob banners, 3-min capped trailer interstitials, and optional rewarded trial ads; removed for Pro users). |
| **Data Collected** | Name & Email (when Google Sign-In is used), App Activity (watchlist, ratings, searches, in-app analytics if enabled), Device/Other IDs (AdMob Advertising ID, Firebase Installation/App Check ID), Photos (user-selected screenshots sent ephemerally for AI title matching only). |
| **Data Security** | Encrypted in transit (HTTPS/TLS). Account & cloud data deletion available in-app (**Profile → Delete Account**) and via `/account-deletion.html`. |
| **Target Audience** | 13+ / Teens & Adults (Not directed at children). |
| **Privacy Policy URL** | Host `/public/privacy.html` on your HTTPS domain and enter URL in Play Console. |
| **Account Deletion URL** | Host `/public/account-deletion.html` on your HTTPS domain and enter URL in Play Console. |
| **app-ads.txt** | Publish `/app-ads.txt` (`google.com, pub-9351754143985661, DIRECT, f08c47fec0942fa0`) at the root of your Developer Website domain. |

---

## 4. Visual Store Assets & Screenshot Shot-List

Already generated in `public/`:
- **App Icon (512×512 PNG):** `public/playstore-app-icon-512x512.png`
- **Feature Graphic (1024×500 PNG):** `public/playstore-feature-graphic-1024x500.png` (and `public/campaign/watchpilot_play_feature_1024x500.png`)

Recommended 6–8 Phone + Tablet Screenshots to capture from a clean emulator/device run before submission:
1. **Home Screen (Dark Theme)** showing Trending Spotlight, Quick Action cards (`Tonight's Pick`, `Swipe Match`, `Snap Scan`), and `+ Watchlist` pills.
2. **WatchPilot Pro Paywall / OTT Money-Saver Radar** showing monthly spend & "Pause & Save" insights.
3. **Tonight's AI Concierge / Snap-to-Watchlist Vision Scanner** showing instant matched posters.
4. **Live Swipe-to-Match Room** showing 6-digit code & matched celebration card.
5. **Media Detail Screen (Light Theme)** showing regional streaming providers, Spoiler-Free AI Recap, and Season/Episode tracker.
6. **Tablet / Foldable Adaptive View** (2-column Continue Watching + side Navigation Rail) for Google Play Tablet / Large Screen feature eligibility.
