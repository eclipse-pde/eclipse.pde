/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.model.plugin;

import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.model.*;
import org.xml.sax.helpers.*;

/**
 * @author melhem
 *
 */
public abstract class PluginModelBase extends XMLEditingModel implements IPluginModelBase {

	private PluginBaseNode fPluginBase;
	private boolean fIsEnabled;
	private PluginDocumentHandler fHandler;
	private NodeOffsetHandler fNodeOffsetHandler;
	private IPluginModelFactory fFactory;
	
	public PluginModelBase(IDocument document, boolean isReconciling) {
		super(document, isReconciling);	
		fFactory = new PluginDocumentNodeFactory(this);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#createPluginBase()
	 */
	public IPluginBase createPluginBase(boolean isFragment) {
		if (isFragment) {
			fPluginBase = new FragmentNode();
			fPluginBase.setXMLTagName("fragment"); //$NON-NLS-1$
		} else {
			fPluginBase = new PluginNode();
			fPluginBase.setXMLTagName("plugin"); //$NON-NLS-1$
		}
		fPluginBase.setInTheModel(true);
		fPluginBase.setModel(this);
		return fPluginBase;
	}
	
	public IPluginBase createPluginBase() {
		return createPluginBase(isFragmentModel());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getBuildModel()
	 */
	public IBuildModel getBuildModel() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getPluginBase()
	 */
	public IPluginBase getPluginBase() {
		return getPluginBase(true);
	}
	
	public IExtensions getExtensions() {
		return getPluginBase();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getPluginBase(boolean)
	 */
	public IPluginBase getPluginBase(boolean createIfMissing) {
		if (!fLoaded && createIfMissing) {
			createPluginBase();
			try {
				load();
			} catch (CoreException e) {
			}
		}
		return fPluginBase;
	}
	
	public IExtensions getExtensions(boolean createIfMissing) {
		return getPluginBase(createIfMissing);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#isEnabled()
	 */
	public boolean isEnabled() {
		return fIsEnabled;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
		fIsEnabled = enabled;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getPluginFactory()
	 */
	public IPluginModelFactory getPluginFactory() {
		return fFactory;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getNLLookupLocation()
	 */
	public URL getNLLookupLocation() {
		String installLocation = getInstallLocation();
		if (installLocation==null) return null;
		if (installLocation.startsWith("file:") == false) //$NON-NLS-1$
			installLocation = "file:" + installLocation; //$NON-NLS-1$
		try {
			URL url = new URL(installLocation + "/"); //$NON-NLS-1$
			return url;
		} catch (MalformedURLException e) {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.ISharedPluginModel#getFactory()
	 */
	public IExtensionsModelFactory getFactory() {
		return fFactory;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.AbstractEditingModel#createNLResourceHelper()
	 */
	protected NLResourceHelper createNLResourceHelper() {
		String name = isFragmentModel() ? "fragment" : "plugin"; //$NON-NLS-1$ //$NON-NLS-2$
		URL lookupLocation = getNLLookupLocation();
		if (lookupLocation==null) return null;
		return new NLResourceHelper(name, new URL[] {lookupLocation});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.XMLEditingModel#createDocumentHandler(org.eclipse.pde.core.IModel)
	 */
	protected DefaultHandler createDocumentHandler(IModel model) {
		if (fHandler == null)
			fHandler = new PluginDocumentHandler(this);
		return fHandler;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.XMLEditingModel#createNodeOffsetHandler(org.eclipse.pde.core.IModel)
	 */
	protected DefaultHandler createNodeOffsetHandler(IModel model) {
		if (fNodeOffsetHandler == null)
			fNodeOffsetHandler = new NodeOffsetHandler(this);
		return fNodeOffsetHandler;
	}	
}
