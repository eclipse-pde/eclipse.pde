/*******************************************************************************
 *  Copyright (c) 2000, 2014 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.plugin;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IExtensions;
import org.eclipse.pde.core.plugin.IExtensionsModelFactory;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginModelFactory;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.PDEManager;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.XMLEditingModel;
import org.xml.sax.helpers.DefaultHandler;

public abstract class PluginModelBase extends XMLEditingModel implements IPluginModelBase, IDocumentListener {

	private PluginBaseNode fPluginBase;
	private boolean fIsEnabled;
	private PluginDocumentHandler fHandler;
	private final IPluginModelFactory fFactory;
	private String fLocalization;
	private boolean fHasTriedToCreateModel;

	public PluginModelBase(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
		fFactory = new PluginDocumentNodeFactory(this);
		document.addDocumentListener(this);
	}

	public IPluginBase createPluginBase(boolean isFragment) {
		if (fPluginBase != null) {
			return fPluginBase;
		}
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

	@Override
	protected IWritable getRoot() {
		return getPluginBase();
	}

	@Override
	public IPluginBase createPluginBase() {
		return createPluginBase(isFragmentModel());
	}

	@Override
	@Deprecated
	public IBuildModel getBuildModel() {
		return null;
	}

	@Override
	public IPluginBase getPluginBase() {
		return getPluginBase(true);
	}

	@Override
	public IExtensions getExtensions() {
		return getPluginBase();
	}

	@Override
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

	@Override
	public IExtensions getExtensions(boolean createIfMissing) {
		return getPluginBase(createIfMissing);
	}

	@Override
	public boolean isEnabled() {
		return fIsEnabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		fIsEnabled = enabled;
	}

	@Override
	public IPluginModelFactory getPluginFactory() {
		return fFactory;
	}

	@Override
	@Deprecated
	public URL getNLLookupLocation() {
		try {
			String installLocation = getInstallLocation();
			return installLocation == null ? null : new URL("file:" + installLocation); //$NON-NLS-1$
		} catch (MalformedURLException e) {
		}
		return null;
	}

	@Override
	public IExtensionsModelFactory getFactory() {
		return fFactory;
	}

	@Override
	protected NLResourceHelper createNLResourceHelper() {
		URL[] locations = PDEManager.getNLLookupLocations(this);
		return (locations.length == 0) ? null : new NLResourceHelper(fLocalization == null ? "plugin" : fLocalization, //$NON-NLS-1$
				locations);
	}

	@Override
	protected DefaultHandler createDocumentHandler(IModel model, boolean reconciling) {
		if (fHandler == null) {
			fHandler = new PluginDocumentHandler(this, reconciling);
		}
		return fHandler;
	}

	public IDocumentElementNode getLastErrorNode() {
		if (fHandler != null) {
			return fHandler.getLastErrorNode();
		}
		return null;
	}

	public void setLocalization(String localization) {
		fLocalization = localization;
	}

	/*
	 * @see org.eclipse.pde.internal.core.text.AbstractEditingModel#dispose()
	 * @since 3.6
	 */
	@Override
	public void dispose() {
		getDocument().removeDocumentListener(this);
		super.dispose();
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
	 * @since 3.6
	 */
	@Override
	public void documentChanged(DocumentEvent event) {
		fHasTriedToCreateModel = false;
		fLoaded = false;
	}

	/*
	 * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
	 * @since 3.6
	 */
	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
	}

}
