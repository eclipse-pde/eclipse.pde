package org.eclipse.pde.internal.ui.model.build;

import java.io.*;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.model.*;


public class BuildModel extends AbstractEditingModel implements IBuildModel {
	
	private Properties fProperties;
	private BuildModelFactory fFactory;
	private Build fBuild;
	private String fLocation;
	private IResource fUnderlyingResource;
	
	/**
	 * @param document
	 * @param isReconciling
	 */
	public BuildModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.AbstractEditingModel#createNLResourceHelper()
	 */
	protected NLResourceHelper createNLResourceHelper() {
		return null;
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
	 * @see org.eclipse.pde.core.IModel#load(java.io.InputStream, boolean)
	 */
	public void load(InputStream source, boolean outOfSync) throws CoreException {
		try {
			fIsLoaded = true;
			Properties prop = getProperties();
			prop.clear();
			prop.load(source);
			((Build)getBuild()).load(prop);
		} catch (IOException e) {
			fIsLoaded = false;
			e.printStackTrace();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#reload(java.io.InputStream, boolean)
	 */
	public void reload(InputStream source, boolean outOfSync)
			throws CoreException {
		load(source, outOfSync);
		fireModelChanged(
			new ModelChangedEvent(this, 
				IModelChangedEvent.WORLD_CHANGED,
				new Object[] {getBuild()},
				null));
	}
	
	private  Properties getProperties() {
		if (fProperties == null) {
			fProperties = new Properties();
		}
		return fProperties;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildModel#getBuild()
	 */
	public IBuild getBuild() {
		if (fBuild == null)
			fBuild = new Build(this);
		return fBuild;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildModel#getFactory()
	 */
	public IBuildModelFactory getFactory() {
		if (fFactory == null)
			fFactory = new BuildModelFactory(this);
		return fFactory;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildModel#getInstallLocation()
	 */
	public String getInstallLocation() {
		return fLocation;
	}
	
	public void setInstallLocation(String location) {
		fLocation = location;
	}
	
}
