/*
 * Created on Oct 19, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.core.osgi.bundle;

import org.eclipse.pde.core.osgi.bundle.IBundleFragmentModel;
import org.eclipse.pde.core.plugin.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BundleFragmentModel
	extends BundlePluginModelBase
	implements IBundleFragmentModel {
	
	public IPluginBase createPluginBase() {
		BundleFragment bfragment = new BundleFragment();
		bfragment.setModel(this);
		return bfragment;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragmentModel#getFragment()
	 */
	public IFragment getFragment() {
		return (IFragment)getPluginBase();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#isFragmentModel()
	 */
	public boolean isFragmentModel() {
		return true;
	}
}
