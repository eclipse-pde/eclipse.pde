package org.eclipse.pde.internal.core.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.apache.xerces.parsers.*;
import org.eclipse.core.resources.IResource;
import org.w3c.dom.*;
import org.xml.sax.*;
import java.io.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.core.plugin.*;
import java.util.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.builders.SourceDOMParser;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.pde.core.*;
import java.net.*;

public abstract class AbstractPluginModelBase
	extends AbstractModel
	implements IPluginModelBase {
	public static final String KEY_ERROR = "AbstractPluginModelBase.error";
	protected PluginBase pluginBase;
	private PluginModelFactory factory;
	private boolean enabled;

	public AbstractPluginModelBase() {
		super();
	}
	public abstract IPluginBase createPluginBase();
	public IPluginModelFactory getFactory() {
		if (factory == null)
			factory = new PluginModelFactory(this);
		return factory;
	}
	public IPluginBase getPluginBase() {
		return pluginBase;
	}
	public IPluginBase getPluginBase(boolean createIfMissing) {
		if (pluginBase == null) {
			pluginBase = (PluginBase) createPluginBase();
			loaded=true;
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
			ExternalModelManager emng = PDEPlugin.getDefault().getExternalModelManager();
			if (emng.hasEnabledModels()) {
				IFragmentModel[] models = emng.getFragmentModels(null);
				addMatchingFragments(id, version, models, result);
			}
			// Add matching workspace fragments
			WorkspaceModelManager wmng = PDEPlugin.getDefault().getWorkspaceModelManager();
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
			if (PDEPlugin.compare(refid, refversion, id, version, refmatch)) {
				URL location = ((AbstractPluginModelBase)model).getNLLookupLocation();
				result.add(location);
				IPluginLibrary libraries[] = fragment.getLibraries();
				for (int j=0; j<libraries.length; j++) {
					IPluginLibrary library = libraries[j];
					try {
						URL libLocation = new URL(location, library.getName());
						result.add(libLocation);
					}
					catch (MalformedURLException e) {
					}
				}
			}
		}
	}

	public void load(InputStream stream, boolean outOfSync) throws CoreException {
		XMLErrorHandler errorHandler = new XMLErrorHandler();
		SourceDOMParser parser = new SourceDOMParser();
		parser.setErrorHandler(errorHandler);
		if (pluginBase == null) {
			pluginBase = (PluginBase) createPluginBase();
			pluginBase.setModel(this);
		}
		pluginBase.reset();
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
	private void throwParseErrorsException() throws CoreException {
		Status status =
			new Status(
				IStatus.ERROR,
				PDEPlugin.getPluginId(),
				IStatus.OK,
				PDEPlugin.getResourceString(KEY_ERROR),
				null);
		throw new CoreException(status);
	}
	public String toString() {
		IPluginBase pluginBase = getPluginBase();
		if (pluginBase != null)
			return pluginBase.getTranslatedName();
		return super.toString();
	}

	protected abstract void updateTimeStamp();
}