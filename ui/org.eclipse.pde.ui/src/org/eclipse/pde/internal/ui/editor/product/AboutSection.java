package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.model.*;


public class AboutSection extends PDESection {

	private FormEntry fImageEntry;
	private FormEntry fTextEntry;

	public AboutSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION|Section.TWISTIE|Section.EXPANDED);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("About Dialog");
		section.setDescription("Customize the text and image of the About dialog.  The GIF image is typically located in the product's defining plug-in and its size must not exceed 250x330 pixels.");
		
		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout(3, false);
		layout.marginTop = 5;
		client.setLayout(layout);
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fImageEntry = new FormEntry(client, toolkit, "Image:", "Browse...", isEditable());
		fImageEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getAboutInfo().setImagePath(entry.getValue());
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse();
			}
			public void linkActivated(HyperlinkEvent e) {
				openImage();
			}
		});
		fImageEntry.setEditable(isEditable());
		
		fTextEntry = new FormEntry(client, toolkit, "Text:", SWT.MULTI|SWT.WRAP);
		fTextEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getAboutInfo().setText(entry.getValue());
			}
		});

		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		fTextEntry.getText().setLayoutData(gd);
		fTextEntry.setEditable(isEditable());
		
		toolkit.paintBordersFor(client);
		section.setClient(client);
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
	}
	
	private void handleBrowse() {
		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(
				getSection().getShell(),
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
				
		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle("Image Selection"); //$NON-NLS-1$
		dialog.setMessage("Select a GIF image:"); //$NON-NLS-1$
		dialog.addFilter(new FileExtensionFilter("gif"));
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			IFile file = (IFile)dialog.getFirstResult();
			fImageEntry.setValue(file.getFullPath().toString());
		}
	}
	
	public void refresh() {
		fImageEntry.setValue(getAboutInfo().getImagePath(), true);
		fTextEntry.setValue(getAboutInfo().getText(), true);
		super.refresh();
	}
	
	public void commit(boolean onSave) {
		fImageEntry.commit();
		fTextEntry.commit();
		super.commit(onSave);
	}
	
	public void cancelEdit() {
		fImageEntry.cancelEdit();
		fTextEntry.cancelEdit();
		super.cancelEdit();
	}
	
	private void openImage() {
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		IPath path = new Path(fImageEntry.getValue());
		if (!path.isAbsolute()) {
			path = getFullPath(path);
		}
		IResource resource = root.findMember(path);
		try {
			if (resource != null && resource instanceof IFile)
				IDE.openEditor(PDEPlugin.getActivePage(), (IFile)resource, true);
			else
				MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), "Open Image", "Specified image could not be found");
		} catch (PartInitException e) {
		}		
	}
	
	private IAboutInfo getAboutInfo() {
		IAboutInfo info = getProduct().getAboutInfo();
		if (info == null) {
			info = getModel().getFactory().createAboutInfo();
			getProduct().setAboutInfo(info);
		}
		return info;
	}
	
	private IProduct getProduct() {
		return getModel().getProduct();
	}
	
	private IProductModel getModel() {
		return (IProductModel)getPage().getPDEEditor().getAggregateModel();
	}
	
	private IPath getFullPath(IPath path) {
		String productId = getProduct().getId();
		int dot = productId.lastIndexOf('.');
		String pluginId = (dot != -1) ? productId.substring(0, dot) : "";
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
