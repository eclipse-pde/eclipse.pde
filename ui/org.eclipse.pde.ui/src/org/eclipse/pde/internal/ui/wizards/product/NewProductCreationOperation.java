package org.eclipse.pde.internal.ui.wizards.product;

import java.lang.reflect.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.product.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.editors.text.*;
import org.eclipse.ui.part.*;


public class NewProductCreationOperation extends WorkspaceModifyOperation {

	private IFile fFile;
	private ILaunchConfiguration fLaunchConfiguration;

	public NewProductCreationOperation(IFile file, ILaunchConfiguration config) {
		fFile = file;
		fLaunchConfiguration = config;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {
		monitor.beginTask("Creating product configuration file...", 2);
		createContent();
		monitor.worked(1);
        openFile();
        monitor.done();
	}
	
	private void createContent() {
        WorkspaceProductModel model = new WorkspaceProductModel(fFile, false);
        IProduct product = model.getProduct();
        if (fLaunchConfiguration != null)
        	initializeProduct(product);
        model.save();
        model.dispose();
	}
	
	private void initializeProduct(IProduct product) {
		try {
			IProductModelFactory factory = product.getModel().getFactory();
			boolean useProduct = fLaunchConfiguration.getAttribute(ILauncherSettings.USE_PRODUCT, false);
			if (useProduct) {
				String id = fLaunchConfiguration.getAttribute(ILauncherSettings.PRODUCT, (String)null);
				if (id != null) {
					product.setId(id);
					IPluginElement element = getProductExtension(id);
					if (element != null) {
						IPluginAttribute attr = element.getAttribute("application");
						if (attr != null)
							product.setApplication(attr.getValue());
						attr = element.getAttribute("name");
						if (attr != null)
							product.setName(attr.getValue());
						Properties prop = getProductProperties(element);
						String aboutText = prop.getProperty("aboutText");
						String aboutImage = prop.getProperty("aboutImage");
						if (aboutText != null || aboutImage != null) {
							IAboutInfo info = factory.createAboutInfo();
							info.setText(aboutText);
							info.setImagePath(aboutImage);
							product.setAboutInfo(info);
						}
					}
				}
			} else {
				String appName = fLaunchConfiguration.getAttribute(ILauncherSettings.APPLICATION, LauncherUtils.getDefaultApplicationName());
				product.setApplication(appName);
			}
			IPluginModelBase[] plugins = getSelectedPlugins();
			for (int i = 0; i < plugins.length; i++) {
				String id = plugins[i].getPluginBase().getId();
				if (id != null && id.length() > 0) {
					IProductPlugin plugin = factory.createPlugin();
					plugin.setId(id);
					product.addPlugin(plugin);
				}
			}
		} catch (CoreException e) {
		}	
	}
	
	private Properties getProductProperties(IPluginElement element) {
		Properties prop = new Properties();
		IPluginObject[] children = element.getChildren();
		for (int i = 0; i < children.length; i++) {
			IPluginElement child = (IPluginElement)children[i];
			if (child.getName().equals("property")) {
				String name = null;
				String value = null;
				IPluginAttribute attr = child.getAttribute("name");
				if (attr != null)
					name = attr.getValue();
				attr = child.getAttribute("value");
				if (attr != null)
					value = attr.getValue();
				if (name != null && value != null)
					prop.put(name, value);
			}
		}
		return prop;
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
	
	private IPluginElement getProductExtension(String productId) {
		int lastDot = productId.lastIndexOf('.');
		if (lastDot == -1)
			return null;
		String pluginId = productId.substring(0, lastDot);
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(pluginId);
		if (model != null) {
			IPluginExtension[] extensions = model.getPluginBase().getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				if ("org.eclipse.core.runtime.products".equals(extensions[i].getPoint()) 
						&& productId.substring(lastDot+1).equals(extensions[i].getId())) {
					IPluginObject[] children = extensions[i].getChildren();
					if (children.length > 0) {
						IPluginElement object = (IPluginElement)children[0];
						if (object.getName().equals("product"))
							return object;
					}
				}
			}
		}
		return null;
	}
	
	private void openFile() {
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();
				if (ww == null) {
					return;
				}
				IWorkbenchPage page = ww.getActivePage();
				if (page == null || !fFile.exists())
					return;
				IWorkbenchPart focusPart = page.getActivePart();
				if (focusPart instanceof ISetSelectionTarget) {
					ISelection selection = new StructuredSelection(fFile);
					((ISetSelectionTarget) focusPart).selectReveal(selection);
				}
				try {
					page.openEditor(new FileEditorInput(fFile), EditorsUI.DEFAULT_TEXT_EDITOR_ID);
				} catch (PartInitException e) {
				}
			}
		});
	}

}
