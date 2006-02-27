/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.product;

import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.iproduct.IAboutInfo;
import org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModelFactory;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.core.iproduct.ISplashInfo;
import org.eclipse.pde.internal.core.iproduct.IWindowImages;
import org.eclipse.pde.internal.core.product.SplashInfo;
import org.eclipse.pde.internal.core.product.WorkspaceProductModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.branding.IProductConstants;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ISetSelectionTarget;


public class BaseProductCreationOperation extends WorkspaceModifyOperation {

	private IFile fFile;
	
	public BaseProductCreationOperation(IFile file) {
		fFile = file;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {
		monitor.beginTask(PDEUIMessages.BaseProductCreationOperation_taskName, 2); 
		createContent();
		monitor.worked(1);
        openFile();
        monitor.done();
	}
	
	private void createContent() {
        WorkspaceProductModel model = new WorkspaceProductModel(fFile, false);
        initializeProduct(model.getProduct());
        model.save();
        model.dispose();
	}
	
	protected void initializeProduct(IProduct product) {
		IProductModelFactory factory = product.getModel().getFactory();
		IConfigurationFileInfo info = factory.createConfigFileInfo();
		info.setUse("default"); //$NON-NLS-1$
		product.setConfigurationFileInfo(info);
	}
	
	private Properties getProductProperties(IPluginElement element) {
		Properties prop = new Properties();
		IPluginObject[] children = element.getChildren();
		for (int i = 0; i < children.length; i++) {
			IPluginElement child = (IPluginElement)children[i];
			if (child.getName().equals("property")) { //$NON-NLS-1$
				String name = null;
				String value = null;
				IPluginAttribute attr = child.getAttribute("name"); //$NON-NLS-1$
				if (attr != null)
					name = attr.getValue();
				attr = child.getAttribute("value"); //$NON-NLS-1$
				if (attr != null)
					value = attr.getValue();
				if (name != null && value != null)
					prop.put(name, value);
			}
		}
		return prop;
	}
	
	protected IPluginElement getProductExtension(String productId) {
		int lastDot = productId.lastIndexOf('.');
		if (lastDot == -1)
			return null;
		String pluginId = productId.substring(0, lastDot);
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(pluginId);
		if (model != null) {
			IPluginExtension[] extensions = model.getPluginBase().getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				if ("org.eclipse.core.runtime.products".equals(extensions[i].getPoint())  //$NON-NLS-1$
						&& productId.substring(lastDot+1).equals(extensions[i].getId())) {
					IPluginObject[] children = extensions[i].getChildren();
					if (children.length > 0) {
						IPluginElement object = (IPluginElement)children[0];
						if (object.getName().equals("product")) //$NON-NLS-1$
							return object;
					}
				}
			}
		}
		return null;
	}
	
	protected void initializeProductInfo(IProductModelFactory factory, IProduct product, String id) {
		product.setId(id);
		IPluginElement element = getProductExtension(id);
		if (element != null) {
			IPluginAttribute attr = element.getAttribute("application"); //$NON-NLS-1$
			if (attr != null)
				product.setApplication(attr.getValue());
			attr = element.getAttribute("name"); //$NON-NLS-1$
			if (attr != null)
				product.setName(attr.getValue());
			Properties prop = getProductProperties(element);
			String aboutText = prop.getProperty(IProductConstants.ABOUT_TEXT);
			String aboutImage = prop.getProperty(IProductConstants.ABOUT_IMAGE);
			if (aboutText != null || aboutImage != null) {
				IAboutInfo info = factory.createAboutInfo();
				info.setText(aboutText);
				info.setImagePath(aboutImage);
				product.setAboutInfo(info);
			}
			IWindowImages winImages = factory.createWindowImages();
			String path = prop.getProperty(IProductConstants.WINDOW_IMAGES);
			if (path != null) {
				StringTokenizer tokenizer = new StringTokenizer(path, ",", true); //$NON-NLS-1$
				int size = 0;
				while (tokenizer.hasMoreTokens()) {
					String token = tokenizer.nextToken();
					if (token.equals(",")) //$NON-NLS-1$
						size++;
					else
						winImages.setImagePath(token, size);
				}
			}
			product.setWindowImages(winImages);
			
			ISplashInfo splashInfo = factory.createSplashInfo();
			splashInfo.setForegroundColor(prop.getProperty(IProductConstants.STARTUP_FOREGROUND_COLOR));
			int[] barGeo = SplashInfo.getGeometryArray(prop.getProperty(IProductConstants.STARTUP_PROGRESS_RECT));
			splashInfo.setProgressGeometry(barGeo);
			int[] messageGeo = SplashInfo.getGeometryArray(prop.getProperty(IProductConstants.STARTUP_MESSAGE_RECT));
			splashInfo.setMessageGeometry(messageGeo);
			product.setSplashInfo(splashInfo);
		}
	}
	
	protected void addPlugins(IProductModelFactory factory, IProduct product, IPluginModelBase[] plugins) {
		IProductPlugin[] pplugins = new IProductPlugin[plugins.length];
		for (int i = 0; i < plugins.length; i++) {
			String id = plugins[i].getPluginBase().getId();
			if (id != null && id.length() > 0) {
				IProductPlugin plugin = factory.createPlugin();
				plugin.setId(id);
				pplugins[i] = plugin;
			}
		}		
		product.addPlugins(pplugins);
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
					IDE.openEditor(page, fFile, PDEPlugin.PRODUCT_EDITOR_ID);
				} catch (PartInitException e) {
				}
			}
		});
	}

}
