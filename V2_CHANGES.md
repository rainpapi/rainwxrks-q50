# RainWxrks Q50 V2

Implemented in this source baseline:

- HOME redesigned around a large MPH readout.
- RPM removed from the HOME screen.
- HOME now has Boost, IAT, Coolant and Voltage slots.
- Dynamic discovery for sensors whose vendor names expose Boost/Turbo, IAT/Intake Air, or Battery/System/Charging Voltage.
- Added RAIN CHECK screen with Scan Vehicle, Clear Codes and Scan History entry points.
- Added a DiagnosticManager boundary that does not fabricate DTCs; it reports the current sensor-only interface limitation.
- Added Performance screen shell for 0-60, 0-100, 1/8 mile, 1/4 mile, G-Force and Drive Recorder.
- Added Settings screen shell.
- Added shared VehicleData model.
- Main activity now routes between Home, Rain Check, Performance and Settings.
- Manifest uses singleTask launch mode for clean App Garage activity reuse.
- Version bumped to 2.0.

## Important

The current project exposes vehicle CAN values as Android sensors. It does not document a DTC
read/write API. Therefore V2 includes the RAIN CHECK UI and a diagnostic transport boundary, but it
must not claim to read or clear codes until the Q50 interface actually exposes those commands.

Auto-start remains dependent on the head unit's App Garage Auto Start Setting. The APK cannot force a
firmware-level auto-start policy that the head unit does not expose.
