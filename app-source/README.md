# 🌱 The Accidental Gardener

An Android app (Kotlin + Jetpack Compose) that plans your beds, tells you when to sow, plant out and harvest, watches the weather and reminds you to water.

## Features

- **Setup** with your name, nearest town and how many outdoor and tunnel beds you have (any size)
- **Seed tin** with 155 veg, fruit, herbs, flowers and bulbs commonly grown in Ireland, plus "Add your own"
- **Planner** that puts each crop in the best bed using
  - tunnel needs (tomatoes, peppers, cucumbers go under cover)
  - crop rotation (avoids last year's plant family in the same bed)
  - companion planting (basil with tomatoes, no onions next to beans)
  - bed capacity (crops per bed, or bed size in m² if you add it)
- **Sowing dates** worked out from your last frost date, with tunnel beds about three weeks ahead
- **Jobs list** on the home screen. Tap a job for step-by-step help shown only when it's due (soaking, chilling or chitting, sowing depth and seeds per pot, hardening off, planting out with a picture of the bed layout, harvesting). Tick it and the crop moves on to the next stage
- **Growing guides** in `plants.json`, built in and updatable from the internet (see `guides/README.md`)
- **Weather** forecasts and official warnings from **Met Éireann** open data (free, no key, fine for apps with ads)
- **Alerts** for frost, strong winds, heavy rain, heat and sharp temperature drops, checked every 4 hours in the background
- **Water advice** for outdoor and tunnel beds based on recent rain, the forecast and when you last watered
- **Journal** for watering, feeding, sowing, harvests, weeding, pests, mulching and notes
- **Calendar** with a 12-month timeline and a month-by-month job list

## Build and run (VS Code or terminal)

1. Make sure you have JDK 17+ and the Android SDK (platform 35) installed and `ANDROID_HOME` set, or create `local.properties` with
   `sdk.dir=/path/to/Android/Sdk`
2. Plug in your phone with USB debugging on
3. From the project folder run

```
./gradlew installDebug
```

On Windows use `gradlew.bat installDebug`. Android Studio also opens the project directly.

## Project layout

```
app/src/main/java/com/accidentalgardener/app/
  data/        Room database, entities and the plant catalogue loader (Crops.kt)
  domain/      Planner, tasks, alerts and watering advice
  weather/     Met Éireann forecast and warnings, town search (Android geocoder)
  work/        Background weather check and notifications
  ui/          Compose screens, theme and shared components
```

To add or tweak plants and guides see `guides/README.md`.

Weather forecasts and warnings copyright Met Éireann, source www.met.ie, CC BY 4.0.
