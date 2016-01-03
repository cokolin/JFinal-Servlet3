package com.jfinal.core;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import com.jfinal.config.JFinalConfig;

/**
 * JFinal Servlet3.0 Initializer class
 * 
 * @author cokolin
 * @version 2.0
 * @since 2.0
 */
public abstract class JFinalInitializer {

	public void onStartup(ServletContext ctx) throws ServletException {
		beforRegister(ctx);

		JFinalServlet jfinalServlet = new JFinalServlet(jfinalConfig());

		ServletRegistration.Dynamic dynamic = ctx.addServlet(servletName(), jfinalServlet);
		dynamic.setAsyncSupported(isAsyncSupported());
		dynamic.setMultipartConfig(multipartConfig());
		dynamic.setLoadOnStartup(loadOnStartup());
		dynamic.addMapping(mapping());

		afterRegister(dynamic);
	}

	/**
	 * before Register JFinalServlet action， you can override this method
	 * 
	 * @param ctx
	 */
	protected void beforRegister(ServletContext ctx) {
	}

	/**
	 * after Register JFinalServlet action， you can override this method
	 * 
	 * @param dynamic
	 *          JFinalServlet Registration Dynamic
	 */
	protected void afterRegister(ServletRegistration.Dynamic dynamic) {
	}

	/**
	 * JFinalServlet name， you can override this method
	 * 
	 * @return
	 */
	protected String servletName() {
		return "JFinalServlet";
	}

	/**
	 * request MultipartConfig
	 * 
	 * @return
	 */
	protected MultipartConfigElement multipartConfig() {
		return new MultipartConfigElement(Const.DEFAULT_FILE_RENDER_BASE_PATH, Const.DEFAULT_MAX_POST_SIZE,
				Const.DEFAULT_MAX_POST_SIZE, Const.DEFAULT_FILE_SIZE_THRESHOLD);
	}

	/**
	 * JFinalServlet load on start setting， you can override this method
	 * 
	 * @return
	 */
	protected int loadOnStartup() {
		return 1;
	}

	/**
	 * JFinalServlet URL mapping， you can override this method
	 * 
	 * @return
	 */
	protected String[] mapping() {
		return new String[] { "/*" };
	}

	/**
	 * is servlet3.0 asynchronous operations Supported
	 * 
	 * @return
	 */
	protected boolean isAsyncSupported() {
		return true;
	}

	/**
	 * JFinalConfig class
	 * 
	 * @return
	 */
	protected abstract JFinalConfig jfinalConfig();
}
