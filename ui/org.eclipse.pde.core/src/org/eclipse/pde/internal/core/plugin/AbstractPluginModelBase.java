package org.eclipse.pde.internal.core.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public abstract class AbstractPluginModelBase
	extends AbstractModel
	implements IPluginModelBase, IPluginModelFactory {
	protected PluginBase pluginBase;
	private boolean enabled;

	public AbstractPluginModelBase() {
		super();
	}
	
	public abstract IPluginBase createPluginBase();
	
	public IPluginModelFactory getFactory() {
		return this;
	}

	public IPluginBase getPluginBase() {
		return pluginBase;
	}
	public IPluginBase getPluginBase(boolean createIfMissing) {
		if (pluginBase == null) {
			pluginBase = (PluginBase) createPluginBase();
			loaded = true;
		}
		return pluginBase;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public boolean isFragmentModel() {
		return false;
	}

	public abstract URL getNLLookupLocation();

	protected URL[] getNLLookupLocations() {
		if (isFragmentModel()) {
			return new URL[] { getNLLookupLocation()};
		} else {
			URL[] fragmentLocations = getFragmentLocations();
			URL[] locations = new URL[1 + fragmentLocations.length];
			locations[0] = getNLLookupLocation();
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
			// Add matching external fragments
			ExternalModelManager emng =
				PDECore.getDefault().getExternalModelManager();
			if (emng.hasEnabledModels()) {
				IFragmentModel[] models = emng.getFragmentModels(null);
				addMatchingFragments(id, version, models, result);
			}
			// Add matching workspace fragments
			WorkspaceModelManager wmng =
				PDECore.getDefault().getWorkspaceModelManager();
			IFragmentModel[] models = wmng.getWorkspaceFragmentModels();
			addMatchingFragments(id, version, models, result);
		}
		URL[] locations = new URL[result.size()];
		result.copyInto(locations);
		return locations;
	}

	private void addMatchingFragments(
		String id,
		String version,
		IFragmentModel[] models,
		Vector result) {
		for (int i = 0; i < models.length; i++) {
			IFragmentModel model = models[i];
			if (model.isEnabled() == false)
				continue;
			IFragment fragment = model.getFragment();
			String refid = fragment.getPluginId();
			String refversion = fragment.getPluginVersion();
			int refmatch = fragment.getRule();
			if (PDECore.compare(refid, refversion, id, version, refmatch)) {
				URL location =
					((AbstractPluginModelBase) model).getNLLookupLocation();
				result.add(location);
				IPluginLibrary libraries[] = fragment.getLibraries();
				for (int j = 0; j < libraries.length; j++) {
					IPluginLibrary library = libraries[j];
					try {
						URL libLocation = new URL(location, library.getName());
						result.add(libLocation);
					} catch (MalformedURLException e) {
					}
				}
			}
		}
	}

	public void load(InputStream stream, boolean outOfSync)
		throws CoreException {
		XMLErrorHandler errorHandler = new XMLErrorHandler();
		SourceDOMParser parser = new SourceDOMParser();
		parser.setErrorHandler(errorHandler);
		if (pluginBase == null) {
			pluginBase = (PluginBase) createPluginBase();
			pluginBase.setModel(this);
		}
		pluginBase.reset();
		loaded = false;
		try {
			InputSource source = new InputSource(stream);
			parser.parse(source);
			if (errorHandler.getErrorCount() > 0
				|| errorHandler.getFatalErrorCount() > 0) {
				throwParseErrorsException();
			}
			processDocument(parser.getDocument(), parser.getLineTable());
			loaded = true;
			if (!outOfSync)
				updateTimeStamp();
		} catch (SAXException e) {
			throwParseErrorsException();
		} catch (IOException e) {
			throwParseErrorsException();
		}
	}
	private void processDocument(Document doc, Hashtable lineTable) {
		Node pluginNode = doc.getDocumentElement();
		pluginBase.load(pluginNode, lineTable);
	}
	public void reload(InputStream stream, boolean outOfSync)
		throws CoreException {
		load(stream, outOfSync);
		fireModelChanged(
			new ModelChangedEvent(
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
}