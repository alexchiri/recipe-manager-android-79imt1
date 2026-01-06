# Recipe Manager - Functional Specification

**Version:** 1.0
**Last Updated:** January 6, 2026
**Purpose:** Cross-platform implementation guide for iOS and Android versions

---

## Table of Contents

1. [Overview](#overview)
2. [Core Features](#core-features)
3. [User Workflows](#user-workflows)
4. [Data Model](#data-model)
5. [Feature Details](#feature-details)
6. [Import/Export Specification](#importexport-specification)
7. [Business Rules](#business-rules)
8. [Localization Requirements](#localization-requirements)
9. [API Integration](#api-integration)

---

## Overview

Recipe Manager is a multi-language recipe management application with AI-powered recipe extraction, meal planning, and cloud synchronization. The app supports three languages (English, Swedish, Romanian) and stores all recipe content in all languages simultaneously.

### Key Capabilities

- **AI-Powered Recipe Extraction**: Extract recipes from photos, text, URLs, and YouTube videos using Claude Vision API
- **Multi-Language Support**: All recipes stored in English, Swedish, and Romanian with automatic translation
- **Meal Planning**: Calendar-based meal planner with shopping list generation
- **Cloud Sync**: Optional cloud synchronization across devices
- **Privacy-First**: Photos discarded after processing, API keys stored securely

---

## Core Features

### 1. Recipe Management

**Recipe Acquisition Methods:**
- Camera capture (single or multiple photos)
- Photo library selection
- Text input (paste recipe text)
- YouTube video captions (via timedtext API URL)
- Web URL import

**Recipe Operations:**
- View recipe details
- Edit title, ingredients, instructions, notes
- Add/remove tags
- Rate recipes (1-5 stars)
- Delete recipes
- Search recipes by name or ingredient
- Filter by tags
- Sort by date added or rating
- Share recipes

### 2. Meal Planning

**Planning Features:**
- Calendar view (90 days past + 180 days future)
- Assign recipes to lunch or dinner slots
- Swap lunch and dinner on same day
- Remove recipes from meal plan
- Generate meal plan summaries with optional shopping lists
- Share meal plans as text or image

### 3. Cloud Synchronization

**Sync Capabilities:**
- Optional cloud sync (can be disabled)
- Automatic sync every 5 minutes
- Manual sync trigger
- Bidirectional sync with conflict resolution (last-write-wins)
- Separate sync for recipes and meal plans

### 4. Settings & Configuration

- API key management (secure storage)
- Cloud sync toggle
- Manual sync trigger
- Language preference (affects UI only)

---

## User Workflows

### Workflow 1: Add Recipe from Photo

1. User taps "Add Recipe" button
2. User selects "Camera" or "Photo Library"
3. User takes/selects one or more photos
4. App uploads photos to Claude Vision API
5. API extracts recipe information:
   - Title
   - Ingredients with amounts and units
   - Step-by-step instructions
   - Servings, prep time, cook time
   - Detects dietary tags
   - Converts measurements to metric
   - Detects original language
6. App auto-translates to all 3 languages
7. User reviews extracted recipe
8. User can edit any field before saving
9. User confirms and saves
10. Recipe stored locally and synced to cloud (if enabled)

### Workflow 2: Browse and Search Recipes

1. User views recipe list
2. User can:
   - Search by name or ingredient
   - Filter by one or more tags
   - Sort by date or rating
   - Switch display language
3. User taps recipe to view details
4. From details, user can:
   - Edit recipe
   - Rate recipe
   - Add notes
   - Share recipe
   - Delete recipe
   - View in different language

### Workflow 3: Plan Meals

1. User switches to Meal Planner tab
2. User scrolls to desired date
3. User taps empty lunch or dinner slot
4. User searches/filters recipes
5. User selects recipe for that meal slot
6. Recipe assigned to meal plan
7. Meal plan synced to cloud (if enabled)
8. User can:
   - Swap lunch and dinner
   - Remove recipe from slot
   - View recipe details
   - Generate meal plan summary

### Workflow 4: Generate Shopping List

1. User taps share button in meal planner
2. User selects date range (This Week, Next Week, or Custom)
3. User toggles "Include Shopping List" option
4. User selects format (Text or Image)
5. App generates meal plan with:
   - Date-formatted schedule
   - All meals listed
   - Optional aggregated shopping list (ingredients grouped and counted)
6. User shares via system share sheet

### Workflow 5: Share Recipe with Shopping App

1. User views recipe details
2. User taps share button
3. User selects "Share Ingredients"
4. App generates quickyshoppy:// URL with base64-encoded JSON
5. User opens URL in QuickyShoppy app
6. Ingredients imported to shopping list

---

## Data Model

### Recipe

```
Recipe {
  // Identifiers
  id: UUID (unique identifier)
  createdAt: Date (ISO 8601 format)
  updatedAt: Date (ISO 8601 format)

  // Multi-Language Content
  titleEnglish: String
  titleSwedish: String
  titleRomanian: String

  ingredientsEnglish: Array<Ingredient>
  ingredientsSwedish: Array<Ingredient>
  ingredientsRomanian: Array<Ingredient>

  instructionsEnglish: Array<String> (ordered steps)
  instructionsSwedish: Array<String>
  instructionsRomanian: Array<String>

  notesEnglish: String | null
  notesSwedish: String | null
  notesRomanian: String | null
  notes: String | null (legacy field, maintain for backward compatibility)

  // Metadata
  tags: Array<String> (tag enum values)
  servings: Integer | null
  prepTime: String | null (e.g., "15 mins", "1 hour")
  cookTime: String | null (e.g., "30 mins", "2 hours")
  rating: Integer | null (1-5)
  detectedLanguage: String | null (e.g., "English", "Swedish", "Romanian")
}
```

### Ingredient

```
Ingredient {
  id: UUID
  text: String (full description, e.g., "2 cups flour")
  amount: String | null (e.g., "2", "1.5")
  unit: String | null (e.g., "cups", "ml", "g")
  name: String (ingredient name, e.g., "flour")
}
```

### Meal Plan

```
MealPlan {
  id: UUID
  date: Date (ISO 8601, normalized to start of day UTC)
  createdAt: Date (ISO 8601 format)
  updatedAt: Date (ISO 8601 format)
  lunchSlot: MealSlot
  dinnerSlot: MealSlot
}
```

### Meal Slot

```
MealSlot {
  id: UUID
  mealType: String ("lunch" | "dinner")
  recipeId: UUID | null
  recipeName: String | null (cached for display if recipe deleted)
}
```

### Available Tags

**Tag Enum Values** (store as strings without emoji):

Dietary Restrictions:
- `vegetarian`
- `vegan`
- `glutenFree`
- `lactoseFree`
- `nutFree`

Diet Styles:
- `lowCarb`
- `keto`
- `paleo`

Meal Types:
- `breakfast`
- `lunch`
- `dinner`
- `dessert`
- `snack`
- `beverage`

Courses:
- `appetizer`
- `mainCourse`
- `sideDish`
- `soup`
- `salad`

Protein Types:
- `pork`
- `chicken`
- `beef`
- `lamb`
- `turkey`
- `fish`
- `seafood`

Cooking Style:
- `quickEasy`
- `slowCook`

**Display Format:**
Each platform should map these enum values to localized display strings with appropriate emoji:
- `vegetarian` → "🌱 Vegetarian" (English), "🌱 Vegetarisk" (Swedish), "🌱 Vegetarian" (Romanian)
- etc.

---

## Feature Details

### Recipe Search

**Search Scope:**
- Recipe titles in all 3 languages
- Ingredient names in all 3 languages

**Search Behavior:**
- Case-insensitive
- Partial matching (substring search)
- Real-time filtering as user types

### Recipe Filtering

**Filter Logic:**
- Multi-select: User can select multiple tags
- Combine with AND logic (recipe must have ALL selected tags)
- Display active filters with option to clear all

**Filter UI:**
- Organize tags by category:
  - Dietary Restrictions
  - Diet Styles
  - Meal Types
  - Courses
  - Protein Types
  - Cooking Style
- Show tag count in each category

### Recipe Sorting

**Sort Options:**
1. **By Date Added** (default)
   - Newest first
   - Uses `createdAt` field

2. **By Rating**
   - Highest rating first
   - Ties broken by date added (newest first)
   - Unrated recipes appear last

### Recipe Sharing

**Full Recipe Share:**
Format as text with the following structure:
```
[Recipe Title]

Servings: [servings] | Prep: [prepTime] | Cook: [cookTime]
Rating: ⭐⭐⭐⭐⭐ ([rating]/5)

Tags: [tag1] [tag2] [tag3]

INGREDIENTS:
- [ingredient text]
- [ingredient text]

INSTRUCTIONS:
1. [instruction step]
2. [instruction step]

NOTES:
[notes text]
```

**Ingredient Share (QuickyShoppy Integration):**
Generate URL: `quickyshoppy://import?data=[base64_json]`

JSON format:
```json
{
  "version": "1.0",
  "items": [
    {
      "name": "flour",
      "quantity": "2 cups"
    },
    {
      "name": "sugar"
    }
  ]
}
```

### Meal Plan Summary

**Text Format:**
```
Meal Plan: [date range]

[Day], [Date] [Month] [Year]
🥗 Lunch: [Recipe Name]
🍽️ Dinner: [Recipe Name]

[Day], [Date] [Month] [Year]
🥗 Lunch: [Recipe Name]
🍽️ Dinner: [Recipe Name]

---
Shopping List:
- [ingredient name] ([count] recipes)
- [ingredient name] ([count] recipes)
```

**Image Format:**
Render text format as image with:
- Clean typography
- Adequate padding/margins
- Readable font size
- System font

### Cloud Sync

**Sync Trigger Conditions:**
- App launch (if sync enabled)
- Every 5 minutes (if sync enabled and app active)
- Manual sync button
- After create/update/delete operations

**Sync Conflict Resolution:**
- Compare `updatedAt` timestamps
- Keep most recent version
- Apply to both local and cloud storage

**Delete Handling:**
- Track deleted items by ID with deletion timestamp
- Prevent deleted items from reappearing during sync
- Clean up deletion tracking after successful sync confirmation

**Sync Failure Handling:**
- Show error message to user
- Retry on next sync cycle
- Don't block user from making changes
- Queue changes for next successful sync

### Multi-Language Support

**Language Switching:**
- User can change app language (affects UI only)
- User can view recipes in any supported language
- Language selection persists across app launches

**Auto-Translation:**
- When recipe created: English → Swedish + Romanian
- When recipe edited in one language: translate changes to other languages
- When notes added: translate to other languages
- Translation happens asynchronously after save
- Show progress indicator during translation

**Translation Requirements:**
- Preserve ingredient structure (amount, unit, name)
- Produce natural, idiomatic translations
- Handle measurement unit translations correctly
- Maintain numbered instruction order

### Rating System

**Rating Options:**
- 1-5 stars
- Optional (can be null/unrated)
- Can be added/edited/removed at any time

**Rating Display:**
- Filled stars: ★
- Empty stars: ☆
- Show rating count (e.g., "4/5")
- Visual emphasis on rated recipes

### Notes Feature

**Notes Behavior:**
- Each language has separate notes field
- Notes can be added/edited/removed
- Auto-translate to other languages when saved
- Display appropriate placeholder when empty

**Notes UI:**
- Expandable section in recipe detail
- Edit mode with save/cancel buttons
- Multi-line text input

---

## Import/Export Specification

### File Format

**Format:** JSON
**Encoding:** UTF-8
**Extension:** `.json`

### Export Format

#### Recipe Export (Single Recipe)

```json
{
  "version": "1.0",
  "exportDate": "2026-01-06T12:00:00Z",
  "type": "recipe",
  "recipe": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "createdAt": "2025-12-01T10:30:00Z",
    "updatedAt": "2025-12-15T14:20:00Z",
    "titleEnglish": "Chocolate Chip Cookies",
    "titleSwedish": "Chokladkakor",
    "titleRomanian": "Biscuiți cu ciocolată",
    "ingredientsEnglish": [
      {
        "id": "660e8400-e29b-41d4-a716-446655440001",
        "text": "2 cups all-purpose flour",
        "amount": "2",
        "unit": "cups",
        "name": "all-purpose flour"
      },
      {
        "id": "660e8400-e29b-41d4-a716-446655440002",
        "text": "200g chocolate chips",
        "amount": "200",
        "unit": "g",
        "name": "chocolate chips"
      }
    ],
    "ingredientsSwedish": [
      {
        "id": "770e8400-e29b-41d4-a716-446655440001",
        "text": "475 ml vetemjöl",
        "amount": "475",
        "unit": "ml",
        "name": "vetemjöl"
      },
      {
        "id": "770e8400-e29b-41d4-a716-446655440002",
        "text": "200g chokladchips",
        "amount": "200",
        "unit": "g",
        "name": "chokladchips"
      }
    ],
    "ingredientsRomanian": [
      {
        "id": "880e8400-e29b-41d4-a716-446655440001",
        "text": "475 ml făină albă",
        "amount": "475",
        "unit": "ml",
        "name": "făină albă"
      },
      {
        "id": "880e8400-e29b-41d4-a716-446655440002",
        "text": "200g ciocolată",
        "amount": "200",
        "unit": "g",
        "name": "ciocolată"
      }
    ],
    "instructionsEnglish": [
      "Preheat oven to 350°F (175°C)",
      "Mix flour and chocolate chips in a bowl",
      "Bake for 12-15 minutes until golden"
    ],
    "instructionsSwedish": [
      "Förvärm ugnen till 175°C",
      "Blanda mjöl och chokladchips i en skål",
      "Grädda i 12-15 minuter tills gyllene"
    ],
    "instructionsRomanian": [
      "Preîncălziți cuptorul la 175°C",
      "Amestecați făina și ciocolata într-un bol",
      "Coaceți 12-15 minute până devine auriu"
    ],
    "notesEnglish": "Best served warm with milk",
    "notesSwedish": "Serveras bäst varma med mjölk",
    "notesRomanian": "Se servește cel mai bine cald cu lapte",
    "notes": null,
    "tags": ["dessert", "quickEasy", "vegetarian"],
    "servings": 24,
    "prepTime": "15 mins",
    "cookTime": "12 mins",
    "rating": 5,
    "detectedLanguage": "English"
  }
}
```

#### Recipe Collection Export (Multiple Recipes)

```json
{
  "version": "1.0",
  "exportDate": "2026-01-06T12:00:00Z",
  "type": "recipe_collection",
  "recipes": [
    {
      "id": "...",
      "createdAt": "...",
      ...
    },
    {
      "id": "...",
      "createdAt": "...",
      ...
    }
  ]
}
```

#### Meal Plan Export

```json
{
  "version": "1.0",
  "exportDate": "2026-01-06T12:00:00Z",
  "type": "meal_plan",
  "mealPlans": [
    {
      "id": "990e8400-e29b-41d4-a716-446655440000",
      "date": "2026-01-07T00:00:00Z",
      "createdAt": "2026-01-01T10:00:00Z",
      "updatedAt": "2026-01-01T10:00:00Z",
      "lunchSlot": {
        "id": "aa0e8400-e29b-41d4-a716-446655440001",
        "mealType": "lunch",
        "recipeId": "550e8400-e29b-41d4-a716-446655440000",
        "recipeName": "Chicken Salad"
      },
      "dinnerSlot": {
        "id": "bb0e8400-e29b-41d4-a716-446655440002",
        "mealType": "dinner",
        "recipeId": "660e8400-e29b-41d4-a716-446655440000",
        "recipeName": "Pasta Carbonara"
      }
    }
  ]
}
```

#### Complete Export (Recipes + Meal Plans)

```json
{
  "version": "1.0",
  "exportDate": "2026-01-06T12:00:00Z",
  "type": "complete",
  "recipes": [
    {
      "id": "...",
      ...
    }
  ],
  "mealPlans": [
    {
      "id": "...",
      ...
    }
  ]
}
```

### Import Behavior

**Import Rules:**

1. **ID Conflict Handling:**
   - If recipe with same ID exists locally:
     - Compare `updatedAt` timestamps
     - Keep most recent version
     - Show user which version was kept

2. **Missing Recipe References in Meal Plans:**
   - If meal plan references recipe not in import file:
     - Keep `recipeName` for display
     - Mark recipe as "missing"
     - Allow user to:
       - Select different recipe
       - Remove from meal plan
       - Keep as-is (show with strikethrough)

3. **UUID Generation:**
   - When importing to new device:
     - Option 1: Preserve original UUIDs (recommended for full migration)
     - Option 2: Generate new UUIDs (for copying recipes to existing collection)
   - Present choice to user during import

4. **Validation:**
   - Check JSON structure matches expected schema
   - Validate required fields present
   - Validate data types
   - Validate tag enum values
   - Validate date formats (ISO 8601)
   - Show detailed error if validation fails

5. **Language Content:**
   - Import all 3 languages
   - If any language missing, show warning
   - Allow user to auto-translate missing languages

6. **Import Options:**
   - Merge with existing data (default)
   - Replace all data (show confirmation warning)
   - Import only recipes
   - Import only meal plans
   - Preview import before applying

### Export Options

**Export Filters:**
- Export all recipes
- Export selected recipes
- Export recipes matching filter (tags, date range, rating)
- Export recipes used in meal plans
- Include/exclude meal plans

**Export Destination:**
- Save to file (user chooses location)
- Share via system share sheet
- Email as attachment
- Upload to cloud storage

---

## Business Rules

### Recipe Management

1. **Recipe Title:** Required, minimum 1 character
2. **Ingredients:** Minimum 1 ingredient required
3. **Instructions:** Minimum 1 instruction step required
4. **Tags:** Optional, unlimited tags allowed
5. **Rating:** Optional, must be 1-5 if provided
6. **Servings:** Optional, must be positive integer if provided
7. **Times:** Optional, free-form text (e.g., "15 mins", "1 hour 30 mins")

### Meal Planning

1. **Meal Slots:** Each day has 2 slots (lunch, dinner)
2. **Recipe Assignment:** One recipe per slot
3. **Empty Slots:** Allowed (meal plan can have empty slots)
4. **Date Range:** Support past and future dates
5. **Duplicate Recipes:** Same recipe can be assigned to multiple slots/dates
6. **Deleted Recipes:** If recipe deleted, meal plan shows name with indication it's deleted

### Search and Filter

1. **Empty Search:** Show all recipes
2. **No Results:** Show "No recipes found" message
3. **Filter + Search:** Both applied simultaneously (AND logic)
4. **Multiple Filters:** All selected tags must be present (AND logic)
5. **Language Switch:** Does not affect search/filter state

### Sharing

1. **Recipe Share:** Shares in currently displayed language only
2. **Meal Plan Share:** Shares in currently selected app language
3. **Shopping List:** Aggregates ingredients by name, shows count
4. **Empty Meal Plan:** Warn user if trying to share empty date range

### Synchronization

1. **Conflict Resolution:** Last-write-wins (based on `updatedAt`)
2. **Partial Sync:** If sync fails for some items, retry those items
3. **Offline Changes:** Queue changes, sync when connection restored
4. **Delete Propagation:** Deletions sync to all devices
5. **Sync Scope:** User can enable/disable sync globally

---

## Localization Requirements

### Supported Languages

1. **English (en)** - Primary language, used for AI processing
2. **Swedish (sv)**
3. **Romanian (ro)**

### Localization Scope

**UI Elements:**
- All buttons, labels, headings
- Navigation titles
- Tab bar labels
- Alert messages and dialogs
- Error messages
- Placeholder text
- Date and number formatting

**Recipe Content:**
- Recipe titles
- Ingredients (including names, full text)
- Instruction steps
- Notes
- Tag display names (with emoji)

**Not Localized:**
- Recipe IDs (UUIDs)
- Tag enum values (stored as English constants)
- API keys
- Date/time in ISO 8601 format (for data storage)

### Date Formatting

**Display Format by Language:**
- English: "Jan 6, 2026", "Monday, Jan 6, 2026"
- Swedish: "6 jan 2026", "måndag, 6 jan 2026"
- Romanian: "6 ian 2026", "luni, 6 ian 2026"

**Relative Dates:**
- English: "Today", "Tomorrow", "Yesterday"
- Swedish: "Idag", "Imorgon", "Igår"
- Romanian: "Astăzi", "Mâine", "Ieri"

### Number Formatting

**Decimal Separator:**
- English: period (1.5)
- Swedish: comma (1,5)
- Romanian: comma (1,5)

**Thousands Separator:**
- English: comma (1,000)
- Swedish: space (1 000)
- Romanian: period (1.000)

---

## API Integration

### Anthropic Claude API

**Required for:**
- Recipe extraction from images (Vision API)
- Recipe extraction from text/URLs (Text API)
- Auto-translation (Text API)

**API Key Management:**
- User must provide their own API key
- Key format: `sk-ant-[alphanumeric]`
- Store securely (platform keychain/secure storage)
- Never include in app bundle
- Never transmit except to Anthropic API

**Models Used:**
- Vision: `claude-sonnet-4-5-20250929` or latest available
- Translation: `claude-sonnet-4-5-20250929` or latest available

**Rate Limiting:**
- Handle 429 (Too Many Requests) errors gracefully
- Show user-friendly error message
- Suggest waiting and retrying

**Error Handling:**
- Network errors: Show "Check internet connection"
- Invalid API key: Show "Check API key in settings"
- Rate limit: Show "API rate limit reached, try again later"
- Other errors: Show error message from API response

### Cloud Storage API

**Platform-Specific:**
- iOS: CloudKit (Apple's private database)
- Android: Firebase Realtime Database or Firestore

**Requirements:**
- User authentication
- Private data storage (not shared between users)
- Bidirectional sync
- Conflict resolution
- Delete propagation

---

## Implementation Notes for Android

### Platform Equivalents

**iOS → Android:**
- SwiftUI → Jetpack Compose
- Keychain → EncryptedSharedPreferences
- CloudKit → Firebase
- UserDefaults → SharedPreferences
- JSONDecoder → Gson or Kotlinx Serialization
- UUID → java.util.UUID
- PhotosPicker → ActivityResultContracts.PickVisualMedia
- Camera → CameraX

### Architecture Recommendations

**Design Pattern:** MVVM with Repository pattern
- ViewModels for UI logic
- Repositories for data operations
- Use cases/interactors for business logic
- Data sources (local and remote)

**Libraries:**
- Jetpack Compose for UI
- Room for local database
- Retrofit for API calls
- Coil for image loading (if needed)
- Hilt for dependency injection
- Kotlinx Coroutines for async operations
- Kotlinx Serialization for JSON

### Storage Strategy

**Local Storage:**
- Room database (SQLite) instead of JSON files
- Define entities for Recipe, Ingredient, MealPlan, MealSlot
- Use relationships and foreign keys
- Support for complex queries

**Alternative:** Keep JSON file approach if preferred:
- Store in app private storage
- Use Kotlin data classes with @Serializable

### API Integration

**Anthropic API:**
- Use Retrofit for HTTP requests
- OkHttp for request/response interceptors
- Handle multipart uploads for images
- Implement retry logic with exponential backoff

**Firebase Setup:**
- Add Firebase to Android project
- Configure Realtime Database or Firestore
- Set up authentication
- Define security rules (private user data)

### Testing Recommendations

**Unit Tests:**
- ViewModels
- Repositories
- Use cases
- JSON serialization/deserialization

**Integration Tests:**
- Database operations
- API calls (with mock server)
- Import/export functionality

**UI Tests:**
- Critical user flows with Compose testing

---

## Appendix: Example Scenarios

### Scenario 1: User Adds Recipe from Website

1. User opens app, taps "Add Recipe"
2. User selects "Web URL"
3. User pastes recipe URL: `https://example.com/recipes/carbonara`
4. App shows loading indicator
5. App fetches HTML content
6. App sends to Claude API with prompt to extract recipe
7. Claude returns structured recipe data in English with metric units
8. App displays recipe for review
9. User sees title: "Spaghetti Carbonara"
10. User sees ingredients: "400g spaghetti", "200g pancetta", etc.
11. User confirms
12. App auto-translates to Swedish and Romanian in background
13. Recipe saved and appears in recipe list

### Scenario 2: User Plans Meals for Week

1. User switches to Meal Planner tab
2. User scrolls to Monday of next week
3. User taps empty lunch slot
4. Recipe picker opens
5. User searches "salad"
6. User selects "Caesar Salad"
7. Recipe assigned to Monday lunch
8. User taps empty dinner slot
9. User selects "Chicken Pasta"
10. Recipe assigned to Monday dinner
11. User repeats for other days
12. User taps share button
13. User selects "This Week"
14. User enables "Include Shopping List"
15. User selects "Text"
16. App generates summary with aggregated ingredients
17. User shares via messaging app

### Scenario 3: User Migrates from iOS to Android

1. User opens iOS app
2. User goes to Settings
3. User taps "Export Data"
4. User selects "Export All" (recipes + meal plans)
5. User saves file or shares via email
6. User receives file on Android device
7. User opens Android app (first time)
8. User taps "Import Data"
9. User selects exported JSON file
10. App validates file structure
11. App shows preview: "45 recipes, 12 meal plans"
12. User confirms import
13. App imports all data
14. User sees all recipes in recipe list
15. User sees meal plans in planner
16. User configures API key
17. User enables cloud sync
18. Data syncs to Firebase

---

## Version History

- **1.0** (2026-01-06) - Initial specification for cross-platform implementation

---

## Questions for Implementation Team

When implementing the Android version, consider:

1. **Database vs. JSON Files:** Use Room database or maintain JSON file approach?
2. **Cloud Backend:** Firebase Realtime Database or Firestore?
3. **Image Handling:** Need temporary image storage or direct upload to API?
4. **Background Sync:** Use WorkManager for periodic sync?
5. **Widget Support:** Add home screen widget for meal plan?
6. **Material Design:** Follow Material 3 design guidelines?
7. **Tablet Support:** Optimize layouts for larger screens?
8. **Wear OS:** Support smartwatch companion app?

---

**End of Functional Specification**
