package org.eclipse.pde.internal.core.bundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.internal.core.ibundle.*;
import org.osgi.framework.*;

public class BundleFragment extends BundlePluginBase implements IBundleFragment {

	public String getPluginId() {
		return parseSingleValuedHeader(Constants.FRAGMENT_HOST);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#getPluginVersion()
	 */
	public String getPluginVersion() {
		return getAttribute(Constants.FRAGMENT_HOST, Constants.BUNDLE_VERSION_ATTRIBUTE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#getRule()
	 */
	public int getRule() {
		return IMatchRules.NONE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setPluginId(java.lang.String)
	 */
	public void setPluginId(String id) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle != null) {
			String oldValue = getPluginId();
			bundle.setHeader(Constants.FRAGMENT_HOST, writeFragmentHost(id, getPluginVersion()));
			model.fireModelObjectChanged(this, P_PLUGIN_ID, oldValue, id);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setPluginVersion(java.lang.String)
	 */
	public void setPluginVersion(String version) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle != null) {
			String oldValue = getPluginVersion();
			bundle.setHeader(Constants.FRAGMENT_HOST, writeFragmentHost(getPluginId(), version));
			model.fireModelObjectChanged(this, P_PLUGIN_VERSION, oldValue, version);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setRule(int)
	 */
	public void setRule(int rule) throws CoreException {
	}
	
	private String writeFragmentHost(String id, String version) {
		StringBuffer buffer = new StringBuffer();
		if (id != null)
			buffer.append(id);
		
		if (version != null && version.trim().length() > 0) {
			buffer.append(";" + Constants.BUNDLE_VERSION_ATTRIBUTE + "=\"" + version.trim() + "\"");
		}
		return buffer.toString();
	}
}
