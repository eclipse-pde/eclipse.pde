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

import java.lang.Runnable;
import java.lang.Thread;

/**
 * AbstractModelUpdateStrategy.java
 */
public abstract class AbstractModelUpdateStrategy implements Runnable {
	private DocumentModel fPluginXMLDocumentModel;
	private boolean fSuccessful;
	private Thread fClient;
	
	public AbstractModelUpdateStrategy(DocumentModel pluginXMLDocumentModel, boolean successful) {
		this(pluginXMLDocumentModel, successful, Thread.currentThread());
	}
	
	public AbstractModelUpdateStrategy(DocumentModel pluginXMLDocumentModel, boolean successful, Thread client) {
		fPluginXMLDocumentModel= pluginXMLDocumentModel;
		fSuccessful= successful;
		fClient= client;
	}
	
	public boolean isSuccessful() {
		return fSuccessful;
	}
	
	public void run() {
		if (fPluginXMLDocumentModel.getTicketManager().tryUseTicket(fClient))
			update();
	}
	
	protected abstract void update();
}