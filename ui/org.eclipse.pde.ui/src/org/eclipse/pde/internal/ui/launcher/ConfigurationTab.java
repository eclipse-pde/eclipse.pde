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
	private Button fAddButton;
	private Button fRemoveButton;
	private Button fUseDefault;
	private Button fClearConfig;
	private TableViewer fTableViewer;
	
	class SelectionDialog extends PluginSelectionDialog {
		
		private Text startLevelText;
		private int startLevel = -1;
		
		public SelectionDialog(Shell parentShell, IPluginModelBase[] models, boolean multipleSelection) {
			super(parentShell, models, multipleSelection);
		}
	
		protected Control createDialogArea(Composite parent) {			
			Composite area = (Composite)super.createDialogArea(parent);
			
			Composite container = new Composite(area, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginHeight = layout.marginWidth = 0;
			container.setLayout(layout);
			container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
			Label label = new Label(container, SWT.NONE);
			label.setText(PDEPlugin.getResourceString("ConfigurationTab.startLevel")); //$NON-NLS-1$
			
			startLevelText = new Text(container, SWT.SINGLE|SWT.BORDER);
			startLevelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));			
			return area;		
		}
		
		public int getStartLevel() {
			return startLevel;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.ui.dialogs.SelectionStatusDialog#okPressed()
		 */
		protected void okPressed() {
			String level = startLevelText.getText().trim();
			if (level.length() > 0) {
				try {
					Integer integer = new Integer(level);
					if (integer.intValue() > 0)
						startLevel = integer.intValue();
				} catch (NumberFormatException e) {
				}
			}
			super.okPressed();
		}

	}
	
	class Entry {
		public IPluginModelBase model;
		public Integer startLevel;
		
		public Entry(IPluginModelBase model, int level) {
			this.model = model;
			startLevel = new Integer(level);
		}
		
		public boolean equals(Object obj) {
			if (obj instanceof Entry)
				return ((Entry)obj).model.getPluginBase().getId().equals(model.getPluginBase().getId());
			return false;
		}
		

	}
	
	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object inputElement) {
			return ((ArrayList)inputElement).toArray();
		}		
	}
	
	class ConfigurationLabelProvider extends LabelProvider implements ITableLabelProvider {
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0 && element instanceof Entry) {
				IPluginModelBase model = ((Entry)element).model;
				return PDEPlugin.getDefault().getLabelProvider().getImage(model);
			}
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof Entry) {
				Entry entry = (Entry)element;
				switch (columnIndex) {
					case 0:
						IPluginBase plugin = entry.model.getPluginBase();
						return plugin.getId() + " (" + plugin.getVersion() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					case 1:
						int start = entry.startLevel.intValue();
						return (start >= 0) ? entry.startLevel.toString() : PDEPlugin.getResourceString("ConfigurationTab.unspecified"); //$NON-NLS-1$
				}
			}
			return null;
		}		
	}
	
	public ConfigurationTab() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fImage = PDEPluginImages.DESC_PLUGIN_CONFIG_OBJ.createImage();
	}
	
	private void initializeDefaultPlugins() {
		fPluginList.clear();
		HashMap map = LauncherUtils.getAutoStartPlugins(true, ""); //$NON-NLS-1$
		map.remove("org.eclipse.osgi"); //$NON-NLS-1$
		Iterator iter = map.keySet().iterator();
		while (iter.hasNext()) {
			Object object = iter.next();
			String id = (String)object.toString().trim();
			IPluginModelBase model = getPlugin(id);
			if (model != null) {
				fPluginList.add(new Entry(model, ((Integer)map.get(object)).intValue()));
			}
		}
	}
	
	private void initializePlugins(String selected) {
		fPluginList.clear();
		StringTokenizer tokenizer = new StringTokenizer(selected, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken().trim();
			String id = token.substring(0,token.indexOf('@'));
			Integer level = new Integer(token.substring(token.indexOf('@') + 1));
			IPluginModelBase model = getPlugin(id);
			if (model != null)
				fPluginList.add(new Entry(model, level.intValue()));
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
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createStartingSpace(container, 1);
		
		Label label = new Label(container, SWT.WRAP);
		label.setText(PDEPlugin.getResourceString("ConfigurationTab.listLabel")); //$NON-NLS-1$
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 300;
		label.setLayoutData(gd);
		
		Composite middle = new Composite(container, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 0;
		middle.setLayout(layout);
		middle.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		createViewer(middle);
		createButtonContainer(middle);
		
		fUseDefault = new Button(container, SWT.CHECK);
		fUseDefault.setText(PDEPlugin.getResourceString("ConfigurationTab.defaultList")); //$NON-NLS-1$
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
		fClearConfig.setText(PDEPlugin.getResourceString("ConfigurationTab.clearArea")); //$NON-NLS-1$
		fClearConfig.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
		Dialog.applyDialogFont(container);
		setControl(container);
	}
	
	private void createViewer(Composite container) {
		Table table = new Table(container, SWT.BORDER|SWT.FULL_SELECTION);
		TableColumn column1 = new TableColumn(table, SWT.NONE);
		column1.setText(PDEPlugin.getResourceString("ConfigurationTab.col1")); //$NON-NLS-1$
		TableColumn column2 = new TableColumn(table, SWT.NONE);
		column2.setText(PDEPlugin.getResourceString("ConfigurationTab.col2")); //$NON-NLS-1$
		table.setHeaderVisible(true);
		
		TableLayout layout = new TableLayout();
		layout.addColumnData(new ColumnWeightData(80));
		layout.addColumnData(new ColumnWeightData(20));
		table.setLayout(layout);
		
		fTableViewer = new TableViewer(table);
		fTableViewer.setContentProvider(new ContentProvider());
		fTableViewer.setLabelProvider(new ConfigurationLabelProvider());
		fTableViewer.setInput(fPluginList);
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				enableButtons(!fUseDefault.getSelection());
			}
		});
		fTableViewer.setSorter(ListUtil.PLUGIN_SORTER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 100;
		gd.widthHint = 300;
		table.setLayoutData(gd);
	}
	
	private void createButtonContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_VERTICAL));
		
		fAddButton = new Button(container, SWT.PUSH);
		fAddButton.setText(PDEPlugin.getResourceString("ConfigurationTab.add")); //$NON-NLS-1$
		fAddButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(fAddButton);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IPluginModelBase[] models = PDECore.getDefault().getModelManager().getPluginsOnly();
				SelectionDialog dialog = new SelectionDialog(getShell(), models, true);
				if (dialog.open() == PluginSelectionDialog.OK) {
					Object[] selected = dialog.getResult();
					for (int i = 0; i < selected.length; i++) {
						fPluginList.add(new Entry((IPluginModelBase)selected[i], dialog.getStartLevel()));
					}
					fTableViewer.refresh();
					updateLaunchConfigurationDialog();
				}
			}
		});
		
		fRemoveButton = new Button(container, SWT.PUSH);
		fRemoveButton.setText(PDEPlugin.getResourceString("ConfigurationTab.remove")); //$NON-NLS-1$
		fRemoveButton.setLayoutData(new GridData());
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
		configuration.setAttribute(CONFIG_CLEAR, false);
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
				initializePlugins(configuration.getAttribute(CONFIG_AUTO_START, "")); //$NON-NLS-1$
			fUseDefault.setSelection(useDefault);
			enableButtons(!useDefault);
			fClearConfig.setSelection(configuration.getAttribute(CONFIG_CLEAR, false));
		} catch (CoreException e) {
		}
		fTableViewer.setInput(fPluginList);
	}
	
	private void enableButtons(boolean enabled) {
		ISelection selection = fTableViewer.getSelection();
		boolean selected = selection != null && !selection.isEmpty();
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
				Entry entry = (Entry)fPluginList.get(i);
				IPluginModelBase model = entry.model;
				buffer.append(model.getPluginBase().getId() + "@" + entry.startLevel); //$NON-NLS-1$
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
		return PDEPlugin.getResourceString("ConfigurationTab.name"); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return fImage;
	}
}
