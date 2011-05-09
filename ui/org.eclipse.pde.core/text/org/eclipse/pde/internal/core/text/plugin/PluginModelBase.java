/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.plugin;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.PDEManager;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.XMLEditingModel;
import org.xml.sax.helpers.DefaultHandler;

public abstract class PluginModelBase extends XMLEditingModel implements IPluginModelBase, IDocumentListener {

	private PluginBaseNode fPluginBase;
	private boolean fIsEnabled;
	private PluginDocumentHandler fHandler;
	private IPluginModelFactory fFactory;
	private String fLocalization;
	private boolean fHasTriedToCreateModel;

	public PluginModelBase(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
		fFactory = new PluginDocumentNodeFactory(this);
		document.addDocumentListener(this);
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

	protected IWritable getRoot() {
		return getPluginBase();
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
		if (!fLoaded && !fHasTriedToCreateModel && createIfMissing) {
			try {
				createPluginBase();
				load();
			} catch (CoreException e) {
			} finally {
				fHasTriedToCreateModel = true;
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
		try {
			String installLocation = getInstallLocation();
			return installLocation == null ? null : new URL("file:" + installLocation); //$NON-NLS-1$
		} catch (MalformedURLException e) {
		}
		return null;
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
		URL[] locations = PDEManager.getNLLookupLocations(this);
		return (locations.length == 0) ? null : new NLResourceHelper(fLocalization == null ? "plugin" : fLocalization, //$NON-NLS-1$
				locations);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.XMLEditingModel#createDocumentHandler(org.eclipse.pde.core.IModel)
	 */
	protected DefaultHandler createDocumentHandler(IModel model, boolean reconciling) {
		if (fHandler == null)
			fHandler = new PluginDocumentHandler(this, reconciling);
		return fHandler;
	}

	public IDocumentElementNode getLastErrorNode() {
		if (fHandler != null)
			return fHandler.getLastErrorNode();
		return null;
	}

	public void setLocalization(String localization) {
		fLocalization = localization;
	}

	/*
	 * @see org.eclipse.pde.internal.core.text.AbstractEditingModel#dispose()
	 * @since 3.6
	 */
	public void dispose() {
		getDocument().removeDocumentListener(this);
		super.dispose();
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
	 * @since 3.6
	 */
	public void documentChanged(DocumentEvent event) {
		fHasTriedToCreateModel = false;
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
	 * @since 3.6
	 */
	public void documentAboutToBeChanged(DocumentEvent event) {
	}

}
