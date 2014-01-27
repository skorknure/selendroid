/*
 * Copyright 2012-2013 eBay Software Foundation and selendroid committers.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.selendroid.server.model;

import io.selendroid.SelendroidCapabilities;
import io.selendroid.SelendroidConfiguration;
import io.selendroid.android.AndroidApp;
import io.selendroid.android.AndroidDevice;
import io.selendroid.android.AndroidEmulator;
import io.selendroid.android.AndroidSdk;
import io.selendroid.android.DeviceManager;
import io.selendroid.android.HardwareDeviceListener;
import io.selendroid.android.impl.DefaultAndroidEmulator;
import io.selendroid.android.impl.DefaultDeviceManager;
import io.selendroid.android.impl.DefaultHardwareDevice;
import io.selendroid.android.impl.InstalledAndroidApp;
import io.selendroid.builder.AndroidDriverAPKBuilder;
import io.selendroid.builder.SelendroidServerBuilder;
import io.selendroid.exceptions.AndroidDeviceException;
import io.selendroid.exceptions.AndroidSdkException;
import io.selendroid.exceptions.DeviceStoreException;
import io.selendroid.exceptions.SelendroidException;
import io.selendroid.exceptions.SessionNotCreatedException;
import io.selendroid.exceptions.ShellCommandException;
import io.selendroid.server.ServerDetails;
import io.selendroid.server.util.HttpClientUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.beust.jcommander.internal.Lists;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class SelendroidStandaloneDriver implements ServerDetails {
  public static final String WD_RESP_KEY_VALUE = "value";
  public static final String WD_RESP_KEY_STATUS = "status";
  public static final String WD_RESP_KEY_SESSION_ID = "sessionId";
  private static int selendroidServerPort = 38080;
  private static final Logger log = Logger.getLogger(SelendroidStandaloneDriver.class.getName());
  private Map<String, AndroidApp> appsStore = new HashMap<String, AndroidApp>();
  private Map<String, AndroidApp> appsToInstall = new HashMap<String, AndroidApp>();
  private Map<String, AndroidApp> selendroidServers = new HashMap<String, AndroidApp>();
  private Map<String, ActiveSession> sessions = new HashMap<String, ActiveSession>();
  private DeviceStore deviceStore = null;
  private SelendroidServerBuilder selendroidApkBuilder = null;
  private AndroidDriverAPKBuilder androidDriverAPKBuilder = null;
  private SelendroidConfiguration serverConfiguration = null;
  private HardwareDeviceListener hardwareDeviceListener = null;
  private DeviceManager hardwareDeviceManager;

  public SelendroidStandaloneDriver(SelendroidConfiguration serverConfiguration)
      throws AndroidSdkException, AndroidDeviceException {
    this.serverConfiguration = serverConfiguration;
    selendroidApkBuilder = new SelendroidServerBuilder(serverConfiguration);
    androidDriverAPKBuilder = new AndroidDriverAPKBuilder();

    selendroidServerPort = serverConfiguration.getSelendroidServerPort();

    initApplicationsUnderTest(serverConfiguration);
    initAndroidDevices();
    deviceStore.setClearData(!serverConfiguration.isNoClearData());
  }

  /**
   * For testing only
   */
  SelendroidStandaloneDriver(SelendroidServerBuilder builder, DeviceManager deviceManager,
                             AndroidDriverAPKBuilder androidDriverAPKBuilder) {
    this.selendroidApkBuilder = builder;
    hardwareDeviceManager = deviceManager;
    this.androidDriverAPKBuilder = androidDriverAPKBuilder;
  }

  /* package */void initApplicationsUnderTest(SelendroidConfiguration serverConfiguration)
      throws AndroidSdkException {
    if (serverConfiguration == null) {
      throw new SelendroidException("Configuration error - serverConfiguration can't be null.");
    }
    this.serverConfiguration = serverConfiguration;

    // each of the apps specified on the command line need to get resigned
    // and 'stored' to be installed on the device
    for (String appPath : serverConfiguration.getSupportedApps()) {
      File file = new File(appPath);
      if (file.exists()) {

        AndroidApp app = null;
        try {
          app = selendroidApkBuilder.resignApp(file);
        } catch (ShellCommandException e1) {
          throw new SessionNotCreatedException("An error occurred while resigning the app '"
              + file.getName() + "'. ", e1);
        }
        String appId = null;
        try {
          appId = app.getAppId();
        } catch (SelendroidException e) {
          log.info("Ignoring app because an error occurred reading the app details: "
              + file.getAbsolutePath());
          log.info(e.getMessage());
        }
        if (appId != null && !appsStore.containsKey(appId)) {
          appsStore.put(appId, app);
          appsToInstall.put(appId, app);
          log.info("App " + appId + " has been added to selendroid standalone server.");
        }
      } else {
        log.severe("Ignoring app because it was not found: " + file.getAbsolutePath());
      }
    }
    if (serverConfiguration.getInstalledApp() != null) {
      AndroidApp app = new InstalledAndroidApp(serverConfiguration.getInstalledApp());
      appsStore.put(app.getAppId(), app);
    }

    if (!serverConfiguration.isNoWebViewApp()) {

      // extract the 'AndroidDriver' app and show it as available
      try {
        // using "android" as the app name, because that is the desired capability default in selenium for
        // DesiredCapabilities.ANDROID
        AndroidApp app = selendroidApkBuilder.resignApp(androidDriverAPKBuilder.extractAndroidDriverAPK());
        appsStore.put(BrowserType.ANDROID, app);
        appsToInstall.put(BrowserType.ANDROID, app);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else if (appsStore.isEmpty()) {
      // note this only happens now when someone uses -noWebViewApp & forgets to specify -aut/app
      // (or the app doesn't exist or some other error condition above ^ )
      throw new SelendroidException(
          "Fatal error initializing SelendroidDriver: configured app(s) have not been found.");
    }

  }

  /* package */void initAndroidDevices() throws AndroidDeviceException {
    deviceStore =
        new DeviceStore(serverConfiguration.isVerbose(), serverConfiguration.getEmulatorPort());

    if (hardwareDeviceListener == null) {
      hardwareDeviceListener = new DefaultHardwareDeviceListener(deviceStore);
    }
    hardwareDeviceManager =
        new DefaultDeviceManager(AndroidSdk.adb().getAbsolutePath(),
            serverConfiguration.shouldKeepAdbAlive());
    hardwareDeviceManager.initialize(hardwareDeviceListener);

    List<AndroidEmulator> emulators = DefaultAndroidEmulator.listAvailableAvds();

    deviceStore.addEmulators(emulators, serverConfiguration.getInstalledApp() != null);

    if (deviceStore.getDevices().isEmpty()) {
      SelendroidException e =
          new SelendroidException(
              "No android virtual devices were found. "
                  + "Please start the android tool and create emulators and restart the selendroid-standalone "
                  + "or plugin an Android hardware device via USB.");
      log.warning("Warning: " + e);
    }
  }


  private String getAvdName(int port) {
    if (port == -1) {
      return null;
    }
    String cmd = "(sleep 4; echo 'avd name') | telnet localhost " + port;
    try {
      Process proc = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", cmd});
      try {
        proc.waitFor();
      } catch (InterruptedException e) {}
      BufferedReader read = new BufferedReader(new InputStreamReader(proc.getInputStream()));
      while (read.ready()) {
        if (read.readLine().equals("OK")) {
          break;
        }
      }
      return read.readLine();
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public String getServerVersion() {
    return selendroidApkBuilder.getJarVersionNumber();
  }

  @Override
  public String getCpuArch() {
    String arch = System.getProperty("os.arch");
    return arch;
  }

  @Override
  public String getOsVersion() {
    String os = System.getProperty("os.version");
    return os;
  }

  @Override
  public String getOsName() {
    String os = System.getProperty("os.name");
    return os;
  }

  public String createNewTestSession(JSONObject caps, Integer retries) throws AndroidSdkException,
      JSONException {
    SelendroidCapabilities desiredCapabilities = null;

    // Convert the JSON capabilities to SelendroidCapabilities
    try {
      desiredCapabilities = new SelendroidCapabilities(caps);
    } catch (JSONException e) {
      throw new SelendroidException("Desired capabilities cannot be parsed.");
    }

    // Find the App being requested for use
    AndroidApp app = appsStore.get(desiredCapabilities.getAut());
    if (app == null) {
      throw new SessionNotCreatedException(
          "The requested application under test is not configured in selendroid server.");
    }

    // Find a device to match the capabilities
    AndroidDevice device = null;
    try {
      device = getAndroidDevice(desiredCapabilities);
    } catch (AndroidDeviceException e) {
      SessionNotCreatedException error =
          new SessionNotCreatedException("Error occured while finding android device: "
              + e.getMessage());
      e.printStackTrace();
      log.severe(error.getMessage());
      throw error;
    }

    // If we are using an emulator need to start it up
    if (device instanceof AndroidEmulator) {
      AndroidEmulator emulator = (AndroidEmulator) device;
      try {
        if (emulator.isEmulatorStarted()) {
          if (desiredCapabilities.getSerial() != null) {
            emulator.setSerial(Integer.parseInt(desiredCapabilities.getSerial().substring(9)));
          }
          // Allow a local developer to have their emulator up and running without restarting it
          // not only for installedApp but also for 'AndroidDriver'
          if (!(app instanceof InstalledAndroidApp) && !BrowserType.ANDROID.equals(desiredCapabilities.getAut())) {
            throw new SessionNotCreatedException("The Emulator '" + emulator
                + "' is already started even though it should be switched off.");
          }
        } else {
          Map<String, Object> config = new HashMap<String, Object>();
          if (serverConfiguration.getEmulatorOptions() != null) {
            config.put(AndroidEmulator.EMULATOR_OPTIONS, serverConfiguration.getEmulatorOptions());
          }
          config.put(AndroidEmulator.TIMEOUT_OPTION, serverConfiguration.getTimeoutEmulatorStart());
          if (desiredCapabilities.asMap().containsKey(SelendroidCapabilities.DISPLAY)) {
            Object d = desiredCapabilities.getCapability(SelendroidCapabilities.DISPLAY);
            config.put(AndroidEmulator.DISPLAY_OPTION, String.valueOf(d));
          }

          Locale locale = parseLocale(desiredCapabilities);
          emulator.start(locale, deviceStore.nextEmulatorPort(), config);
        }
      } catch (AndroidDeviceException e) {
        deviceStore.release(device, app);
        if (retries > 0) {
          return createNewTestSession(caps, retries - 1);
        }
        throw new SessionNotCreatedException("Error occured while interacting with the emulator: "
            + emulator + ": " + e.getMessage());
      }
      emulator.setIDevice(hardwareDeviceManager.getVirtualDevice(emulator.getSerial()));
    }

    // Uninstalling looks probably a bit like an overhead, but
    // this prevents errors like INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES
    // this is for the AUT
    for (AndroidApp supportedApp : appsToInstall.values()) {
      if (device.isInstalled(supportedApp)) {
        device.uninstall(supportedApp);
      }
      if (!device.install(supportedApp)) {
        device.install(supportedApp);
      }
    }

    int port = getNextSelendroidServerPort();
    // The DEPRECATED original AndroidDriver app provided by SeHQ
    // it has it's own 'server' running, we won't compete with it
    // we'll just forward it's default port (8080) on the device
    // and act as a proxy
    if ("org.openqa.selenium.android.app".equals(app.getBasePackage())) {
      device.start(app);
      device.forwardPort(port, 8080);
    } else {
      // "Normal" Selendroid usage, not running against the SeHQ AndroidDriver

      AndroidApp selendroidServer = createSelendroidServerApk(app);

      // An InstalledAndroidApp won't install/uninstall selendroid server
      // If the SelendroidServer is already installed, don't uninstall/reinstall
      // when using an InstalledAndroidApp.
      Boolean selendroidInstalledSuccessfully = true;
      if (device.isInstalled(selendroidServer)) {
        if (!(app instanceof InstalledAndroidApp)) {
          device.uninstall(selendroidServer);
          selendroidInstalledSuccessfully = device.install(selendroidServer);
        }
      } else {
        selendroidInstalledSuccessfully = device.install(selendroidServer);
      }
      if (!selendroidInstalledSuccessfully) {
        if (!device.install(selendroidServer)) {

          deviceStore.release(device, app);

          if (retries > 0) {
            return createNewTestSession(caps, retries - 1);
          }
        }
      }

      // Run any adb commands requested in the capabilities
      List<String> adbCommands = desiredCapabilities.getPreSessionAdbCommands();
      if (adbCommands != null && !adbCommands.isEmpty()) {
        for (String adbCommandParameter : adbCommands) {
          device.runAdbCommand(adbCommandParameter);
        }
      }


      // It's GO TIME!
      // start the selendroid server on the device and make sure it's up
      try {
        device.startSelendroid(app, port);
      } catch (AndroidSdkException e) {
        log.info("error while starting selendroid: " + e.getMessage());

        deviceStore.release(device, app);
        if (retries > 0) {
          return createNewTestSession(caps, retries - 1);
        }
        throw new SessionNotCreatedException("Error occurred while starting instrumentation: "
            + e.getMessage());
      }
      long start = System.currentTimeMillis();
      long startTimeOut = 20000;
      long timemoutEnd = start + startTimeOut;
      while (device.isSelendroidRunning() == false) {
        if (timemoutEnd >= System.currentTimeMillis()) {
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {}
        } else {
          throw new SelendroidException("Selendroid server on the device didn't came up after "
              + startTimeOut / 1000 + "sec:");
        }
      }
    }

    // arbitrary sleeps? yay...
    // looks like after the server starts responding
    // we need to give it a moment before starting a session?
    try {
      Thread.sleep(500);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }

    // create the new session on the device server
    RemoteWebDriver driver;
    try {
      driver = new RemoteWebDriver(new URL("http://localhost:" + port + "/wd/hub"), desiredCapabilities);
    } catch (Exception e) {
      e.printStackTrace();
      deviceStore.release(device, app);
      throw new SessionNotCreatedException(
          "Error occurred while creating session on Android device", e);
    }
    String sessionId = driver.getSessionId().toString();
    SelendroidCapabilities requiredCapabilities =
        new SelendroidCapabilities(driver.getCapabilities().asMap());
    ActiveSession session = new ActiveSession(sessionId, requiredCapabilities, app, device, port);

    this.sessions.put(sessionId, session);

    // We are requesting an "AndroidDriver" so automatically switch to the webview
    if (BrowserType.ANDROID.equals(desiredCapabilities.getAut())) {
      // arbitrarily high wait time, will this cover our slowest possible device/emulator?
      WebDriverWait wait = new WebDriverWait(driver, 60);
      // wait for the WebView to appear
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("android.webkit.WebView")));
      driver.switchTo().window("WEBVIEW");
      // the 'android-driver' webview has an h1 with id 'AndroidDriver' embedded in it
      wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("AndroidDriver")));
    }

    return sessionId;
  }

  private AndroidApp createSelendroidServerApk(AndroidApp aut) throws AndroidSdkException {
    if (!selendroidServers.containsKey(aut.getAppId())) {
      try {
        AndroidApp selendroidServer = selendroidApkBuilder.createSelendroidServer(aut);
        selendroidServers.put(aut.getAppId(), selendroidServer);
      } catch (Exception e) {
        e.printStackTrace();
        throw new SessionNotCreatedException(
            "An error occurred while building the selendroid-server.apk for aut '" + aut + "': "
                + e.getMessage());
      }
    }
    return selendroidServers.get(aut.getAppId());
  }

  private Locale parseLocale(SelendroidCapabilities capa) {
    if (capa.getLocale() == null) {
      return null;
    }
    String[] localeStr = capa.getLocale().split("_");
    Locale locale = new Locale(localeStr[0], localeStr[1]);

    return locale;
  }

  /* package */AndroidDevice getAndroidDevice(SelendroidCapabilities caps)
      throws AndroidDeviceException {
    AndroidDevice device = null;
    String serial = caps.getSerial();
    if (serial != null) {
      if (serial.startsWith("emulator")) {
        deviceStore.setAvdName(getAvdName(Integer.parseInt(serial.substring(9))));
      } else {
        deviceStore.setDeviceSerial(serial);
      }
    }
    try {
      device = deviceStore.findAndroidDevice(caps);
    } catch (DeviceStoreException e) {
      e.printStackTrace();
      log.fine(caps.getRawCapabilities().toString());
      throw new AndroidDeviceException("Error occurred while looking for devices/emulators.", e);
    }

    return device;
  }

  /**
   * For testing only
   */
  /* package */Map<String, AndroidApp> getConfiguredApps() {
    return Collections.unmodifiableMap(appsStore);
  }

  /**
   * For testing only
   */
  /* package */void setDeviceStore(DeviceStore store) {
    this.deviceStore = store;
  }

  private synchronized int getNextSelendroidServerPort() {
    return selendroidServerPort++;
  }

  /** FOR TESTING ONLY */
  public List<ActiveSession> getActiveSessions() {
    return Lists.newArrayList(sessions.values());
  }

  public boolean isValidSession(String sessionId) {
    if (sessionId != null && sessionId.isEmpty() == false) {
      return sessions.containsKey(sessionId);
    }
    return false;
  }

  public void stopSession(String sessionId) throws AndroidDeviceException {
    if (isValidSession(sessionId)) {
      ActiveSession session = sessions.get(sessionId);
      try {
        HttpClientUtil.executeRequest("http://localhost:" + session.getSelendroidServerPort()
            + "/wd/hub/session/" + sessionId, HttpMethod.DELETE);
      } catch (Exception e) {
        // can happen, ignore
      }
      deviceStore.release(session.getDevice(), session.getAut());

      // remove session
      sessions.remove(sessionId);
      session = null;
    }
  }

  public void quitSelendroid() {
    List<String> sessionsToQuit = Lists.newArrayList(sessions.keySet());
    if (sessionsToQuit != null && sessionsToQuit.isEmpty() == false) {
      for (String sessionId : sessionsToQuit) {
        try {
          stopSession(sessionId);
        } catch (AndroidDeviceException e) {
          log.severe("Error occured while stopping session: " + e.getMessage());
          e.printStackTrace();
        }
      }
    }
    hardwareDeviceManager.shutdown();
  }

  public SelendroidCapabilities getSessionCapabilities(String sessionId) {
    if (sessions.containsKey(sessionId)) {
      return sessions.get(sessionId).getDesiredCapabilities();
    }
    return null;
  }

  public ActiveSession getActiveSession(String sessionId) {
    if (sessionId != null && sessions.containsKey(sessionId)) {
      return sessions.get(sessionId);
    }

    return null;
  }

  @Override
  public synchronized JSONArray getSupportedApps() {
    JSONArray list = new JSONArray();
    for (AndroidApp app : appsStore.values()) {
      JSONObject appInfo = new JSONObject();
      try {
        appInfo.put("appId", app.getAppId());
        appInfo.put("basePackage", app.getBasePackage());
        appInfo.put("mainActivity", app.getMainActivity());
        list.put(appInfo);
      } catch (Exception e) {}
    }
    return list;
  }

  @Override
  public synchronized JSONArray getSupportedDevices() {
    JSONArray list = new JSONArray();
    for (AndroidDevice device : deviceStore.getDevices()) {
      JSONObject deviceInfo = new JSONObject();
      try {
        if (device instanceof DefaultAndroidEmulator) {
          deviceInfo.put(SelendroidCapabilities.EMULATOR, true);
          deviceInfo.put("avdName", ((DefaultAndroidEmulator) device).getAvdName());
        } else {
          deviceInfo.put(SelendroidCapabilities.EMULATOR, false);
          deviceInfo.put("model", ((DefaultHardwareDevice) device).getModel());
        }
        deviceInfo.put(SelendroidCapabilities.ANDROID_TARGET, device.getTargetPlatform());
        deviceInfo.put(SelendroidCapabilities.SCREEN_SIZE, device.getScreenSize());

        list.put(deviceInfo);
      } catch (Exception e) {
        log.info("Error occured when building suported device info: " + e.getMessage());
      }
    }
    return list;
  }

  public class DefaultHardwareDeviceListener implements HardwareDeviceListener {
    private DeviceStore store = null;

    public DefaultHardwareDeviceListener(DeviceStore store) {
      this.store = store;
    }

    @Override
    public void onDeviceConnected(AndroidDevice device) {
      try {
        store.addDevice(device);
      } catch (AndroidDeviceException e) {
        log.info(e.getMessage());
      }
    }

    @Override
    public void onDeviceDisconnected(AndroidDevice device) {
      try {
        // if there is an active session on the device,
        // mark it as invalid.
        ActiveSession session = findActiveSession(device);
        if (session != null) {
          session.invalidate();
        }

        // remove device from store
        store.removeAndroidDevice(device);
      } catch (DeviceStoreException e) {
        log.severe("The device cannot be removed: " + e.getMessage());
      }
    }
  }

  private ActiveSession findActiveSession(AndroidDevice device) {
    for (ActiveSession session : sessions.values()) {
      if (session.getDevice().equals(device)) {
        return session;
      }
    }
    return null;

  }

  public byte[] takeScreenshot(String sessionId) throws AndroidDeviceException {
    if (sessionId == null || sessions.containsKey(sessionId) == false) {
      throw new SelendroidException("The gicen session id '" + sessionId + "' was not found.");
    }
    return sessions.get(sessionId).getDevice().takeScreenshot();
  }
}
