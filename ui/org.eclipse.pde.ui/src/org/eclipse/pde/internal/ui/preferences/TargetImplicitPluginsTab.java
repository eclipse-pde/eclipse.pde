package org.eclipse.pde.internal.ui.preferences;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.itarget.IImplicitDependenciesInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetPlugin;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public class TargetImplicitPluginsTab {
	
	private TableViewer fElementViewer;
	protected Set fElements;
	
	private Button fAddButton;
	private Button fRemoveButton;
	private Button fRemoveAllButton;
	
	private TargetPlatformPreferencePage fPage;
	
	public TargetImplicitPluginsTab(TargetPlatformPreferencePage page) {
		fPage = page;
	}
	
	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object inputElement) {
			return fElements.toArray();
		}
	}
	
	public Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createLabel(container);
		createTable(container);
		createButtons(container);
		
		return container;
	}
	
	private void createLabel(Composite container) {
		Label label = new Label(container, SWT.NONE);
		label.setText(PDEUIMessages.TargetImplicitPluginsTab_desc);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
	}
	
	private void createTable(Composite container) {
		fElementViewer = new TableViewer(container, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		fElementViewer.getControl().setLayoutData(gd);
		fElementViewer.setContentProvider(new ArrayContentProvider());
		fElementViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fElementViewer.setInput(PDEPlugin.getDefault());
		fElementViewer.setSorter(new ViewerSorter());
		fElementViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}		
		});
		fElementViewer.getTable().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.DEL && e.stateMask == 0) {
					handleRemove();
				}
			}
		});
		loadTable();
	}
	
	protected void loadTable() {
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String value = preferences.getString(ICoreConstants.IMPLICIT_DEPENDENCIES);
		StringTokenizer tokens = new StringTokenizer(value,","); //$NON-NLS-1$
		fElements = new HashSet((4/3) * tokens.countTokens() + 1);
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		while (tokens.hasMoreElements()) {
			IPluginModelBase base = manager.findModel(tokens.nextToken());
			if (base != null) {
				BundleDescription desc = base.getBundleDescription();
				fElements.add(desc);
				fElementViewer.add(desc);
			}
		}
	}
	
	private void createButtons(Composite container) {
		Composite buttonContainer = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		buttonContainer.setLayout(layout);
		buttonContainer.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		
		fAddButton = new Button(buttonContainer, SWT.PUSH);
		fAddButton.setText(PDEUIMessages.SourceBlock_add); 
		fAddButton.setLayoutData(new GridData(GridData.FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fAddButton);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});
		
		fRemoveButton = new Button(buttonContainer, SWT.PUSH);
		fRemoveButton.setText(PDEUIMessages.SourceBlock_remove); 
		fRemoveButton.setLayoutData(new GridData(GridData.FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fRemoveButton);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		
		fRemoveAllButton = new Button(buttonContainer, SWT.PUSH);
		fRemoveAllButton.setText(PDEUIMessages.TargetImplicitPluginsTab_removeAll3);
		fRemoveAllButton.setLayoutData(new GridData(GridData.FILL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fRemoveAllButton);
		fRemoveAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemoveAll();
			}
		});
		if (fElements.size() == 0) {
			fRemoveButton.setEnabled(false);
			fRemoveAllButton.setEnabled(false);
		}
	}
	
	private void updateButtons() {
		boolean empty = fElementViewer.getSelection().isEmpty();
		fRemoveButton.setEnabled(!empty);
		fRemoveAllButton.setEnabled(fElementViewer.getElementAt(0) != null);
	}
	
	private void handleAdd() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(
				PDEPlugin.getActiveWorkbenchShell(), 
				PDEPlugin.getDefault().getLabelProvider());
		
		dialog.setElements(getValidBundles());
		dialog.setTitle(PDEUIMessages.PluginSelectionDialog_title); 
		dialog.setMessage(PDEUIMessages.PluginSelectionDialog_message);
		dialog.setMultipleSelection(true);
		if (dialog.open() == Window.OK) {
			Object[] bundles = dialog.getResult();
			for (int i = 0; i < bundles.length; i++) {
				fElementViewer.add(bundles[i]);
				fElements.add(bundles[i]);
			}
			updateButtons();
		}
	}
	
	protected Object[] getValidBundles() {
		
		Set currentPlugins = new HashSet((4/3) * fElements.size() + 1);
		Iterator it = fElements.iterator();
		while (it.hasNext()) {
			BundleDescription desc = (BundleDescription)it.next();
			currentPlugins.add(desc.getSymbolicName());
		}
		
		IPluginModelBase[] models = fPage.getCurrentModels();
		Set result = new HashSet((4/3) * models.length + 1);
		for (int i = 0; i < models.length; i++) {
			BundleDescription desc = models[i].getBundleDescription();
			if (!currentPlugins.contains(desc.getSymbolicName()))
				result.add(desc);
		}
		return result.toArray();
	}
	
	private void handleRemove() {
		IStructuredSelection ssel = (IStructuredSelection)fElementViewer.getSelection();
		Iterator it = ssel.iterator();
		while (it.hasNext()) {
			Object item = it.next();
			fElements.remove(item);
			fElementViewer.remove(item);
		}
		if (fElements.size() == 0) 
			fRemoveButton.setEnabled(false);
		updateButtons();
	}
	
	private void handleRemoveAll(){
		fElementViewer.remove(fElements.toArray());
		fElements.clear();
		updateButtons();
	}

	public void performDefauls() {
		fElementViewer.remove(fElements.toArray());
		fElements.clear();
		fRemoveButton.setEnabled(false);
	}

	public void performOk() {
		StringBuffer buffer = new StringBuffer();
		Iterator it = fElements.iterator();
		while (it.hasNext()) {
			if (buffer.length() > 0)
				buffer.append(",");
			BundleDescription desc = (BundleDescription) it.next();
			buffer.append(desc.getSymbolicName());
		}
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		preferences.setValue(ICoreConstants.IMPLICIT_DEPENDENCIES, buffer.toString());
	}
	
	public void loadTargetProfile(ITarget target) {
		fElements.clear();
		IImplicitDependenciesInfo info = target.getImplicitPluginsInfo();
		if (info != null) {
			State state = fPage.getCurrentState().getState();
			ITargetPlugin[] plugins = info.getPlugins();
			for (int i = 0; i < plugins.length; i++) {
				BundleDescription desc = state.getBundle(plugins[i].getId(), null);
				if (desc != null)
					fElements.add(desc);
			}
		}
		fElementViewer.refresh();
	}

}
