# WorkoutTracker

WorkoutTracker is an Android app on Kotlin + Jetpack Compose for tracking workouts, nutrition, progress, and profile data. The current version uses local persistence with user separation and a main navigation of four sections: Training, Nutrition, Analytics, and Profile.

## Current functionality

### Training

- Workout list and workout history
- Templates and reusable training scenarios
- Exercise library with built-in catalog
- Custom exercises, favorites, and recent exercises
- Logging sets, reps, and weight
- Saving active workout state

### Nutrition

- Food diary with meals and daily entries
- Nutrition profile and personal calorie/macronutrient targets
- Meal plan generation and reuse
- Fridge inventory
- Barcode scanning for products
- Product cache for repeated lookups
- Manual and quick product entry flows

### Analytics

- Step tracking
- Weight history
- Nutrition summaries
- Best exercises and training metrics
- Weather block
- Analytics settings
- Health Connect integration for supported step scenarios

### Profile

- Registration and login
- Personal data editing
- Goal and measurement tracking
- Avatar selection
- Theme switching

## Navigation

Bottom navigation currently contains:

1. Training
2. Nutrition
3. Analytics
4. Profile

## Tech stack

- Kotlin
- Jetpack Compose
- Navigation Compose
- Hilt
- Room
- DataStore
- WorkManager
- Retrofit + Gson + OkHttp
- Coil
- ML Kit Barcode Scanning
- CameraX
- Health Connect
- Google Play Services Location / Fitness
- MPAndroidChart

## Data storage

The app currently stores data locally:

- `Room` for core user, training, nutrition, analytics, and cache data
- `DataStore` for app settings
- `SharedPreferences` only for legacy migration / compatibility paths
- Local files for selected images

## Local configuration

Some integrations require local keys in `local.properties`.

### Nutrition AI

```properties
LLM_API_KEY=your_api_key
LLM_BASE_URL=https://openrouter.ai/api/v1
LLM_MODEL_ID=openai/gpt-4o-mini
LLM_HTTP_REFERER=
LLM_APP_TITLE=WorkoutTracker Nutrition AI
```

### FatSecret

```properties
FATSECRET_CLIENT_ID=your_client_id
FATSECRET_CLIENT_SECRET=your_client_secret
```

### Weather

Add the OpenWeather key to the app resources/config used by the analytics weather block.

## Notes

- Minimum SDK: `26`
- Target / compile SDK: `36`
- The project contains legacy migration code for older local app data formats
- The README describes the current implemented functionality in this branch
