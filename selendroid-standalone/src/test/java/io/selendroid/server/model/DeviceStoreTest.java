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

import static io.selendroid.server.model.DeviceStoreFixture.anDevice;
import static io.selendroid.server.model.DeviceStoreFixture.anDeviceManager;
import static io.selendroid.server.model.DeviceStoreFixture.anEmulator;
import static io.selendroid.server.model.DeviceStoreFixture.withDefaultCapabilities;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.selendroid.SelendroidCapabilities;
import io.selendroid.android.AndroidDevice;
import io.selendroid.android.AndroidEmulator;
import io.selendroid.android.impl.DefaultAndroidEmulator;
import io.selendroid.android.impl.DefaultHardwareDevice;
import io.selendroid.device.DeviceTargetPlatform;
import io.selendroid.exceptions.AndroidDeviceException;
import io.selendroid.exceptions.DeviceStoreException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author ddary
 */
public class DeviceStoreTest {
  public static final String ANY_STRING = "ANY";
  private static final Integer EMULATOR_PORT = 5560;

  @Test
  public void shouldAddStartedEmulator() throws Exception {
    AndroidEmulator emulator = anEmulator("de", DeviceTargetPlatform.ANDROID10, false);

    DeviceStore deviceStore = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());
    deviceStore.addEmulators(Collections.singletonList(emulator));
    Assert.assertEquals(deviceStore.getDevicesInUse().size(), 0);
    Assert.assertEquals(deviceStore.getDevicesList().size(), 1);

    Assert.assertTrue(deviceStore.getDevicesList().containsKey(DeviceTargetPlatform.ANDROID10));
    Assert.assertTrue(deviceStore.getDevicesList().get(DeviceTargetPlatform.ANDROID10)
        .contains(emulator));
  }

  @Test
  public void shouldIncrementEmulatorPortsByTwo() throws Exception {
    DeviceStore deviceStore = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());
    Assert.assertEquals(5560, deviceStore.nextEmulatorPort().intValue());
    Assert.assertEquals(5562, deviceStore.nextEmulatorPort().intValue());
    Assert.assertEquals(5564, deviceStore.nextEmulatorPort().intValue());
  }

  @Test
  public void shouldReleaseActiveEmulators() throws Exception {
    AndroidEmulator deEmulator = anEmulator("de", DeviceTargetPlatform.ANDROID16, false);
    when(deEmulator.getPort()).thenReturn(5554);

    EmulatorPortFinder finder = mock(EmulatorPortFinder.class);
    when(finder.next()).thenReturn(5554);
    DeviceStore deviceStore = new DeviceStore(finder, false, anDeviceManager());
    deviceStore.addEmulators(Arrays.asList(new AndroidEmulator[] {deEmulator}));
    Assert.assertEquals(deviceStore.getDevicesInUse().size(), 0);
    AndroidDevice foundDevice = deviceStore.findAndroidDevice(withDefaultCapabilities());
    Assert.assertEquals(deEmulator, foundDevice);
    Assert.assertEquals(deviceStore.getDevicesInUse().size(), 1);
    deviceStore.release(foundDevice, null);
    // make sure the emulator has been stopped
    verify(deEmulator, times(1)).stop();
    verify(finder, times(1)).release(5554);
    // Make sure the device has been removed from devices in use
    Assert.assertEquals(deviceStore.getDevicesInUse().size(), 0);
  }

  @Test
  public void shouldRegisterMultipleNotStatedEmulators() throws Exception {
    AndroidEmulator deEmulator10 = anEmulator("de", DeviceTargetPlatform.ANDROID10, false);
    AndroidEmulator enEmulator10 = anEmulator("en", DeviceTargetPlatform.ANDROID10, false);
    AndroidEmulator deEmulator16 = anEmulator("de", DeviceTargetPlatform.ANDROID16, false);

    DeviceStore deviceStore = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());
    deviceStore.addEmulators(Arrays.asList(new AndroidEmulator[] {deEmulator10, enEmulator10,
        deEmulator16}));
    Assert.assertEquals(deviceStore.getDevicesInUse().size(), 0);
    // Expecting two entries for two android target platforms
    Assert.assertEquals(deviceStore.getDevicesList().size(), 2);

    Assert.assertTrue(deviceStore.getDevicesList().containsKey(DeviceTargetPlatform.ANDROID10));
    Assert.assertTrue(deviceStore.getDevicesList().get(DeviceTargetPlatform.ANDROID10)
        .contains(deEmulator10));
    Assert.assertTrue(deviceStore.getDevicesList().get(DeviceTargetPlatform.ANDROID10)
        .contains(enEmulator10));
    Assert.assertTrue(deviceStore.getDevicesList().get(DeviceTargetPlatform.ANDROID16)
        .contains(deEmulator16));
  }

  @Test
  public void shouldRegisterStartedAndStoppedEmulators() throws Exception {
    AndroidEmulator deEmulator10 = anEmulator("de", DeviceTargetPlatform.ANDROID10, false);
    AndroidEmulator enEmulator10 = anEmulator("en", DeviceTargetPlatform.ANDROID10, true);
    AndroidEmulator deEmulator16 = anEmulator("de", DeviceTargetPlatform.ANDROID16, true);

    DeviceStore deviceStore = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());
    deviceStore.addEmulators(Arrays.asList(new AndroidEmulator[] {deEmulator10, enEmulator10,
        deEmulator16}));
    Assert.assertEquals(deviceStore.getDevicesInUse().size(), 0);

    Assert.assertTrue("Should have an entry for target version 10.", deviceStore.getDevicesList()
        .containsKey(DeviceTargetPlatform.ANDROID10));
    List<AndroidDevice> devicesApi10 =
        deviceStore.getDevicesList().get(DeviceTargetPlatform.ANDROID10);
    Assert.assertTrue("Entry of target version 10 should contain de emulator.",
        devicesApi10.contains(deEmulator10));
    Assert.assertTrue("Entry of target version 10 should contain en emulator.",
        devicesApi10.contains(enEmulator10));

    Assert.assertTrue("Should have an entry for target version 16.", deviceStore.getDevicesList()
        .containsKey(DeviceTargetPlatform.ANDROID16));
    Assert.assertTrue("Entry of target version 16 should contain the emulator.", deviceStore
        .getDevicesList().get(DeviceTargetPlatform.ANDROID16).contains(deEmulator16));
  }

  @Test
  public void shouldNotIgnoreRunningEmulators() throws Exception {
    AndroidEmulator enEmulator10 = anEmulator("en", DeviceTargetPlatform.ANDROID10, true);
    AndroidEmulator deEmulator16 = anEmulator("de", DeviceTargetPlatform.ANDROID16, true);

    DeviceStore deviceStore = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());
    deviceStore.addEmulators(Arrays.asList(new AndroidEmulator[] {enEmulator10, deEmulator16}));

    // Nothing has been added.
    Assert.assertEquals(deviceStore.getDevicesList().size(), 2);
  }

  @Test
  public void storeShouldDoNothingIfInitializedWithEmptyList() throws AndroidDeviceException {
    DeviceStore store = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());

    store.addEmulators(new ArrayList<AndroidEmulator>());
    Assert.assertEquals(store.getDevicesList().size(), 0);
  }

  @Test
  public void storeShouldDoNothingIfInitializedWithNull() throws AndroidDeviceException {
    DeviceStore store = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());

    store.addEmulators(null);
    Assert.assertEquals(store.getDevicesList().size(), 0);
  }



  @Test
  public void shouldFindSwitchedOffEmulator() throws Exception {
    // prepare device store
    DefaultAndroidEmulator deEmulator16 = anEmulator("de", DeviceTargetPlatform.ANDROID16, false);
    DeviceStore deviceStore = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());
    deviceStore.addEmulators(Arrays.asList(new AndroidEmulator[] {deEmulator16}));

    // find by Capabilities
    AndroidDevice device = deviceStore.findAndroidDevice(withDefaultCapabilities());
    // The right device is found
    assertThat(device, equalTo((AndroidDevice) deEmulator16));
    // the device is in use when found
    assertThat(deviceStore.getDevicesInUse(), contains((AndroidDevice) deEmulator16));
  }

  @Test
  public void shouldThrowAnExceptionIfTargetPlatformIsMissingInCapabilities() throws Exception {
    // prepare device store
    DefaultAndroidEmulator deEmulator16 = anEmulator("de", DeviceTargetPlatform.ANDROID16, false);
    DeviceStore deviceStore = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());
    deviceStore.addEmulators(Arrays.asList(new AndroidEmulator[] {deEmulator16}));

    SelendroidCapabilities capa = new SelendroidCapabilities();
    try {
      deviceStore.findAndroidDevice(capa);
      Assert.fail();
    } catch (DeviceStoreException e) {
      Assert
          .assertEquals(
              "No devices are found. This can happen if the devices are in use or no device screen matches the required capabilities.",
              e.getMessage());
    }
  }

  @Test
  public void shouldNotFindDeviceIfTargetPlatformIsNotSuported() throws Exception {
    // prepare device store
    DefaultAndroidEmulator deEmulator10 = anEmulator("de", DeviceTargetPlatform.ANDROID10, false);
    AndroidEmulator enEmulator10 = anEmulator("en", DeviceTargetPlatform.ANDROID10, false);
    DeviceStore deviceStore = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());
    deviceStore.addEmulators(Arrays.asList(new AndroidEmulator[] {deEmulator10, enEmulator10}));

    // find by Capabilities
    try {
      deviceStore.findAndroidDevice(withDefaultCapabilities());
      Assert.fail();
    } catch (DeviceStoreException e) {
      assertThat(
          e.getMessage(),
          equalTo("No devices are found. This can happen if the devices are in use or no device screen matches the required capabilities."));
    }

    assertThat(deviceStore.getDevicesInUse(), hasSize(0));
  }

  @Test
  public void shouldNotFindDeviceIfScreenSizeIsNotSupported() throws Exception {
    // prepare device store
    DefaultAndroidEmulator deEmulator10 = anEmulator("de", DeviceTargetPlatform.ANDROID16, false);
    AndroidEmulator enEmulator10 = anEmulator("en", DeviceTargetPlatform.ANDROID10, false);
    DeviceStore deviceStore = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());
    deviceStore.addEmulators(Arrays.asList(new AndroidEmulator[] {deEmulator10, enEmulator10}));

    // find by Capabilities
    SelendroidCapabilities capa = withDefaultCapabilities();
    capa.setScreenSize("768x1024");
    try {
      deviceStore.findAndroidDevice(capa);
      Assert.fail();
    } catch (DeviceStoreException e) {
      assertThat(e.getMessage(), containsString("No devices are found."));
    }

    assertThat(deviceStore.getDevicesInUse(), hasSize(0));
    assertThat(deviceStore.getDevicesList().values(), hasSize(2));
  }



  @Test
  public void testShouldBeAbleToAddDevices() throws Exception {
    AndroidDevice device = mock(AndroidDevice.class);
    when(device.getTargetPlatform()).thenReturn(DeviceTargetPlatform.ANDROID16);
    when(device.isDeviceReady()).thenReturn(Boolean.TRUE);

    DeviceStore store = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());
    store.addDevice(device);
    assertThat(store.getDevicesList().values(), hasSize(1));
    assertThat(store.getDevicesInUse(), hasSize(0));
    assertThat(store.getDevicesList().values().iterator().next(), contains(device));
  }

  @Test
  public void shouldIgnoreNotReadyDevices() throws Exception {
    AndroidDevice device = mock(AndroidDevice.class);
    when(device.getTargetPlatform()).thenReturn(DeviceTargetPlatform.ANDROID16);
    when(device.isDeviceReady()).thenReturn(Boolean.FALSE);

    DeviceStore store = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());
    store.addDevice(device);
    assertThat(store.getDevicesList().values(), hasSize(0));
    assertThat(store.getDevicesInUse(), hasSize(0));
  }

  @Test
  public void shouldFindRealDeviceForCapabilities() throws Exception {
    AndroidDevice device = mock(DefaultHardwareDevice.class);
    when(device.getTargetPlatform()).thenReturn(DeviceTargetPlatform.ANDROID16);
    when(device.isDeviceReady()).thenReturn(Boolean.TRUE);
    when(device.getScreenSize()).thenReturn("320x480");
    when(device.screenSizeMatches("320x480")).thenReturn(Boolean.TRUE);

    DeviceStore store = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());
    store.addDevice(device);
    assertThat(store.getDevicesList().values(), hasSize(1));
    assertThat(store.getDevicesInUse(), hasSize(0));
    SelendroidCapabilities capa = withDefaultCapabilities();
    capa.setEmulator(false);
    AndroidDevice foundDevice = store.findAndroidDevice(capa);
    assertThat(foundDevice, equalTo(device));
    assertThat(store.getDevicesInUse(), hasSize(1));
  }

  @Test
  public void shouldNotFindRealDeviceForCapabilities() throws Exception {
    AndroidDevice device = mock(AndroidDevice.class);
    when(device.getTargetPlatform()).thenReturn(DeviceTargetPlatform.ANDROID16);
    when(device.isDeviceReady()).thenReturn(Boolean.TRUE);
    when(device.getScreenSize()).thenReturn("320x500");
    when(device.screenSizeMatches("320x500")).thenReturn(Boolean.FALSE);

    DeviceStore store = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());
    store.addDevice(device);
    assertThat(store.getDevicesList().values(), hasSize(1));
    assertThat(store.getDevicesInUse(), hasSize(0));
    try {
      store.findAndroidDevice(withDefaultCapabilities());
      Assert.fail();
    } catch (DeviceStoreException e) {
      // expected
    }

    assertThat(store.getDevicesInUse(), hasSize(0));
  }

  @Test
  public void shouldRemoveHardwareDevice() throws Exception {
    DefaultHardwareDevice device = anDevice("de", DeviceTargetPlatform.ANDROID16);
    DeviceStore store = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());
    store.addDevice(device);
    assertThat(store.getDevicesList().values(), hasSize(1));
    store.removeAndroidDevice(device);
    assertThat(store.getDevicesList().values(), hasSize(0));
  }

  @Test
  public void shouldNotRemoveAnEmulator() throws Exception {
    DefaultAndroidEmulator deEmulator10 = anEmulator("de", DeviceTargetPlatform.ANDROID16, false);
    DeviceStore store = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());
    store.addEmulators(Arrays.asList(new AndroidEmulator[] {deEmulator10}));

    assertThat(store.getDevicesList().values(), hasSize(1));
    try {
      store.removeAndroidDevice(deEmulator10);
      Assert.fail("Only hardware devices should be able to be removed.");
    } catch (DeviceStoreException e) {
      // expected
    }
  }

  @Test
  public void shouldFindStartedEmulator() throws Exception {
    // prepare device store
    DefaultAndroidEmulator deEmulator16 = anEmulator("de", DeviceTargetPlatform.ANDROID16, true);
    DeviceStore deviceStore = new DeviceStore(false, EMULATOR_PORT, anDeviceManager());
    deviceStore.addEmulators(Arrays.asList(new AndroidEmulator[] {deEmulator16}));

    // find by Capabilities
    AndroidDevice device = deviceStore.findAndroidDevice(withDefaultCapabilities());
    // The right device is found
    assertThat(device, equalTo((AndroidDevice) deEmulator16));
    // the device is in use when found
    assertThat(deviceStore.getDevicesInUse(), contains((AndroidDevice) deEmulator16));
  }
}
