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
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IWindowImages;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.AbstractFormValidator;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.IEditorValidationProvider;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.FileExtensionFilter;
import org.eclipse.pde.internal.ui.util.FileValidator;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;


public class WindowImagesSection extends PDESection {

	private static final String[] F_ICON_LABELS = new String[] {
		PDEUIMessages.WindowImagesSection_16,
		PDEUIMessages.WindowImagesSection_32,
		PDEUIMessages.WindowImagesSection_48,
		PDEUIMessages.WindowImagesSection_64,
		PDEUIMessages.WindowImagesSection_128
	};
	private static final int[][] F_ICON_DIMENSIONS = new int[][] {
		{16, 16}, {32, 32}, {48, 48}, {64, 64}, {128, 128}
	};
	private FormEntry[] fImages = new FormEntry[F_ICON_LABELS.length];
	
	public WindowImagesSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.WindowImagesSection_title); 
		section.setDescription(PDEUIMessages.WindowImagesSection_desc); 

		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout(3, false));
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		
		for (int i = 0; i < fImages.length; i++) {
			final int index = i;
			fImages[index] = new FormEntry(client, 
					toolkit, F_ICON_LABELS[index], 
					PDEUIMessages.WindowImagesSection_browse, 
					isEditable());
			fImages[index].setFormEntryListener(new FormEntryAdapter(this, actionBars) {
				public void textValueChanged(FormEntry entry) {
					getWindowImages().setImagePath(entry.getValue(), index);
				}
				public void browseButtonSelected(FormEntry entry) {
					handleBrowse(entry);
				}
				public void linkActivated(HyperlinkEvent e) {
					openImage(fImages[index].getValue());
				}
			});
			fImages[index].setEditable(isEditable());
			fImages[index].setValidator(new AbstractFormValidator(this) {
				public boolean inputValidates() {
					return isValidImage(fImages[index], F_ICON_DIMENSIONS[index]);
				}
			});
		}
		
		toolkit.paintBordersFor(client);
		section.setClient(client);
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING));
	}
	
	public void refresh() {
		IWindowImages images = getWindowImages();
		for (int i = 0; i < F_ICON_LABELS.length; i++) {
			fImages[i].setValue(images.getImagePath(i), true);
		}
		super.refresh();
	}

	private IWindowImages getWindowImages() {
		IWindowImages images = getProduct().getWindowImages();
		if (images == null) {
			images = getModel().getFactory().createWindowImages();
			getProduct().setWindowImages(images);
		}
		return images;
	}
	
	private IProduct getProduct() {
		return getModel().getProduct();
	}
	
	private IProductModel getModel() {
		return (IProductModel)getPage().getPDEEditor().getAggregateModel();
	}

	public void commit(boolean onSave) {
		for (int i = 0; i < F_ICON_LABELS.length; i++) {
			fImages[i].commit();
		}
		super.commit(onSave);
	}
	
	public void cancelEdit() {
		for (int i = 0; i < F_ICON_LABELS.length; i++) {
			fImages[i].cancelEdit();
		}
		super.cancelEdit();
	}
	
	private void handleBrowse(FormEntry entry) {
		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(
				getSection().getShell(),
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
				
		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.WindowImagesSection_dialogTitle);  
		dialog.setMessage(PDEUIMessages.WindowImagesSection_dialogMessage); 
		dialog.addFilter(new FileExtensionFilter("gif")); //$NON-NLS-1$
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			IFile file = (IFile)dialog.getFirstResult();
			entry.setValue(file.getFullPath().toString());
		}
	}
	
	private void openImage(String value) {
		IResource resource = getImageResource(value);
		try {
			if (resource != null && resource instanceof IFile)
				IDE.openEditor(PDEPlugin.getActivePage(), (IFile)resource, true);
			else
				MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_warning); // 
		} catch (PartInitException e) {
		}			
	}
	
	private IPath getFullPath(IPath path) {
		String productId = getProduct().getId();
		int dot = productId.lastIndexOf('.');
		String pluginId = (dot != -1) ? productId.substring(0, dot) : ""; //$NON-NLS-1$
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(pluginId);
		if (model != null && model.getUnderlyingResource() != null) {
			IPath newPath = new Path(model.getInstallLocation()).append(path);
			IContainer container = PDEPlugin.getWorkspace().getRoot().getContainerForLocation(newPath);
			if (container != null) {
				return container.getFullPath();
			}
		}
		return path;
	}

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

	private IResource getImageResource(String value) {
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		IPath path = new Path(value);
		if(path.isEmpty()){
			MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_emptyPath); // 
			return null;
		}
		if (!path.isAbsolute()) {
			path = getFullPath(path);
		}
		return root.findMember(path);
	}
	
	
	/*
	 * dimensions[0] = width of image
	 * dimensions[1] = height of image
	 */
	private boolean isValidImage(IEditorValidationProvider provider, int[] dimensions) {
		String imagePath = provider.getProviderValue();
		String message = null;
		if (imagePath.length() == 0) // ignore empty strings
			return true;
		IResource resource = getImageResource(imagePath);
		if (resource != null && resource instanceof IFile) {
			Image image = null;
			try {
				image = new Image(getSection().getDisplay(),
						((IFile)resource).getLocation().toString());
			} catch (SWTException e) {
				message = imagePath + " is not a path to a valid image.";
			}
			if (image != null) {
				Rectangle bounds = image.getBounds();
				if (bounds.width != dimensions[0]) {
					message = "Image has incorrect width: " + bounds.width;
				} else if (bounds.height != dimensions[1]) {
					message = "Image has incorrect height: " + bounds.height;
				}
				image.dispose();
			}
		} else
			message = imagePath + " is not a path to a valid file.";
		
		if (message != null) {
			String fieldName = null;
			if (provider instanceof FormEntry)
				fieldName = ((FormEntry)provider).getLabelValue();
			if (fieldName != null)
				fieldName = fieldName + " ";
			else
				fieldName = "";
			provider.getValidator().setMessage(fieldName + message);
		}
		
		return message == null;
	}
}
