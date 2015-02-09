package org.tttalk.openfire.plugin;

//import java.io.File;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dom4j.Element;
import org.jivesoftware.admin.AuthCheckFilter;
import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import com.github.kevinsawicki.http.HttpRequest;

/**
 * 1. Accept translate request, then call translate api.<br/>
 * 2. Present a callback interface, send result to a particular user.
 *
 * @author zhaolei
 */
public class TranslatorPlugin implements Plugin, PacketInterceptor {
	private static final Logger log = LoggerFactory
			.getLogger(TranslatorPlugin.class);

	private final InterceptorManager interceptorManager;
	private static final String TTTALK_USER_TRANSLATOR = "tttalk.user.translator";

	private static final int ENCRYPT_KEY = 10;
	private static final String TTTALK_APP_KEY = "6m86y";
	private static final String TTTALK_APP_SECRET = "af21d3f06127074be8c8b92e2a0e4d1edde4c484";
	private static final String TTTALK_TRANSLATE_URL = "http://211.149.218.190:8080/litieshuai.api/v1.4/tttalk_api.php";

	public String getTranslator() {
		return JiveGlobals.getProperty(TTTALK_USER_TRANSLATOR);
	}

	private final XMPPServer server;

	public TranslatorPlugin() {
		server = XMPPServer.getInstance();
		interceptorManager = InterceptorManager.getInstance();
		router = server.getMessageRouter();
		AuthCheckFilter.removeExclude("tttalk.translator/translate_callback");
	}

	@Override
	public void initializePlugin(PluginManager pManager, File pluginDirectory) {
		// register with interceptor manager
		interceptorManager.addInterceptor(this);
	}

	@Override
	public void destroyPlugin() {
		// unregister with interceptor manager
		interceptorManager.removeInterceptor(this);
	}

	private final MessageRouter router;

	@Override
	public void interceptPacket(Packet packet, Session session,
			boolean incoming, boolean processed) throws PacketRejectedException {

		if ((!processed) && (incoming) && (packet instanceof Message)) {
			Message msg = (Message) packet;
			log.info(msg.toXML());
			if (msg.getType() == Message.Type.chat) {
				if (packet.getTo().getNode().equals(getTranslator())) {
					log.info("===============Translate================");
					String message = msg.getBody();
					Element tttalk = msg.getChildElement("tttalk",
							"http://jabber.org/protocol/tranlate");
					if (tttalk != null) {
						// TODO: CURL REQUEST TRANSLATE
						String from_lang = tttalk.attribute("fromlang")
								.getValue();
						String to_lang = tttalk.attribute("tolang").getValue();
						String callback_id = String.format("%s %s", tttalk
								.attribute("original_id").getValue(), msg
								.getFrom());

						requestTranslate(message, from_lang, to_lang,
								callback_id);
					}
				}
			}
		}
	}

	public String requestTranslate(String message, String from_lang,
			String to_lang, String callback_id) {
		Map<String, String> postParams = new HashMap<String, String>();
		postParams.put("app_key", TTTALK_APP_KEY);
		postParams.put("app_secret", TTTALK_APP_SECRET);
		postParams.put("content", message);
		postParams.put("from_lang", from_lang);
		postParams.put("to_lang", to_lang);
		postParams.put("callback_id", callback_id);

		return post(TTTALK_TRANSLATE_URL, postParams);
	}

	public String post(String url, Map<String, String> postParams) {
		log.info("post request=" + url + " " + postParams.toString());
		String body = HttpRequest.post(url).form(postParams).body();
		log.info("post response=" + body);
		return body;
	}

	public void processPostCallback(HttpServletRequest request) {

		// String from_lang = request.getParameter("from_lang");
		// String to_lang = request.getParameter("to_lang");
		// String from_content = request.getParameter("from_content");
		String to_content = request.getParameter("to_content");
		String callback_id = request.getParameter("callback_id");

		String items[] = callback_id.split(" ");
		String originId = items[0];
		String user_id = items[1];

		log.info(to_content + " " + callback_id);
		// String auto_translate = request.getParameter("auto_translate");
		// String message_id = request.getParameter("message_id");
		// String loginid = request.getParameter("loginid");
		// // String user_id = request.getParameter("user_id");
		// String app_name = request.getParameter("app_name");
		// String translator_id = request.getParameter("translator_id");

		int cost = 300;
		translateCallback(originId, user_id, to_content, cost);
	}

	public void translateCallback(String originId, String user_id,
			String toContent, int cost) {
		Message message = new Message();
		message.setTo(user_id);
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = "translated";
		message.setSubject(subject);
		message.setBody(toContent);

		Element tttalkNode = message.addChildElement("tttalk",
				"http://jabber.org/protocol/tranlate");
		tttalkNode.addAttribute("original_id", originId);
		tttalkNode.addAttribute("cost", String.valueOf(cost));

		log.info(message.toXML());
		router.route(message);
	}
}
