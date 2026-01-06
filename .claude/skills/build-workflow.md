# Build Workflow Skill

Creates a GitHub Actions workflow for building and uploading Android APKs.

## Instructions

When this skill is invoked:

1. Ask the user for:
   - Package name (e.g., `com.example.myapp`)
   - App name (e.g., `My App`)

2. Create the file `.github/workflows/build-and-upload.yml` with the following content, replacing `{{PACKAGE_NAME}}` and `{{APP_NAME}}` with the user-provided values:

```yaml
name: Build and Upload APK

on:
  workflow_dispatch:
    inputs:
      bump_type:
        description: 'Version bump type'
        required: true
        default: 'patch'
        type: choice
        options:
          - patch
          - minor
          - major
      release_notes:
        description: 'Release notes (optional)'
        required: false
        type: string

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: write

    steps:
      - uses: actions/checkout@v4
        with:
          token: ${{ secrets.GITHUB_TOKEN }}

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          cache-read-only: false

      - name: Bump version
        id: bump
        run: |
          # Read current version from build.gradle.kts
          CURRENT_VERSION=$(grep 'versionName = ' app/build.gradle.kts | sed 's/.*versionName = "\(.*\)"/\1/')
          CURRENT_CODE=$(grep 'versionCode = ' app/build.gradle.kts | sed 's/.*versionCode = \([0-9]*\)/\1/')

          echo "Current version: $CURRENT_VERSION (code: $CURRENT_CODE)"

          # Parse version components
          IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"

          # Bump based on input
          case "${{ inputs.bump_type }}" in
            major)
              MAJOR=$((MAJOR + 1))
              MINOR=0
              PATCH=0
              ;;
            minor)
              MINOR=$((MINOR + 1))
              PATCH=0
              ;;
            patch)
              PATCH=$((PATCH + 1))
              ;;
          esac

          NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}"
          NEW_CODE=$((CURRENT_CODE + 1))

          echo "New version: $NEW_VERSION (code: $NEW_CODE)"

          # Update build.gradle.kts
          sed -i "s/versionCode = $CURRENT_CODE/versionCode = $NEW_CODE/" app/build.gradle.kts
          sed -i "s/versionName = \"$CURRENT_VERSION\"/versionName = \"$NEW_VERSION\"/" app/build.gradle.kts

          # Set outputs
          echo "version=$NEW_VERSION" >> $GITHUB_OUTPUT
          echo "code=$NEW_CODE" >> $GITHUB_OUTPUT

      - name: Commit version bump
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add app/build.gradle.kts
          git commit -m "Bump version to ${{ steps.bump.outputs.version }}"
          git tag "v${{ steps.bump.outputs.version }}"
          git push origin HEAD --tags

      - name: Decode Keystore
        run: |
          echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > ${{ github.workspace }}/release.keystore

      - name: Build Release APK
        env:
          KEYSTORE_FILE: ${{ github.workspace }}/release.keystore
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew assembleRelease

      - name: Upload to App Hosting
        run: |
          NOTES="${{ inputs.release_notes }}"
          if [ -z "$NOTES" ]; then
            NOTES="Release v${{ steps.bump.outputs.version }}"
          fi

          # Capture both HTTP status code and response body
          HTTP_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
            -H "X-API-Key: ${{ secrets.APP_HOSTING_API_KEY }}" \
            -F "apk=@app/build/outputs/apk/release/app-release.apk" \
            -F "packageName={{PACKAGE_NAME}}" \
            -F "appName={{APP_NAME}}" \
            -F "versionName=${{ steps.bump.outputs.version }}" \
            -F "versionCode=${{ steps.bump.outputs.code }}" \
            -F "releaseNotes=$NOTES" \
            ${{ secrets.APP_HOSTING_URL }}/api/upload)

          # Extract HTTP status code (last line) and response body
          HTTP_STATUS=$(echo "$HTTP_RESPONSE" | tail -n1)
          RESPONSE_BODY=$(echo "$HTTP_RESPONSE" | sed '$d')

          echo "Response: $RESPONSE_BODY"
          echo "HTTP Status: $HTTP_STATUS"

          # Check for HTTP errors (non-2xx status codes)
          if [[ ! "$HTTP_STATUS" =~ ^2 ]]; then
            echo "::error::Upload failed with HTTP status $HTTP_STATUS"
            exit 1
          fi

          # Check for error in response body
          if echo "$RESPONSE_BODY" | grep -q '"error"'; then
            echo "::error::Upload failed: $RESPONSE_BODY"
            exit 1
          fi

          echo "Upload successful!"

      - name: Clean up keystore
        if: always()
        run: rm -f ${{ github.workspace }}/release.keystore
```

3. After creating the workflow file, remind the user to configure the required repository secrets:
   - `KEYSTORE_BASE64`
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`
   - `APP_HOSTING_API_KEY`
   - `APP_HOSTING_URL`
