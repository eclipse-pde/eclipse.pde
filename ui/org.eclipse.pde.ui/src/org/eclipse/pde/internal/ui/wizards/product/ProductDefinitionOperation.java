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

import java.io.*;
import java.lang.reflect.*;

import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.text.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.model.*;
import org.eclipse.pde.internal.ui.model.plugin.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.text.edits.*;
import org.eclipse.ui.branding.*;

public class ProductDefinitionOperation implements IRunnableWithProgress {

	protected String fPluginId;
	private String fProductId;
	private String fApplication;
	private Shell fShell;
	private IProduct fProduct;
	private IDocument fDocument;

	public ProductDefinitionOperation(IProduct product, String pluginId, String productId, String application, Shell shell) {
		fPluginId = pluginId;
		fProductId = productId;
		fApplication = application;
		fShell = shell;
		fProduct = product;
	}
	
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		try {
			IFile file = getFile();
			if (!file.exists()) {
				createNewFile(file);
			} else {
				modifyExistingFile(file, monitor);
			}
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		} catch (IOException e) {
			throw new InvocationTargetException(e);
		} catch (MalformedTreeException e) {
			throw new InvocationTargetException(e);
		} catch (BadLocationException e) {
			throw new InvocationTargetException(e);
		}
	}
	
	private IFile getFile() {
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(fPluginId);
		IProject project = model.getUnderlyingResource().getProject();
		String filename = model instanceof IFragmentModel ? "fragment.xml" : "plugin.xml"; //$NON-NLS-1$ //$NON-NLS-2$
		return project.getFile(filename);	
	}
	
	private IPluginModelBase getModel(IFile file) {
		if ("plugin.xml".equals(file.getName())) //$NON-NLS-1$
			return new WorkspacePluginModel(file);
		return new WorkspaceFragmentModel(file);
	}

	private PluginModelBase getEditingModel(boolean isFragment) {
		if (isFragment) 
			return new FragmentModel(fDocument, false);
		return new PluginModel(fDocument, false);
	}

	private void createNewFile(IFile file) throws CoreException {
		WorkspacePluginModelBase model = (WorkspacePluginModelBase)getModel(file);
		IPluginBase base = model.getPluginBase();
		base.setSchemaVersion("3.0"); //$NON-NLS-1$
		base.add(createExtension(model));
		model.save();
	}
	
	private IPluginExtension createExtension(IPluginModelBase model) throws CoreException{
		IPluginExtension extension = model.getFactory().createExtension();
		extension.setPoint("org.eclipse.core.runtime.products"); //$NON-NLS-1$
		extension.setId(fProductId);
		extension.add(createExtensionContent(extension));
		return extension;
	}
	
	private IPluginElement createExtensionContent(IPluginExtension extension) throws CoreException  {
		IPluginElement element = extension.getModel().getFactory().createElement(extension);
		element.setName("product"); //$NON-NLS-1$
		element.setAttribute("name", fProduct.getName()); //$NON-NLS-1$
		element.setAttribute("application", fApplication); //$NON-NLS-1$

		IPluginElement child = createWindowImagesElement(element);
		if (child != null)
			element.add(child);
		
		child = createAboutTextElement(element);
		if (child != null)
			element.add(child);
			
		child = createAboutImageElement(element);
		if (child != null)
			element.add(child);		
		
		return element;
	}
	
	private IPluginElement createAboutTextElement(IPluginElement parent) throws CoreException {
		String value = getAboutText();
		IPluginElement element = null;
		if (value != null && value.length() > 0) {
			element = parent.getModel().getFactory().createElement(parent);
			element.setName("property"); //$NON-NLS-1$
			element.setAttribute("name", IProductConstants.ABOUT_TEXT); //$NON-NLS-1$ 
			element.setAttribute("value", value); //$NON-NLS-1$ 
		}
		return element;
	}
	
	private IPluginElement createAboutImageElement(IPluginElement parent) throws CoreException {
		String image = getAboutImage();
		IPluginElement element = null;
		if (image != null && image.length() > 0) {
			element = parent.getModel().getFactory().createElement(parent);
			element.setName("property"); //$NON-NLS-1$
			element.setAttribute("name", IProductConstants.ABOUT_IMAGE); //$NON-NLS-1$ 
			element.setAttribute("value", image); //$NON-NLS-1$ 
		}
		return element;
	}
	
	private IPluginElement createWindowImagesElement(IPluginElement parent) throws CoreException {
		IPluginElement element = null;
		String value = getWindowImagesString();
		if (value != null) {
			element = parent.getModel().getFactory().createElement(parent);
			element.setName("property"); //$NON-NLS-1$
			element.setAttribute("name", IProductConstants.WINDOW_IMAGES); //$NON-NLS-1$ 
			element.setAttribute("value", value); //$NON-NLS-1$ 
		}
		return element;
	}
	
	private String getAboutText() {
		IAboutInfo info = fProduct.getAboutInfo();
		if (info != null) {
			String text = info.getText();
			return text == null || text.length() == 0 ? null : text;
		}
		return null;
	}
	
	private String getAboutImage() {
		IAboutInfo info = fProduct.getAboutInfo();
		return info != null ? getURL(info.getImagePath()) : null;
	}
	
	private String getURL(String location) {
		if (location == null || location.trim().length() == 0)
			return null;
		IPath path = new Path(location);
		if (!path.isAbsolute())
			return location;
		String projectName = path.segment(0);
		IProject project = PDEPlugin.getWorkspace().getRoot().getProject(projectName);
		if (project.exists()) {
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(project);
			if (model != null) {
				String id = model.getPluginBase().getId();
				if (fPluginId.equals(id))
					return path.removeFirstSegments(1).toString();
				return "platform:/plugin/" + id + "/" + path.removeFirstSegments(1); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return location;
	}
	
	private String getWindowImagesString() {
		IWindowImages images = fProduct.getWindowImages();
		StringBuffer buffer = new StringBuffer();
		if (images != null) {
			String image16 = getURL(images.getSmallImagePath());
			if (image16 != null)
				buffer.append(image16);

			String image32 = getURL(images.getLargeImagePath());
			if (image32 != null) {
				if (buffer.length() > 0)
					buffer.append(","); //$NON-NLS-1$
				buffer.append(image32);
			}
		}
		return buffer.length() == 0 ? null : buffer.toString();
	}
	
	private void modifyExistingFile(IFile file, IProgressMonitor monitor) throws CoreException, IOException, MalformedTreeException, BadLocationException {
		IStatus status = PDEPlugin.getWorkspace().validateEdit(new IFile[] {file}, fShell);
		if (status.getSeverity() != IStatus.OK)
			throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.pde.ui", IStatus.ERROR, NLS.bind(PDEUIMessages.ProductDefinitionOperation_readOnly, fPluginId), null)); //$NON-NLS-1$ //$NON-NLS-2$
		
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(file.getFullPath(), monitor);
			ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath());
			
			fDocument = buffer.getDocument();
			PluginModelBase model = getEditingModel("fragment.xml".equals(file.getName())); //$NON-NLS-1$
			try {
				model.load();
				if (!model.isLoaded())
					throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.pde.ui", IStatus.ERROR, NLS.bind(PDEUIMessages.ProductDefinitionOperation_malformed, fPluginId), null)); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (CoreException e) {
				throw e;
			}
			
			IPluginExtension extension = findProductExtension(model);
			TextEdit edit = null;
			if (extension == null) {
				edit = insertNewExtension(model);
			} else {
				edit = modifyExistingExtension(extension);
			}
			if (edit != null) {
				edit.apply(fDocument);
				buffer.commit(monitor, true);
			}
		} finally {
			manager.disconnect(file.getFullPath(), monitor);
		}	
	}
	
	private IPluginExtension findProductExtension(IPluginModelBase model) {
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			String point = extensions[i].getPoint();
			String id = extensions[i].getId();
			if (fProductId.equals(id) && "org.eclipse.core.runtime.products".equals(point)) { //$NON-NLS-1$
				return extensions[i];
			}
		}
		return null;
	}
	
	private TextEdit insertNewExtension(IPluginModelBase model) throws BadLocationException, CoreException {
		IPluginExtension extension = createExtension(model);
		model.getPluginBase().add(extension);
		return TextEditUtilities.getInsertOperation((IDocumentNode)extension, fDocument);
	}
	
	private TextEdit modifyExistingExtension(IPluginExtension extension) throws CoreException, MalformedTreeException, BadLocationException {
		if (extension.getChildCount() == 0) 
			return insertNewProductElement(extension);
		
		PluginElementNode element = (PluginElementNode)extension.getChildren()[0];
		
		if (!"product".equals(element.getName())) //$NON-NLS-1$
			return insertNewProductElement(extension);
		
		element.setAttribute("application", fApplication); //$NON-NLS-1$
		element.setAttribute("name", fProduct.getName()); //$NON-NLS-1$
		
		synchronizeChild(element, IProductConstants.ABOUT_IMAGE, getAboutImage());
		synchronizeChild(element, IProductConstants.ABOUT_TEXT, getAboutText());
		synchronizeChild(element, IProductConstants.WINDOW_IMAGES, getWindowImagesString());
		
		return new ReplaceEdit(element.getOffset(), element.getLength(), element.write(false));
	}
	
	private void synchronizeChild(IPluginElement element, String propertyName, String value) throws CoreException {
		IPluginElement child = null;
		IPluginObject[] children = element.getChildren();
		for (int i = 0; i < children.length; i++) {
			IPluginElement candidate = (IPluginElement)children[i];
			if (candidate.getName().equals("property")) { //$NON-NLS-1$
				IPluginAttribute attr = candidate.getAttribute("name"); //$NON-NLS-1$
				if (attr != null && attr.getValue().equals(propertyName)) {
					child = candidate;
					break;
				}
			}
		}
		if (child != null && value == null)
			element.remove(child);
		
		if (value == null)
			return;
		
		if (child == null) {
			child = element.getModel().getFactory().createElement(element);
			child.setName("property"); //$NON-NLS-1$
			element.add(child);
		}
		child.setAttribute("value", value); //$NON-NLS-1$
		child.setAttribute("name", propertyName); //$NON-NLS-1$
	}
	
	private TextEdit insertNewProductElement(IPluginExtension extension) throws CoreException {
		IPluginElement element = createExtensionContent(extension);
		extension.add(element);
		return TextEditUtilities.getInsertOperation((IDocumentNode)element, fDocument);
	}
	
}
