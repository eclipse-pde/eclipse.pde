/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import com.ibm.icu.text.MessageFormat;
import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.metadata.TranslationSupport;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.target.*;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
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
	private TranslationSupport fTranslations;

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
		fTranslations = TranslationSupport.getInstance();
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
			appendBundleInfo(styledString, ((BundleInfo) element));
		} else if (element instanceof NameVersionDescriptor) {
			appendBundleInfo(styledString, new BundleInfo(((NameVersionDescriptor) element).getId(), ((NameVersionDescriptor) element).getVersion(), null, BundleInfo.NO_LEVEL, false));
		} else if (element instanceof IResolvedBundle) {
			IResolvedBundle bundle = ((IResolvedBundle) element);
			if (bundle.getStatus().isOK()) {
				appendBundleInfo(styledString, bundle.getBundleInfo());
			} else {
				// TODO Better error message for missing bundles
				styledString.append(bundle.getStatus().getMessage());
			}
		} else if (element instanceof IStatus) {
			styledString.append(((IStatus) element).getMessage());
		} else if (element instanceof IPath) {
			styledString.append(((IPath) element).removeFirstSegments(1).toString());
		} else if (element instanceof IFeatureModel) {
			// Use a bundle info to reuse existing code
			appendBundleInfo(styledString, new BundleInfo(((IFeatureModel) element).getFeature().getId(), ((IFeatureModel) element).getFeature().getVersion(), null, BundleInfo.NO_LEVEL, false));
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
			if (fAppendResolvedVariables) {
				appendLocation(styledString, container, true);
			}
			appendLocation(styledString, container, false);
			appendBundleCount(styledString, container);
		} else if (element instanceof DirectoryBundleContainer) {
			DirectoryBundleContainer container = (DirectoryBundleContainer) element;
			try {
				styledString.append(container.getLocation(false));
			} catch (CoreException e) {
				// TODO:
			}
			if (fAppendResolvedVariables) {
				appendLocation(styledString, container, true);
			}
			appendBundleCount(styledString, container);
		} else if (element instanceof ProfileBundleContainer) {
			ProfileBundleContainer container = (ProfileBundleContainer) element;
			try {
				styledString.append(container.getLocation(false));
			} catch (CoreException e) {
				// TODO:
			}
			if (fAppendResolvedVariables) {
				appendLocation(styledString, container, true);
			}
			appendBundleCount(styledString, container);
		} else if (element instanceof IUBundleContainer) {
			IUBundleContainer container = (IUBundleContainer) element;
			URI[] repos = container.getRepositories();
			if (repos == null || repos.length == 0) {
				styledString.append(Messages.BundleContainerTable_8);
			} else {
				styledString.append(repos[0].toString());
			}
			appendBundleCount(styledString, container);
		} else if (element instanceof IInstallableUnit) {
			IInstallableUnit iu = (IInstallableUnit) element;
			String name = fTranslations.getIUProperty(iu, IInstallableUnit.PROP_NAME);
			if (name == null) {
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
	private void appendBundleInfo(StyledString styledString, BundleInfo info) {
		styledString.append(info.getSymbolicName());
		if (fShowVersion) {
			String version = info.getVersion();
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
	private void appendLocation(StyledString styledString, AbstractBundleContainer container, boolean resolved) {
		try {
			String location = container.getLocation(resolved);
			styledString.append(" - ", StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
			styledString.append(location, StyledString.DECORATIONS_STYLER);
		} catch (CoreException e) {
		}
	}

	/**
	 * Appends a label describing the number of bundles included (ex. 5 of 10 plug-ins).
	 * 
	 * @param styledString label to append to
	 * @param container bundle container to check for inclusions
	 */
	private void appendBundleCount(StyledString styledString, IBundleContainer container) {
		if (!container.isResolved() || (!container.getStatus().isOK() && !container.getStatus().isMultiStatus()) || container.getBundles() == null) {
			return;
		}
		int bundleCount = container.getBundles().length;
		String bundleCountString = Integer.toString(bundleCount);

		styledString.append(' ');
		styledString.append(MessageFormat.format(Messages.BundleContainerTable_10, new String[] {bundleCountString}), StyledString.COUNTER_STYLER);
	}

	/**
	 * Returns an image for the given object or <code>null</code> if none.
	 * 
	 * @param element 
	 * @return image or <code>null</code>
	 */
	public Image getImage(Object element) {
		if (element instanceof IResolvedBundle) {
			IResolvedBundle bundle = (IResolvedBundle) element;
			int flag = 0;
			if (bundle.getStatus().getSeverity() == IStatus.WARNING || bundle.getStatus().getSeverity() == IStatus.INFO) {
				flag = SharedLabelProvider.F_WARNING;
			} else if (bundle.getStatus().getSeverity() == IStatus.ERROR) {
				flag = SharedLabelProvider.F_ERROR;
			}

			if (bundle.isFragment()) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FRAGMENT_OBJ, flag);
			} else if (bundle.isSourceBundle()) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_MF_OBJ, flag);
			} else {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ, flag);
			}
		} else if (element instanceof BundleInfo) {
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ);
		} else if (element instanceof NameVersionDescriptor) {
			if (((NameVersionDescriptor) element).getType() == NameVersionDescriptor.TYPE_FEATURE) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ);
			} else if (((NameVersionDescriptor) element).getType() == NameVersionDescriptor.TYPE_PLUGIN) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ);
			}
		} else if (element instanceof IStatus) {
			int severity = ((IStatus) element).getSeverity();
			if (severity == IStatus.WARNING || severity == IStatus.INFO) {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
			} else if (severity == IStatus.ERROR) {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
			}
		} else if (element instanceof IPath) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
		} else if (element instanceof IFeatureModel) {
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ);
		} else if (element instanceof IBundleContainer) {
			int flag = 0;
			IBundleContainer container = (IBundleContainer) element;
			if (container.isResolved()) {
				IStatus status = container.getStatus();
				if (status.getSeverity() == IStatus.WARNING || status.getSeverity() == IStatus.INFO) {
					flag = SharedLabelProvider.F_WARNING;
				} else if (status.getSeverity() == IStatus.ERROR) {
					flag = SharedLabelProvider.F_ERROR;
				}
			}
			if (element instanceof FeatureBundleContainer) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ, flag);
			} else if (element instanceof DirectoryBundleContainer) {
				ImageDescriptor image = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
				return PDEPlugin.getDefault().getLabelProvider().get(image, flag);
			} else if (element instanceof ProfileBundleContainer) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PRODUCT_DEFINITION, flag);
			} else if (element instanceof IUBundleContainer) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_REPOSITORY_OBJ, flag);
			}
		} else if (element instanceof IInstallableUnit) {
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_NOREF_FEATURE_OBJ);
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
