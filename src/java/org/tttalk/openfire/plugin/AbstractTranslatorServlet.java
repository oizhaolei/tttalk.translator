package org.tttalk.openfire.plugin;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jivesoftware.admin.AuthCheckFilter;
import org.jivesoftware.openfire.XMPPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public abstract class AbstractTranslatorServlet extends HttpServlet {

	private static final long serialVersionUID = 7171852473707442671L;

	static final Logger Log = LoggerFactory
			.getLogger(AbstractTranslatorServlet.class);

	TranslatorPlugin plugin;

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		plugin = (TranslatorPlugin) XMPPServer.getInstance().getPluginManager()
				.getPlugin(TranslatorPlugin.PLUGIN_NAME);
		AuthCheckFilter.addExclude(TranslatorPlugin.PLUGIN_NAME + getUri());
	}

	// TODO: sign check
	abstract String getUri();

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		logParameters(request);
		doGet(request, response);
	}

	@Override
	public void destroy() {
		super.destroy();
	}

	private void logParameters(HttpServletRequest request) {
		Map<String, String[]> map = request.getParameterMap();
		for (Map.Entry<String, String[]> entry : map.entrySet()) {
			String values[] = entry.getValue();
			String value = "";
			for (String val : values) {
				value += val + " ";
			}
			Log.info(String.format("[%s] = { %s}", entry.getKey(), value));
		}
	}
}
