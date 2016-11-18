package org.androidpn.server.xmpp.handler;

import org.androidpn.server.xmpp.UnauthorizedException;
import org.androidpn.server.xmpp.session.ClientSession;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

public class IQPingHandler extends IQHandler {
	
	private static final String NAMESPACE = "urn:xmpp:ping";
//	private Element probeResponse;
	
	public IQPingHandler() {
//	    probeResponse = DocumentHelper.createElement(QName.get("ping",NAMESPACE));
	}

	@Override
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
//		System.out.println("收到ping"+packet.toXML());
		JID sender = packet.getFrom();
        ClientSession session = sessionManager.getSession(sender);
		if(packet != null){
			IQ reply = IQ.createResultIQ(packet);
			reply.setType(IQ.Type.result);
//			System.out.println("回应ping"+reply.toXML());
			session.process(reply);
		}
		return null;
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}

}
