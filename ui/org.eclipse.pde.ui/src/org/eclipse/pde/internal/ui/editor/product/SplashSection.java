package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.widgets.*;


public class SplashSection extends PDESection {

	private FormEntry fPluginEntry;

	public SplashSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION|Section.TWISTIE|Section.EXPANDED);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("Splash Screen");
		section.setDescription("The splash screen appears when the product launches.  It must be named 'splash.bmp' and is typically located in the product's defining plug-in.");

		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout(3, false);
		layout.marginTop = 5;
		client.setLayout(layout);
		
		Label label = toolkit.createLabel(client, "Specify the plug-in in which the splash screen is located:");
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fPluginEntry = new FormEntry(client, toolkit, "Plug-in:", "Browse...", false);
		fPluginEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getSplashInfo().setLocation(entry.getValue());
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse();
			}
		});
		fPluginEntry.setEditable(isEditable());
				
		toolkit.paintBordersFor(client);
		section.setClient(client);
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING));
	}
	
	public void refresh() {
		fPluginEntry.setValue(getSplashInfo().getLocation());
		super.refresh();
	}
	
	public void commit(boolean onSave) {
		fPluginEntry.commit();
		super.commit(onSave);
	}
	
	public void cancelEdit() {
		fPluginEntry.cancelEdit();
		super.cancelEdit();
	}
	
	private ISplashInfo getSplashInfo() {
		ISplashInfo info = getProduct().getSplashInfo();
		if (info == null) {
			info = getModel().getFactory().createSplashInfo();
			getProduct().setSplashInfo(info);
		}
		return info;
	}
	
	private IProduct getProduct() {
		return getModel().getProduct();
	}
	
	private IProductModel getModel() {
		return (IProductModel)getPage().getPDEEditor().getAggregateModel();
	}
	
	private void handleBrowse() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), PDEPlugin.getDefault().getLabelProvider());
		dialog.setElements(PDECore.getDefault().getWorkspaceModelManager().getAllModels());
		dialog.setMultipleSelection(false);
		dialog.setTitle("Plug-in Selection");
		dialog.setMessage("Select the plug-in where the splash screen is located:");
		if (dialog.open() == ElementListSelectionDialog.OK) {
			IPluginModelBase model = (IPluginModelBase)dialog.getFirstResult();
			fPluginEntry.setValue(model.getPluginBase().getId());
		}
	}


}
