/*
 * Created on Feb 26, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.osgi.bundle.IBundlePluginBase;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.newparts.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class GeneralInfoSection extends PDESection {
	public static final String KEY_MATCH = "ManifestEditor.PluginSpecSection.versionMatch";
	public static final String KEY_MATCH_PERFECT = "ManifestEditor.MatchSection.perfect";
	public static final String KEY_MATCH_EQUIVALENT = "ManifestEditor.MatchSection.equivalent";
	public static final String KEY_MATCH_COMPATIBLE = "ManifestEditor.MatchSection.compatible";
	public static final String KEY_MATCH_GREATER = "ManifestEditor.MatchSection.greater";
	private Button olderVersions;
	private FormEntry idEntry;
	private FormEntry versionEntry;
	private FormEntry nameEntry;
	private FormEntry providerEntry;
	private FormEntry classEntry;
	private FormEntry pluginIdEntry;
	private FormEntry pluginVersionEntry;
	private ComboPart matchCombo;
	/**
	 * @param page
	 * @param parent
	 * @param style
	 */
	public GeneralInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION | Section.EXPANDED
				| Section.TWISTIE);
		createClient(getSection(), page.getEditor().getToolkit());
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEPlugin
				.getResourceString("ManifestEditor.PluginSpecSection.title"));
		toolkit.createCompositeSeparator(section);
		section.setDescription(PDEPlugin
				.getResourceString("ManifestEditor.PluginSpecSection.desc"));
		createEntries(section, toolkit);
	}
	public String getContextId() {
		IPluginBase pluginBase = getPluginBase();
		if (pluginBase instanceof IBundlePluginBase)
			return BundleInputContext.CONTEXT_ID;
		else
			return PluginInputContext.CONTEXT_ID;
	}
	private IPluginBase getPluginBase() {
		IModel model = getPage().getPDEEditor().getAggregateModel();
		return ((IPluginModelBase) model).getPluginBase();
	}
	private void createEntries(Section section, FormToolkit toolkit) {
		IPluginBase pluginBase = getPluginBase();
		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		client.setLayout(layout);
		section.setClient(client);
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite()
				.getActionBars();
		idEntry = new FormEntry(client, toolkit, "Id:", null, false);
		idEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setId(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		idEntry.setEditable(isEditable());
		versionEntry = new FormEntry(client, toolkit, "Version:", null, false);
		versionEntry
				.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
					public void textValueChanged(FormEntry entry) {
						try {
							getPluginBase().setVersion(entry.getValue());
						} catch (CoreException e) {
							PDEPlugin.logException(e);
						}
					}
				});
		versionEntry.setEditable(isEditable());
		nameEntry = new FormEntry(client, toolkit, "Name:", null, false);
		nameEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setName(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		nameEntry.setEditable(isEditable());
		providerEntry = new FormEntry(client, toolkit, "Provider:", null, false);
		providerEntry.setFormEntryListener(new FormEntryAdapter(this,
				actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					getPluginBase().setProviderName(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		providerEntry.setEditable(isEditable());
		if (isFragment()) {
			pluginIdEntry = new FormEntry(client, toolkit, "Plug-in Id:", null,
					true);
			pluginIdEntry.setFormEntryListener(new FormEntryAdapter(this,
					actionBars) {
				public void textValueChanged(FormEntry entry) {
					try {
						((IFragment) getPluginBase()).setPluginId(entry
								.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
				}
				public void linkActivated(HyperlinkEvent e) {
				}
			});
			pluginIdEntry.setEditable(isEditable());
			pluginVersionEntry = new FormEntry(client, toolkit,
					"Plug-in Version:", null, false);
			pluginVersionEntry.setFormEntryListener(new FormEntryAdapter(this,
					actionBars) {
				public void textValueChanged(FormEntry entry) {
					try {
						((IFragment) getPluginBase()).setPluginVersion(entry
								.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
				}
			});
			pluginVersionEntry.setEditable(isEditable());
			toolkit.createLabel(client, PDEPlugin.getResourceString(KEY_MATCH));
			matchCombo = new ComboPart();
			matchCombo.createControl(client, toolkit, SWT.READ_ONLY);
			GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
			gd.horizontalSpan = 2;
			gd.widthHint = 20;
			matchCombo.getControl().setLayoutData(gd);
			String[] items = new String[]{"",
					PDEPlugin.getResourceString(KEY_MATCH_EQUIVALENT),
					PDEPlugin.getResourceString(KEY_MATCH_COMPATIBLE),
					PDEPlugin.getResourceString(KEY_MATCH_PERFECT),
					PDEPlugin.getResourceString(KEY_MATCH_GREATER)};
			matchCombo.setItems(items);
			matchCombo.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent event) {
					int match = matchCombo.getSelectionIndex();
					try {
						((IFragment) getPluginBase()).setRule(match);
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
				}
			});
			matchCombo.getControl().setEnabled(isEditable());
		} else {
			classEntry = new FormEntry(client, toolkit, "Class:", "Browse...",
					true);
			classEntry.setFormEntryListener(new FormEntryAdapter(this,
					actionBars) {
				public void textValueChanged(FormEntry entry) {
				}
				public void linkActivated(HyperlinkEvent e) {
					doOpenClass();
				}
			});
			classEntry.setEditable(isEditable());
		}
		olderVersions = toolkit
				.createButton(
						client,
						PDEPlugin
								.getFormattedMessage(
										"ManifestEditor.PluginSpecSection.isCompatible",
										isFragment()
												? PDEPlugin
														.getResourceString("ManifestEditor.PluginSpecSection.fragment")
												: PDEPlugin
														.getResourceString("ManifestEditor.PluginSpecSection.plugin")),
						SWT.CHECK);
		olderVersions.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
			}
		});
		GridData gd = new GridData();
		gd.horizontalSpan = layout.numColumns;
		olderVersions.setLayoutData(gd);
		toolkit.paintBordersFor(client);
	}
	private boolean isFragment() {
		IPluginModelBase model = (IPluginModelBase) getPage().getPDEEditor()
				.getContextManager().getAggregateModel();
		return model.isFragmentModel();
	}
	public void commit(boolean onSave) {
		idEntry.commit();
		nameEntry.commit();
		providerEntry.commit();
		if (isFragment()) {
			pluginIdEntry.commit();
			pluginVersionEntry.commit();
		} else {
			classEntry.commit();
		}
		//olderVersions.getSelection();
		super.commit(onSave);
	}
	public void refresh() {
		IPluginModelBase model = (IPluginModelBase) getPage().getPDEEditor()
				.getContextManager().getAggregateModel();
		IPluginBase pluginBase = model.getPluginBase();
		idEntry.setValue(pluginBase.getId(), true);
		nameEntry.setValue(pluginBase.getName(), true);
		versionEntry.setValue(pluginBase.getVersion(), true);
		providerEntry.setValue(pluginBase.getProviderName(), true);
		if (isFragment()) {
			IFragment fragment = (IFragment) pluginBase;
			pluginIdEntry.setValue(fragment.getPluginId(), true);
			pluginVersionEntry.setValue(fragment.getPluginVersion(), true);
			matchCombo.select(fragment.getRule());
		} else {
			IPlugin plugin = (IPlugin) pluginBase;
			classEntry.setValue(plugin.getClassName(), true);
		}
		super.refresh();
	}
	private void doOpenClass() {
		String name = classEntry.getText().getText();
		IProject project = getPage().getPDEEditor().getCommonProject();
		IJavaProject javaProject = JavaCore.create(project);
		String path = name.replace('.', '/') + ".java";
		try {
			IJavaElement result = javaProject.findElement(new Path(path));
			if (result != null) {
				JavaUI.openInEditor(result);
			}
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		} catch (JavaModelException e) {
			// nothing
			Display.getCurrent().beep();
		}
	}
}