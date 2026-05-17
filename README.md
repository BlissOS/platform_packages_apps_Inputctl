# Inputctl

A sophisticated input hardware management tool for Android. Made for [BlissOS](https://blissos.org/) first and foremost.

## Features

- Manual Blacklist inputs with 2 modes:
  - Soft Blocking: apply InputManager.disableInputDevice() at boot or on click. This method is quick & immediately, however because it rely on a service to disable the input at boot, inputs will still be active until the service kick in.
  - Hard Blocking: basically create an xml that is equivalent to `excluded-input-devices.xml` file. This method provide a more robust blocking. However you'll have to reboot the device whenever you're done selecting inputs.
- Automatically disable inputs using Tablet Mode Switch:
  - Convertible 2-in-1 laptops with touchscreens usually provide a sensor. On Linux this sensor provides an input event called `SW_TABLET_MODE` that can be used to detect whenever the laptop is being flipped or not to automatically disable some components such as the internal keyboard, touchpad or trackpoint. We brought this same mechanism to Inputctl, but inputs have to choose manually.
- Show a toast whenever the device is flipped to indicate that X inputs has been disabled.
- You can find the app under Settings => System => Inputctl.

## Integration

- Sync this repo to `packages/apps/Inputctl`.
- Add `Inputctl` to your `PRODUCT_PACKAGES`:
```make
PRODUCT_PACKAGES += \
    Inputctl
```
- You'll need to apply these patches to `frameworks/base` for some features to work:
  - For Hard Blocking: [InputManagerService: read a custom excluded-input-devices.xml file at /data/system](https://github.com/BlissRoms-x86/platform_frameworks_base/commit/98d124b48afc75e615b7907b7a68f02c557880f3)
  - Allow Inputctl to check which input has a Tablet Mode Switch : [core: Expose method to get switch state from inputs](https://github.com/BlissRoms-x86/platform_frameworks_base/commit/70f37bd3c2335e78a5825db2998ff30c38b593c3)

## License

This is an open-source project by BlissLabs (https://blisslabs.org), released and protected under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

Copyright © 2026 BlissLabs.
