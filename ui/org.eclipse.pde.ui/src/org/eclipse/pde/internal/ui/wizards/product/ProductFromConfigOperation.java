package org.eclipse.pde.internal.ui.wizards.product;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.launcher.*;


public class ProductFromConfigOperation extends BaseProductCreationOperation {

	private ILaunchConfiguration fLaunchConfiguration;

	public ProductFromConfigOperation(IFile file, ILaunchConfiguration config) {
		super(file);
		fLaunchConfiguration = config;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.product.BaseProductCreationOperation#initializeProduct(org.eclipse.pde.internal.core.iproduct.IProduct)
	 */
	protected void initializeProduct(IProduct product) {
		if (fLaunchConfiguration == null)
			return;
		try {
			IProductModelFactory factory = product.getModel().getFactory();
			boolean useProduct = fLaunchConfiguration.getAttribute(ILauncherSettings.USE_PRODUCT, false);
			if (useProduct) {
				String id = fLaunchConfiguration.getAttribute(ILauncherSettings.PRODUCT, (String)null);
				if (id != null) {
					initializeProductInfo(factory, product, id);
				}
			} else {
				String appName = fLaunchConfiguration.getAttribute(ILauncherSettings.APPLICATION, LauncherUtils.getDefaultApplicationName());
				product.setApplication(appName);
			}
			addPlugins(factory, product, getSelectedPlugins());
		} catch (CoreException e) {
		}	
	}
	
	private IPluginModelBase[] getSelectedPlugins() {
		try {
			if (fLaunchConfiguration.getAttribute(ILauncherSettings.USE_DEFAULT, true)) {
				return PDECore.getDefault().getModelManager().getPlugins();
			}
			return LauncherUtils.getSelectedPlugins(fLaunchConfiguration);
		} catch (CoreException e) {
		}
		return new IPluginModelBase[0];
	}

}
