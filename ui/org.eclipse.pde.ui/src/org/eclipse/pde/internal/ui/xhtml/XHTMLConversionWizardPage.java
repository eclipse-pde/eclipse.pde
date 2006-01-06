package org.eclipse.pde.internal.ui.xhtml;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.xhtml.TocReplaceTable.TocReplaceEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.model.WorkbenchLabelProvider;


public class XHTMLConversionWizardPage extends WizardPage {

	private TocReplaceTable fTable;
	private ContainerCheckedTreeViewer fInputViewer;
	private TreeViewer fInvalidViewer;
	
	private class CP implements ITreeContentProvider {
		private boolean fInvalid;
		public CP(boolean invalidEntries) {
			fInvalid = invalidEntries;
		}
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IFile)
				return fTable.getToBeConverted((IFile)parentElement, fInvalid);
			return null;
		}
		public Object getParent(Object element) {
			if (element instanceof TocReplaceEntry)
				return ((TocReplaceEntry)element).getTocFile();
			return null;
		}
		public boolean hasChildren(Object element) {
			return element instanceof IFile;
		}
		public Object[] getElements(Object inputElement) {
			return fTable.getTocs(fInvalid);
		}
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	protected XHTMLConversionWizardPage(TocReplaceTable table) {
		super("XHTML Conversion Wizard");
		setDescription("Convert your HTML help files to XHTML");
		fTable = table;
	}

	public void createControl(Composite parent) {
		Composite columns = createComposite(parent, false, 2, false);
		SashForm viewers = new SashForm(columns, SWT.VERTICAL);
		viewers.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite valid = createComposite(viewers, true, 1, false);
		Label label = new Label(valid, SWT.NONE);
		fInputViewer = new ContainerCheckedTreeViewer(valid, SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE | SWT.BORDER);
		fInputViewer.setContentProvider(new CP(false));
		fInputViewer.setLabelProvider(new WorkbenchLabelProvider());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 400;
		gd.heightHint = 170;
		fInputViewer.getTree().setLayoutData(gd);
		fInputViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				setPageComplete(fInputViewer.getCheckedElements().length > 0);
			}
		});
		fInputViewer.setInput(new Object());
		fInputViewer.setAllChecked(true);
		fInputViewer.expandAll();
		
		if (fTable.containsInvalidEntires()) {
			Composite invalid = createComposite(viewers, true, 1, false);
			Label invalidPaths = new Label(invalid, SWT.NONE);
			invalidPaths.setText("The following entries contain invalid filepaths (and will not be converted):");
			fInvalidViewer = new TreeViewer(invalid, SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE | SWT.BORDER);
			fInvalidViewer.setContentProvider(new CP(true));
			fInvalidViewer.setLabelProvider(new WorkbenchLabelProvider());
			fInvalidViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
			fInvalidViewer.setInput(new Object());
			fInvalidViewer.expandAll();
			viewers.setWeights(new int[] {5,3});
		}
		
		Composite buttonComp = createComposite(columns, true, 1, true);
		Label blankLabel = new Label(buttonComp, SWT.NONE);
		blankLabel.setText("");
		Button selectAll = new Button(buttonComp, SWT.PUSH);
		selectAll.setText("Select All");
		selectAll.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		selectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fInputViewer.setAllChecked(true);
			}
		});
		Button deselectAll = new Button(buttonComp, SWT.PUSH);
		deselectAll.setText("Deselect All");
		deselectAll.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		deselectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fInputViewer.setAllChecked(false);
			}
		});
		
		int numValidEntries = fTable.numValidEntries();
		if (numValidEntries == 0) {
			selectAll.setEnabled(false);
			deselectAll.setEnabled(false);
			setPageComplete(false);
			label.setText("There are no entries needed for conversion");
		} else
			label.setText("Select toc entires for conversion (" + numValidEntries + " found):");
		
		setControl(columns);
		Dialog.applyDialogFont(columns);
	}
	
	protected Composite createComposite(Composite parent, boolean noMargin, int cols, boolean valignTop) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(cols, false);
		if (noMargin)
			layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);
		if (valignTop)
			comp.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		else
			comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		return comp;
	}

	protected TocReplaceEntry[] getCheckedEntries() {
		ArrayList list = new ArrayList();
		Object[] entries = fInputViewer.getCheckedElements();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i] instanceof TocReplaceEntry)
				list.add(entries[i]);
		}
		return (TocReplaceEntry[]) list.toArray(new TocReplaceEntry[list.size()]);
	}
}
