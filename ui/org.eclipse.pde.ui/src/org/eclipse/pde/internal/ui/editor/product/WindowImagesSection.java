package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.model.*;


public class WindowImagesSection extends PDESection {

	private FormEntry fImage16;
	private FormEntry fImage32;

	public WindowImagesSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION|Section.TWISTIE|Section.EXPANDED);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEPlugin.getResourceString("WindowImagesSection.title")); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString("WindowImagesSection.desc")); //$NON-NLS-1$

		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout(3, false));
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fImage16 = new FormEntry(client, toolkit, PDEPlugin.getResourceString("WindowImagesSection.small"), PDEPlugin.getResourceString("WindowImagesSection.browse"), isEditable()); //$NON-NLS-1$ //$NON-NLS-2$
		fImage16.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getWindowImages().setSmallImagePath(entry.getValue());
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse(entry);
			}
			public void linkActivated(HyperlinkEvent e) {
				openImage(fImage16.getValue());
			}
		});
		fImage16.setEditable(isEditable());
		
		fImage32 = new FormEntry(client, toolkit, PDEPlugin.getResourceString("WindowImagesSection.large"), PDEPlugin.getResourceString("WindowImagesSection.browse"), isEditable()); //$NON-NLS-1$ //$NON-NLS-2$
		fImage32.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getWindowImages().setLargeImagePath(entry.getValue());
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse(entry);
			}
			public void linkActivated(HyperlinkEvent e) {
				openImage(fImage32.getValue());
			}
		});
		fImage32.setEditable(isEditable());
		
		toolkit.paintBordersFor(client);
		section.setClient(client);
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING));
	}
	
	public void refresh() {
		IWindowImages images = getWindowImages();
		fImage16.setValue(images.getSmallImagePath(), true);
		fImage32.setValue(images.getLargeImagePath(), true);
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
		fImage16.commit();
		fImage32.commit();
		super.commit(onSave);
	}
	
	public void cancelEdit() {
		fImage16.cancelEdit();
		fImage32.cancelEdit();
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
		dialog.setTitle("Image Selection"); //$NON-NLS-1$
		dialog.setMessage("Select a GIF image:"); //$NON-NLS-1$
		dialog.addFilter(new FileExtensionFilter("gif")); //$NON-NLS-1$
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			IFile file = (IFile)dialog.getFirstResult();
			entry.setValue(file.getFullPath().toString());
		}
	}
	
	private void openImage(String value) {
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		IPath path = new Path(value);
		if (!path.isAbsolute()) {
			path = getFullPath(path);
		}
		IResource resource = root.findMember(path);
		try {
			if (resource != null && resource instanceof IFile)
				IDE.openEditor(PDEPlugin.getActivePage(), (IFile)resource, true);
			else
				MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEPlugin.getResourceString("WindowImagesSection.open"), PDEPlugin.getResourceString("WindowImagesSection.warning")); //$NON-NLS-1$ //$NON-NLS-2$
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


}
