#!/bin/bash

# Event Monitor - Complete Branch → Venue Refactoring Script
# This script completes the remaining file updates for the venue refactoring

set -e

PROJECT_ROOT="/Users/nick/StudioProjects/android-cop-head-counter"
cd "$PROJECT_ROOT"

echo "========================================="
echo "Event Monitor: Completing Venue Refactoring"
echo "========================================="
echo ""

# Step 1: Rename branches directory to venues
echo "[1/6] Renaming branches directory to venues..."
if [ -d "app/src/main/java/com/eventmonitor/app/presentation/screens/branches" ]; then
    mv app/src/main/java/com/eventmonitor/app/presentation/screens/branches \
       app/src/main/java/com/eventmonitor/app/presentation/screens/venues
    echo "✓ Directory renamed successfully"
else
    echo "⚠ Branches directory not found or already renamed"
fi

# Step 2: Rename files in venues directory
echo ""
echo "[2/6] Renaming files in venues directory..."
cd app/src/main/java/com/eventmonitor/app/presentation/screens/venues 2>/dev/null || echo "⚠ Venues directory not found"

if [ -f "BranchListScreen.kt" ]; then
    mv BranchListScreen.kt VenueListScreen.kt
    echo "✓ BranchListScreen.kt → VenueListScreen.kt"
fi

if [ -f "BranchListViewModel.kt" ]; then
    mv BranchListViewModel.kt VenueListViewModel.kt
    echo "✓ BranchListViewModel.kt → VenueListViewModel.kt"
fi

if [ -f "BranchSetupScreen.kt" ]; then
    mv BranchSetupScreen.kt VenueSetupScreen.kt
    echo "✓ BranchSetupScreen.kt → VenueSetupScreen.kt"
fi

if [ -f "BranchSetupViewModel.kt" ]; then
    mv BranchSetupViewModel.kt VenueSetupViewModel.kt
    echo "✓ BranchSetupViewModel.kt → VenueSetupViewModel.kt"
fi

cd "$PROJECT_ROOT"

# Step 3: Update file contents using sed (batch replace)
echo ""
echo "[3/6] Updating file contents (this may take a moment)..."

# Find all Kotlin files excluding build directories
find . -name "*.kt" -type f ! -path "*/build/*" ! -path "*/.gradle/*" ! -path "*/generated/*" | while read file; do
    # Skip if file contains database migration SQL (preserve backward compatibility)
    if [[ "$file" == *"Migrations.kt"* ]]; then
        continue
    fi

    # Perform replacements
    sed -i '' -e 's/\bbranchId\b/venueId/g' "$file"
    sed -i '' -e 's/\bBranchRepository\b/VenueRepository/g' "$file"
    sed -i '' -e 's/\bBranchRepositoryImpl\b/VenueRepositoryImpl/g' "$file"
    sed -i '' -e 's/\bBranchDao\b/VenueDao/g' "$file"
    sed -i '' -e 's/\bBranchEntity\b/VenueEntity/g' "$file"
    sed -i '' -e 's/\bBranchWithAreas\b/VenueWithAreas/g' "$file"
    sed -i '' -e 's/\bBranchListScreen\b/VenueListScreen/g' "$file"
    sed -i '' -e 's/\bBranchListViewModel\b/VenueListViewModel/g' "$file"
    sed -i '' -e 's/\bBranchSetupScreen\b/VenueSetupScreen/g' "$file"
    sed -i '' -e 's/\bBranchSetupViewModel\b/VenueSetupViewModel/g' "$file"
    sed -i '' -e 's/getAreasByBranch/getAreasByVenue/g' "$file"
    sed -i '' -e 's/getIncidentsByBranch/getIncidentsByVenue/g' "$file"
    sed -i '' -e 's/getRecentServicesByBranch/getRecentEventsByVenue/g' "$file"
    sed -i '' -e 's/getServicesByBranchAndDateRange/getEventsByVenueAndDateRange/g' "$file"
    sed -i '' -e 's/getServicesAcrossBranches/getEventsAcrossVenues/g' "$file"
    sed -i '' -e 's/getTotalCapacityForBranch/getTotalCapacityForVenue/g' "$file"
    sed -i '' -e 's/getIncidentsByBranchAndStatus/getIncidentsByVenueAndStatus/g' "$file"
    sed -i '' -e 's/getIncidentsByBranchAndSeverity/getIncidentsByVenueAndSeverity/g' "$file"
    sed -i '' -e 's/getActiveIncidentCountByBranch/getActiveIncidentCountByVenue/g' "$file"
    sed -i '' -e 's/deleteAllAreasForBranch/deleteAllAreasForVenue/g' "$file"
    sed -i '' -e 's/duplicateAreaToBranches/duplicateAreaToVenues/g' "$file"
    sed -i '' -e 's/targetBranchIds/targetVenueIds/g' "$file"
    sed -i '' -e 's/exportBranchComparisonReport/exportVenueComparisonReport/g' "$file"
    sed -i '' -e 's/provideBranchDao/provideVenueDao/g' "$file"
    sed -i '' -e 's/bindBranchRepository/bindVenueRepository/g' "$file"
    sed -i '' -e 's/createDefaultAreasForBranch/createDefaultAreasForVenue/g' "$file"
    sed -i '' -e 's/hasServices/hasEvents/g' "$file"
done

echo "✓ File contents updated"

# Step 4: Delete old Branch* files
echo ""
echo "[4/6] Deleting old Branch* files..."

rm -f core/data/src/main/java/com/eventmonitor/core/data/local/entities/BranchEntity.kt
rm -f core/data/src/main/java/com/eventmonitor/core/data/local/dao/BranchDao.kt
rm -f core/data/src/main/java/com/eventmonitor/core/data/repository/interfaces/BranchRepository.kt
rm -f core/data/src/main/java/com/eventmonitor/core/data/repository/BranchRepositoryImpl.kt

echo "✓ Old files deleted"

# Step 5: Clean build
echo ""
echo "[5/6] Cleaning build..."
./gradlew clean

echo "✓ Build cleaned"

# Step 6: Compile and verify
echo ""
echo "[6/6] Compiling project..."
./gradlew assembleDebug

echo ""
echo "========================================="
echo "✓ Refactoring Complete!"
echo "========================================="
echo ""
echo "Next steps:"
echo "1. Review the changes in version control"
echo "2. Run tests: ./gradlew test"
echo "3. Run the app and verify functionality"
echo "4. Check VENUE_REFACTORING_SUMMARY.md for details"
echo ""
