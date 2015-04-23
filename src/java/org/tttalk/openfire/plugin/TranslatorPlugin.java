package org.tttalk.openfire.plugin;

//import java.io.File;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Element;
import org.gearman.client.GearmanClient;
import org.gearman.client.GearmanClientImpl;
import org.gearman.client.GearmanJob;
import org.gearman.client.GearmanJobImpl;
import org.gearman.common.GearmanNIOJobServerConnection;
import org.gearman.util.ByteUtils;
import org.jivesoftware.openfire.MessageRouter;
import org.jivesoftware.openfire.OfflineMessage;
import org.jivesoftware.openfire.OfflineMessageStore;
import org.jivesoftware.openfire.PresenceManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserAlreadyExistsException;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

/**
 * 1. Accept translate request, then call translate api.<br/>
 * 2. Present a callback interface, send result to a particular user.
 *
 * @author zhaolei
 */
public class TranslatorPlugin implements Plugin, PacketInterceptor {
	private static final Logger log = LoggerFactory
			.getLogger(TranslatorPlugin.class);
	public static final String PLUGIN_NAME = "tttalk.translator";

	private static final String APP_NAME = "chinatalk";

	private static final String TTTALK_NAMESPACE = "http://tttalk.org/protocol/tttalk";
	private static final String TAG_TRANSLATED = "translated";
	private static final String TAG_TRANSLATING = "translating";
	private static final String TAG_TTTALK = "tttalk";
	private static final String TAG_OLD_VERSION_TRANSLATED = "old_version_translated";

	private static final String REQUEST_TAG = "request";
	private static final String RECEIVED_TAG = "received";
	private static final String RECEIVED_NAMASPACE = "urn:xmpp:receipts";
	private static final String DELAY_TAG = "delay";
	private static final String DELAY_NAMASPACE = "urn:xmpp:delay";

	private static final String CHAT_TYPE_TEXT = "text";
	private static final int AUTO_BAIDU = 2;
	private static final int AUTO_MANUAL = 1;
	private static final int AUTO_NONE = 0;

	private static final String TTTALK_USER_TRANSLATOR = "tttalk.user.translator";
	private final InterceptorManager interceptorManager;

	public String getTranslator() {
		return JiveGlobals.getProperty(TTTALK_USER_TRANSLATOR);
	}

	private final XMPPServer server;
	private final UserManager userManager;
	private final PresenceManager presenceManager;
	private final OfflineMessageStore offlineMessageStore;

	public TranslatorPlugin() {
		server = XMPPServer.getInstance();
		router = server.getMessageRouter();
		userManager = server.getUserManager();
		interceptorManager = InterceptorManager.getInstance();
		presenceManager = server.getPresenceManager();
		gearmanClient = genGearmanClient();
		offlineMessageStore = OfflineMessageStore.getInstance();

	}

	private GearmanClient genGearmanClient() {
		GearmanClientImpl gearmanClient = new GearmanClientImpl();
		GearmanNIOJobServerConnection gearmanConnection = new GearmanNIOJobServerConnection(
				Utils.getGearmanHost(), Utils.getGearmanPort());
		gearmanClient.addJobServer(gearmanConnection);
		return gearmanClient;
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
	private GearmanClient gearmanClient;

	public void translated(String messageId, String userId, String toContent,
			String cost, String auto_translate) {
		Message message = new Message();
		message.setTo(userId);
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = TAG_TRANSLATED;
		message.setSubject(subject);
		message.setBody(toContent);

		Element tttalkNode = message.addChildElement(TAG_TRANSLATED,
				TTTALK_NAMESPACE);
		// tttalkNode.addAttribute("test", "true");
		// tttalkNode.addAttribute("ver", "1");

		tttalkNode.addAttribute("title", subject);
		tttalkNode.addAttribute("message_id", messageId);
		tttalkNode.addAttribute("cost", cost);
		tttalkNode.addAttribute("auto_translate", auto_translate);

		addRequestReceipts(message);

		log.info(message.toXML());
		router.route(message);
	}

	public void translating(String messageId, String userId) {
		Message message = new Message();
		message.setTo(userId);
		message.setFrom(getTranslator() + "@"
				+ server.getServerInfo().getXMPPDomain());
		String subject = TAG_TRANSLATING;
		message.setSubject(subject);
		message.setBody(subject);

		Element tttalkNode = message.addChildElement(TAG_TRANSLATING,
				TTTALK_NAMESPACE);
		// tttalkNode.addAttribute("test", "true");
		// tttalkNode.addAttribute("ver", "1");

		tttalkNode.addAttribute("title", subject);
		tttalkNode.addAttribute("message_id", messageId);

		addRequestReceipts(message);

		log.info(message.toXML());
		router.route(message);
	}

	public void oldVersionTranslated(String[] to_users, String message_id,
			String userid, String from_lang, String to_lang, String file_path,
			String file_type, String file_length, String from_content,
			String to_content, String create_date) {
		Message message = new Message();
		message.setType(Message.Type.chat);
		message.setID(userid);

		message.setFrom(createXMPPuser(userid));
		String subject = TAG_OLD_VERSION_TRANSLATED;
		message.setSubject(subject);

		Element tttalkNode = message.addChildElement(
				TAG_OLD_VERSION_TRANSLATED, TTTALK_NAMESPACE);

		tttalkNode.addAttribute("title", subject);
		tttalkNode.addAttribute("message_id", message_id);
		tttalkNode.addAttribute("userid", userid);
		tttalkNode.addAttribute("from_lang", from_lang);
		tttalkNode.addAttribute("to_lang", to_lang);
		tttalkNode.addAttribute("file_path", file_path);
		tttalkNode.addAttribute("file_type", file_type);
		tttalkNode.addAttribute("file_length", file_length);
		tttalkNode.addAttribute("from_content", from_content);
		tttalkNode.addAttribute("to_content", to_content);
		tttalkNode.addAttribute("create_date", create_date);

		addRequestReceipts(message);

		for (String v : to_users) {
			message.setTo(v);
			log.info(message.toXML());
			router.route(message);
		}
	}

	public String createXMPPuser(String userid) {
		return "chinatalk_" + userid + "@tttalk.org/tttalk";
	}

	public void changePassword(String username, String password) {

		try {
			User user = userManager.getUser(username);
			user.setPassword(password);
			log.info(String.format("changePassword:%s,%s", username, password));
		} catch (UserNotFoundException e) {
			log.error(username, e);
		}
	}

	public void createAccount(String username, String password) {
		try {
			User user = userManager.createUser(username, password, null, null);
			log.info(String.format("createAccount:%s,%s", user.getUID(),
					user.getUsername()));
		} catch (UserAlreadyExistsException e) {
			log.error(username + " UserAlreadyExists.");
		}
	}

	@Override
	public void interceptPacket(Packet packet, Session session,
			boolean incoming, boolean processed) throws PacketRejectedException {

		// Save to offline table
		saveMessageToOfflineTable(packet, session, incoming, processed);

		if ((!processed) && (incoming) && (packet instanceof Message)) {
			Message msg = (Message) packet;
			if (msg.getType() == Message.Type.chat) {
				Element tttalk = msg.getChildElement(TAG_TTTALK,
						TTTALK_NAMESPACE);
				if (tttalk != null) {
					String from_lang = tttalk.attributeValue("from_lang");
					String to_lang = tttalk.attributeValue("to_lang");
					String type = tttalk.attributeValue("type");
					// String auto_translate = tttalk
					// .attributeValue("auto_translate");
					log.info(msg.toXML());
					if (from_lang != null && to_lang != null
							&& CHAT_TYPE_TEXT.equalsIgnoreCase(type)
							&& !from_lang.equalsIgnoreCase(to_lang)) {
						String auto_translate = null;
						String username = getUsernameFromJID(msg.getTo());
						String key = getUsernameFromJID(msg.getFrom())
								+ "_auto_translate";
						auto_translate = getProperty(username, key,
								String.valueOf(AUTO_BAIDU));

						tttalk.addAttribute("auto_translate", auto_translate);
						int at_mode = Integer.parseInt(auto_translate);
						log.info(String.format("auto_translate=%d", at_mode));
						switch (at_mode) {
						case AUTO_NONE:
							log.info("AUTO_NONE");
							break;
						case AUTO_MANUAL:
							log.info("AUTO_MANUAL START");
							requestManualTranslate(msg);
							log.info("AUTO_MANUAL END");
							break;
						case AUTO_BAIDU:

							log.info("AUTO_BAIDU");
							requestBaiduTranslate(msg);
							break;
						}
					}
				}

				/**
				 * 百度推送，apns推送通知
				 */

				// tttalk
				// old_version_translated
				try {
					String packetId = msg.getID();
					String fromTTTalkId = getTTTalkId(msg.getFrom());
					String toTTTalkId = getTTTalkId(msg.getTo());
					String body = msg.getBody();

					if (tttalk != null) {
						log.info("submitTTTalkJob");

						submitTTTalkJob(packetId, fromTTTalkId, toTTTalkId,
								body, tttalk);
						return;
					}
					Element oldVersion = msg.getChildElement(
							TAG_OLD_VERSION_TRANSLATED, TTTALK_NAMESPACE);
					if (oldVersion != null) {
						log.info("submitOldVersionJob");
						submitOldVersionJob(packetId, toTTTalkId, oldVersion);
						return;
					}

				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}

			}
			deleteMessageFromOfflineTable(msg);
		}
	}

	private void addRequestReceipts(Message message) {
		if (message.getID() == null) {
			message.setID(String.valueOf(System.currentTimeMillis()));
		}
		message.addChildElement(REQUEST_TAG, RECEIVED_NAMASPACE);
	}

	private void saveMessageToOfflineTable(Packet packet, Session session,
			boolean incoming, boolean processed) {

		if ((processed) && (!incoming) && (packet instanceof Message)
				&& isUserAvailable(packet.getTo().getNode())) {
			Message msg = (Message) packet;
			Element received = msg.getChildElement(RECEIVED_TAG,
					RECEIVED_NAMASPACE);
			Element delay = msg.getChildElement(DELAY_TAG, DELAY_NAMASPACE);
			if (delay == null && received == null) {
				offlineMessageStore.addMessage((Message) packet);
			}
		}
	}

	private boolean isUserAvailable(String username) {
		try {
			User user = userManager.getUser(username);
			log.info(String.format("user %s %s", user.getName(),
					presenceManager.isAvailable(user) ? "available"
							: "not available"));
			return presenceManager.isAvailable(user);
		} catch (Exception e) {
			return false;
		}
	}

	private void deleteMessageFromOfflineTable(Message receivedMessage) {
		Element received = receivedMessage.getChildElement(RECEIVED_TAG,
				RECEIVED_NAMASPACE);
		if (received == null)
			return;

		String receivedId = received.attributeValue("id");
		log.info(String.format("Received %s=>%s(%s)", receivedMessage.getFrom()
				.getNode(), receivedMessage.getTo().getNode(), receivedId));

		Collection<OfflineMessage> offlineMessages = offlineMessageStore
				.getMessages(receivedMessage.getFrom().getNode(), false);
		Iterator<OfflineMessage> iterator = offlineMessages.iterator();

		while (iterator.hasNext()) {
			OfflineMessage offlineMessage = iterator.next();
			log.info(String.format("offlineMessage= %s %d %s", receivedMessage
					.getFrom().getNode(), offlineMessage.getCreationDate()
					.getTime(), offlineMessage.getID()));
			if (receivedId.equals(offlineMessage.getID())) {
				offlineMessageStore.deleteMessage(receivedMessage.getFrom()
						.getNode(), offlineMessage.getCreationDate());
				break;
			}
		}
	}

	private void submitOldVersionJob(String packetId, String toTTTalkId,
			Element oldVersion) throws Exception {
		String function = "push_message";

		String file_type = oldVersion.attributeValue("file_type");
		String from_content = oldVersion.attributeValue("from_content");
		String to_content = oldVersion.attributeValue("to_content");
		String userid = oldVersion.attributeValue("userid");
		String to_lang = oldVersion.attributeValue("to_lang");
		String to_userid = toTTTalkId;

		JSONObject jo = new JSONObject();
		jo.put("app_name", APP_NAME);
		jo.put("userid", userid);
		jo.put("title", TAG_OLD_VERSION_TRANSLATED);
		jo.put("file_type", file_type);
		jo.put("from_content", from_content);
		jo.put("to_content", to_content);
		jo.put("to_lang", to_lang);
		jo.put("user_id", to_userid);

		byte[] data = ByteUtils.toUTF8Bytes(jo.toString());
		String uniqueId = null;
		createBackgroundJob(function, data, uniqueId);
	}

	private void createBackgroundJob(String function, byte[] data,
			String uniqueId) throws IOException {
		GearmanJob job = GearmanJobImpl.createBackgroundJob(function, data,
				uniqueId);
		try {
			gearmanClient.submit(job);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			gearmanClient = genGearmanClient();
			gearmanClient.submit(job);
		}
	}

	private void submitTTTalkJob(String packetId, String fromTTTalkId,
			String toTTTalkId, String body, Element tttalk) throws Exception {
		String function = "push_message";

		String type = tttalk.attributeValue("type");
		String to_lang = tttalk.attributeValue("to_lang");

		JSONObject jo = new JSONObject();
		jo.put("app_name", APP_NAME);
		jo.put("title", TAG_TTTALK);
		jo.put("type", type);
		jo.put("userid", fromTTTalkId);
		jo.put("user_id", toTTTalkId);
		jo.put("body", body);
		jo.put("to_lang", to_lang);
		jo.put("pid", packetId);
		log.info("submitTTTalkJob: " + jo.toString());

		byte[] data = ByteUtils.toUTF8Bytes(jo.toString());
		String uniqueId = null;
		createBackgroundJob(function, data, uniqueId);
	}

	private String getTTTalkId(JID jid) {
		String temp = jid.toString();
		String tttalkId = null;
		String pattern = "chinatalk_";
		if (temp.contains(pattern)) {
			tttalkId = temp.substring("chinatalk_".length(), temp.indexOf("@"));
		}
		return tttalkId;
	}

	private String getUsernameFromJID(JID jid) {
		String temp = jid.toString();
		return temp.substring(0, temp.indexOf("@"));
	}

	public void requestBaiduTranslate(Message msg) {
		Element tttalk = msg.getChildElement(TAG_TTTALK, TTTALK_NAMESPACE);
		tttalk.addAttribute("message_id",
				String.valueOf(System.currentTimeMillis()));

		new Thread(new BaiduTranslateRunnable(msg), "Baidu").start();
	}

	public class BaiduTranslateRunnable implements Runnable {

		private final Message msg;

		public BaiduTranslateRunnable(Message msg) {
			this.msg = msg;
		}

		@Override
		public void run() {
			Element tttalk = msg.getChildElement(TAG_TTTALK, TTTALK_NAMESPACE);
			String userid = getTTTalkId(msg.getTo());
			String message_id = tttalk.attributeValue("message_id");

			final Map<String, String> postParams = new HashMap<String, String>();
			postParams.put("userid", userid);
			postParams.put("loginid", userid);
			postParams.put("from_lang", tttalk.attributeValue("from_lang"));
			postParams.put("to_lang", tttalk.attributeValue("to_lang"));
			postParams.put("text", msg.getBody());
			String response = Utils.post(Utils.getBaiduTranslateUrl(),
					postParams);
			String to_content = parseBaiduResponse(response);

			translated(message_id, msg.getTo().toString(), to_content, "0", "1");
		}
	}

	public class ManualTranslateRunnable implements Runnable {

		private final Message msg;

		public ManualTranslateRunnable(Message msg) {
			this.msg = msg;
		}

		@Override
		public void run() {
			Element tttalk = msg.getChildElement(TAG_TTTALK, TTTALK_NAMESPACE);
			String userid = getTTTalkId(msg.getTo());

			Map<String, String> postParams = new HashMap<String, String>();

			postParams.put("userid", userid);
			postParams.put("loginid", userid);
			postParams.put("local_id",
					String.valueOf(System.currentTimeMillis()));
			postParams.put("from_lang", tttalk.attributeValue("from_lang"));
			postParams.put("to_lang", tttalk.attributeValue("to_lang"));
			postParams.put("text", msg.getBody());
			postParams.put("filetype", tttalk.attributeValue("type"));
			postParams.put("local_id", tttalk.attributeValue("message_id"));
			postParams.put("content_length",
					tttalk.attributeValue("content_length"));
			postParams.put("to_userid", String.valueOf(-1));

			Utils.post(Utils.getManualTranslateUrl(), postParams);

		}
	}

	public void requestManualTranslate(Message msg) {
		Element tttalk = msg.getChildElement(TAG_TTTALK, TTTALK_NAMESPACE);
		tttalk.addAttribute("message_id",
				String.valueOf(System.currentTimeMillis()));

		new Thread(new ManualTranslateRunnable(msg), "Manual").start();
	}

	private String parseBaiduResponse(String body) {
		try {
			JSONObject obj = new JSONObject(body);
			boolean success = obj.getBoolean("success");
			if (success) {
				JSONObject data = obj.getJSONObject("data");
				return data.getString("to_content");
			}
		} catch (JSONException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public String getBaiduTranslateUrl() {
		return Utils.getBaiduTranslateUrl();
	}

	public void setBaiduTranslateUrl(String url) {
		Utils.setBaiduTranslateUrl(url);
	}

	public String getManualTranslateUrl() {
		return Utils.getManualTranslateUrl();
	}

	public void setManualTranslateUrl(String url) {
		Utils.setManualTranslateUrl(url);
	}

	public String getProperty(String username, String key, String def) {
		try {
			User user = userManager.getUser(username);
			if (key == null)
				return user.getProperties().toString();
			String value = user.getProperties().get(key);
			if (value == null) {
				value = def;
			}
			return value;
		} catch (UserNotFoundException e) {
			log.error(username, e);
			return def;
		}
	}

	public void updateProperty(String username, String key, String value)
			throws UserNotFoundException {
		User user = userManager.getUser(username);
		user.getProperties().put(key, value);
	}
}
