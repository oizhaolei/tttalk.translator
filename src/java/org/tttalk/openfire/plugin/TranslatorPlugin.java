package org.tttalk.openfire.plugin;

//import java.io.File;
import java.io.File;

import org.dom4j.Element;
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

/**
 * 1. Accept translate request, then call translate api.<br/>
 * 2. Present a callback interface, send result to a particular user.
 * 
 * @author zhaolei
 *
 */
public class TranslatorPlugin implements Plugin, PacketInterceptor {
	private static final Logger log = LoggerFactory
			.getLogger(TranslatorPlugin.class);

	private InterceptorManager interceptorManager;
	private static final String TTTALK_USER_TRANSLATOR = "tttalk.user.translator";

	public String getTranslator() {
		return JiveGlobals.getProperty(TTTALK_USER_TRANSLATOR);
	}

	private XMPPServer server;

	public TranslatorPlugin() {
		server = XMPPServer.getInstance();
		interceptorManager = InterceptorManager.getInstance();
		router = server.getMessageRouter();
	}

	public void initializePlugin(PluginManager pManager, File pluginDirectory) {
		// register with interceptor manager
		interceptorManager.addInterceptor(this);
	}

	public void destroyPlugin() {
		// unregister with interceptor manager
		interceptorManager.removeInterceptor(this);
	}

	private MessageRouter router;

	public void interceptPacket(Packet packet, Session session,
			boolean incoming, boolean processed) throws PacketRejectedException {

		if ((!processed) && (incoming) && (packet instanceof Message)) {
			Message msg = (Message) packet;
			if (msg.getType() == Message.Type.chat) {
				if (packet.getTo().getNode().equals(getTranslator())) {
					// REQUEST TRANSLATE
					Element fromlang = msg.getChildElement("fromlang",
							"http://jabber.org/protocol/tranlate");
					Element tolang = msg.getChildElement("tolang",
							"http://jabber.org/protocol/tranlate");
					if (fromlang != null && tolang != null) {
						log.info(msg.toXML());
					}
				}
			}
		}
	}

	public void translateCallback(String id, String user, String toContent,
			int cost) {

		Message message = new Message();
		message.setTo(user);
		message.setFrom(getTranslator());
		String subject = "translated";
		message.setSubject(subject);
		message.setBody(toContent);

		Element idNode = message.addChildElement("origin_id",
				"http://jabber.org/protocol/tranlate");
		idNode.setText(id);
		Element costNode = message.addChildElement("cost",
				"http://jabber.org/protocol/tranlate");
		costNode.setText(String.valueOf(cost));
		
		log.info(message.toXML());
		router.route(message);
	}

}
