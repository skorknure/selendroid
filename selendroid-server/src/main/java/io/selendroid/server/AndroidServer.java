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
package io.selendroid.server;

import io.selendroid.ServerInstrumentation;
import io.selendroid.server.inspector.InspectorServlet;
import io.selendroid.server.model.DefaultSelendroidDriver;
import io.selendroid.server.model.SelendroidDriver;

import java.net.UnknownHostException;

public class AndroidServer {
  private int driverPort = 8080;
  private SelendroidWebServer webServer;

  /** for testing only */
  protected AndroidServer(int port, ServerInstrumentation androidInstrumentation)
      throws UnknownHostException {
    this.driverPort = port;
    webServer = new SelendroidWebServer(driverPort);
    init(androidInstrumentation);
  }

  public AndroidServer(ServerInstrumentation androidInstrumentation, int port) {
    driverPort = port;
    webServer = new SelendroidWebServer(driverPort);
    init(androidInstrumentation);
  }

  protected void init(ServerInstrumentation androidInstrumentation) {
    SelendroidDriver driver = new DefaultSelendroidDriver(androidInstrumentation);

    webServer.setStaleConnectionTimeout(604800000); // 1 week.
    // If the stale connection cleanup is called a ConcurrentModification exception will be thrown.
    // Thus the significantly high timeout.
    webServer.addHandler(new StatusServlet(androidInstrumentation));
    webServer.addHandler(new InspectorServlet(driver, androidInstrumentation));
    webServer.addHandler(new AndroidServlet(driver));
  }

  /**
   * just make sure if the server timeout is set that this method is called as well.
   *
   * @param millies
   */
  public void setConnectionTimeout(long millies) {
    System.out.println("using staleConnectionTimeout: " + millies);
    webServer.setStaleConnectionTimeout(millies);
  }

  public void start() {
    webServer.start();
  }

  public void stop() {
    webServer.stop();
  }

  public int getPort() {
    return webServer.getPort();
  }

}
