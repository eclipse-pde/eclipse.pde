/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.build.BuildObject;
import org.eclipse.pde.internal.core.build.IBuildObject;
import org.eclipse.pde.internal.core.bundle.*;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.PluginAttribute;
import org.eclipse.pde.internal.core.text.bundle.*;
import org.eclipse.pde.internal.core.text.plugin.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ModelUndoManager;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.osgi.framework.Constants;

public class PluginUndoManager extends ModelUndoManager {

	public PluginUndoManager(PDEFormEditor editor) {
		super(editor);
		setUndoLevelLimit(30);
	}

	@Override
	protected String getPageId(Object obj) {
		if (obj instanceof IPluginBase)
			return OverviewPage.PAGE_ID;
		if (obj instanceof IPluginImport)
			return DependenciesPage.PAGE_ID;
		if (obj instanceof IPluginLibrary || (obj instanceof IPluginElement && ((IPluginElement) obj).getParent() instanceof IPluginLibrary))
			return RuntimePage.PAGE_ID;
		if (obj instanceof IPluginExtension || (obj instanceof IPluginElement && ((IPluginElement) obj).getParent() instanceof IPluginParent) || obj instanceof IPluginAttribute)
			return ExtensionsPage.PAGE_ID;
		if (obj instanceof IPluginExtensionPoint)
			return ExtensionPointsPage.PAGE_ID;
		return null;
	}

	@Override
	protected void execute(IModelChangedEvent event, boolean undo) {
		Object[] elements = event.getChangedObjects();
		int type = event.getChangeType();
		String propertyName = event.getChangedProperty();
		IModelChangeProvider model = event.getChangeProvider();

		switch (type) {
			case IModelChangedEvent.INSERT :
				if (undo)
					executeRemove(model, elements);
				else
					executeAdd(model, elements);
				break;
			case IModelChangedEvent.REMOVE :
				if (undo)
					executeAdd(model, elements);
				else
					executeRemove(model, elements);
				break;
			case IModelChangedEvent.CHANGE :
				if (event instanceof AttributeChangedEvent) {
					executeAttributeChange((AttributeChangedEvent) event, undo);
				} else {
					if (undo)
						executeChange(elements[0], propertyName, event.getNewValue(), event.getOldValue());
					else
						executeChange(elements[0], propertyName, event.getOldValue(), event.getNewValue());
				}
		}
	}

	private void executeAdd(IModelChangeProvider model, Object[] elements) {
		IPluginBase pluginBase = null;
		IBuild build = null;
		IBundleModel bundleModel = null;
		if (model instanceof IPluginModelBase) {
			pluginBase = ((IPluginModelBase) model).getPluginBase();
		} else if (model instanceof IBuildModel) {
			build = ((IBuildModel) model).getBuild();
		} else if (model instanceof IBundleModel) {
			bundleModel = (IBundleModel) model;
		}

		try {
			for (Object element : elements) {
				if (element instanceof IPluginImport) {
					pluginBase.add((IPluginImport) element);
				} else if (element instanceof IPluginLibrary) {
					pluginBase.add((IPluginLibrary) element);
				} else if (element instanceof IPluginExtensionPoint) {
					pluginBase.add((IPluginExtensionPoint) element);
				} else if (element instanceof IPluginExtension) {
					pluginBase.add((IPluginExtension) element);
				} else if (element instanceof IPluginElement) {
					IPluginElement e = (IPluginElement) element;
					Object parent = e.getParent();
					if (parent instanceof PluginLibraryNode && e instanceof PluginElementNode) {
						((PluginLibraryNode) parent).addContentFilter((PluginElementNode) e);
					} else if (parent instanceof IPluginParent) {
						((IPluginParent) parent).add(e);
					}
				} else if (element instanceof IBuildEntry) {
					IBuildEntry e = (IBuildEntry) element;
					build.add(e);
				} else if (element instanceof BundleObject) {
					if (element instanceof ImportPackageObject) {
						IManifestHeader header = bundleModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
						if (header != null && header instanceof ImportPackageHeader) {
							((ImportPackageHeader) header).addPackage((PackageObject) element);
						}
					}
					if (element instanceof RequireBundleObject) {
						IBaseModel aggModel = getEditor().getAggregateModel();
						if (aggModel instanceof BundlePluginModel) {
							BundlePluginModel pluginModel = (BundlePluginModel) aggModel;
							RequireBundleObject requireBundle = (RequireBundleObject) element;
							pluginBase = pluginModel.getPluginBase();
							String elementValue = requireBundle.getValue();
							IPluginImport importNode = null;
							if (pluginModel.getPluginFactory() instanceof BundlePluginModelBase)
								importNode = ((BundlePluginModelBase) pluginModel.getPluginFactory()).createImport(elementValue);
							String version = ((RequireBundleObject) element).getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
							IManifestHeader header = bundleModel.getBundle().getManifestHeader(Constants.REQUIRE_BUNDLE);
							int bundleManifestVersion = BundlePluginBase.getBundleManifestVersion(((RequireBundleHeader) header).getBundle());
							boolean option = (bundleManifestVersion > 1) ? Constants.RESOLUTION_OPTIONAL.equals(requireBundle.getDirective(Constants.RESOLUTION_DIRECTIVE)) : "true".equals(requireBundle.getAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE)); //$NON-NLS-1$;
							boolean exported = (bundleManifestVersion > 1) ? Constants.VISIBILITY_REEXPORT.equals(requireBundle.getDirective(Constants.VISIBILITY_DIRECTIVE)) : "true".equals(requireBundle.getAttribute(ICoreConstants.REPROVIDE_ATTRIBUTE)); //$NON-NLS-1$;
							if (importNode != null) {
								importNode.setVersion(version);
								importNode.setOptional(option);
								importNode.setReexported(exported);
							}
							if (pluginBase instanceof BundlePluginBase && importNode != null)
								((BundlePluginBase) pluginBase).add(importNode);
						}
					}
					if (element instanceof ExportPackageObject) {
						IManifestHeader header = bundleModel.getBundle().getManifestHeader(Constants.EXPORT_PACKAGE);
						if (header != null && header instanceof ExportPackageHeader) {
							((ExportPackageHeader) header).addPackage((PackageObject) element);
						}
					}
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void executeRemove(IModelChangeProvider model, Object[] elements) {
		IPluginBase pluginBase = null;
		IBuild build = null;
		IBundleModel bundleModel = null;
		if (model instanceof IPluginModelBase) {
			pluginBase = ((IPluginModelBase) model).getPluginBase();
		} else if (model instanceof IBuildModel) {
			build = ((IBuildModel) model).getBuild();
		} else if (model instanceof IBundleModel) {
			bundleModel = (IBundleModel) model;
		}

		try {
			for (Object element : elements) {
				if (element instanceof IPluginImport) {
					pluginBase.remove((IPluginImport) element);
				} else if (element instanceof IPluginLibrary) {
					pluginBase.remove((IPluginLibrary) element);
				} else if (element instanceof IPluginExtensionPoint) {
					pluginBase.remove((IPluginExtensionPoint) element);
				} else if (element instanceof IPluginExtension) {
					pluginBase.remove((IPluginExtension) element);
				} else if (element instanceof IPluginElement) {
					IPluginElement e = (IPluginElement) element;
					Object parent = e.getParent();
					if (parent instanceof PluginLibraryNode && e instanceof PluginElementNode) {
						((PluginLibraryNode) parent).removeContentFilter((PluginElementNode) e);
					} else if (parent instanceof IPluginParent) {
						((IPluginParent) parent).remove(e);
					}
				} else if (element instanceof IBuildEntry) {
					IBuildEntry e = (IBuildEntry) element;
					build.remove(e);
				} else if (element instanceof BundleObject) {
					if (element instanceof ImportPackageObject) {
						IManifestHeader header = bundleModel.getBundle().getManifestHeader(Constants.IMPORT_PACKAGE);
						if (header != null && header instanceof ImportPackageHeader) {
							((ImportPackageHeader) header).removePackage((PackageObject) element);
						}
					}
					if (element instanceof RequireBundleObject) {
						IBaseModel aggModel = getEditor().getAggregateModel();
						if (aggModel instanceof BundlePluginModel) {
							BundlePluginModel mod = (BundlePluginModel) aggModel;
							pluginBase = mod.getPluginBase();
							IPluginImport[] imports = pluginBase.getImports();
							IPluginImport currentImport = null;
							for (IPluginImport pluginImport : imports) {
								String elementValue = ((RequireBundleObject) element).getValue();
								if (pluginImport.getId().equals(elementValue)) {
									currentImport = pluginImport;
									break;
								}
							}
							IPluginImport[] plugins = {currentImport};
							if (pluginBase instanceof BundlePluginBase && currentImport != null)
								((BundlePluginBase) pluginBase).remove(plugins);
						}
					}
					if (element instanceof ExportPackageObject) {
						IManifestHeader header = bundleModel.getBundle().getManifestHeader(Constants.EXPORT_PACKAGE);
						if (header != null && header instanceof ExportPackageHeader) {
							((ExportPackageHeader) header).removePackage((PackageObject) element);
						}
					}
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void executeAttributeChange(AttributeChangedEvent e, boolean undo) {
		PluginElement element = (PluginElement) e.getChangedObjects()[0];
		PluginAttribute att = (PluginAttribute) e.getChangedAttribute();
		Object oldValue = e.getOldValue();
		Object newValue = e.getNewValue();
		try {
			if (undo)
				element.setAttribute(att.getName(), oldValue.toString());
			else
				element.setAttribute(att.getName(), newValue.toString());
		} catch (CoreException ex) {
			PDEPlugin.logException(ex);
		}
	}

	private void executeChange(Object element, String propertyName, Object oldValue, Object newValue) {
		if (element instanceof PluginObject) {
			PluginObject pobj = (PluginObject) element;
			try {
				pobj.restoreProperty(propertyName, oldValue, newValue);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		} else if (element instanceof BuildObject) {
			BuildObject bobj = (BuildObject) element;
			try {
				bobj.restoreProperty(propertyName, oldValue, newValue);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		} else if (element instanceof PluginObjectNode) {
			PluginObjectNode node = (PluginObjectNode) element;
			String newString = newValue != null ? newValue.toString() : null;
			node.setXMLAttribute(propertyName, newString);
		} else if (element instanceof BundleObject) {
			if (element instanceof ImportPackageObject) {
				ImportPackageObject ipObj = (ImportPackageObject) element;
				ipObj.restoreProperty(propertyName, oldValue, newValue);
			}
		}
	}

	@Override
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.CHANGE) {
			Object changedObject = event.getChangedObjects()[0];
			if (changedObject instanceof IPluginObject) {
				IPluginObject obj = (IPluginObject) event.getChangedObjects()[0];
				//Ignore events from objects that are not yet in the model.
				if (!(obj instanceof IPluginBase) && obj.isInTheModel() == false)
					return;
			}
			if (changedObject instanceof IBuildObject) {
				IBuildObject obj = (IBuildObject) event.getChangedObjects()[0];
				//Ignore events from objects that are not yet in the model.
				if (obj.isInTheModel() == false)
					return;
			}
		}
		super.modelChanged(event);
	}
}
