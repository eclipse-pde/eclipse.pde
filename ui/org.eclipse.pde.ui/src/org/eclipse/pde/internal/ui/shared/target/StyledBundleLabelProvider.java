/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.util.Collection;
import java.util.Iterator;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IProvidedCapability;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.P2Utils;
import org.eclipse.pde.internal.core.target.*;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.NameVersionDescriptor;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Provides labels for resolved bundles, bundle information objects, and bundle containers.
 * 
 * @since 3.5
 */
public class StyledBundleLabelProvider extends StyledCellLabelProvider implements ILabelProvider {

	private boolean fShowVersion = true;
	private boolean fAppendResolvedVariables = false;

	/**
	 * Creates a label provider.
	 * 
	 * @param showVersion whether version information should be shown in labels
	 * @param appendResolvedVariables whether locations with variables should be shown
	 *  with variables resolved, in addition to unresolved
	 */
	public StyledBundleLabelProvider(boolean showVersion, boolean appendResolvedVariables) {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fShowVersion = showVersion;
		fAppendResolvedVariables = appendResolvedVariables;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StyledCellLabelProvider#dispose()
	 */
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.StyledCellLabelProvider#update(org.eclipse.jface.viewers.ViewerCell)
	 */
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		StyledString styledString = getStyledString(element);
		cell.setText(styledString.toString());
		cell.setStyleRanges(styledString.getStyleRanges());
		cell.setImage(getImage(element));
		super.update(cell);
	}

	private StyledString getStyledString(Object element) {
		StyledString styledString = new StyledString();
		if (element instanceof BundleInfo) {
			appendBundleInfo(styledString, ((BundleInfo) element).getSymbolicName(), ((BundleInfo) element).getVersion());
		} else if (element instanceof NameVersionDescriptor) {
			appendBundleInfo(styledString, ((NameVersionDescriptor) element).getId(), ((NameVersionDescriptor) element).getVersion());
		} else if (element instanceof InstallableUnitWrapper) {
			return getStyledString(((InstallableUnitWrapper) element).getBestUnit());
		} else if (element instanceof IStatus) {
			styledString.append(((IStatus) element).getMessage());
		} else if (element instanceof IPath) {
			styledString.append(((IPath) element).removeFirstSegments(1).toString());
		} else if (element instanceof FeatureBundleContainer) {
			FeatureBundleContainer container = (FeatureBundleContainer) element;

			styledString.append(container.getFeatureId());
			String version = container.getFeatureVersion();
			if (version != null) {
				styledString.append(' ');
				styledString.append('[', StyledString.QUALIFIER_STYLER);
				styledString.append(version, StyledString.QUALIFIER_STYLER);
				styledString.append(']', StyledString.QUALIFIER_STYLER);
			}
			try {
				String location = container.getLocation(false);
				styledString.append(' ').append(location);
			} catch (CoreException e) {
			}
		} else if (element instanceof DirectoryBundleContainer) {
			DirectoryBundleContainer container = (DirectoryBundleContainer) element;
			appendLocation(styledString, container);
		} else if (element instanceof ProfileBundleContainer) {
			ProfileBundleContainer container = (ProfileBundleContainer) element;
			appendLocation(styledString, container);
		} else if (element instanceof IUBundleContainer) {
			styledString.append("Repository Container");
		} else if (element instanceof IInstallableUnit) {
			IInstallableUnit iu = (IInstallableUnit) element;
			String name = iu.getProperty(IInstallableUnit.PROP_NAME);
			if (name == null || name.startsWith("%")) { //$NON-NLS-1$
				name = iu.getId();
			}
			styledString.append(name);
			styledString.append(' ');
			styledString.append(iu.getVersion().toString(), StyledString.QUALIFIER_STYLER);
		} else if (element instanceof String) {
			styledString.append((String) element);
		} else {
			styledString.append(element.toString());
		}
		return styledString;
	}

	/**
	 * Generates a styled string for a bundle information object.
	 * 
	 * @param styledString string to append to
	 * @param info element to append
	 */
	private void appendBundleInfo(StyledString styledString, String id, String version) {
		styledString.append(id);
		if (fShowVersion) {
			if (version != null) {
				styledString.append(' ');
				styledString.append('(', StyledString.QUALIFIER_STYLER);
				styledString.append(version, StyledString.QUALIFIER_STYLER);
				styledString.append(')', StyledString.QUALIFIER_STYLER);
			}
		}
	}

	/**
	 * Appends the container location to the string.
	 * 
	 * @param styledString label to append to
	 * @param container container to append
	 * @param resolved whether to resolve the location
	 */
	private void appendLocation(StyledString styledString, AbstractLocalBundleContainer container) {
		try {
			String location = container.getLocation(false);
			styledString.append(location);
			if (fAppendResolvedVariables) {
				String resolveLocation = container.getLocation(true);
				if (!location.equals(resolveLocation)) {
					styledString.append(' ').append(resolveLocation, StyledString.DECORATIONS_STYLER);
				}
			}

		} catch (CoreException e) {
		}
	}

	/**
	 * Returns an image for the given object or <code>null</code> if none.
	 * 
	 * @param element 
	 * @return image or <code>null</code>
	 */
	public Image getImage(Object element) {
		if (element instanceof BundleInfo) {
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ);
		} else if (element instanceof InstallableUnitWrapper) {
			return getImage(((InstallableUnitWrapper) element).getBestUnit());
		} else if (element instanceof NameVersionDescriptor) {
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ);
		} else if (element instanceof IStatus) {
			int severity = ((IStatus) element).getSeverity();
			if (severity == IStatus.WARNING || severity == IStatus.INFO) {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
			} else if (severity == IStatus.ERROR) {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
			}
		} else if (element instanceof IPath) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
		} else if (element instanceof IBundleContainer) {
			if (element instanceof FeatureBundleContainer) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ);
			} else if (element instanceof DirectoryBundleContainer) {
				ImageDescriptor image = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
				return PDEPlugin.getDefault().getLabelProvider().get(image);
			} else if (element instanceof ProfileBundleContainer) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PRODUCT_DEFINITION);
			} else if (element instanceof IUBundleContainer) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_REPOSITORY_OBJ);
			}
		} else if (element instanceof IInstallableUnit) {
			IInstallableUnit unit = (IInstallableUnit) element;

			boolean isBundle = false;
			boolean isFragment = false;
			boolean isSource = false;

			// Check for bundle property
			// There are utility methods on P2Utils for this, but save time by doing a single loop
			Collection provided = unit.getProvidedCapabilities();
			for (Iterator iterator = provided.iterator(); iterator.hasNext();) {
				IProvidedCapability currentProvided = (IProvidedCapability) iterator.next();
				if (currentProvided.getNamespace().equals(P2Utils.NAMESPACE_ECLIPSE_TYPE)) {
					if (currentProvided.getName().equals(P2Utils.TYPE_ECLIPSE_SOURCE)) {
						isSource = true;
					}
				}
				if (currentProvided.getNamespace().equals(P2Utils.CAPABILITY_NS_OSGI_BUNDLE)) {
					isBundle = true;
				}
				if (currentProvided.getNamespace().equals(P2Utils.CAPABILITY_NS_OSGI_FRAGMENT)) {
					isFragment = true;
					break; // Fragments will only ever get a fragment icon
				}
			}
			// Order of these tests matter, as fragments and source are also bundles
			if (isFragment) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FRAGMENT_OBJ);
			}
			if (isSource) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_MF_OBJ);
			}
			if (isBundle) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ);
			}
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		StyledString string = getStyledString(element);
		return string.getString();
	}
}
