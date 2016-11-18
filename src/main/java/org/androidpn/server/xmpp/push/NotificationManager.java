package org.androidpn.server.xmpp.push;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.androidpn.server.consdef.ConsDef;
import org.androidpn.server.model.NotificationPO;
import org.androidpn.server.service.NotificationService;
import org.androidpn.server.service.ServiceLocator;
import org.androidpn.server.xmpp.session.ClientSession;
import org.androidpn.server.xmpp.session.SessionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.xmpp.packet.IQ;

/**
 * This class is to manage sending the notifcations to the users.
 * 
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class NotificationManager {

	private static final String NOTIFICATION_NAMESPACE = "androidpn:iq:notification";

	private final Log log = LogFactory.getLog(getClass());

	private SessionManager sessionManager;

	private NotificationService notificationService;
	
	private static NotificationManager instance;


	/**
	 * Constructor.
	 */
	private NotificationManager() {
		sessionManager = SessionManager.getInstance();
		notificationService = ServiceLocator.getNotificationService();
	}
	
	public static NotificationManager getInstance() {
	    if (instance == null) {
	        synchronized (NotificationManager.class) {
	            instance = new NotificationManager();
	        }
	    }
	    return instance;
	}

	/**
	 * Broadcasts a newly created notification message to all connected users.
	 * 
	 * @param apiKey
	 *            the API key
	 * @param title
	 *            the title
	 * @param message
	 *            the message details
	 * @param uri
	 *            the uri
	 */
	public void sendBroadcast(String apiKey, String title, String message,
			String uri) {
		log.debug("sendBroadcast()...");
		List<NotificationPO> notificationMOs = new ArrayList<NotificationPO>();
		IQ notificationIQ = createNotificationIQ(apiKey, title, message, uri);

		for (ClientSession session : sessionManager.getSessions()) {
			if (session.getPresence().isAvailable()) {
				notificationIQ.setTo(session.getAddress());
				session.deliver(notificationIQ);
			}
		}
		try {
			notificationService.createNotifications(notificationMOs);
		} catch (Exception e) {
			log.warn(" notifications insert to database failure!!");
		}

	}

	public void sendAllBroadcast(String apiKey, String title, String message,
			String uri) throws SQLException {
		IQ notificationIQ = createNotificationIQ(apiKey, title, message,
				ConsDef.STR_PUSH_URL);
		this.sendNotifcationToUser(apiKey, "687f1cf85e7811e4ab8200163e00180a",
				title, message, ConsDef.STR_PUSH_URL, notificationIQ);
		// this.sendNotifcationToUser(apiKey, user.getUsername(), title,
		// message, uri,notificationIQ);
	}

	/**
	 * Sends a newly created notification message to the specific user.
	 * 
	 * @param apiKey
	 *            the API key
	 * @param title
	 *            the title
	 * @param message
	 *            the message details
	 * @param uri
	 *            the uri
	 */
	public void sendNotifcationToUser(String apiKey, String username,
			String title, String message, String uri, IQ notificationIQ) {
		log.debug("sendNotifcationToUser()...");
		ClientSession session = sessionManager.getSession(username);

		if (session != null && session.getPresence().isAvailable()) {
			notificationIQ.setTo(session.getAddress());
			session.deliver(notificationIQ);
		}
	}
	
	public void sendNotifcationToUser(String username,IQ notificationIQ){
		log.debug("sendNotifcationToUser()...");
		ClientSession session = sessionManager.getSession(username);
		if (session != null && session.getPresence().isAvailable()) {
			notificationIQ.setTo(session.getAddress());
			session.deliver(notificationIQ);
		}
	}

	/**
	 * Creates a new notification IQ and returns it.
	 */
	public IQ createNotificationIQ(String apiKey, String title,
			String message, String uri) {
		Random random = new Random();
		String id = Integer.toHexString(random.nextInt());

		Element notification = DocumentHelper.createElement(QName.get(
				"notification", NOTIFICATION_NAMESPACE));
		notification.addElement("id").setText(id);
		notification.addElement("apiKey").setText(apiKey);
		notification.addElement("title").setText(title);
		notification.addElement("message").setText(message);
		notification.addElement("uri").setText(uri);

		IQ iq = new IQ();
		iq.setType(IQ.Type.set);
		iq.setChildElement(notification);
		return iq;
	}

	public void sendNotifications(String apiKey, String username, String title,
			String message, String uri) {
		IQ notificationIQ = createNotificationIQ(apiKey, title, message, uri);
		if (username.indexOf(";") != -1) {
			String[] users = username.split(";");
			for (String user : users) {
				this.sendNotifcationToUser(apiKey, user, title, message, uri,
						notificationIQ);
			}
		} else {
			this.sendNotifcationToUser(apiKey, username, title, message, uri,
					notificationIQ);
		}
	}

	public void sendOfflineNotification(NotificationPO notificationMO) {
		log.debug("sendOfflineNotifcation()...");
		IQ notificationIQ = createNotificationIQ(ConsDef.STR_PUSH_API_KEY,
				notificationMO.getStrFromUser(),
				notificationMO.getStrContent(), ConsDef.STR_PUSH_URL);
		notificationIQ.setID(notificationMO.getIntId() + "");
		ClientSession session = sessionManager.getSession(notificationMO
				.getStrToUser());

		if (session != null && session.getPresence().isAvailable()) {
			notificationIQ.setTo(session.getAddress());
			session.deliver(notificationIQ);
		}
	}
}
