/**
 * Copyright (c) 2011-2015, James Zhan 詹波 (jfinal@126.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jfinal.core;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jfinal.aop.Invocation;
import com.jfinal.config.Constants;
import com.jfinal.handler.Handler;
import com.jfinal.render.Render;
import com.jfinal.render.RenderException;
import com.jfinal.render.RenderFactory;

/**
 * ActionHandler
 */
final class ActionHandler extends Handler {

  private final boolean devMode;
  private final ActionMapping actionMapping;
  private static final RenderFactory renderFactory = RenderFactory.me();
  private static final Logger log = LoggerFactory.getLogger(ActionHandler.class);

  public ActionHandler(ActionMapping actionMapping, Constants constants) {
    this.actionMapping = actionMapping;
    this.devMode = constants.getDevMode();
  }

  /**
   * handle 1: Action action = actionMapping.getAction(target) 2: new
   * Invocation(...).invoke() 3: render(...)
   */
  public final void handle(String target, HttpServletRequest request, HttpServletResponse response,
      boolean[] isHandled) {
    if (target.indexOf('.') != -1) {
      return;
    }

    isHandled[0] = true;
    String[] urlPara = { null };
    Action action = actionMapping.getAction(target, urlPara);

    if (action == null) {
      if (log.isWarnEnabled()) {
        String qs = request.getQueryString();
        log.warn("404 Action Not Found: " + (qs == null ? target : target + "?" + qs));
      }
      renderFactory.getErrorRender(404).setContext(request, response).render();
      return;
    }

    if (ActionReporter.isMultipartRequest(request)) {
      boolean isOverSize = false;
      MultipartConfigElement cfg = JFinal.me().getMultipartConfig();
      if (this.getContentLength(request) > cfg.getMaxRequestSize()) {
        isOverSize = true;
      } else {
        try {
          long maxPostSize = cfg.getMaxFileSize();
          Iterator<Part> parts = request.getParts().iterator();
          while (parts.hasNext()) {
            if (parts.next().getSize() > maxPostSize) {
              isOverSize = true;
              break;
            }
          }
        } catch (IOException e) {
          log.warn(e.getMessage(), e);
        } catch (ServletException e) {
          log.warn(e.getMessage(), e);
        }
      }
      if (isOverSize) {
        log.error("Upload file size max than config size.");
        renderFactory.getErrorRender(500).setContext(request, response).render();
        return;
      }
    }

    try {
      Controller controller = action.getControllerClass().newInstance();
      controller.init(request, response, urlPara[0]);

      if (devMode) {
        boolean isMultipartRequest = ActionReporter.reportCommonRequest(controller, action);
        new Invocation(action, controller).invoke();
        if (isMultipartRequest)
          ActionReporter.reportMultipartRequest(controller, action);
      } else {
        new Invocation(action, controller).invoke();
      }

      Render render = controller.getRender();
      if (render instanceof ActionRender) {
        String actionUrl = ((ActionRender) render).getActionUrl();
        if (target.equals(actionUrl))
          throw new RuntimeException("The forward action url is the same as before.");
        else
          handle(actionUrl, request, response, isHandled);
        return;
      }

      if (render == null)
        render = renderFactory.getDefaultRender(action.getViewPath() + action.getMethodName());
      render.setContext(request, response, action.getViewPath()).render();
    } catch (RenderException e) {
      if (log.isErrorEnabled()) {
        String qs = request.getQueryString();
        log.error(qs == null ? target : target + "?" + qs, e);
      }
    } catch (ActionException e) {
      int errorCode = e.getErrorCode();
      if (errorCode == 404 && log.isWarnEnabled()) {
        String qs = request.getQueryString();
        log.warn("404 Not Found: " + (qs == null ? target : target + "?" + qs));
      } else if (errorCode == 401 && log.isWarnEnabled()) {
        String qs = request.getQueryString();
        log.warn("401 Unauthorized: " + (qs == null ? target : target + "?" + qs));
      } else if (errorCode == 403 && log.isWarnEnabled()) {
        String qs = request.getQueryString();
        log.warn("403 Forbidden: " + (qs == null ? target : target + "?" + qs));
      } else if (log.isErrorEnabled()) {
        String qs = request.getQueryString();
        log.error(qs == null ? target : target + "?" + qs, e);
      }
      e.getErrorRender().setContext(request, response, action.getViewPath()).render();
    } catch (Throwable t) {
      if (log.isErrorEnabled()) {
        String qs = request.getQueryString();
        log.error(qs == null ? target : target + "?" + qs, t);
      }
      renderFactory.getErrorRender(500).setContext(request, response, action.getViewPath()).render();
    }
  }

  private long getContentLength(HttpServletRequest request) {
    long contentLength = request.getContentLength();
    String header = request.getHeader("Content-Length");
    try {
      long length = Long.parseLong(header);
      return length > contentLength ? length : contentLength;
    } catch (Exception e) {
      return contentLength;
    }
  }
}
