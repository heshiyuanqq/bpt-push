/**
 * 
 */
package org.androidpn.server.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.androidpn.server.consdef.ConsDef;
import org.androidpn.server.model.NotificationPO;
import org.androidpn.server.service.NotificationService;
import org.androidpn.server.service.UserNotFoundException;
import org.androidpn.server.xmpp.push.NotificationManager;
import org.androidpn.server.xmpp.session.ClientSession;
import org.androidpn.server.xmpp.session.SessionManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.xmpp.packet.IQ;

import com.cmri.bpt.common.user.UserContext;
import com.cmri.bpt.common.user.UserContextHolder;
import com.cmri.bpt.entity.auth.AppTokenSession;
import com.cmri.bpt.entity.push.PushEnum;
import com.cmri.bpt.service.token.AppTokenSessionService;
//import com.cmri.bpt.web.servlet.UserNotFoundException;

public class NotificationServiceImpl implements NotificationService {
	
	private static final Logger logger = Logger.getLogger(NotificationServiceImpl.class);
	
	@Autowired
	private AppTokenSessionService apptokenserver;

	@Override
	public void saveNotification(NotificationPO notificationPO)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateNotification(NotificationPO notificationPO)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public NotificationPO queryNotificationById(Long id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createNotifications(List<NotificationPO> notificationPOs)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public NotificationPO queryNotificationByUserName(String userName,
			String messageId) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NotificationPO> queryNotification(String username)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer queryNotificationInfoCount(String username)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer queryNotificationBusInfoCount(String username)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 任务下发
	 */
	@Override
	public void sendCmd(AppTokenSession tokenSession, String message) {
				// 生成推送信息
				IQ notificationIQ = NotificationManager.getInstance().createNotificationIQ(ConsDef.STR_PUSH_API_KEY, PushEnum.SendCase,
						message, ConsDef.STR_PUSH_URL);
				// 向终端发起推送
				if (tokenSession != null) {
						NotificationManager.getInstance().sendNotifcationToUser(tokenSession.getXppId(), notificationIQ);
						logger.debug("向 " + tokenSession.getXppId() + " 分组:" + tokenSession.getTag() + " 推送：" + message);
				}
	}
	
	@Override
	public void stopCmd(List<AppTokenSession> tokenSessions,String message){
				IQ notificationIQ = NotificationManager.getInstance().createNotificationIQ(ConsDef.STR_PUSH_API_KEY, PushEnum.StopCase,
						message, ConsDef.STR_PUSH_URL);
				NotificationManager notiMgr = NotificationManager.getInstance();
				for (AppTokenSession sessionItem : tokenSessions) {
						notiMgr.sendNotifcationToUser(sessionItem.getXppId(), notificationIQ);
				}
	}
	
	
	
	/**
	 * 推送命令之：飞行模式(PushEnum:FlyModeOfUe = "Cmd_Ue_Flymode";)
	 * @param message
	 * @return
	 */
	public void flymodeCmd(List<AppTokenSession>  tokenSessions,String message){
					NotificationManager manager = NotificationManager.getInstance();
					IQ notificationIQ = manager.createNotificationIQ(ConsDef.STR_PUSH_API_KEY, PushEnum.FlyModeOfUe,
							message, ConsDef.STR_PUSH_URL);
					for (AppTokenSession sessionItem : tokenSessions) {
							manager.sendNotifcationToUser(sessionItem.getXppId(), notificationIQ);
					}
	}
	
	
	
	public void stopCaseLogCmd(List<AppTokenSession> tokenSessions,String message){
			    NotificationManager manager = NotificationManager.getInstance();
			    
				IQ notificationIQ = manager.createNotificationIQ(ConsDef.STR_PUSH_API_KEY, PushEnum.StopCaseLog,
						message, ConsDef.STR_PUSH_URL);
		
				for (AppTokenSession sessionItem : tokenSessions) {
						manager.sendNotifcationToUser(sessionItem.getXppId(), notificationIQ);
				}
	}
	
	
	public void receiveLogCmd(AppTokenSession tokenSession,String message){
		    NotificationManager manager = NotificationManager.getInstance();
			IQ notificationIQ = manager.createNotificationIQ(ConsDef.STR_PUSH_API_KEY,
					PushEnum.ReceiveCaseLog, message, ConsDef.STR_PUSH_URL);
	
			manager.sendNotifcationToUser(tokenSession.getXppId(), notificationIQ);
	}
	
	
	public void ueupdateCmd(AppTokenSession tokenSession,String messageTitle,String message){
		    NotificationManager manager = NotificationManager.getInstance();
			IQ notificationIQ = manager.createNotificationIQ(ConsDef.STR_PUSH_API_KEY, messageTitle, message,
					ConsDef.STR_PUSH_URL);
			manager.sendNotifcationToUser(tokenSession.getXppId(), notificationIQ);
	}
	
	
	public void syncTimeCmd(AppTokenSession tokenSession,String message){
		    NotificationManager manager = NotificationManager.getInstance();
			IQ notificationIQ = manager.createNotificationIQ(ConsDef.STR_PUSH_API_KEY,
					PushEnum.UeSyncTime, message, ConsDef.STR_PUSH_URL);
			manager.sendNotifcationToUser(tokenSession.getXppId(), notificationIQ);
	}
	
	
	public void storeCallCmd(AppTokenSession tokenSession,String message){
		    NotificationManager manager = NotificationManager.getInstance();
			IQ notificationIQ = manager.createNotificationIQ(ConsDef.STR_PUSH_API_KEY,
					PushEnum.UeStoreCallNumber, message, ConsDef.STR_PUSH_URL);
	
			manager.sendNotifcationToUser(tokenSession.getXppId(), notificationIQ);
	}
	
	public boolean expireCallCmd(AppTokenSession tokenSession,String message){
			ClientSession clientSession = SessionManager.getInstance().getSession(tokenSession.getXppId());
			if (clientSession != null && clientSession.getPresence().isAvailable()) {
					NotificationManager manager = NotificationManager.getInstance();
					IQ notificationIQ = manager.createNotificationIQ(ConsDef.STR_PUSH_API_KEY,
							PushEnum.UeExpireCallNumber, message, ConsDef.STR_PUSH_URL);
					manager.sendNotifcationToUser(tokenSession.getXppId(), notificationIQ);
					return true;
			}
			return false;
	}
	
	
	public void uecontrolCmd(AppTokenSession tokenSession,String messageTitle,String message){
			NotificationManager manager = NotificationManager.getInstance();
			IQ notificationIQ = manager.createNotificationIQ(ConsDef.STR_PUSH_API_KEY, messageTitle, message,
					ConsDef.STR_PUSH_URL);
			manager.sendNotifcationToUser(tokenSession.getXppId(), notificationIQ);
	}
	
	
	
	
	
	
	
	
	
	/**
	 * 取得在线会话
	 * @return
	 */
	
	public List<AppTokenSession> getLiveSession(Integer  userId) {
			List<AppTokenSession> allSessions = apptokenserver.queryByUserId(userId);
			SessionManager mgr = SessionManager.getInstance();
			List<AppTokenSession> aliveSession = new ArrayList<AppTokenSession>();
			for (AppTokenSession s : allSessions) {
					ClientSession cs = mgr.getSession(s.getXppId());
					if (cs != null&&cs.getPresence().isAvailable()) {
							aliveSession.add(s);
					}
			}
			return aliveSession;
	}
	
	/**
	 * key:ClientSession.username,value:isAvailable
	 */
	public Map<String, Boolean>   getAliveMap(){
			Collection<ClientSession> clientS = SessionManager.getInstance().getSessions();
			Map<String, Boolean> aliveMap=null;
			if(clientS!=null&&clientS.size()>0){
				aliveMap = new HashMap<String, Boolean>();
				for (ClientSession s : clientS) {
					try {
							aliveMap.put(s.getUsername(), s.getPresence().isAvailable());
					} catch (UserNotFoundException e) {
							e.printStackTrace();
					}
				}
			}
			return aliveMap;
	}
}