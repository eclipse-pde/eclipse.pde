package org.eclipse.pde.internal.ui.model.plugin;

import java.io.*;
import java.net.*;

import org.eclipse.core.resources.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.core.plugin.PluginAttribute;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.model.*;
import org.xml.sax.helpers.*;

/**
 * @author melhem
 *
 */
public abstract class PluginModelBase extends XMLEditingModel implements IPluginModelBase, IPluginModelFactory {

	private PluginBaseNode fPluginBase;
	private boolean fIsEnabled;
	private String fInstallLocation;
	private IResource fUnderlyingResource;
	
	public PluginModelBase(IDocument document, boolean isReconciling) {
		super(document, isReconciling);	
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#createPluginBase()
	 */
	public IPluginBase createPluginBase(boolean isFragment) {
		if (isFragment)
			fPluginBase = new FragmentNode();
		else 
			fPluginBase = new PluginNode();
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getPluginBase(boolean)
	 */
	public IPluginBase getPluginBase(boolean createIfMissing) {
		if (!fIsLoaded && createIfMissing) {
			createPluginBase();
			load();
			fIsLoaded = true;
		}
		return fPluginBase;
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
	 * @see org.eclipse.pde.core.IModel#getUnderlyingResource()
	 */
	public IResource getUnderlyingResource() {
		return fUnderlyingResource;
	}
	
	public void setUnderlyingResource(IResource resource) {
		fUnderlyingResource = resource;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getPluginFactory()
	 */
	public IPluginModelFactory getPluginFactory() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getNLLookupLocation()
	 */
	public URL getNLLookupLocation() {
		String installLocation = getInstallLocation();
		if (installLocation.startsWith("file:") == false)
			installLocation = "file:" + installLocation;
		try {
			URL url = new URL(installLocation + "/");
			return url;
		} catch (MalformedURLException e) {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.ISharedPluginModel#getFactory()
	 */
	public IExtensionsModelFactory getFactory() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.ISharedPluginModel#getInstallLocation()
	 */
	public String getInstallLocation() {
		if (fUnderlyingResource != null)
			return fUnderlyingResource.getProject().getLocation().toString();
		return fInstallLocation;
	}
	
	public void setInstallLocation(String location) {
		fInstallLocation = location;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.AbstractEditingModel#createNLResourceHelper()
	 */
	protected NLResourceHelper createNLResourceHelper() {
		String name = isFragmentModel() ? "fragment" : "plugin";
		return new NLResourceHelper(name, new URL[] {getNLLookupLocation()});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#reload(java.io.InputStream, boolean)
	 */
	public void reload(InputStream source, boolean outOfSync) {
		load(source, outOfSync);
		fireModelChanged(
			new ModelChangedEvent(
				IModelChangedEvent.WORLD_CHANGED,
				new Object[] {getPluginBase()},
				null));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.XMLEditingModel#createDocumentHandler(org.eclipse.pde.core.IModel)
	 */
	protected DefaultHandler createDocumentHandler(IModel model) {
		return null;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// Create PDE Core objects to distinguish them from ones created by parsing.
	////////////////////////////////////////////////////////////////////////////
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
