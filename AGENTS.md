# Inputctl Architecture & Developer Notes

Welcome to the **Inputctl** developer guide. This document outlines the core architectural decisions, system-level integrations, and UI/UX paradigms used in this project. Because Inputctl operates as a system-level application on BlissOS, it interacts with Android's framework and kernel layers in ways that bypass standard SDK limitations.

## 1. System Integration & Build Configuration

### 1.1. In-Tree Build (`platform_apis: true`)

Inputctl is built in-tree using `Android.bp` with the `platform_apis: true` flag.

* **Why:** This exposes `@hide` framework APIs. Instead of relying on complex AIDL/Binder IPC workarounds (like `IInputManager.Stub`) or Java reflection, we can directly use native system wrappers like `InputManager.registerOnTabletModeChangedListener()`.
* **Benefit:** Cleaner codebase, guaranteed compatibility with the underlying OS framework, and automatic thread handling (by passing a `Handler` directly to the listener).

### 1.2. Permissions & System UID

The application declares `android:sharedUserId="android.uid.system"` in the `AndroidManifest.xml` and includes a `privapp-permissions-inputctl.xml` configuration.

* **Why:** The `android.permission.TABLET_MODE` permission is protected at the `signature` level. Without the system UID and proper priv-app whitelisting, `InputManagerService` will throw a `SecurityException` when the app attempts to listen for hardware state changes or disable devices.

### 1.3. Service Implementation

`InputctlService` is a "Started Service" (triggered via `BootReceiver` and running in the background).

* **Quirk:** Due to Java's abstract class constraints for `android.app.Service`, we must override the `onBind()` method. Since this service does not support external binding, it strictly returns `null`.

## 2. Hardware Interaction (InputManager & EventHub)

### 2.1. The `-1` Device ID Wildcard

When querying the system for the current tablet mode state (e.g., in `InputDeviceManagerHelper.java`), **do not** iterate through a list of all physical devices.

* **Implementation:** Use `mInputManager.getSwitchState(-1, InputDevice.SOURCE_ANY, SW_TABLET_MODE)`.
* **Why:** In the Linux Kernel and Android's `EventHub`, `-1` acts as a wildcard. The native C++ layer will automatically scan all available `/dev/input/` nodes and return `1` (KEY_STATE_DOWN) if *any* device triggers the tablet mode switch. This mimics AOSP's internal implementation and is highly optimized.

### 2.2. Boot State Synchronization

The `InputManager.OnTabletModeChangedListener` only fires upon *state transitions* (e.g., flipping the hinge). If the device boots while already folded, the listener remains silent. Therefore, `InputctlService` actively checks the initial hardware state via `mHelper.isCurrentlyInTabletMode()` during `onCreate()` to ensure immediate synchronization.

## 3. UI/UX & Settings Dashboard Injection

### 3.1. Settings Dashboard Injection

Inputctl does not appear in the standard app drawer. It injects itself directly into the Android Settings app using `com.android.settings.action.EXTRA_SETTINGS` in the `InputctlActivity`.

* **Icon Tinting:** To ensure the app icon adapts to the system's Accent Color (Dark/Light mode), the `<meta-data android:name="com.android.settings.icon_tintable" android:value="true" />` is declared in the manifest.

### 3.2. Vector Asset Theming

When using standard Material Vector assets (e.g., `ic_tune.xml`, `ic_block.xml`, `ic_devices_fold_2.xml`), strict rules apply for system-level dynamic tinting:

1. **Base Color:** The `android:fillColor` inside the `<path>` is hardcoded to `#FF000000` (Solid Black).
2. **Tint Namespace:** The vector must include `android:tint="?android:attr/colorControlNormal"`.

* *Warning:* Ensure the `android:` prefix is present in the `tint` attribute. Otherwise, the system framework (`Theme.DeviceDefault`) will ignore the tint command and render a solid black icon in Dark Mode.

### 3.3. Preference Alignment & Margins

To fix the `androidx.preference` "ghost padding" issue without hardcoding layout margins:

* All functional preference categories utilize icons.
* The custom `AboutPreference` layout (`layout_about_preference.xml`) uses `android:paddingStart="72dp"` to perfectly align its text with the icon-bearing preferences above it, ensuring standard AOSP visual compliance.



## 4. State Management & "Poka-Yoke" Safeguards

### 4.1. Race Condition Prevention (Tablet Mode vs. Soft Block)

`InputctlService` cross-references the auto-disable list with the manual Soft Block list. When a device unfolds (leaving Tablet Mode), the service verifies that the keyboard/touchpad is **not** present in the Soft Block list before calling `enableDevice()`. This prevents the system from accidentally waking a device that the user explicitly wanted dead.

### 4.2. UI Safeguard Logic

To prevent users from accidentally locking themselves out of the device, the `ManualBlacklistFragment` implements strict interception logic:

* **UI Freezing:** In `SwitchPreference.setOnPreferenceChangeListener`, if the user attempts to disable a critical device (or if the Safeguard toggle is active), the method immediately returns `false`.
* **Asynchronous Resolution:** This freezes the UI switch in its current state and displays an `AlertDialog`. Only upon positive confirmation does the code programmatically apply the block and update the UI visually (`preference.setChecked(false)`). This guarantees that the UI state strictly reflects the underlying hardware state.
