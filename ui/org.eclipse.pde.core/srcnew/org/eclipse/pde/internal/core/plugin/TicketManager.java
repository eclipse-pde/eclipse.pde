/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * TicketManager.java
 */
public class TicketManager {
	private Map fTickets= new HashMap();
	private int fLatestUsedTicket= -1;
	private int fLatestNewTicket= 0;
	
	public synchronized void buyTicket() {
		 fTickets.put(Thread.currentThread(), new Integer(++fLatestNewTicket));
	}
	
	public synchronized boolean tryUseTicket() {
		return tryUseTicket(Thread.currentThread());
	}

	public synchronized boolean tryUseTicket(Thread client) {
		boolean result= isTicketValid(client);
		if (result) {
			Integer ticket= removeTicket(client);
			if (ticket != null) {
				fLatestUsedTicket= ticket.intValue();
			}
		}
		return result;
	}
	
	public synchronized boolean isTicketValid() {
		return isTicketValid(Thread.currentThread());
	}

	public synchronized boolean isTicketValid(Thread client) {
		boolean result;
		Integer ticket= (Integer) fTickets.get(client);
		if (ticket == null) {
			result= true; //REVISIT: original behaviour
		} else {
			if (ticket.intValue() == fLatestNewTicket) {
				result= true;
			} else {
				result= false;
			}
		}
		return result;
	}
	
	public synchronized Integer removeTicket() {
		return removeTicket(Thread.currentThread());
	}
	
	public synchronized Integer removeTicket(Thread client) {
		return (Integer) fTickets.remove(client);
	}
	
	public synchronized boolean isAllTicketsUsed() {
		return fLatestUsedTicket == fLatestNewTicket;
	}
}
