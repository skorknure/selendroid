/*
 * Copyright 2012 Selenium committers Copyright 2012 Software Freedom Conservancy
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

import java.nio.charset.Charset;

public class WebbitHttpResponse implements HttpResponse {

  private final org.webbitserver.HttpResponse response;
  private boolean closed;

  public WebbitHttpResponse(org.webbitserver.HttpResponse response) {
    this.response = response;
  }

  public void setStatus(int status) {
    response.status(status);
  }

  public void setContentType(String mimeType) {
    response.header("Content-Type", mimeType);
  }

  public void setContent(byte[] data) {
    response.header("Content-Length", data.length);
    response.content(data);
  }

  public void setContent(String message) {
    response.content(message);
  }

  public void setEncoding(Charset charset) {
    response.charset(charset);
  }

  public void sendRedirect(String to) {
    response.status(301);
    response.header("location", to);
    end();
  }

  public void sendTemporaryRedirect(String to) {
    response.status(302);
    response.header("location", to);
    end();
  }

  public void end() {
    if (closed) {
      return;
    }
    closed = true;
    try {
      response.end();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
