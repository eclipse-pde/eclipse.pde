package org.eclipse.pde.internal.ui.editor.product;

import java.lang.reflect.*;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.wizards.product.*;
import org.eclipse.swt.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.progress.*;


public class ExportSection extends PDESection implements IHyperlinkListener{

	private FormEntry fArchiveEntry;
	private Button fIncludeSource;
	private Action fExportAction;

	public ExportSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEPlugin.getResourceString("Product.ExportSection.title")); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString("Product.ExportSection.desc")); //$NON-NLS-1$
		
		Composite comp = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.marginLeft = layout.marginRight = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
		layout.numColumns = 3;
		comp.setLayout(layout);
		
		FormText text = toolkit.createFormText(comp, true);
		text.setText(PDEPlugin.getResourceString("Product.overview.validate"), true, false); //$NON-NLS-1$
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		text.setLayoutData(gd);
		text.addHyperlinkListener(this);
				
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fArchiveEntry = new FormEntry(comp, toolkit, PDEPlugin.getResourceString("Product.ExportSection.archive"), PDEPlugin.getResourceString("Product.ExportSection.browse"), false, 25); //$NON-NLS-1$ //$NON-NLS-2$
		fArchiveEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getProduct().setExportDestination(entry.getValue().trim());
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse();
			}
			private void handleBrowse() {
				FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
				dialog.setFileName(fArchiveEntry.getValue());
				String extension = Platform.getOS().equals("macosx") ? ".tar.gz" : ".zip"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				dialog.setFilterExtensions(new String[] {"*" + extension}); //$NON-NLS-1$
				String res = dialog.open();
				if (res != null) {
					fArchiveEntry.getText().setText(res);
				}
			}
		});
		fArchiveEntry.setEditable(isEditable());
		fArchiveEntry.getText().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fIncludeSource = toolkit.createButton(comp, PDEPlugin.getResourceString("Product.ExportSection.includeSource"), SWT.CHECK); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalIndent = 25;
		gd.horizontalSpan = 3;
		fIncludeSource.setLayoutData(gd);
		fIncludeSource.setEnabled(isEditable());
		fIncludeSource.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getProduct().setIncludeSource(fIncludeSource.getSelection());
			}
		});
		
		text = toolkit.createFormText(comp, true);
		text.setText(PDEPlugin.getResourceString("Product.overview.export"), true, false); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		text.setLayoutData(gd);
		text.addHyperlinkListener(this);
		
		toolkit.paintBordersFor(comp);
		section.setClient(comp);
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		fArchiveEntry.commit();
		super.commit(onSave);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#cancelEdit()
	 */
	public void cancelEdit() {
		fArchiveEntry.cancelEdit();
		super.cancelEdit();
	}
	
	private IProduct getProduct() {
		IBaseModel model = getPage().getPDEEditor().getAggregateModel();
		return ((IProductModel)model).getProduct();
	}
	
	private Shell getShell() {
		return getPage().getEditor().getSite().getShell();
	}
	
	public void refresh() {
		fArchiveEntry.setValue(getProduct().getExportDestination(), true);
		fIncludeSource.setSelection(getProduct().includeSource());
		super.refresh();
	}

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

	public void linkEntered(HyperlinkEvent e) {
		getStatusLineManager().setMessage(e.getLabel());
	}

	public void linkExited(HyperlinkEvent e) {
		getStatusLineManager().setMessage(null);
	}

	public void linkActivated(HyperlinkEvent e) {
		String href = (String) e.getHref();
		if (href.equals("action.export"))  { //$NON-NLS-1$
			getExportAction().run();
		} else if (href.equals("action.synchronize")) { //$NON-NLS-1$
			handleSynchronize();
		}
	}
	
	private IStatusLineManager getStatusLineManager() {
		IEditorSite site = getPage().getEditor().getEditorSite();
		return site.getActionBars().getStatusLineManager();
	}
	
	private Action getExportAction() {
		if (fExportAction == null)
			fExportAction = new ProductExportAction(getPage().getPDEEditor());
		return fExportAction;
	}
	
	private void handleSynchronize() {
		try {
			IProgressService service = PlatformUI.getWorkbench().getProgressService();
			SynchronizationOperation op = new SynchronizationOperation(getProduct(), getPage().getSite().getShell());
			service.runInUI(service, op, PDEPlugin.getWorkspace().getRoot());
			MessageDialog.openInformation(getPage().getSite().getShell(), "Synchronize", "The product's defining plug-in has been synchronized successfully.");
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {		
			MessageDialog.openError(getPage().getSite().getShell(), "Synchronize", e.getTargetException().getMessage()); //$NON-NLS-1$
		}
	}
	


}
