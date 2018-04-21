# droidpuppet

Low-latency screen mirroring and input simulation (over adb) for not-rooted Android 5.0+ devices

## Start

Compile and install apk on your phone, then run the following commands via `adb shell`:

```bash
PACKAGE=`pm path org.breeswish.droidpuppet`
CLASSPATH=${PACKAGE#*:} app_process /system/bin org.breeswish.droidpuppet.Main my_token
```

Since the application will connect to `localabstract:droidpuppet_endpoint` on the phone, you may want to make a port forward first. For example, let the application connecting to port 12345 on the host:

```bash
adb reverse localabstract:droidpuppet_endpoint tcp:12345
```

## Protocol

See the [protobuf file](./app/src/main/proto/protocol.proto) for details.

General flows:

1. Application connect to the host.

2. Application send `ClientGreeting` packet to the host.

3. Host send `StreamRequest` packet to the application with desired streaming configurations.

4. Application continuously send `Frame` packet:

   - `type == VIDEO`: A single video frame of screen content.

   - `type == STREAM_RESTART`: The screen orientation is changed and the `Frame` packet follows will belong to a new video stream. Normally you will expect a width / height change.

## Develop

This project heavily uses `@hide` APIs in Android SDK. You need to grab and replace your SDK jar from [here](https://github.com/anggrayudi/android-hidden-api) in order to compile.

## Technical Details

VirtualDisplay + MediaCodec + LocalSocket + adb shell

`adb shell` plays an important role. By starting the application from `adb shell` you will be `shell` user and can:

- Capture screen without an authorization popup.

- Simulate input events without being a DeviceManager.

## Alternatives

- [vysor](https://www.vysor.io)

- [scrcpy](https://github.com/Genymobile/scrcpy)

- [minicap](https://github.com/openstf/minicap)
