package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.jface.text.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.plugin.*;

/**
 * @author melhem
 *
 */
public class FragmentModel extends PluginModelBase implements IFragmentModel{
	
	public FragmentModel(IDocument document, boolean isReconciling) {
		super(document, isReconciling);
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getBundleDescription()
	 */
	public BundleDescription getBundleDescription() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#setBundleDescription(org.eclipse.osgi.service.resolver.BundleDescription)
	 */
	public void setBundleDescription(BundleDescription description) {
	}
}
