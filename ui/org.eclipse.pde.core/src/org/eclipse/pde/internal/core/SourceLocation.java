package org.eclipse.pde.internal.core;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.internal.boot.InternalBootLoader;
import org.eclipse.core.runtime.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 */
public class SourceLocation {
	private String name;
	private IPath path;
	private boolean userDefined = true;
	private boolean enabled;

	public SourceLocation(String name, IPath path, boolean enabled) {
		this.name = name;
		this.path = path;
		this.enabled = enabled;
	}

	public SourceLocation(IConfigurationElement config) {
		initialize(config);
		userDefined = false;
		this.enabled = true;
		this.name = computeName(config);
	}
	
	private String computeName(IConfigurationElement config) {
		String providedName = config.getAttribute("name");
		if (providedName!=null) return providedName;
		String computedName = config.getDeclaringExtension().getDeclaringPluginDescriptor().getUniqueIdentifier();
		computedName = computedName.replace('.', '_');
		return "_"+computedName.toUpperCase();
	}
	
	public String getName() {
		return name;
	}

	public IPath getPath() {
		return path;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setPath(IPath path) {
		this.path = path;
	}

	public boolean isUserDefined() {
		return userDefined;
	}

	public String toString() {
		return path.toOSString();
	}
	
	private void initialize(IConfigurationElement config) {
		String pathName = config.getAttribute("path");
		if (pathName != null) {
			IPluginDescriptor pd =
				config.getDeclaringExtension().getDeclaringPluginDescriptor();
			URL locationURL = pd.getInstallURL();
			try {
				URL url = InternalBootLoader.resolve(locationURL);
				IPath fullPath = new Path(url.getFile());
				fullPath = fullPath.append(pathName);
				this.path = fullPath;
			} catch (IOException e) {
			}
		}
	}
	/**
	 * Gets the enabled.
	 * @return Returns a boolean
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Sets the enabled.
	 * @param enabled The enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}