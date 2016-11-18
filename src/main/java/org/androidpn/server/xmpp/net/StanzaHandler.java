package org.androidpn.server.xmpp.net;

import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.Random;

import org.androidpn.server.service.NotificationService;
import org.androidpn.server.service.ServiceLocator;
import org.androidpn.server.util.Config;
import org.androidpn.server.xmpp.push.NotificationManager;
import org.androidpn.server.xmpp.router.PacketRouter;
import org.androidpn.server.xmpp.session.ClientSession;
import org.androidpn.server.xmpp.session.Session;
import org.androidpn.server.xmpp.session.SessionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.dom4j.io.XMPPPacketReader;
import org.source.openfire.net.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.PacketError;
import org.xmpp.packet.Presence;
import org.xmpp.packet.Roster;
import org.xmpp.packet.StreamError;

/**
 * This class is to handle incoming XML stanzas.
 * 
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class StanzaHandler {

	private static final Log log = LogFactory.getLog(StanzaHandler.class);

	protected Connection connection;

	protected Session session;

	protected String serverName;

	private boolean sessionCreated = false;

	private boolean startedTLS = false;

	private PacketRouter router;

	private NotificationService notificationService;

	@SuppressWarnings("unused")
	private NotificationManager notificationManager;

	private static final String NOTIFICATION_NAMESPACE = "androidpn:iq:notification";
	private SessionManager sessionManager;

	/**
	 * Constructor.
	 * 
	 * @param serverName
	 *            the server name
	 * @param connection
	 *            the connection
	 */
	public StanzaHandler(String serverName, Connection connection) {
		this.serverName = serverName;
		this.connection = connection;
		this.router = new PacketRouter();
		this.notificationService = ServiceLocator.getNotificationService();
		this.notificationManager = NotificationManager.getInstance();
	}

	/**
	 * Process the received stanza using the given XMPP packet reader.
	 * 
	 * @param stanza
	 *            the received statza
	 * @param reader
	 *            the XMPP packet reader
	 * @throws Exception
	 *             if the XML stream is not valid.
	 */
	public void process(String stanza, XMPPPacketReader reader)
			throws Exception {
		boolean initialStream = stanza.startsWith("<stream:stream");
		if (!sessionCreated || initialStream) {
			if (!initialStream) {
				return;
			}
			if (!sessionCreated) {
				sessionCreated = true;
				MXParser parser = reader.getXPPParser();
				parser.setInput(new StringReader(stanza));
				createSession(parser);
			} else if (startedTLS) {
				startedTLS = false;
				tlsNegotiated();
			}
			return;
		}

		// If end of stream was requested
		if (stanza.equals("</stream:stream>")) {
			session.close();
			return;
		}
		// Ignore <?xml version="1.0"?>
		if (stanza.startsWith("<?xml")) {
			return;
		}
		// Create DOM object
		Element doc = reader.read(new StringReader(stanza)).getRootElement();
		if (doc == null) {
			return;
		}

		String tag = doc.getName();
		if ("starttls".equals(tag)) {
			if (negotiateTLS()) { // Negotiate TLS
				startedTLS = true;
			} else {
				connection.close();
				session = null;
			}
		} else if ("message".equals(tag)) {
			processMessage(doc);
		} else if ("presence".equals(tag)) {
			log.debug("presence...");
			processPresence(doc);
		} else if ("iq".equals(tag)) {
			log.debug("iq...");
			processIQ(doc);
		} else {
			log.warn("Unexpected packet tag (not message, iq, presence)"
					+ doc.asXML());
			session.close();
		}

	}

	private void processMessage(Element doc) {
		log.debug("processMessage()...");
		Message packet;
		try {
			packet = new Message(doc, false);
		} catch (IllegalArgumentException e) {
			log.debug("Rejecting packet. JID malformed", e);
			Message reply = new Message();
			reply.setID(doc.attributeValue("id"));
			reply.setTo(session.getAddress());
			reply.getElement().addAttribute("from", doc.attributeValue("to"));
			reply.setError(PacketError.Condition.jid_malformed);
			session.process(reply);
			return;
		}

		packet.setFrom(session.getAddress());
		router.route(packet);
		session.incrementClientPacketCount();
	}

	/**
	 * 
	 * @param doc
	 * @throws SQLException
	 *             其中包含断线重连发送消息机制
	 */
	private void processPresence(Element doc) throws SQLException {
		log.debug("processPresence()...");
		Presence packet;
		try {
			packet = new Presence(doc, false);
			System.out.println(doc.asXML());
		} catch (IllegalArgumentException e) {
			log.debug("Rejecting packet. JID malformed", e);
			Presence reply = new Presence();
			reply.setID(doc.attributeValue("id"));
			reply.setTo(session.getAddress());
			reply.getElement().addAttribute("from", doc.attributeValue("to"));
			reply.setError(PacketError.Condition.jid_malformed);
			session.process(reply);
			return;
		}
		if (session.getStatus() == Session.STATUS_CLOSED
				&& packet.isAvailable()) {
			log.warn("Ignoring available presence packet of closed session: "
					+ packet);
			return;
		}
		packet.setFrom(session.getAddress());
		router.route(packet);
		session.incrementClientPacketCount();

		// if(session.getStatus() == Session.STATUS_AUTHENTICATED &&
		// packet.isAvailable()){
		// String userName = session.getAddress().getNode();
		// if(null != userName && !"".equals(userName)){
		// Integer infoCount =
		// notificationService.queryNotificationInfoCount(userName);
		// Integer busInfoCount =
		// notificationService.queryNotificationBusInfoCount(userName);
		//
		// if(null == sessionManager){
		// sessionManager = SessionManager.getInstance();
		// }
		//
		// if(infoCount > 0){
		// IQ notificationIQ = createNotificationIQ(ConsDef.STR_PUSH_API_KEY,
		// ConsDef.STR_PUSH_TITTLE,("您好，您有" + infoCount +
		// "条未读消息"),ConsDef.STR_PUSH_URL,ConsDef.STR_PUSH_INFO_TYPE_I_INIT,infoCount
		// + "");
		// notificationIQ.setID(UUID.randomUUID().toString().replace("-",""));
		// ClientSession session = sessionManager.getSession(userName);
		// if (session != null && session.getPresence().isAvailable()) {
		// notificationIQ.setTo(session.getAddress());
		// session.deliver(notificationIQ);
		// }
		//
		// }
		//
		// if(busInfoCount > 0){
		// IQ notificationIQ =
		// createNotificationIQ(ConsDef.STR_PUSH_API_KEY,ConsDef.STR_PUSH_TITTLE,("您好，您有"
		// + busInfoCount +
		// "条未读资讯"),ConsDef.STR_PUSH_URL,ConsDef.STR_PUSH_INFO_TYPE_BI_INIT,busInfoCount
		// + "");
		// notificationIQ.setID(UUID.randomUUID().toString().replace("-",""));
		// ClientSession session = sessionManager.getSession(userName);
		// if (session != null && session.getPresence().isAvailable()) {
		// notificationIQ.setTo(session.getAddress());
		// session.deliver(notificationIQ);
		// }
		// }

		/**
		 * //查询该用户的离线消息并发送 List<NotificationPO> list =
		 * notificationService.queryNotification(userName); if(!list.isEmpty()){
		 * for (NotificationPO notificationMO : list) {
		 * notificationManager.sendOfflineNotification(notificationMO); } }
		 * 
		 * else{ log.info(" no offline notification, username = " + userName); }
		 **/
		// }else{
		// log.warn("userName is null !!!!!!");
		// }

		// }
	}

//	private IQ createNotificationIQ(String apiKey, String title,
//			String message, String uri, String messageType, String messageCount) {
//		Random random = new Random();
//		String id = Integer.toHexString(random.nextInt());
//
//		Element notification = DocumentHelper.createElement(QName.get(
//				"notification", NOTIFICATION_NAMESPACE));
//		notification.addElement("id").setText(id);
//		notification.addElement("apiKey").setText(apiKey);
//		notification.addElement("title").setText(title);
//		notification.addElement("message").setText(message);
//		notification.addElement("uri").setText(uri);
//
//		notification.addElement("messageType").setText(messageType); // 消息类型（咨询、消息）
//		notification.addElement("messageCount").setText(messageCount); // 消息数量（离线消息会用到）
//
//		IQ iq = new IQ();
//		iq.setType(IQ.Type.set);
//		iq.setChildElement(notification);
//		return iq;
//	}

	private void processIQ(Element doc) {
		log.debug("processIQ()...");
		IQ packet;
		try {
			packet = getIQ(doc);
		} catch (IllegalArgumentException e) {
			log.debug("Rejecting packet. JID malformed", e);
			IQ reply = new IQ();
			if (!doc.elements().isEmpty()) {
				reply.setChildElement(((Element) doc.elements().get(0))
						.createCopy());
			}
			reply.setID(doc.attributeValue("id"));
			reply.setTo(session.getAddress());
			String to = doc.attributeValue("to");
			if (to != null) {
				reply.getElement().addAttribute("from", to);
			}
			reply.setError(PacketError.Condition.jid_malformed);
			session.process(reply);
			return;
		}

		// if (packet.getID() == null) {
		// // IQ packets MUST have an 'id' attribute
		// StreamError error = new StreamError(
		// StreamError.Condition.invalid_xml);
		// session.deliverRawText(error.toXML());
		// session.close();
		// return;
		// }

		packet.setFrom(session.getAddress());
		router.route(packet);
		session.incrementClientPacketCount();
	}

	private IQ getIQ(Element doc) {
		Element query = doc.element("query");
		if (query != null && "jabber:iq:roster".equals(query.getNamespaceURI())) {
			return new Roster(doc);
		} else {
			return new IQ(doc, false);
		}
	}

	private void createSession(XmlPullParser xpp)
			throws XmlPullParserException, IOException {
		for (int eventType = xpp.getEventType(); eventType != XmlPullParser.START_TAG;) {
			eventType = xpp.next();
		}
		// Create the correct session based on the sent namespace
		String namespace = xpp.getNamespace(null);
		if ("jabber:client".equals(namespace)) {
			session = ClientSession.createSession(serverName, connection, xpp);
			if (session == null) {
				StringBuilder sb = new StringBuilder(250);
				sb.append("<?xml version='1.0' encoding='UTF-8'?>");
				sb.append("<stream:stream from=\"").append(serverName);
				sb.append("\" id=\"").append(randomString(5));
				sb.append("\" xmlns=\"").append(xpp.getNamespace(null));
				sb.append("\" xmlns:stream=\"").append(
						xpp.getNamespace("stream"));
				sb.append("\" version=\"1.0\">");

				// bad-namespace-prefix in the response
				StreamError error = new StreamError(
						StreamError.Condition.bad_namespace_prefix);
				sb.append(error.toXML());
				connection.deliverRawText(sb.toString());
				connection.close();
				log.warn("Closing session due to bad_namespace_prefix in stream header: "
						+ namespace);
			}
		}
	}

	private boolean negotiateTLS() {
		if (connection.getTlsPolicy() == Connection.TLSPolicy.disabled) {
			// Set the not_authorized error
			StreamError error = new StreamError(
					StreamError.Condition.not_authorized);
			connection.deliverRawText(error.toXML());
			connection.close();
			log.warn("TLS requested by initiator when TLS was never offered"
					+ " by server. Closing connection : " + connection);
			return false;
		}
		// Client requested to secure the connection using TLS.
		try {
			startTLS();
		} catch (Exception e) {
			log.error("Error while negotiating TLS", e);
			connection
					.deliverRawText("<failure xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\">");
			connection.close();
			return false;
		}
		return true;
	}

	private void startTLS() throws Exception {
		Connection.ClientAuth policy;
		try {
			policy = Connection.ClientAuth.valueOf(Config.getString(
					"xmpp.client.cert.policy", "disabled"));
		} catch (IllegalArgumentException e) {
			policy = Connection.ClientAuth.disabled;
		}
		connection.startTLS(policy);
	}

	private void tlsNegotiated() {
		// Offer stream features including SASL Mechanisms
		StringBuilder sb = new StringBuilder(620);
		sb.append("<?xml version='1.0' encoding='UTF-8'?>");
		sb.append("<stream:stream ");
		sb.append("xmlns:stream=\"http://etherx.jabber.org/streams\" ");
		sb.append("xmlns=\"jabber:client\" from=\"");
		sb.append(serverName);
		sb.append("\" id=\"");
		sb.append(session.getStreamID());
		sb.append("\" xml:lang=\"");
		sb.append(connection.getLanguage());
		sb.append("\" version=\"");
		sb.append(Session.MAJOR_VERSION).append(".")
				.append(Session.MINOR_VERSION);
		sb.append("\">");
		sb.append("<stream:features>");
		// Include specific features such as auth and register for client
		// sessions
		String specificFeatures = session.getAvailableStreamFeatures();
		if (specificFeatures != null) {
			sb.append(specificFeatures);
		}
		sb.append("</stream:features>");
		connection.deliverRawText(sb.toString());
	}

	private String randomString(int length) {
		if (length < 1) {
			return null;
		}
		char[] numbersAndLetters = ("0123456789abcdefghijklmnopqrstuvwxyz"
				+ "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();
		char[] randBuffer = new char[length];
		for (int i = 0; i < randBuffer.length; i++) {
			randBuffer[i] = numbersAndLetters[new Random().nextInt(71)];
		}
		return new String(randBuffer);
	}

	// public String getNamespace() {
	// return "jabber:client";
	// }

}
