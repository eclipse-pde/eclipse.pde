package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.builders.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.util.StringMatcher;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.preferences.MainPreferencePage;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.help.WorkbenchHelp;

public class NewDependencyWizardPage extends WizardPage {
	public static final String KEY_TITLE =
		"ManifestEditor.ImportListSection.new.title";
	public static final String KEY_DESC =
		"ManifestEditor.ImportListSection.new.desc";
	public static final String KEY_PLUGINS =
		"ManifestEditor.ImportListSection.new.label";
	public static final String KEY_FILTER =
		"ManifestEditor.ImportList.new.filter";
	public static final String KEY_WORKSPACE_PLUGINS =
		"AdvancedLauncherTab.workspacePlugins";
	public static final String KEY_EXTERNAL_PLUGINS =
		"AdvancedLauncherTab.externalPlugins";
	public static final String KEY_LOOP_WARNING =
		"ManifestEditor.ImportListSection.loopWarning";
	private IPluginModelBase modelBase;
	private CheckboxTreeViewer pluginTreeViewer;
	private Text filterText;
	private Image pluginImage;
	private Image errorPluginImage;
	private Image pluginsImage;
	private NamedElement workspacePlugins;
	private NamedElement externalPlugins;
	private Vector externalList;
	private Vector workspaceList;
	private Vector candidates = new Vector();
	private StringMatcher stringMatcher;

	class PluginLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof IPluginModel) {
				IPluginModel model = (IPluginModel) obj;
				return PDEPlugin.getDefault().getLabelProvider().getObjectText(
					model.getPlugin());
			}
			return obj.toString();
		}
		public Image getImage(Object obj) {
			if (obj instanceof IPluginModel) {
				IPluginModel model = (IPluginModel) obj;
				return PDEPlugin.getDefault().getLabelProvider().getImage(
					model);
			}
			if (obj instanceof NamedElement)
				return ((NamedElement) obj).getImage();
			return null;
		}
	}

	class PluginContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public boolean hasChildren(Object parent) {
			if (parent instanceof IPluginModel)
				return false;
			return true;
		}
		public Object[] getChildren(Object parent) {
			if (parent == externalPlugins) {
				return getExternalPlugins();
			}
			if (parent == workspacePlugins) {
				return getWorkspacePlugins();
			}
			return new Object[0];
		}
		public Object getParent(Object child) {
			if (child instanceof IPluginModel) {
				IPluginModel model = (IPluginModel) child;
				if (model.getUnderlyingResource() != null)
					return workspacePlugins;
				else
					return externalPlugins;
			}
			return null;
		}
		public Object[] getElements(Object input) {
			return new Object[] { workspacePlugins, externalPlugins };
		}
	}

	public NewDependencyWizardPage(IPluginModelBase modelBase) {
		super("newDependencyPage");
		this.modelBase = modelBase;
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		provider.connect(this);
		pluginImage = provider.get(PDEPluginImages.DESC_PLUGIN_OBJ);
		errorPluginImage =
			provider.get(PDEPluginImages.DESC_PLUGIN_OBJ, PDELabelProvider.F_ERROR);
		pluginsImage = provider.get(PDEPluginImages.DESC_REQ_PLUGINS_OBJ);
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
		setPageComplete(false);
		stringMatcher = new StringMatcher("", true, false);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		//layout.numColumns = 2;
		//layout.makeColumnsEqualWidth=true;
		container.setLayout(layout);

		Label label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_FILTER));
		filterText = new Text(container, SWT.SINGLE | SWT.BORDER);
		filterText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				stringMatcher.setPattern(filterText.getText());
				pluginTreeViewer.refresh();
			}
		});
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		filterText.setLayoutData(gd);

		label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_PLUGINS));

		Control c = createPluginList(container);
		gd = new GridData(GridData.FILL_BOTH);
		c.setLayoutData(gd);

		initialize();
		setControl(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.MANIFEST_ADD_DEPENDENCIES);
	}

	protected Control createPluginList(Composite parent) {
		pluginTreeViewer = new CheckboxTreeViewer(parent, SWT.BORDER);
		pluginTreeViewer.setContentProvider(new PluginContentProvider());
		pluginTreeViewer.setLabelProvider(new PluginLabelProvider());
		pluginTreeViewer.setSorter(new ListUtil.PluginSorter() {
			public int category(Object obj) {
				if (obj == workspacePlugins)
					return -1;
				if (obj == externalPlugins)
					return 1;
				return 0;
			}
		});
		pluginTreeViewer.setAutoExpandLevel(2);
		pluginTreeViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer v, Object parent, Object object) {
				if (object instanceof IPluginModel) {
					IPluginModel model = (IPluginModel) object;
					boolean include = model.isEnabled();
					if (include) {
						include = !isOnTheList(model);
					}
					if (include) {
						String prefix = filterText.getText();
						if (prefix.length() > 0) {
							String name;
							if (MainPreferencePage.isFullNameModeEnabled())
								name = model.getPlugin().getName();
							else
								name = model.getPlugin().getId();
							//include = name.startsWith(prefix);
							include = stringMatcher.match(name);
						}
					}
					return include;
				}
				return true;
			}
		});
		pluginTreeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object element = event.getElement();
				if (element instanceof IPluginModel) {
					IPluginModel model = (IPluginModel) event.getElement();
					handleCheckStateChanged(model, event.getChecked());
				} else {
					pluginTreeViewer.setChecked(element, false);
				}
			}
		});
		pluginTreeViewer
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				Object item =
					((IStructuredSelection) e.getSelection()).getFirstElement();
				if (item instanceof IPluginModel)
					pluginSelected((IPluginModel) item);
				else
					pluginSelected(null);
			}
		});
		workspacePlugins =
			new NamedElement(
				PDEPlugin.getResourceString(KEY_WORKSPACE_PLUGINS),
				pluginsImage);
		externalPlugins =
			new NamedElement(
				PDEPlugin.getResourceString(KEY_EXTERNAL_PLUGINS),
				pluginsImage);
		return pluginTreeViewer.getTree();
	}

	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	private boolean isOnTheList(IPluginModel candidate) {
		IPlugin plugin = candidate.getPlugin();

		if (!modelBase.isFragmentModel()) {
			IPlugin thisPlugin = (IPlugin) modelBase.getPluginBase();
			if (plugin.getId().equals(thisPlugin.getId()))
				return true;
		}

		IPluginImport[] imports = modelBase.getPluginBase().getImports();

		for (int i = 0; i < imports.length; i++) {
			IPluginImport iimport = imports[i];
			if (iimport.getId().equals(plugin.getId()))
				return true;
		}
		return false;
	}

	private Object[] getExternalPlugins() {
		return PDECore.getDefault().getExternalModelManager().getModels();
	}

	private Object[] getWorkspacePlugins() {
		return PDECore
			.getDefault()
			.getWorkspaceModelManager()
			.getWorkspacePluginModels();
	}

	public void init(IWorkbench workbench) {
	}

	private void initialize() {
		pluginTreeViewer.setInput(PDEPlugin.getDefault());
		pluginTreeViewer.setGrayed(workspacePlugins, true);
		pluginTreeViewer.setGrayed(externalPlugins, true);
		pluginTreeViewer.reveal(workspacePlugins);
	}

	private void pluginSelected(IPluginModel model) {
	}

	private void handleCheckStateChanged(
		IPluginModel candidate,
		boolean checked) {
		if (checked)
			candidates.add(candidate);
		else
			candidates.remove(candidate);
		setPageComplete(candidates.size() > 0);
		if (candidates.size() > 0) {
			IPlugin[] plugins = new IPlugin[candidates.size()];
			for (int i = 0; i < candidates.size(); i++) {
				plugins[i] = ((IPluginModel) candidates.get(i)).getPlugin();
			}
			if (modelBase instanceof IPluginModel) {
				DependencyLoop[] loops =
					DependencyLoopFinder.findLoops(
						(IPlugin) modelBase.getPluginBase(),
						plugins,
						true);
				if (loops.length > 0) {
					setMessage(
						PDEPlugin.getResourceString(KEY_LOOP_WARNING),
						WARNING);
				} else
					setMessage(null);
			}
		} else
			setMessage(null);
	}

	public boolean finish() {
		IPluginBase pluginBase = modelBase.getPluginBase();
		try {
			for (int i = 0; i < candidates.size(); i++) {
				IPluginModel candidate = (IPluginModel) candidates.get(i);
				IPluginImport importNode =
					modelBase.getFactory().createImport();
				importNode.setId(candidate.getPlugin().getId());
				pluginBase.add(importNode);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
			return false;
		}
		return true;
	}

}