/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.standalone.parser;

import java.util.*;

/**
 * PluginCore.java
 */
public class XMLCore {
	
	public final static boolean NEW_CODE_PATHS= true;
	public final static boolean VALIDATE= false;

	private static XMLCore fInstance;

	protected boolean fValidate= VALIDATE;	
	protected List fModelChangeListeners= new ArrayList();

	public static XMLCore getDefault() {
		if (fInstance == null) {
			fInstance= new XMLCore();
		}
		return fInstance;
	}


	public XMLDocumentModelBuilder createXMLModelBuilder(IDocumentModelFactory modelFactory) {
		XMLDocumentModelBuilder builder= new XMLDocumentModelBuilder(new XEParserConfiguration(), fValidate);
		builder.setDocumentModelFactory(modelFactory);
		return builder;
	}
	
}
