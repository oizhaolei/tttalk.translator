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
public class FriendServlet extends AbstractTranslatorServlet {
	private static final long serialVersionUID = 1159875340630997082L;
	private static final Logger Log = LoggerFactory
			.getLogger(FriendServlet.class);

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Log.info(request.toString());

		String friend_id = request.getParameter("friend_id");
		String fullname = request.getParameter("fullname");
		String[] translators = request.getParameter("translators").split(",");
		plugin.friend(translators, friend_id, fullname);

		PrintWriter out = response.getWriter();
		out.println("success");
	}

	@Override
	String getUri() {
		return "/friend";
	}
}
