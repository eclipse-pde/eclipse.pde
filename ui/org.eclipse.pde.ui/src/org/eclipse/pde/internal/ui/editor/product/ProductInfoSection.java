package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.jface.action.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.product.*;
import org.eclipse.swt.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;


public class ProductInfoSection extends PDESection {

	private FormEntry fNameEntry;
	private ComboPart fAppCombo;
	private ComboPart fProductCombo;
	private Button fPluginButton;
	private Button fFeatureButton;
	
	private static int NUM_COLUMNS = 3;

	public ProductInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEPlugin.getResourceString("ProductInfoSection.title")); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString("ProductInfoSection.desc")); //$NON-NLS-1$

		Composite client = toolkit.createComposite(section);
		TableWrapLayout layout = new TableWrapLayout();
		layout.leftMargin = layout.rightMargin = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
		layout.numColumns = NUM_COLUMNS;
		client.setLayout(layout);

		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		
		createIdEntry(client, toolkit, actionBars);
		createApplicationEntry(client, toolkit, actionBars);
		createNameEntry(client, toolkit, actionBars);
		createConfigurationOption(client, toolkit);
		
		toolkit.paintBordersFor(client);
		section.setClient(client);	
		GridData gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
		gd.verticalSpan = 2;
		section.setLayoutData(gd);
	}
	
	private void createNameEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		createLabel(client, toolkit, ""); //$NON-NLS-1$
		createLabel(client, toolkit, PDEPlugin.getResourceString("ProductInfoSection.titleLabel")); //$NON-NLS-1$

		fNameEntry = new FormEntry(client, toolkit, "Product Name:", null, false); //$NON-NLS-1$
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getProduct().setName(entry.getValue().trim());
			}
		});
		TableWrapData td = (TableWrapData)fNameEntry.getText().getLayoutData();
		td.grabHorizontal = false;
		fNameEntry.setEditable(isEditable());
	}
	
	private void createIdEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		createLabel(client, toolkit, PDEPlugin.getResourceString("ProductInfoSection.prodIdLabel")); //$NON-NLS-1$

		Label label = toolkit.createLabel(client, PDEPlugin.getResourceString("ProductInfoSection.id")); //$NON-NLS-1$
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		TableWrapData td = new TableWrapData();
		td.valign = TableWrapData.MIDDLE;
		label.setLayoutData(td);
		
		fProductCombo = new ComboPart();
		fProductCombo.createControl(client, toolkit, SWT.READ_ONLY);
		td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.valign = TableWrapData.MIDDLE;
		td.maxWidth = 100;
		fProductCombo.getControl().setLayoutData(td);
		fProductCombo.setItems(TargetPlatform.getProductNames());
		fProductCombo.add(""); //$NON-NLS-1$
		fProductCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getProduct().setId(fProductCombo.getSelection());
			}
		});
		
		Button button = toolkit.createButton(client, PDEPlugin.getResourceString("ProductInfoSection.new"), SWT.PUSH); //$NON-NLS-1$
		button.setEnabled(isEditable());
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleNewDefinition();
			}
		});	
		fProductCombo.getControl().setEnabled(isEditable());
	}

	private void handleNewDefinition() {
		ProductDefinitionWizard wizard = new ProductDefinitionWizard(getProduct());
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 400, 500);
		if (dialog.open() == WizardDialog.OK) {
			String id = wizard.getProductId();
			fProductCombo.add(id, 0);
			fProductCombo.setText(id);
			getProduct().setId(id);
			String app = wizard.getApplication();
			fAppCombo.setText(app);
			getProduct().setApplication(app);
		}
	}

	private void createApplicationEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		createLabel(client, toolkit, ""); //$NON-NLS-1$
		createLabel(client, toolkit, PDEPlugin.getResourceString("ProductInfoSection.appLabel")); //$NON-NLS-1$
		
		Label label = toolkit.createLabel(client, PDEPlugin.getResourceString("ProductInfoSection.app"), SWT.WRAP); //$NON-NLS-1$
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		TableWrapData td = new TableWrapData();
		td.valign = TableWrapData.MIDDLE;
		label.setLayoutData(td);
		
		fAppCombo = new ComboPart();
		fAppCombo.createControl(client, toolkit, SWT.READ_ONLY);
		td = new TableWrapData(TableWrapData.FILL);
		td.colspan = NUM_COLUMNS - 1;
		td.valign = TableWrapData.MIDDLE;
		td.maxWidth = 100;
		fAppCombo.getControl().setLayoutData(td);
		fAppCombo.setItems(TargetPlatform.getApplicationNames());
		fAppCombo.add(""); //$NON-NLS-1$
		fAppCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getProduct().setApplication(fAppCombo.getSelection());
			}
		});
		
		fAppCombo.getControl().setEnabled(isEditable());
	}
	
	private void createConfigurationOption(Composite client, FormToolkit toolkit) {
		createLabel(client, toolkit, ""); //$NON-NLS-1$
		FormText text = toolkit.createFormText(client, true);
		text.setText(PDEPlugin.getResourceString("Product.overview.configuration"), true, true); //$NON-NLS-1$
		TableWrapData gd = new TableWrapData(TableWrapData.FILL);
		gd.colspan = NUM_COLUMNS;
		text.setLayoutData(gd);
		text.addHyperlinkListener(new IHyperlinkListener() {
			public void linkEntered(HyperlinkEvent e) {
				getStatusLineManager().setMessage(e.getLabel());
			}
			public void linkExited(HyperlinkEvent e) {
				getStatusLineManager().setMessage(null);
			}
			public void linkActivated(HyperlinkEvent e) {
				String pageId = fPluginButton.getSelection() ? ConfigurationPage.PLUGIN_ID : ConfigurationPage.FEATURE_ID;
				getPage().getEditor().setActivePage(pageId);
			}
		});
		
		fPluginButton = toolkit.createButton(client, "plug-ins", SWT.RADIO); //$NON-NLS-1$
		gd = new TableWrapData();
		gd.indent = 25;
		fPluginButton.setLayoutData(gd);
		fPluginButton.setEnabled(isEditable());
		fPluginButton.addSelectionListener(new SelectionAdapter() {	
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fPluginButton.getSelection();
				getProduct().setUseFeatures(!selected);
				((ProductEditor)getPage().getEditor()).updateConfigurationPage();
			}
		});
		
		fFeatureButton = toolkit.createButton(client, "features", SWT.RADIO); //$NON-NLS-1$
		gd = new TableWrapData();
		gd.indent = 25;
		fFeatureButton.setLayoutData(gd);
		fFeatureButton.setEnabled(isEditable());
	}
	
	private void createLabel(Composite client, FormToolkit toolkit, String text) {
		Label label = toolkit.createLabel(client, text, SWT.WRAP);
		TableWrapData td = new TableWrapData();
		td.colspan = NUM_COLUMNS;
		label.setLayoutData(td);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		fNameEntry.commit();
		super.commit(onSave);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#cancelEdit()
	 */
	public void cancelEdit() {
		fNameEntry.cancelEdit();
		super.cancelEdit();
	}
	
	private IProduct getProduct() {
		IBaseModel model = getPage().getPDEEditor().getAggregateModel();
		return ((IProductModel)model).getProduct();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		IProduct product = getProduct();
		fNameEntry.setValue(product.getName(), true);
		fProductCombo.setText(product.getId());
		fAppCombo.setText(product.getApplication());
		fPluginButton.setSelection(!product.useFeatures());
		fFeatureButton.setSelection(product.useFeatures());
		super.refresh();
	}
	
	private IStatusLineManager getStatusLineManager() {
		IEditorSite site = getPage().getEditor().getEditorSite();
		return site.getActionBars().getStatusLineManager();
	}
	
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}


}
