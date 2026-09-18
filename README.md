# RainWxrks Q50

This repository contains the source for the RainWxrks Q50 App Garage application.

## Important
The checked-in `q50.epk` was an incomplete 268-byte placeholder, not a real application package. It has been removed from this fixed source bundle.

The installable EPK must be generated from the current Java source and APK by `build.sh`. The build script now:
1. compiles `MainActivity.java` and `GaugeView.java`
2. creates the APK
3. signs it
4. wraps that exact APK with `tools/epk_tool.py`
5. copies the resulting EPK to the repository root as `q50.epk`

Install the newly generated EPK from the GitHub Actions artifact, not the old placeholder.

## UI source
The on-screen dashboard is defined entirely in:
`src/com/rainwxrks/q50/GaugeView.java`

`MainActivity.java` simply creates `GaugeView` and displays it. Therefore, if the installed app shows a different dashboard, that APK/EPK was not built from this source revision.
