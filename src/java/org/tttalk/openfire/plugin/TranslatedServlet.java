package org.tttalk.openfire.plugin;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 *
 */
public class TranslatedServlet extends AbstractTranslatorServlet {
	private static final long serialVersionUID = 9008949607840140354L;

	@Override
	String getUri() {
		return "/translated";
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		Log.info(request.toString());
		String messageId = request.getParameter("message_id");
		String user = request.getParameter("to_user");
		String toContent = new String(Base64.decode(request
				.getParameter("to_content")));
		int cost = Integer.valueOf(request.getParameter("cost"));
		int balance = Integer.valueOf(request.getParameter("balance"));
		plugin.translated(messageId, user, toContent, cost, balance);

		PrintWriter out = response.getWriter();
		out.println("success");

	}

}
