package org.eclipse.pde.internal.ui.wizards.provisioner;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.LocationDialog;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class DirectorySelectionPage extends WizardPage {
	
	class FileLabelProvider extends SharedLabelProvider {
		public Image getImage(Object obj) {
			return get(PDEPluginImages.DESC_SITE_OBJ);
		}
	}

	Text fDir = null;
	private TableViewer fTableViewer = null;
	private ArrayList fElements = new ArrayList();
	private Button fAddButton = null;
	private Button fEditButton = null;
	private Button fRemoveButton = null;

	protected DirectorySelectionPage(String pageName) {
		super(pageName);
		setTitle(PDEUIMessages.DirectorySelectionPage_title);
		setDescription(PDEUIMessages.DirectorySelectionPage_description);
		setPageComplete(false);
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
		
		fEditButton = new Button(parent, SWT.PUSH);
		fEditButton.setText(PDEUIMessages.DirectorySelectionPage_edit);
		fEditButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fEditButton);
		fEditButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleEdit();
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
	}
	
	private void handleAdd() {
		showDialog(-1);
	}
	
	private void handleEdit() {
		showDialog(fTableViewer.getTable().getSelectionIndex());
	}
	
	private void handleRemove() {
		Object[] elements = ((IStructuredSelection)fTableViewer.getSelection()).toArray();
		for (int i = 0; i < elements.length; i++)
			fElements.remove(elements[i]);
		fTableViewer.remove(elements);
		setPageComplete(!fElements.isEmpty());
	}
		
	
	private void showDialog(final int selectionIndex) {
		final Object selection = fTableViewer.getElementAt(selectionIndex);
		final String location = (selection != null) ? ((File)selection).getPath() : null;
		BusyIndicator.showWhile(fTableViewer.getTable().getDisplay(), new Runnable() {
			public void run() {
				LocationDialog dialog = new LocationDialog(fTableViewer.getTable()
						.getShell(), location) {

					protected boolean hasPath(String path) {
						return fElements.contains(new File(path));
					}

				};
				dialog.create();
				SWTUtil.setDialogSize(dialog, 500, -1);
				if (dialog.open() == Window.OK) {
					String path = dialog.getLocation();
					File newDirectory = new File(path);
					fElements.add(newDirectory);
					if (selectionIndex > -1) {
						fTableViewer.replace(newDirectory, selectionIndex);
						fElements.remove(selection);
					} else
						fTableViewer.add(newDirectory);
					setPageComplete(true);
				}
			}
		});
	}
	
	public File[] getLocations() {
		return (File[]) fElements.toArray(new File[fElements.size()]);
	}
	
	protected void updateButtons() {
		int num = fTableViewer.getTable().getSelectionCount();
		fEditButton.setEnabled(num == 1);
		fRemoveButton.setEnabled(num > 0);
	}

}
