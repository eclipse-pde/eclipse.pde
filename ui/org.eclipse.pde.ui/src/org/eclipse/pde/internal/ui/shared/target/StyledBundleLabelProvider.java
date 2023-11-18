/*******************************************************************************
 * Copyright (c) 2009, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Christoph Läubrich - Bug 576630 - Target Editor renders custom target locations using Object.toString() in the content tab
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.target.DirectoryBundleContainer;
import org.eclipse.pde.internal.core.target.FeatureBundleContainer;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.ProfileBundleContainer;
import org.eclipse.pde.internal.core.target.TargetDefinition;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.shared.target.IUContentProvider.IUWrapper;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Provides labels for resolved bundles, bundle information objects, and bundle
 * containers.
 *
 * @since 3.5
 */
public class StyledBundleLabelProvider extends StyledCellLabelProvider implements ILabelProvider {

	private boolean fShowVersion = true;
	private boolean fAppendResolvedVariables = false;
	@SuppressWarnings("restriction")
	private final org.eclipse.equinox.internal.p2.metadata.TranslationSupport fTranslations = org.eclipse.equinox.internal.p2.metadata.TranslationSupport
			.getInstance();

	/**
	 * Creates a label provider.
	 *
	 * @param showVersion
	 *            whether version information should be shown in labels
	 * @param appendResolvedVariables
	 *            whether locations with variables should be shown with
	 *            variables resolved, in addition to unresolved
	 */
	public StyledBundleLabelProvider(boolean showVersion, boolean appendResolvedVariables) {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fShowVersion = showVersion;
		fAppendResolvedVariables = appendResolvedVariables;
	}

	@Override
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		StyledString styledString = getStyledString(element);
		cell.setText(styledString.toString());
		cell.setStyleRanges(styledString.getStyleRanges());
		cell.setImage(getImage(element));
		super.update(cell);
	}

	protected StyledString getStyledString(Object element) {
		return Optional
				.ofNullable(Adapters.adapt(element, DelegatingStyledCellLabelProvider.IStyledLabelProvider.class))
				.map(styleProvider -> styleProvider.getStyledText(element)).or(() -> {
					return Optional.ofNullable(Adapters.adapt(element, ILabelProvider.class)).map(provider -> {
						if (provider instanceof StyledBundleLabelProvider bundleLabelProvider) {
							// just in case someone return this class itself...
							return bundleLabelProvider.getInternalStyledString(element);
						}
						String text = provider.getText(element);
						if (text == null) {
							// trigger the default handling
							return null;
						}
						StyledString styledString = new StyledString(text);
						if (element instanceof ITargetLocation location) {
							appendBundleCount(styledString, location);
						}
						return styledString;
					});
				}).orElseGet(() -> getInternalStyledString(element));
	}

	/**
	 * Returns a styled string for some internal objects
	 *
	 * @return the styled string
	 */
	private StyledString getInternalStyledString(Object element) {
		StyledString styledString = new StyledString();
		if (element instanceof BundleInfo info) {
			appendBundleInfo(styledString, info);
		} else if (element instanceof NameVersionDescriptor descriptor) {
			appendBundleInfo(styledString,
					new BundleInfo(descriptor.getId(), descriptor.getVersion(), null, BundleInfo.NO_LEVEL, false));
		} else if (element instanceof TargetBundle bundle) {
			if (bundle.getStatus().isOK()) {
				appendBundleInfo(styledString, bundle.getBundleInfo());
			} else {
				// TODO Better error message for missing bundles
				styledString.append(bundle.getStatus().getMessage());
			}
		} else if (element instanceof IStatus status) {
			styledString.append(status.getMessage());
		} else if (element instanceof IPath path) {
			styledString.append(path.removeFirstSegments(1).toString());
		} else if (element instanceof TargetFeature feature) {
			// Use a bundle info to reuse existing code
			appendBundleInfo(styledString,
					new BundleInfo(feature.getId(), feature.getVersion(), null, BundleInfo.NO_LEVEL, false));
		} else if (element instanceof FeatureBundleContainer container) {
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
		} else if (element instanceof DirectoryBundleContainer container) {
			try {
				styledString.append(container.getLocation(false));
			} catch (CoreException e) {
				// TODO:
			}
			if (fAppendResolvedVariables) {
				appendLocation(styledString, container, true);
			}
			appendBundleCount(styledString, container);
		} else if (element instanceof ProfileBundleContainer container) {
			try {
				styledString.append(container.getLocation(false));
			} catch (CoreException e) {
				// TODO:
			}
			if (fAppendResolvedVariables) {
				appendLocation(styledString, container, true);
			}
			appendBundleCount(styledString, container);
		} else if (element instanceof IUBundleContainer container) {
			List<URI> repos = container.getRepositories();
			styledString.append(repos.isEmpty() ? Messages.BundleContainerTable_8 : repos.get(0).toString());
			appendBundleCount(styledString, container);
		} else if (element instanceof IUWrapper wrapper) {
			styledString = getStyledString(wrapper.iu());
		} else if (element instanceof IInstallableUnit iu) {
			@SuppressWarnings("restriction")
			String name = fTranslations.getIUProperty(iu, IInstallableUnit.PROP_NAME);
			if (name == null) {
				name = iu.getId();
			}
			styledString.append(name);
			styledString.append(' ');
			styledString.append(iu.getVersion().toString(), StyledString.QUALIFIER_STYLER);
		} else {
			styledString.append(String.valueOf(element));
		}
		return styledString;
	}

	/**
	 * Generates a styled string for a bundle information object.
	 *
	 * @param styledString
	 *            string to append to
	 * @param info
	 *            element to append
	 */
	private void appendBundleInfo(StyledString styledString, BundleInfo info) {
		String name = info.getSymbolicName();
		if (name == null) {
			// Try the location instead
			URI location = info.getLocation();
			if (location != null) {
				name = location.toString();
			}
		}
		if (name != null) {
			styledString.append(name);
		}
		if (fShowVersion) {
			String version = info.getVersion();
			if (version != null && !version.equals(BundleInfo.EMPTY_VERSION)) {
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
	 * @param styledString
	 *            label to append to
	 * @param container
	 *            container to append
	 * @param resolved
	 *            whether to resolve the location
	 */
	private void appendLocation(StyledString styledString, ITargetLocation container, boolean resolved) {
		try {
			String location = container.getLocation(resolved);
			styledString.append(" - ", StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
			styledString.append(location, StyledString.DECORATIONS_STYLER);
		} catch (CoreException e) {
		}
	}

	/**
	 * Appends a label describing the number of bundles included (ex. 5 of 10
	 * plug-ins).
	 *
	 * @param styledString
	 *            label to append to
	 * @param container
	 *            bundle container to check for inclusions
	 */
	private void appendBundleCount(StyledString styledString, ITargetLocation container) {
		if (!container.isResolved() || (!container.getStatus().isOK() && !container.getStatus().isMultiStatus())
				|| container.getBundles() == null) {
			return;
		}
		int bundleCount = container.getBundles().length;
		String bundleCountString = Integer.toString(bundleCount);

		styledString.append(' ');
		styledString.append(MessageFormat.format(Messages.BundleContainerTable_10, bundleCountString),
				StyledString.COUNTER_STYLER);
	}

	/**
	 * Returns an image for the given object or <code>null</code> if none.
	 *
	 * @param element
	 *            the element to get an image for
	 * @return image or <code>null</code>
	 */
	@Override
	public Image getImage(Object element) {
		return Optional
				.ofNullable(Adapters.adapt(element, DelegatingStyledCellLabelProvider.IStyledLabelProvider.class))
				.map(styleProvider -> styleProvider.getImage(element)).or(() -> {

					return Optional.ofNullable(Adapters.adapt(element, ILabelProvider.class)).map(provider -> {
						if (provider instanceof StyledBundleLabelProvider styleLabelProvider) {
							// just in case someone return this class itself...
							return styleLabelProvider.getInternalImage(element);
						}
						Image image = provider.getImage(element);
						if (image == null) {
							// trigger the default handling
							return null;
						}
						return image;
					});
				}).orElseGet(() -> getInternalImage(element));
	}

	/**
	 *
	 * @param element
	 *            the element to get an image for
	 * @return an image for some PDE specific objects
	 */
	private Image getInternalImage(Object element) {
		if (element instanceof TargetBundle bundle) {
			int flag = switch (bundle.getStatus().getSeverity()) {
				case IStatus.WARNING, IStatus.INFO -> SharedLabelProvider.F_WARNING;
				case IStatus.ERROR -> SharedLabelProvider.F_ERROR;
				default -> 0;
			};
			if (bundle.getStatus().getSeverity() == IStatus.ERROR
					&& bundle.getStatus().getCode() == TargetBundle.STATUS_FEATURE_DOES_NOT_EXIST) {
				// Missing features are represented by resolved bundles in the
				// tree
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ, flag);
			} else if (bundle.isFragment()) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FRAGMENT_OBJ, flag);
			} else if (bundle.isSourceBundle()) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_MF_OBJ, flag);
			} else {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ, flag);
			}
		} else if (element instanceof BundleInfo) {
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ);
		} else if (element instanceof NameVersionDescriptor descriptor) {
			if (NameVersionDescriptor.TYPE_FEATURE.equals(descriptor.getType())) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ);
			} else if (NameVersionDescriptor.TYPE_PLUGIN.equals(descriptor.getType())) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGIN_OBJ);
			}
		} else if (element instanceof IStatus status) {
			int severity = status.getSeverity();
			if (severity == IStatus.WARNING || severity == IStatus.INFO) {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
			} else if (severity == IStatus.ERROR) {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
			}
		} else if (element instanceof IPath) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
		} else if (element instanceof TargetFeature) {
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ);
		} else if (element instanceof ITargetLocation container) {
			int flag = 0;
			if (container.isResolved()) {
				IStatus status = container.getStatus();
				if (status.getSeverity() == IStatus.WARNING || status.getSeverity() == IStatus.INFO) {
					flag = SharedLabelProvider.F_WARNING;
				} else if (status.getSeverity() == IStatus.ERROR) {
					flag = SharedLabelProvider.F_ERROR;
				}
			} else {
				Collection<List<TargetDefinition>> targetFlags = TargetPlatformHelper.getTargetDefinitionMap().values();
				for (List<TargetDefinition> targetDefinitionValues : targetFlags) {
					if (!targetDefinitionValues.isEmpty()) {
						ITargetLocation[] locs = targetDefinitionValues.get(0).getTargetLocations();
						if (locs != null) {
							for (ITargetLocation loc : locs) {
								if (container.equals(loc)) {
									IStatus status = loc.getStatus();
									if (status == null)
										continue;
									if (status.getSeverity() == IStatus.WARNING
											|| status.getSeverity() == IStatus.INFO) {
										flag = SharedLabelProvider.F_WARNING;
									} else if (status.getSeverity() == IStatus.ERROR) {
										flag = SharedLabelProvider.F_ERROR;
									}
								}
							}
						}
					}
				}
			}
			if (element instanceof FeatureBundleContainer) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ, flag);
			} else if (element instanceof DirectoryBundleContainer) {
				ImageDescriptor image = PlatformUI.getWorkbench().getSharedImages()
						.getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
				return PDEPlugin.getDefault().getLabelProvider().get(image, flag);
			} else if (element instanceof ProfileBundleContainer) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PRODUCT_DEFINITION, flag);
			} else if (element instanceof IUBundleContainer) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_REPOSITORY_OBJ, flag);
			}
		} else if (element instanceof IUWrapper wrapper) {
			return getImage(wrapper.iu());
		} else if (element instanceof IInstallableUnit) {
			return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_NOREF_FEATURE_OBJ);
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		StyledString string = getStyledString(element);
		return string.getString();
	}
}
