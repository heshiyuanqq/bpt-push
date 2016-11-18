/**
 * 
 */
package org.androidpn.server.util;

import org.androidpn.server.model.NotificationPO;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.xmpp.packet.IQ;


public class CopyMessageUtil {
	
	public static void IQ2Message(IQ iq,NotificationPO notificationMO){
		
		Element root = iq.getElement();
		Attribute idAttr=root.attribute("id");
		if(idAttr != null){
			String id = idAttr.getValue();
			notificationMO.setIntId(Integer.parseInt(id));
		}
	}
}
