/*
 * Copyright (C) 2010 Moduad Co., Ltd.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.androidpn.server.xmpp.handler;

import org.androidpn.server.service.UserExistsException;
import org.androidpn.server.service.UserNotFoundException;
import org.androidpn.server.xmpp.UnauthorizedException;
import org.androidpn.server.xmpp.session.ClientSession;
import org.androidpn.server.xmpp.session.Session;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

import gnu.inet.encoding.Stringprep;
import gnu.inet.encoding.StringprepException;


/**
 * This class is to handle the TYPE_IQ jabber:iq:register protocol.
 * 
 * @author Sehwan Noh (devnoh@gmail.com)
 */
public class IQRegisterHandler extends IQHandler {

	private static final String NAMESPACE = "jabber:iq:register";


	private Element probeResponse;

	/**
	 * Constructor.
	 */
	public IQRegisterHandler() {
		probeResponse = DocumentHelper.createElement(QName.get("query",
				NAMESPACE));
		probeResponse.addElement("strImsi");
		probeResponse.addElement("strFactory");
		probeResponse.addElement("strType");
	}

	/**
	 * Handles the received IQ packet.
	 * 
	 * @param packet
	 *            the packet
	 * @return the response to send back
	 * @throws UnauthorizedException
	 *             if the user is not authorized
	 */
	public IQ handleIQ(IQ packet) throws UnauthorizedException {
		IQ reply = null;

		ClientSession session = sessionManager.getSession(packet.getFrom());
		System.out.println("注册  found for key" + session);
		if (session == null) {

			log.error("Session not found for key " + packet.getFrom());
			System.out.println("Session not found for key");
			reply = IQ.createResultIQ(packet);
			reply.setChildElement(packet.getChildElement().createCopy());
			reply.setError(PacketError.Condition.internal_server_error);
			return reply;
		}

		if (IQ.Type.get.equals(packet.getType())) {
			reply = IQ.createResultIQ(packet);
			if (session.getStatus() == Session.STATUS_AUTHENTICATED) {
			} else {
				reply.setTo((JID) null);
				reply.setChildElement(probeResponse.createCopy());
			}
		} else if (IQ.Type.set.equals(packet.getType())) {
			try {
				Element query = packet.getChildElement();
				if (query.element("remove") != null) {
					if (session.getStatus() == Session.STATUS_AUTHENTICATED) {
					} else {
						throw new UnauthorizedException();
					}
				} else {
					String strImsi = query.elementText("strImsi");
					String strName = query.elementText("strName");
					String strFactory = query.elementText("strFactory");
					String strType = query.elementText("strType");

					// Verify the username
					// 验证用户名
					if (strImsi != null) {
						Stringprep.nodeprep(strImsi);
					}
					reply = IQ.createResultIQ(packet);
					session.getStatus();

					/*TerminalPO terminalByImsi = terminalService.gainterminalByImsi(strImsi);
					//TerminalPO terminalByName = terminalService.gainterminalByName(strName);
					if (terminalByImsi != null) {
						TerminalPO terminalpo = new TerminalPO();
						terminalpo.setStrFactory(strFactory);
						terminalpo.setStrType(strType);
						terminalpo.setStrImsi(strImsi);
						terminalService.updateTerminal(terminalpo);
					} */
				}
			} catch (Exception ex) {
				log.error(ex);
				reply = IQ.createResultIQ(packet);
				reply.setChildElement(packet.getChildElement().createCopy());
				if (ex instanceof UserExistsException) {
					reply.setError(PacketError.Condition.conflict);
				} else if (ex instanceof UserNotFoundException) {
					reply.setError(PacketError.Condition.bad_request);
				} else if (ex instanceof StringprepException) {
					reply.setError(PacketError.Condition.jid_malformed);
				} else if (ex instanceof IllegalArgumentException) {
					reply.setError(PacketError.Condition.not_acceptable);
				} else {
					reply.setError(PacketError.Condition.internal_server_error);
				}
			}
		}

		// Send the response directly to the session
		if (reply != null) {
			session.process(reply);
		}
		return null;
	}

	/**
	 * Returns the namespace of the handler.
	 * 
	 * @return the namespace
	 */
	public String getNamespace() {
		return NAMESPACE;
	}

}
