/*
 * Created on Oct 19, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.core.osgi.bundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.osgi.bundle.*;
import org.eclipse.pde.core.plugin.IMatchRules;

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
		if (futil==null) {
			futil = new FragmentUtil(getBundle().getHeader(IBundle.KEY_HOST_BUNDLE));
		}
		return futil;
	}
	
	public String getPluginId() {
		IBundle bundle = getBundle();
		if (bundle!=null) {
			return getFragmentUtil().getPluginId();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#getPluginVersion()
	 */
	public String getPluginVersion() {
		IBundle bundle = getBundle();
		if (bundle!=null) {
			return getFragmentUtil().getPluginVersion();
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#getRule()
	 */
	public int getRule() {
		IBundle bundle = getBundle();
		if (bundle!=null) {
			return getFragmentUtil().getMatch();
		}
		return IMatchRules.NONE;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setPluginId(java.lang.String)
	 */
	public void setPluginId(String id) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle!=null) {
			FragmentUtil futil = getFragmentUtil();
			futil.setPluginId(id);
			bundle.setHeader(IBundle.KEY_HOST_BUNDLE, futil.getHeader());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setPluginVersion(java.lang.String)
	 */
	public void setPluginVersion(String version) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle!=null) {
			FragmentUtil futil = getFragmentUtil();
			futil.setPluginVersion(version);
			bundle.setHeader(IBundle.KEY_HOST_BUNDLE, futil.getHeader());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setRule(int)
	 */
	public void setRule(int rule) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle!=null) {
			FragmentUtil futil = getFragmentUtil();
			futil.setMatch(rule);
			bundle.setHeader(IBundle.KEY_HOST_BUNDLE, futil.getHeader());
		}
	}
}
