/*
 * Created on Oct 19, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.core.osgi.bundle;

import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.osgi.bundle.*;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.osgi.framework.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BundleFragment extends BundlePluginBase implements IBundleFragment {
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#getPluginId()
	 */
	private FragmentUtil futil;
	
	public void reset() {
		super.reset();
		futil = null;
	}
	
	private FragmentUtil getFragmentUtil() {
		if (futil == null) {
			futil = new FragmentUtil((String)getManifest().get(Constants.FRAGMENT_HOST));
		}
		return futil;
	}
	
	public String getPluginId() {
		Dictionary manifest = getManifest();
		if (manifest != null) {
			return getFragmentUtil().getPluginId();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#getPluginVersion()
	 */
	public String getPluginVersion() {
		Dictionary manifest = getManifest();
		if (manifest != null) {
			return getFragmentUtil().getPluginVersion();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#getRule()
	 */
	public int getRule() {
		Dictionary manifest = getManifest();
		if (manifest!=null) {
			return getFragmentUtil().getMatch();
		}
		return IMatchRules.NONE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setPluginId(java.lang.String)
	 */
	public void setPluginId(String id) throws CoreException {
		Dictionary manifest = getManifest();
		if (manifest != null) {
			FragmentUtil futil = getFragmentUtil();
			futil.setPluginId(id);
			manifest.put(Constants.FRAGMENT_HOST, futil.getHeader());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setPluginVersion(java.lang.String)
	 */
	public void setPluginVersion(String version) throws CoreException {
		Dictionary manifest = getManifest();
		if (manifest != null) {
			FragmentUtil futil = getFragmentUtil();
			futil.setPluginVersion(version);
			manifest.put(Constants.FRAGMENT_HOST, futil.getHeader());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setRule(int)
	 */
	public void setRule(int rule) throws CoreException {
		Dictionary manifest = getManifest();
		if (manifest != null) {
			FragmentUtil futil = getFragmentUtil();
			futil.setMatch(rule);
			manifest.put(Constants.FRAGMENT_HOST, futil.getHeader());
		}
	}
}
