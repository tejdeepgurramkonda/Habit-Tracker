# Statistics Integration Setup Guide

## Overview
This guide covers the setup for Health Connect (primary) and Google Fit (fallback) integration for the Habit Tracker app's Statistics feature.

## Health Connect Setup

### 1. Health Connect Availability Check
Health Connect is available on Android 14+ (API 34+) devices or through the Google Play Store app on Android 8+ devices.

```kotlin
// Check Health Connect availability
val healthConnectClient = HealthConnectClient.getOrCreate(context)
val availabilityStatus = HealthConnectClient.getSdkStatus(context)
```

### 2. Required Permissions
The app requests the following Health Connect permissions:
- `android.permission.health.READ_STEPS`
- `android.permission.health.READ_DISTANCE`
- `android.permission.health.READ_TOTAL_CALORIES_BURNED`
- `android.permission.health.READ_ACTIVE_CALORIES_BURNED`
- `android.permission.health.READ_EXERCISE`
- `android.permission.health.READ_SLEEP`
- `android.permission.health.READ_HEART_RATE`

### 3. Privacy Policy Requirements
Ensure your privacy policy includes:
- What health data is collected
- How the data is used
- Data retention policies
- User rights and controls

## Google Fit Setup (Fallback)

### 1. Google Cloud Console Setup
1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create or select a project
3. Enable the Fitness API
4. Create OAuth 2.0 credentials:
   - Application type: Android
   - Package name: `com.example.habittrackerr`
   - SHA-1 certificate fingerprint: (your debug/release SHA-1)

### 2. Required OAuth Scopes
```
https://www.googleapis.com/auth/fitness.activity.read
https://www.googleapis.com/auth/fitness.body.read
https://www.googleapis.com/auth/fitness.location.read
```

### 3. Verification Requirements
For sensitive scopes, Google may require app verification:
- Submit for OAuth verification if using sensitive scopes
- Provide privacy policy and terms of service
- Complete security assessment if required

## Firestore Database Setup

### 1. Required Composite Indexes

Create the following composite indexes in Firebase Console:

#### Index 1: Habits Query with Soft Delete
- **Collection ID**: `habits`
- **Fields**: 
  - `userId` (Ascending)
  - `isDeleted` (Ascending) 
  - `createdAt` (Descending)
- **Query scope**: Collection

#### Index 2: Task Events Query
- **Collection ID**: `task_events`
- **Fields**:
  - `userId` (Ascending)
  - `taskId` (Ascending)
  - `timestamp` (Descending)
- **Query scope**: Collection

#### Index 3: Fitness Data Query
- **Collection ID**: `fitness_data`
- **Fields**:
  - `userId` (Ascending)
  - `date` (Descending)
  - `dataType` (Ascending)
- **Query scope**: Collection

### 2. Firestore Rules Update
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Habits collection with soft delete support
    match /habits/{habitId} {
      allow read, write: if request.auth != null && 
        resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && 
        request.resource.data.userId == request.auth.uid;
    }
    
    // Task events collection
    match /task_events/{eventId} {
      allow read, write: if request.auth != null && 
        resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && 
        request.resource.data.userId == request.auth.uid;
    }
    
    // Fitness data collection (metadata only)
    match /fitness_data/{dataId} {
      allow read, write: if request.auth != null && 
        resource.data.userId == request.auth.uid;
      allow create: if request.auth != null && 
        request.resource.data.userId == request.auth.uid;
    }
  }
}
```

## Database Migration

### Room Migration Script
A Room migration is included to add soft delete support:
- Adds `isDeleted: Boolean` column to `habits` table
- Adds `deletedAt: Long?` column to `habits` table
- Creates new `task_events` table for analytics
- Creates new `fitness_data` table for cached health data

### Migration Rollback
If rollback is needed:
1. Remove `isDeleted` and `deletedAt` columns from habits table
2. Drop `task_events` and `fitness_data` tables
3. Revert app to previous version

## Testing Checklist

### Health Connect Testing
- [ ] Install Health Connect app from Play Store
- [ ] Grant all required permissions
- [ ] Verify data reads correctly
- [ ] Test permission revocation handling
- [ ] Test with Health Connect unavailable

### Google Fit Testing  
- [ ] Configure OAuth credentials correctly
- [ ] Test authentication flow
- [ ] Verify fallback activation when Health Connect unavailable
- [ ] Test with limited permissions

### Data Integration Testing
- [ ] Verify Room database migration completes
- [ ] Test analytics computation with various data patterns
- [ ] Verify Firestore sync works correctly
- [ ] Test offline behavior

## Privacy & Compliance

### User Controls
- Clear explanation of data collection
- Toggle switches for Health Connect vs Google Fit
- Data export functionality
- Data deletion options

### Data Handling
- Health data stored locally in Room database
- Only aggregated metadata synced to Firestore
- No raw health data sent to remote servers
- Comply with GDPR, CCPA, and health data regulations

## Troubleshooting

### Common Issues
1. **Health Connect not available**: App falls back to Google Fit
2. **Permissions denied**: Show educational dialog explaining benefits
3. **Google Fit quota exceeded**: Implement exponential backoff
4. **Firestore index errors**: Create required composite indexes
5. **Migration failures**: Implement rollback mechanism

### Debug Configuration
Add to `local.properties` for testing:
```
enable_google_fit_fallback=true
health_connect_debug_mode=true
fitness_sync_interval_minutes=15
```
