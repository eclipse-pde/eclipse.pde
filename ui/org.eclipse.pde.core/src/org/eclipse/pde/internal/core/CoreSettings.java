/**
 * Created on Apr 10, 2002
 *
 * To change this generated comment edit the template variable "filecomment":
 * Workbench>Preferences>Java>Templates.
 */
package org.eclipse.pde.internal.core;

import java.io.*;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Workbench>Preferences>Java>Templates.
 */
public class CoreSettings extends Properties {
	private static final String FILE = "settings.properties";
	IPath path;
	private Properties defaults;

	/**
	 * Constructor for CoreSettings.
	 */
	public CoreSettings() {
		defaults = new Properties();
	}

	/**
	 * Constructor for CoreSettings.
	 * @param defaults
	 */
	public CoreSettings(Properties defaults) {
		super(defaults);
	}

	public void load(IPath location) {
		path = location.append(FILE);
		File file = new File(path.toOSString());
		if (file.exists()) {
			try {
				FileInputStream fis = new FileInputStream(file);
				super.load(fis);
				fis.close();
			} catch (IOException e) {
			}
		}
	}
	
	public String getString(String key) {
		return getProperty(key, defaults.getProperty(key));
	}
	
	public void setValue(String key, String value) {
		setProperty(key, value);
	}
	public void setDefault(String key, String value) {
		defaults.setProperty(key, value);
	}

	public void store() {
		File file = new File(path.toOSString());
		try {
			FileOutputStream fos = new FileOutputStream(file);
			store(fos, "PDE Core settings");
			fos.flush();
			fos.close();
		} catch (IOException e) {
		}
	}

}