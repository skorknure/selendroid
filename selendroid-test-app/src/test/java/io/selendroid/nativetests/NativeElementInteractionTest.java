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
package io.selendroid.nativetests;

import io.selendroid.support.BaseAndroidTest;
import io.selendroid.waiter.TestWaiter;
import io.selendroid.waiter.WaitingConditions;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.touch.TouchActions;


public class NativeElementInteractionTest extends BaseAndroidTest {
  @Test
  public void shouldGetTextOfElement() {
    openStartActivity();
    WebElement button = driver().findElement(By.id("waitingButtonTest"));
    Assert.assertEquals(button.getText(), "Show Progress Bar for a while");
  }

  @Test()
  public void shouldClickOnElement() {
    openStartActivity();
    WebElement button = driver().findElement(By.id("startUserRegistration"));
    button.click();

    TestWaiter.waitFor(WaitingConditions.driverUrlToBe(driver(),
        "and-activity://RegisterUserActivity"));
  }

  @Test
  public void shouldGetAttributeOfTextField() {
    openStartActivity();
    WebElement inputField = driver().findElement(By.id("my_text_field"));
    Assert.assertEquals(inputField.getAttribute("enabled"), "true");
  }

  @Test
  public void shouldoGetTagName() {
    openStartActivity();
    WebElement inputField = driver().findElement(By.id("my_text_field"));
    Assert.assertEquals(inputField.getTagName(), "EditText");
  }

  @Test
  public void shouldGetAttributeOfButton() {
    openStartActivity();
    WebElement button = driver().findElement(By.id("waitingButtonTest"));
    Assert.assertEquals(button.getAttribute("ContentDescription"), "waitingButtonTestCD");
    Assert.assertEquals(button.getAttribute("text"), "Show Progress Bar for a while");
  }

  @Test
  public void shouldSendKeysAndClearAnElement() {
    openStartActivity();
    WebElement inputField = driver().findElement(By.id("my_text_field"));
    String text = "a.anyString@not.existent%.1.de";
    inputField.sendKeys(text);
    Assert.assertEquals(inputField.getText(), text);
    inputField.clear();
    Assert.assertEquals(inputField.getAttribute("text"), "");
  }

  @Test
  public void shouldGetSelectedStateOfElement() {
    openStartActivity();
    WebElement checkBox = driver().findElement(By.id("input_adds_check_box"));
    Assert.assertEquals(checkBox.isSelected(), true);
    checkBox.click();
    Assert.assertEquals(checkBox.isSelected(), false);
  }

  /**
   * Based on the default test emulator used to verify build that has a resolution of 320x480
   * pixels.
   */
  @Test
  public void shouldGetSizeOfElement() {
    openStartActivity();
    WebElement button = driver().findElement(By.id("waitingButtonTest"));
    Dimension dimension = button.getSize();
    Assert.assertTrue(dimension.height >= 48);
    Assert.assertTrue(dimension.width >= 210);
  }

  /**
   * Based on the default test emulator used to verify build that has a resolution of 320x480
   * pixels.
   */
  @Test
  public void shouldGetLocationOfElement() {
    openStartActivity();
    WebElement button = driver().findElement(By.id("waitingButtonTest"));
    Point location = button.getLocation();
    Assert.assertEquals(location.x, 0);
    // this is not perfect but guarantees that it works on different screen sizes
    Assert.assertTrue(location.y >= 247);
  }

  @Test
  public void shouldExecuteSimpleJavaScript() {
    openStartActivity();

    Object translatedText =
        ((JavascriptExecutor) driver()).executeScript("getL10nKeyTranslation", "button");
    Assert.assertEquals(translatedText, "EN Button");
  }

  @Test
  public void shouldGetDisplayedStateOfElement() {
    openStartActivity();
    WebElement button = driver().findElement(By.id("waitingButtonTest"));
    Assert.assertEquals(button.isDisplayed(), true);
  }

  @Test
  public void shouldGetEnbledStateOfElement() {
    openStartActivity();
    WebElement button = driver().findElement(By.id("waitingButtonTest"));
    Assert.assertEquals(button.isEnabled(), true);
  }

  @Test
  public void shouldSubmitAnElement() {
    openStartActivity();
    WebElement button = driver().findElement(By.id("waitingButtonTest"));
    try {
      button.submit();
      Assert.fail();
    } catch (WebDriverException e) {
      Assert.assertTrue(e.getMessage().contains("Submit is not supported for native elements."));
    }
  }

  @Test
  public void shouldLongPressOnElement() {
    openStartActivity();
    WebElement button = driver().findElement(By.id("buttonTest"));
    TouchActions longPress = new TouchActions(driver()).longPress(button);
    longPress.perform();
    WebElement text = driver().findElement(By.partialLinkText("Long Press Tap"));
    Assert.assertNotNull(text);
    // TODO ddary this is essential, not perfect. must be refactored
    driver().findElement(By.id("button1")).click();
  }

  @Test
  public void shouldTapOnElement() {
    openStartActivity();
    WebElement button = driver().findElement(By.id("buttonTest"));
    TouchActions longPress = new TouchActions(driver()).singleTap(button);
    longPress.perform();
    WebElement text = driver().findElement(By.partialLinkText("end the activity"));
    Assert.assertNotNull(text);
    // TODO ddary this is essential, not perfect. must be refactored
    driver().findElement(By.id("button2")).click();
  }

  @Test
  public void shouldGetWindowSize() {
    openStartActivity();
    Dimension dim = driver().manage().window().getSize();
    Assert.assertTrue(dim.getHeight() > 100);
    Assert.assertTrue(dim.getWidth() > 100);
  }

}
