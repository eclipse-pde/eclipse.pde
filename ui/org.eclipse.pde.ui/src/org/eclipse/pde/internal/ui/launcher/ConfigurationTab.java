package org.eclipse.pde.internal.ui.launcher;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 * @author melhem
 *
 */
public class ConfigurationTab extends AbstractLauncherTab implements ILauncherSettings {
	private Image fImage;
	private ArrayList fPluginList = new ArrayList();
	private Button fUpButton;
	private Button fDownButton;
	private Button fAddButton;
	private Button fRemoveButton;
	private Button fUseDefault;
	private Button fClearConfig;
	private TableViewer fTableViewer;
	
	class ContentProvider extends DefaultTableProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ArrayList) {
				return ((ArrayList)inputElement).toArray();
			}
			return new Object[0];
		}
		
	}
	
	public ConfigurationTab() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fImage = PDEPluginImages.DESC_PLUGIN_CONFIG_OBJ.createImage();
	}
	
	private void initializeDefaultPlugins() {
		fPluginList.clear();
		String[] plugins = new String[]{"org.eclipse.osgi.services",
				"org.eclipse.osgi.util", "org.eclipse.core.runtime",
				"org.eclipse.update.configurator"};
		for (int i = 0; i < plugins.length; i++) {
			IPluginModelBase model = getPlugin(plugins[i]);
			if (model != null) {
				fPluginList.add(model);
			}
		}
	}
	
	private void initializePlugins(String selected) {
		fPluginList.clear();
		StringTokenizer tokenizer = new StringTokenizer(selected, ",");
		while (tokenizer.hasMoreTokens()) {
			IPluginModelBase model = getPlugin(tokenizer.nextToken());
			if (model != null)
				fPluginList.add(model);
		}		
	}
	
	private IPluginModelBase getPlugin(String id) {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		ModelEntry entry = manager.findEntry(id);
		return (entry == null) ? null : entry.getActiveModel();		
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createStartingSpace(container, 2);
		
		Label label = new Label(container, SWT.NONE);
		label.setText(PDEPlugin.getResourceString("ConfigurationTab.listLabel"));
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);
		
		createViewer(container);
		createButtonContainer(container);
		
		createStartingSpace(container, 2);
		
		fUseDefault = new Button(container, SWT.CHECK);
		fUseDefault.setText(PDEPlugin.getResourceString("ConfigurationTab.defaultList"));
		gd = new GridData();
		gd.horizontalSpan = 2;
		fUseDefault.setLayoutData(gd);
		fUseDefault.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fUseDefault.getSelection()) {
					initializeDefaultPlugins();
					fTableViewer.refresh();
				}
				enableButtons(!fUseDefault.getSelection());
				updateLaunchConfigurationDialog();
			}
		});
		
		fClearConfig = new Button(container, SWT.CHECK);
		fClearConfig.setText(PDEPlugin.getResourceString("ConfigurationTab.clearArea"));
		gd = new GridData();
		gd.horizontalSpan = 2;
		fClearConfig.setLayoutData(gd);
		fClearConfig.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
		Dialog.applyDialogFont(container);
		setControl(container);
	}
	
	private void createViewer(Composite container) {
		fTableViewer = new TableViewer(container, SWT.BORDER);
		fTableViewer.setContentProvider(new ContentProvider());
		fTableViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fTableViewer.setInput(fPluginList);
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				enableButtons(!fUseDefault.getSelection());
			}
		});
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 100;
		gd.widthHint = 250;
		fTableViewer.getControl().setLayoutData(gd);
	}
	
	private void createButtonContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		
		fUpButton = new Button(container, SWT.PUSH);
		fUpButton.setText(PDEPlugin.getResourceString("ConfigurationTab.up"));
		fUpButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(fUpButton);
		fUpButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index = fTableViewer.getTable().getSelectionIndex();
				Object object = fPluginList.remove(index);
				fPluginList.add(index - 1, object);
				fTableViewer.refresh();
				fTableViewer.setSelection(new StructuredSelection(object));
				updateLaunchConfigurationDialog();
			}
		});
		
		fDownButton = new Button(container, SWT.PUSH);
		fDownButton.setText(PDEPlugin.getResourceString("ConfigurationTab.down"));
		fDownButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(fDownButton);
		fDownButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int index = fTableViewer.getTable().getSelectionIndex();
				Object object = fPluginList.remove(index);
				fPluginList.add(index + 1, object);
				fTableViewer.refresh();
				fTableViewer.setSelection(new StructuredSelection(object));
				updateLaunchConfigurationDialog();
			}
		});
		
		fAddButton = new Button(container, SWT.PUSH);
		fAddButton.setText(PDEPlugin.getResourceString("ConfigurationTab.add"));
		fAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(fAddButton);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ArrayList result = new ArrayList();
				IPluginModelBase[] models = PDECore.getDefault().getModelManager().getPluginsOnly();
				for (int i = 0; i < models.length; i++) {
					if (!fPluginList.contains(models[i]))
						result.add(models[i]);
				}
				PluginSelectionDialog dialog = new PluginSelectionDialog(getShell(), (IPluginModelBase[])result.toArray(new IPluginModelBase[result.size()]), true);
				if (dialog.open() == PluginSelectionDialog.OK) {
					Object[] selected = dialog.getResult();
					int index = fTableViewer.getTable().getSelectionIndex();
					index = (index == -1) ? fPluginList.size() : index + 1;
					for (int i = 0; i < selected.length; i++) {
						fPluginList.add(index + i, selected[i]);
					}
					fTableViewer.refresh();
					fTableViewer.setSelection(new StructuredSelection(selected[selected.length - 1]));
					updateLaunchConfigurationDialog();
				}
			}
		});
		
		fRemoveButton = new Button(container, SWT.PUSH);
		fRemoveButton.setText(PDEPlugin.getResourceString("ConfigurationTab.remove"));
		fRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(fRemoveButton);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			int index = fTableViewer.getTable().getSelectionIndex();
			fPluginList.remove(index);
			fTableViewer.refresh();
			if (index > fPluginList.size() - 1)
				index = fPluginList.size() - 1;
			if (index > -1)
				fTableViewer.setSelection(new StructuredSelection(fPluginList.get(index)));
			updateLaunchConfigurationDialog();
		}});
		
		setControl(parent);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#dispose()
	 */
	public void dispose() {
		fImage.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(CONFIG_USE_DEFAULT, true);
		configuration.setAttribute(CONFIG_CLEAR, true);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			boolean useDefault = configuration.getAttribute(CONFIG_USE_DEFAULT, true);
			if (useDefault)
				initializeDefaultPlugins();
			else
				initializePlugins(configuration.getAttribute(CONFIG_AUTO_START, ""));
			fUseDefault.setSelection(useDefault);
			enableButtons(!useDefault);
			fClearConfig.setSelection(configuration.getAttribute(CONFIG_CLEAR, true));
		} catch (CoreException e) {
		}
		fTableViewer.setInput(fPluginList);
	}
	
	private void enableButtons(boolean enabled) {
		ISelection selection = fTableViewer.getSelection();
		boolean selected = selection != null && !selection.isEmpty();
		fUpButton.setEnabled(enabled && selected && !fTableViewer.getTable().isSelected(0));
		fDownButton.setEnabled(enabled && selected && !fTableViewer.getTable().isSelected(fPluginList.size()-1));
		fAddButton.setEnabled(enabled);
		fRemoveButton.setEnabled(selected && enabled);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(CONFIG_CLEAR, fClearConfig.getSelection());
		configuration.setAttribute(CONFIG_USE_DEFAULT, fUseDefault.getSelection());
		if (!fUseDefault.getSelection()) {
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < fPluginList.size(); i++) {
				IPluginModelBase model = (IPluginModelBase)fPluginList.get(i);
				buffer.append(model.getPluginBase().getId());
				if (i < fPluginList.size() - 1)
					buffer.append(',');
			}
			configuration.setAttribute(CONFIG_AUTO_START, buffer.toString());
		} else {
			configuration.setAttribute(CONFIG_AUTO_START, (String)null);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return PDEPlugin.getResourceString("ConfigurationTab.name");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return fImage;
	}
}
