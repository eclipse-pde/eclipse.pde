package org.eclipse.pde.internal.ui.wizards.product;

import java.io.*;
import java.lang.reflect.*;

import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.text.edits.*;
import org.eclipse.ui.branding.*;

public class ProductDefinitionOperation implements IRunnableWithProgress {

	private String fPluginId;
	private String fProductId;
	private String fApplication;
	private Shell fShell;
	private IProduct fProduct;

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
		
		IPluginElement element = model.getFactory().createElement(extension);
		element.setName("product"); //$NON-NLS-1$
		element.setAttribute("name", fProduct.getName()); //$NON-NLS-1$
		element.setAttribute("application", fApplication); //$NON-NLS-1$

		IAboutInfo info = fProduct.getAboutInfo();
		if (info != null) {
			String value = info.getText();
			if (value != null && value.length() > 0) {
				IPluginElement property = model.getFactory().createElement(element);
				property.setName("property"); //$NON-NLS-1$
				property.setAttribute("name", IProductConstants.ABOUT_TEXT); //$NON-NLS-1$ //$NON-NLS-2$
				property.setAttribute("value", value); //$NON-NLS-1$
				element.add(property);
			}
		
			String image = info.getImagePath();
			if (image != null && image.length() > 0) {
				IPluginElement property = model.getFactory().createElement(element);
				property.setName("property"); //$NON-NLS-1$
				property.setAttribute("name", IProductConstants.ABOUT_IMAGE); //$NON-NLS-1$ 
				property.setAttribute("value", image); //$NON-NLS-1$ 
				element.add(property);
			}
		}
		
		IWindowImages images = fProduct.getWindowImages();
		if (images != null) {
			StringBuffer buffer = new StringBuffer();
			
			String image16 = images.getSmallImagePath();
			if (image16 != null && image16.length() > 0)
				buffer.append(image16);
			
			String image32 = images.getLargeImagePath();
			if (image32 != null && image32.length() > 0) {
				if (buffer.length() > 0)
					buffer.append(","); //$NON-NLS-1$
				buffer.append(image32);
			}
			if (buffer.length() > 0) {
				IPluginElement property = model.getFactory().createElement(element);
				property.setName("property"); //$NON-NLS-1$
				property.setAttribute("name", IProductConstants.WINDOW_IMAGES); //$NON-NLS-1$ 
				property.setAttribute("value", buffer.toString()); //$NON-NLS-1$ 
				element.add(property);
			}
		}
	
		extension.add(element);
		return extension;
	}
	
	private void modifyExistingFile(IFile file, IProgressMonitor monitor) throws CoreException, IOException, MalformedTreeException, BadLocationException {
		IStatus status = PDEPlugin.getWorkspace().validateEdit(new IFile[] {file}, fShell);
		if (status.getSeverity() != IStatus.OK)
			throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.pde.ui", IStatus.ERROR, "The operation cannot proceed because plug-in '" + fPluginId + "' has a read-only manifest file.", null)); //$NON-NLS-1$
		
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(file.getFullPath(), monitor);
			ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath());
			if (buffer.isDirty()) {
				buffer.commit(monitor, true);
			}
			IPluginModelBase model = getModel(file);
			try {
				model.load();
				if (!model.isLoaded())
					throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.pde.ui", IStatus.ERROR, "The operation cannot proceed because plug-in '" + fPluginId + "' has a malformed manifest file.", null)); //$NON-NLS-1$
			} catch (CoreException e) {
				throw e;
			}
			IDocument document = buffer.getDocument();
			int offset = getInsertOffset(document, model instanceof IFragmentModel ? "fragment" : "plugin"); //$NON-NLS-1$ //$NON-NLS-2$
			if (offset != -1) {
				InsertEdit edit = new InsertEdit(offset, getTextToBeInserted(model, TextUtilities.getDefaultLineDelimiter(document)));
				edit.apply(document);
			}
			buffer.commit(monitor, true);
		} finally {
			manager.disconnect(file.getFullPath(), monitor);
		}	
	}
	
	private String getTextToBeInserted(IPluginModelBase model, String separator) throws CoreException {
		IPluginExtension extension = createExtension(model);
		StringBuffer buffer = new StringBuffer();
		buffer.append("   <extension" + separator); //$NON-NLS-1$
		buffer.append("         id=\"" + extension.getId() + "\"" + separator); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append("         point=\"" + extension.getPoint() + "\">" + separator); //$NON-NLS-1$ //$NON-NLS-2$

		IPluginObject[] children = extension.getChildren();
		for (int i = 0; i < children.length; i++) {
			writeElement((IPluginElement)children[i], "      ", separator, buffer); //$NON-NLS-1$
		}
		buffer.append("   </extension>" + separator); //$NON-NLS-1$
		return buffer.toString();
	}
	
	private void writeElement(IPluginElement element, String indent, String separator, StringBuffer buffer) {
		buffer.append(indent + "<" + element.getName()); //$NON-NLS-1$
		IPluginAttribute[] attrs = element.getAttributes();
		for (int i = 0; i < attrs.length; i++) {
			buffer.append(separator);
			buffer.append(indent + "      " + attrs[i].getName() + "=\"" + CoreUtility.getWritableString(attrs[i].getValue()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		buffer.append(">"); //$NON-NLS-1$
		buffer.append(separator);
		
		IPluginObject[] children = element.getChildren();
		for (int i = 0; i < children.length; i++) {
			writeElement((IPluginElement)children[i], indent + "   ", separator, buffer); //$NON-NLS-1$
		}
		buffer.append(indent + "</" + element.getName() + ">" + separator); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private int getInsertOffset(IDocument document, String tagName) throws BadLocationException {
		FindReplaceDocumentAdapter adapter = new FindReplaceDocumentAdapter(document);
		IRegion region = adapter.find(document.getLength() - 1, "</" + tagName + ">", false, true, false, false); //$NON-NLS-1$ //$NON-NLS-2$
		if (region != null)
			return region.getOffset();
		return -1;
	}
	
}
