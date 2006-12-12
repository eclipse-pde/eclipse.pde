package org.eclipse.pde.internal.ui.wizards.provisioner;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class DirectorySelectionPage extends WizardPage {
	
	class FileLabelProvider extends SharedLabelProvider {
		public Image getImage(Object obj) {
			return get(PDEPluginImages.DESC_SITE_OBJ);
		}
	}
	
	private static String LAST_LOCATION = "last_location"; //$NON-NLS-1$
	
	Text fDir = null;
	private TableViewer fTableViewer = null;
	private ArrayList fElements = new ArrayList();
	private Button fAddButton = null;
	private Button fRemoveButton = null;
	private String fLastLocation = null;

	protected DirectorySelectionPage(String pageName) {
		super(pageName);
		setTitle(PDEUIMessages.DirectorySelectionPage_title);
		setDescription(PDEUIMessages.DirectorySelectionPage_description);
		setPageComplete(false);
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		fLastLocation = pref.getString(LAST_LOCATION);
		if (fLastLocation.length() == 0)
			fLastLocation = pref.getString(ICoreConstants.PLATFORM_PATH);
	}

	public void createControl(Composite parent) {
		Composite client = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 2;
		layout.numColumns = 2;
		client.setLayout(layout);
		client.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label label = new Label(client, SWT.None);
		label.setText(PDEUIMessages.DirectorySelectionPage_label);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		
		fTableViewer = new TableViewer(client);
		fTableViewer.setLabelProvider(new FileLabelProvider());
		fTableViewer.setContentProvider(new ArrayContentProvider());
		fTableViewer.setInput(fElements);
		gd = new GridData(GridData.FILL_BOTH);
		gd.verticalSpan = 3;
		fTableViewer.getControl().setLayoutData(gd);
		
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
			
		});
		fTableViewer.getTable().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.character == SWT.DEL && event.stateMask == 0) {
					handleRemove();
				}
			}
		});
		Dialog.applyDialogFont(fTableViewer.getControl());
		Dialog.applyDialogFont(label);
		
		createButtons(client);
		
		setControl(client);
	}
	
	protected void createButtons(Composite parent) {
		fAddButton = new Button(parent, SWT.PUSH);
		fAddButton.setText(PDEUIMessages.DirectorySelectionPage_add);
		fAddButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fAddButton);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});
		
		fRemoveButton = new Button(parent, SWT.PUSH);
		fRemoveButton.setText(PDEUIMessages.DirectorySelectionPage_remove);
		fRemoveButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fRemoveButton);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		updateButtons();
		
		// automatically bring up dialog so user doesn't click "Add..." to add a directory
		handleAdd();
	}
	
	private void handleAdd() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setMessage(PDEUIMessages.DirectorySelectionPage_message);
		dialog.setFilterPath(fLastLocation);
		String path = dialog.open();
		if (path != null) {
			fLastLocation = path;
			File newDirectory = new File(path);
			fElements.add(newDirectory);
			fTableViewer.add(newDirectory);
			setPageComplete(true);
		}
	}
	
	private void handleRemove() {
		Object[] elements = ((IStructuredSelection)fTableViewer.getSelection()).toArray();
		for (int i = 0; i < elements.length; i++)
			fElements.remove(elements[i]);
		fTableViewer.remove(elements);
		setPageComplete(!fElements.isEmpty());
	}
		
	public File[] getLocations() {
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		pref.setValue(LAST_LOCATION, fLastLocation);
		return (File[]) fElements.toArray(new File[fElements.size()]);
	}
	
	protected void updateButtons() {
		int num = fTableViewer.getTable().getSelectionCount();
		fRemoveButton.setEnabled(num > 0);
	}

}
