package org.tttalk.openfire.plugin;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class BalanceServlet extends AbstractTranslatorServlet {
	private static final long serialVersionUID = 1159875340630997082L;
	private static final Logger Log = LoggerFactory
			.getLogger(BalanceServlet.class);

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Log.info(request.toString());

		String balance = request.getParameter("balance");
		String translator = request.getParameter("translator");
		plugin.balance(translator, balance);

		PrintWriter out = response.getWriter();
		out.println("success");
	}

	@Override
	String getUri() {
		return "/balance";
	}
}
