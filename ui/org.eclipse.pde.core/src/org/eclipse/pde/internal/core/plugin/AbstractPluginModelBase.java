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
package org.eclipse.pde.internal.core.plugin;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.parsers.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.w3c.dom.*;

public abstract class AbstractPluginModelBase
	extends AbstractModel
	implements IPluginModelBase, IPluginModelFactory {
	protected PluginBase pluginBase;
	private boolean enabled;
	private boolean reconcilingModel=false;
	private BundleDescription fBundleDescription;
	
	public AbstractPluginModelBase() {
		super();
	}
	
	public abstract IPluginBase createPluginBase();
	
	public IExtensions createExtensions() {
		return createPluginBase();
	}
	
	public IExtensionsModelFactory getFactory() {
		return this;
	}
	
	public IPluginModelFactory getPluginFactory() {
		return this;
	}

	public IPluginBase getPluginBase() {
		return getPluginBase(true);
	}
	public IPluginBase getPluginBase(boolean createIfMissing) {
		if (pluginBase == null && createIfMissing) {
			pluginBase = (PluginBase) createPluginBase();
			loaded = true;
		}
		return pluginBase;
	}
	
	public IExtensions getExtensions() {
		return getPluginBase();
	}
	public IExtensions getExtensions(boolean createIfMissing) {
		return getPluginBase(createIfMissing);
	}
	public boolean isEnabled() {
		return enabled;
	}
	public boolean isFragmentModel() {
		return false;
	}

	public abstract URL getNLLookupLocation();

	protected URL[] getNLLookupLocations() {
		URL thisLocation = getNLLookupLocation();
		if (isFragmentModel()) {
			if (thisLocation!=null)
				return new URL[] { thisLocation };
			else
				return new URL[0];
		} else {
			URL[] fragmentLocations = getFragmentLocations();
			URL[] locations = new URL[(thisLocation!=null?1:0) + fragmentLocations.length];
			if (thisLocation!=null)
				locations[0] = thisLocation;
			for (int i = 1; i < locations.length; i++) {
				locations[i] = fragmentLocations[i - 1];
			}
			return locations;
		}
	}

	protected URL[] getFragmentLocations() {
		Vector result = new Vector();
		if (pluginBase != null) {
			String id = pluginBase.getId();
			String version = pluginBase.getVersion();
			addMatchingFragments(PDECore.getDefault().findFragmentsFor(id, version), result);
		}
		URL[] locations = new URL[result.size()];
		result.copyInto(locations);
		return locations;
	}

	private void addMatchingFragments(IFragment[] fragments, Vector result) {
		for (int i = 0; i < fragments.length; i++) {
			IFragment fragment = fragments[i];
			URL location = ((IFragmentModel)fragment.getModel()).getNLLookupLocation();
			if (location == null) continue;
			IPluginLibrary libraries[] = fragment.getLibraries();
			for (int j = 0; j < libraries.length; j++) {
				try {
					result.add(new URL(location, libraries[j].getName()));
				} catch (MalformedURLException e) {
				}
			}
		}
	}
	
	public void load(InputStream stream, boolean outOfSync)
		throws CoreException {

		if (pluginBase == null) {
			pluginBase = (PluginBase) createPluginBase();
			pluginBase.setModel(this);
		}
		pluginBase.reset();
		loaded = false;
		try {
			SAXParser parser = getSaxParser();
			XMLDefaultHandler handler = new XMLDefaultHandler();
			parser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
			parser.parse(stream, handler);
			processDocument(handler.getDocument(), handler.getLineTable());
			loaded = true;
			if (!outOfSync)
				updateTimeStamp();
		} catch (Exception e) {
		}
	}

	private void processDocument(Document doc, Hashtable lineTable) {
		String schemaVersion = processSchemaVersion(doc);
		//System.out.println("Schema Version="+schemaVersion);
		Node pluginNode = doc.getDocumentElement();
		pluginBase.load(pluginNode, schemaVersion, lineTable);
	}

	private String processSchemaVersion(Document doc) {
		NodeList children = doc.getChildNodes();
		for (int i=0; i<children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeType()==Node.PROCESSING_INSTRUCTION_NODE) {
				ProcessingInstruction pi = (ProcessingInstruction)node;
				String target = pi.getTarget();
				if (target.equals("eclipse")) {
					String data = pi.getData();
					if (data!=null) {
						data = data.trim().toLowerCase();
						int loc = data.indexOf('=');
						if (loc!=-1) {
							String key = data.substring(0, loc);
							if (key.equals("version")) {
								int start = loc+1;
								if (data.charAt(start)=='\"')
									start++;
								int end = data.length()-1;
								if (data.charAt(end)=='\"')
									end--;
								return data.substring(start, end+1);
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	public void reload(InputStream stream, boolean outOfSync)
		throws CoreException {
		load(stream, outOfSync);
		fireModelChanged(
			new ModelChangedEvent(this,
				IModelChangedEvent.WORLD_CHANGED,
				new Object[] { pluginBase },
				null));
	}
	public void setEnabled(boolean newEnabled) {
		enabled = newEnabled;
	}
	public String toString() {
		IPluginBase pluginBase = getPluginBase();
		if (pluginBase != null)
			return pluginBase.getTranslatedName();
		return super.toString();
	}

	protected abstract void updateTimeStamp();

	public IPluginAttribute createAttribute(IPluginElement element) {
		PluginAttribute attribute = new PluginAttribute();
		attribute.setModel(this);
		attribute.setParent(element);
		return attribute;
	}
	public IPluginElement createElement(IPluginObject parent) {
		PluginElement element = new PluginElement();
		element.setModel(this);
		element.setParent(parent);
		return element;
	}
	public IPluginExtension createExtension() {
		PluginExtension extension = new PluginExtension();
		extension.setParent(getPluginBase());
		extension.setModel(this);
		return extension;
	}
	public IPluginExtensionPoint createExtensionPoint() {
		PluginExtensionPoint extensionPoint = new PluginExtensionPoint();
		extensionPoint.setModel(this);
		extensionPoint.setParent(getPluginBase());
		return extensionPoint;
	}
	public IPluginImport createImport() {
		PluginImport iimport = new PluginImport();
		iimport.setModel(this);
		iimport.setParent(getPluginBase());
		return iimport;
	}
	public IPluginLibrary createLibrary() {
		PluginLibrary library = new PluginLibrary();
		library.setModel(this);
		library.setParent(getPluginBase());
		return library;
	}
	
	public boolean isValid() {
		if (!isLoaded()) return false;
		if (pluginBase==null) return false;
		return pluginBase.isValid();	
	}

	public boolean isReconcilingModel() {
		return reconcilingModel;
	}

	public void setReconcilingModel(boolean reconcilingModel) {
		this.reconcilingModel = reconcilingModel;
	}
	public boolean isBundleModel() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#dispose()
	 */
	public void dispose() {
		fBundleDescription = null;
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getBundleDescription()
	 */
	public BundleDescription getBundleDescription() {
		return fBundleDescription;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#setBundleDescription(org.eclipse.osgi.service.resolver.BundleDescription)
	 */
	public void setBundleDescription(BundleDescription description) {
		fBundleDescription = description;
	}

}
