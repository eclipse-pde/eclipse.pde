package org.eclipse.pde.internal.core.product;

import org.eclipse.pde.internal.core.iproduct.*;


public class ProductModelFactory implements IProductModelFactory {

	private IProductModel fModel;

	public ProductModelFactory(IProductModel model) {
		fModel = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModelFactory#createProduct()
	 */
	public IProduct createProduct() {
		return new Product(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModelFactory#createAboutInfo()
	 */
	public IAboutInfo createAboutInfo() {
		return new AboutInfo(fModel);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModelFactory#createPlugin()
	 */
	public IProductPlugin createPlugin() {
		return new ProductPlugin(fModel);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductModelFactory#createConfigFileInfo()
	 */
	public IConfigurationFileInfo createConfigFileInfo() {
		return new ConfigurationFileInfo(fModel);
	}

	public IWindowImages createWindowImages() {
		return new WindowImages(fModel);
	}

	public ISplashInfo createSplashInfo() {
		return new SplashInfo(fModel);
	}

	public ILauncherInfo createLauncherInfo() {
		return new LauncherInfo(fModel);
	}

	public IProductFeature createFeature() {
		return new ProductFeature(fModel);
	}

	public IArgumentsInfo createLauncherArguments() {
		return new ArgumentsInfo(fModel);
	}

}
