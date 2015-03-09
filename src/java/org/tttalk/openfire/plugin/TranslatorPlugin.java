package org.tttalk.openfire.plugin;

//import java.io.File;
import java.io.File;

import org.dom4j.Element;
import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Message;

/**
 * 1. Accept translate request, then call translate api.<br/>
 * 2. Present a callback interface, send result to a particular user.
 *
 * @author zhaolei
 */
public class TranslatorPlugin implements Plugin {
	private static final Logger log = LoggerFactory
			.getLogger(TranslatorPlugin.class);

	private static final String TTTALK_USER_TRANSLATOR = "tttalk.user.translator";

	public String getTranslator() {
		return JiveGlobals.getProperty(TTTALK_USER_TRANSLATOR);
	}

	private final XMPPServer server;

	public TranslatorPlugin() {
		server = XMPPServer.getInstance();
		router = server.getMessageRouter();
	}

	@Override
	public void initializePlugin(PluginManager pManager, File pluginDirectory) {
		// register with interceptor manager
	}

	@Override
	public void destroyPlugin() {
		// unregister with interceptor manager
	}

	private final MessageRouter router;

	public void translated(String messageId, String userId,
			String toContent, int cost, int balance) {
		Message message = new Message();
		message.setTo(userId);
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = "translated";
		message.setSubject(subject);
		message.setBody(toContent);

		Element tttalkNode = message.addChildElement("tttalk",
				"http://jabber.org/protocol/tranlate");
		// tttalkNode.addAttribute("test", "true");
		// tttalkNode.addAttribute("ver", "1");

		tttalkNode.addAttribute("title", "translated");
		tttalkNode.addAttribute("message_id", messageId);
		tttalkNode.addAttribute("cost", String.valueOf(cost));
		tttalkNode.addAttribute("balance", String.valueOf(balance));

		log.info(message.toXML());
		router.route(message);
	}

	public void translating(String messageId, String userId) {
		Message message = new Message();
		message.setTo(userId);
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = "translated";
		message.setSubject(subject);
		message.setBody(subject);

		Element tttalkNode = message.addChildElement("tttalk",
				"http://jabber.org/protocol/tranlate");
		// tttalkNode.addAttribute("test", "true");
		// tttalkNode.addAttribute("ver", "1");

		tttalkNode.addAttribute("title", "translating");
		tttalkNode.addAttribute("message_id", messageId);

		log.info(message.toXML());
		router.route(message);
	}

	public void qa(String[] translators, String qaId, String answer) {
		Message message = new Message();
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = "qa";
		message.setSubject(subject);
		message.setBody(answer);

		Element tttalkNode = message.addChildElement("tttalk",
				"http://jabber.org/protocol/tranlate");

		tttalkNode.addAttribute("title", subject);
		tttalkNode.addAttribute("qa_id", qaId);

		for (String v : translators) {
			message.setTo(v);
			log.info(message.toXML());
			router.route(message);
		}
	}

	public void announcement(String[] translators, String announcementId,
			String title) {
		Message message = new Message();
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = "announcement";
		message.setSubject(subject);
		message.setBody(title);

		Element tttalkNode = message.addChildElement("tttalk",
				"http://jabber.org/protocol/tranlate");

		tttalkNode.addAttribute("title", subject);
		tttalkNode.addAttribute("announcement_id", announcementId);

		for (String v : translators) {
			message.setTo(v);
			log.info(message.toXML());
			router.route(message);
		}
	}
}
