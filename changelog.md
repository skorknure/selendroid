0.8.0-SNAPSHOT (under developement)
---
- Default selendroid-standalone port is changed from 5555 to 4444
- Added support for switching to a frame in webview
- Fixes:  [#155](../../issues/155), [#163](../../issues/163), [#177](../../issues/177), [#184](../../issues/184), [#188](../../issues/188), [#202](../../issues/202), [#209](../../issues/209), [#210](../../issues/210), [#211](../../issues/211), [#214](../../issues/214), [#216](../../issues/216), [#223](../../issues/223), [#231](../../issues/231)
- Refactoring of our end-to-end tests
- Support for navigating back, forward and refresh
- removed -restartAdb option
- Adding Alert api support while in a webview
- adding -noClearData option to avoid calling adb shell pm clear  when calling driver.quit()


0.7.0
---
- Support for running AndroidDriver to test mobile web pages.
- Introduce an API to control the screen brightness and whether it's on.
- making screenshots work again for emulators and using the -installedApp option
- Avoid retaining references to Views that have been disposed.
- Prevent temporary APKs from cluttering the cwd.


0.6.0
-----
- [#114](../../issues/114) Optimizing logcat handling and retry handling for selendroid-server start
- Emulators are started on display configured in capabilities
- Device logcats are available as logging type "logcat"
- The feature to restart adb has been removed
- Selendroid-standalone can register himself as a node to a Selenium Grid hub by using command line parameters (-hub and -host) 
- Selenium Automation Atoms has been updated to fix [#127](../../issues/127)
- Support for Android KitKat (Api Level 19)
- Full support for Windows [#146](../../issues/146)
- Support for running multiple instrumentation servers [#112](../../issues/112)
- Better support for different platform names Android Sdk [#128](../../issues/128)
- Smaller refactorings
- Selendroid-Standalone can be configured to use specific emulator starting options
- Selendroid-Standalone can be configured to use a specific keystore for signing apks

0.5.1
-----
- fix for 'Arbitrary port number binding is broken' [#105](../../issues/105)

0.5.0
---
- Added xpath locator for native context
- selendroid can now handle multiple web views on the same activity
- new inspector with the ability to display html source code of a web view.
- multiple bug fixes
- Support for Set, Get, Delete Cookies for WebViews
- hardware device handling is now based on the ddmlib library which means devices can be now hot plugged.
- selendroid-standalone can is now taking screenshots by default using ddmlib library.
- selendroid-shell has been removed from the core project
- selendroid-server port can now be configured
- Updated to use the lastest Selenium automation Atoms.
- Official support for windows
- Emulator and instrumentation start handling was massively improved
- selendroid-standalone can handle installed apps which is can speed up especially test case development.
- Selenium Log API is now supported


0.4.2
-----
- small bugfixes


0.4.1
-----
- several smaller bug fixes: [#52](../../issues/52), [#51](../../issues/51), [#50](../../issues/50)
- In the capabilities locale, emulator and androidTarget are now optional.

0.4.0
-------------

- moved all packages to domain selendroid.io
- added new component: selendroid-standalone
- moved the documentation from wiki to web page: http://selendroid.io
- selendroid gem has been refactored to support only starting selendroid-shell
- Removed dependencies: Guava, commons-io, slf4j-android
- Find By Tag Name refactored to find by the class now, not by text 
- L10n Locators not longer available, the translation can be done executing the script: ((JavascriptExecutor) driver).executeScript("getL10nKeyTranslation", "l10nKey");
- Several bug fixes


0.3.2
-----
- Gem does support the new location of aapt.

0.3.1
-----
- Gem does require Ruby version >= 1.9.2 



0.3 
----
- Moved from google gson library to org.json
- Added first support of Selenium Advanced User Interactions API
- Added to the selendroid-gem new commands to support
   - build-and-start: automatically build the selendroid-server
     and resigns the given apk with the debug certificate and
     starts afterwards the selendroid server.
   - start: Starts the selendroid server on the first available Android device.
- Fix for bug [#6](../../issues/6) that solves send key to an element.
- Fixed a bug in getCapabilities handler that adds now e.g. support for javascript.
- Fixed a  bug in getElementLocation handler
- Added support for:
   - Get size of an element
   - Get attribute of an element
   - Get the info if an element is displayed or enabled
   - execute script (only supported for webview mode)
- Added css locator support in webviews
- *selendroid-grid-plugin* that adds support to use the selenium grid for parallel testing
- Fix for native UI hierarchy handling
- Several smaller bugfixes with regards to element handling
- Gem can now be use on Windows
- Gem source has been added to the main project
- Added FindByPartialLinkText locator support for native and webview elements
- Adding (experimental) mechanism to add 'executeScript' in Native

0.2 
----
- fixed to major bugs [#1](../../issues/1) and [#2](../../issues/2)
- Created an inspector that allows to inspect the application under test and makes it easier to write test cases: http://localhost:8080/inspector
- Added support for pressing keys like the Android menu button.
- Fixed a bug in taking screenshots. Now pop up dialogs are included as well.  
- Added /sessionId/keys support and a client library that contains already selenium as dependency and the interface SelendroidKeys.
- The gem contains an interactive shell that starts automatically selendroid-server and a Ruby webdriver client that allows to interactively try commands out.
- Added locator strategy by class for native and web view context
- added support for all available locators findElement, findElements and corresponding find child element and find child elements.

0.1
-----
- initial Selendroid version including support for android native and hybrid apps.
  - Driver support:
      - takeScreenshot
      - getPageSource
      - get current url/ activity and open activity
  - Driver supports following find element locators:
      - for web views: by id, by xpath, by text, by name
      - for Android native: by id, by l10n key, by text
  - Found elements features:
      - click, clear, enter text, getValue, is selected and submit
      
