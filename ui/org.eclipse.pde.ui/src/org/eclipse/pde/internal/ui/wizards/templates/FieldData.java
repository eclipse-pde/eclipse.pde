
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.pde.ui.templates.IFieldData;

	public class FieldData implements IFieldData {
		boolean fragment;
		String name;
		String version;
		String pluginId;
		String pluginVersion;
		int match;
		String provider;
		boolean doMain;
		String className;
		boolean thisCheck;
		boolean bundleCheck;
		boolean workspaceCheck;
		
		public boolean isFragment() {
			return fragment;
		}
		
		public String getName() {
			return name;
		}
		public String getVersion() {
			return version;
		}
		public String getProvider() {
			return provider;
		}
		public String getClassName() {
			return className;
		}
	}
