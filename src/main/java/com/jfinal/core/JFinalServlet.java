/**
 * Copyright (c) 2015, cokolin (cokolin@126.com).
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jfinal.config.JFinalConfig;
import com.jfinal.config.Constants;
import com.jfinal.handler.Handler;

/**
 * JFinal Servlet class
 * 
 * @author cokolin
 * @see com.jfinal.core.JFinalFilter 2.0
 */
public class JFinalServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(JFinalServlet.class);

	private static final JFinal jfinal = JFinal.me();

	private Handler handler;
	private String encoding;
	private Constants constants;
	private int contextPathLength;
	private JFinalConfig jfinalConfig;

	public JFinalServlet() {
		super();
	}

	public JFinalServlet(JFinalConfig jfinalConfig) {
		this.jfinalConfig = jfinalConfig;
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		if (jfinalConfig == null) {
			this.createJFinalConfig(config.getInitParameter("configClass"));
		}
		if (!jfinal.init(jfinalConfig, config.getServletContext())) {
			throw new RuntimeException("JFinal init error!");
		}
		handler = jfinal.getHandler();
		constants = Config.getConstants();
		encoding = constants.getEncoding();
		jfinalConfig.afterJFinalStart();

		String contextPath = config.getServletContext().getContextPath();
		contextPathLength = (contextPath == null || "/".equals(contextPath) ? 0 : contextPath.length());
		super.init(config);
	}

	private void doing(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		request.setCharacterEncoding(encoding);

		String target = request.getRequestURI();
		if (contextPathLength != 0)
			target = target.substring(contextPathLength);

		boolean[] isHandled = { false };
		try {
			handler.handle(target, request, response, isHandled);
		} catch (Exception e) {
			log.error(target + "?" + request.getQueryString(), e);
			throw new ServletException(e.getMessage(), e);
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doing(req, resp);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doing(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doing(req, resp);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doing(req, resp);
	}

	@Override
	public void destroy() {
		jfinalConfig.beforeJFinalStop();
		jfinal.stopPlugins();
		super.destroy();
	}

	private void createJFinalConfig(String configClass) throws ServletException {
		if (configClass == null) {
			throw new RuntimeException("Please set configClass parameter of JFinalServlet in web.xml");
		}
		try {
			jfinalConfig = (JFinalConfig) Class.forName(configClass).newInstance();
		} catch (ClassNotFoundException e) {
			throw new ServletException("Can not find class: " + configClass, e);
		} catch (ClassCastException e) {
			throw new ServletException("Class '" + configClass + "' can not case to class com.jfinal.config.JFinalConfig", e);
		} catch (Exception e) {
			throw new ServletException("Can not create instance of class: " + configClass, e);
		}
	}

}