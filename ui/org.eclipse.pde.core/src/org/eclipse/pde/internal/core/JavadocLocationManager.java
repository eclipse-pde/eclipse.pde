package org.eclipse.pde.internal.core;

import java.net.*;
import java.util.*;

import org.eclipse.pde.core.plugin.*;

public class JavadocLocationManager {
	
	private static String JAVADOC_ID = "org.eclipse.pde.core.javadoc"; //$NON-NLS-1$
	
	private HashMap fLocations;
	
	public URL getJavadocLocation(IPluginModelBase model) {
		URL url = model.getResourceURL("doc"); //$NON-NLS-1$
		if (url == null) {
			String id = model.getPluginBase().getId();
			if (id != null)
				url = (URL)getLocations().get(id);
		}
		return url;
	}

	private void processExtensions(ModelEntry entry, boolean useExternal) {
		IPluginModelBase model = useExternal ? entry.getExternalModel() : entry.getActiveModel();		
		if (model == null)
			return;
		
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int j = 0; j < extensions.length; j++) {
			IPluginExtension extension = extensions[j];
			if (JAVADOC_ID.equals(extension.getPoint())) {
				int origLength = fLocations.size();
				processExtension(extension);
				if (fLocations.size() == origLength && model.getUnderlyingResource() != null) {
					processExtensions(entry, true);					
				}
			}
		}		
	}
	
	private void processExtension(IPluginExtension extension) {
		IPluginObject[] children = extension.getChildren();
		for (int j = 0; j < children.length; j++) {
			if (children[j].getName().equals("javadoc")) { //$NON-NLS-1$
				IPluginElement element = (IPluginElement) children[j];
				String pluginID = getAttribute(element, "plugin"); //$NON-NLS-1$
				String path = getAttribute(element, "path"); //$NON-NLS-1$
				if (pluginID != null && path != null) {
					ISharedPluginModel model = extension.getModel();
					URL url = model.getResourceURL(path);
					if (url != null) {
						getLocations().put(pluginID, url);
					}
				}
			}
		}
	}
	
	private String getAttribute(IPluginElement element, String attrName) {
		IPluginAttribute attr = element.getAttribute(attrName);
		return attr == null ? null : attr.getValue();
	}
	
	public void reset() {
		fLocations = null;
	}
	
	private synchronized Map getLocations() {
		initialize();
		return fLocations;
	}
	
	private void initialize() {
		if (fLocations == null)
			return;
		ModelEntry[] entries = PDECore.getDefault().getModelManager().getEntries();
		for (int i = 0; i < entries.length; i++)
			processExtensions(entries[i], false);
	}

}
