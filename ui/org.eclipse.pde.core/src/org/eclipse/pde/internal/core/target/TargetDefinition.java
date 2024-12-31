/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
 *     Manumitting Technologies Inc - bug 437726: wrong error messages opening target definition
 *     Lucas Bullen (Red Hat Inc.) - Bug 531602 - [Generic TP Editor] formatting munged by editor
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.ExternalFeatureModelManager;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.SAXException;

/**
 * Target definition implementation.
 *
 * @since 3.5
 */
public class TargetDefinition implements ITargetDefinition {
	// xml document making the targetDefinition
	private Document fDocument;
	private Element fRoot;

	// name and description
	private String fName;

	// included and optional filtering
	private NameVersionDescriptor[] fIncluded;

	// arguments
	private String fProgramArgs;
	private String fVMArgs;

	// environment settings
	private IPath fJREContainer;
	private String fArch;
	private String fOS;
	private String fWS;
	private String fNL;

	// bundle containers
	private ITargetLocation[] fContainers;

	// handle
	private final ITargetHandle fHandle;

	/**
	 * Status generated when this target was resolved, possibly <code>null</code>
	 */
	private IStatus fResolutionStatus;

	// implicit dependencies
	private NameVersionDescriptor[] fImplicit;

	// internal settings for UI mode (how content is displayed to the user
	private int fUIMode = MODE_PLUGIN;
	public static final int MODE_PLUGIN = 0;
	public static final int MODE_FEATURE = 1;

	// cache of features found for a given location, maps a string path location to a array of IFeatureModels (IFeatureModel[])
	private static Map<String, TargetFeature[]> fFeaturesInLocation = new HashMap<>();

	// internal cache for features.  A target managed by features will contain a set of features as well as a set of plug-ins that don't belong to a feature
	private TargetFeature[] fFeatures;
	private TargetBundle[] fOtherBundles;

	/**
	 * Constructs a target definition based on the given handle.
	 */
	TargetDefinition(ITargetHandle handle) {
		fHandle = handle;
		setDocument(createNewDocument());
	}

	private static Document createNewDocument() {
		try {
			@SuppressWarnings("restriction")
			Document doc = org.eclipse.core.internal.runtime.XmlProcessorFactory.newDocumentWithErrorOnDOCTYPE();
			ProcessingInstruction instruction = doc.createProcessingInstruction(
					TargetDefinitionPersistenceHelper.PDE_INSTRUCTION,
					TargetDefinitionPersistenceHelper.ATTR_VERSION + "=\"" + ICoreConstants.TARGET38 + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			doc.appendChild(instruction);
			doc.appendChild(doc.createElement(TargetDefinitionPersistenceHelper.ROOT));
			return doc;
		} catch (ParserConfigurationException e) {
			return null;
		}
	}

	@Override
	public void setDocument(Document document) {
		if (document != null) {
			Element root = document.getDocumentElement();
			if (root != null && root.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.ROOT)) {
				fDocument = document;
				fRoot = root;
			}
			return;
		}
		fDocument = null;
		fRoot = null;
	}

	@Override
	public Document getDocument() {
		return fDocument;
	}

	@Override
	public String getArch() {
		return fArch;
	}

	@Override
	public ITargetLocation[] getTargetLocations() {
		return fContainers;
	}

	@Override
	public String getNL() {
		return fNL;
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public String getOS() {
		return fOS;
	}

	@Override
	public String getProgramArguments() {
		return fProgramArgs;
	}

	@Override
	public String getVMArguments() {
		return fVMArgs;
	}

	@Override
	public String getWS() {
		return fWS;
	}

	@Override
	public void setArch(String arch) {
		fArch = arch;
		if (fRoot != null && arch != null && !arch.isEmpty()) {
			Element archNode = TargetDefinitionDocumentTools.getChildElement(fRoot,
					TargetDefinitionPersistenceHelper.ENVIRONMENT,
					TargetDefinitionPersistenceHelper.ARCH);
			archNode.setTextContent(arch);
		} else {
			removeElement(TargetDefinitionPersistenceHelper.ENVIRONMENT, TargetDefinitionPersistenceHelper.ARCH);
		}
	}

	@Override
	public void setNL(String nl) {
		fNL = nl;
		if (fRoot != null && nl != null && !nl.isEmpty()) {
			Element nlNode = TargetDefinitionDocumentTools.getChildElement(fRoot,
					TargetDefinitionPersistenceHelper.ENVIRONMENT,
					TargetDefinitionPersistenceHelper.NL);
			nlNode.setTextContent(nl);
		} else {
			removeElement(TargetDefinitionPersistenceHelper.ENVIRONMENT, TargetDefinitionPersistenceHelper.NL);
		}
	}

	@Override
	public void setName(String name) {
		fName = name;
		if (fRoot != null) {
			if (name != null && !name.isEmpty()) {
				fRoot.setAttribute(TargetDefinitionPersistenceHelper.ATTR_NAME, name);
			} else {
				fRoot.removeAttribute(TargetDefinitionPersistenceHelper.ATTR_NAME);
			}
		}
	}

	@Override
	public void setOS(String os) {
		fOS = os;
		if (fRoot != null && os != null && !os.isEmpty()) {
			Element nlNode = TargetDefinitionDocumentTools.getChildElement(fRoot,
					TargetDefinitionPersistenceHelper.ENVIRONMENT,
					TargetDefinitionPersistenceHelper.OS);
			nlNode.setTextContent(os);
		} else {
			removeElement(TargetDefinitionPersistenceHelper.ENVIRONMENT, TargetDefinitionPersistenceHelper.OS);
		}
	}

	@Override
	public void setProgramArguments(String args) {
		if (args != null && args.isEmpty()) {
			args = null;
		}
		fProgramArgs = args;
		if (fRoot != null && args != null) {
			Element programArgsNode = TargetDefinitionDocumentTools.getChildElement(fRoot,
					TargetDefinitionPersistenceHelper.ARGUMENTS,
					TargetDefinitionPersistenceHelper.PROGRAM_ARGS);
			programArgsNode.setTextContent(args);
		} else {
			removeElement(TargetDefinitionPersistenceHelper.ARGUMENTS, TargetDefinitionPersistenceHelper.PROGRAM_ARGS);
		}
	}

	@Override
	public void setVMArguments(String args) {
		if (args != null && args.isEmpty()) {
			args = null;
		}
		fVMArgs = args;
		if (fRoot != null && args != null) {
			Element programArgsNode = TargetDefinitionDocumentTools.getChildElement(fRoot,
					TargetDefinitionPersistenceHelper.ARGUMENTS,
					TargetDefinitionPersistenceHelper.VM_ARGS);
			programArgsNode.setTextContent(args);
		} else {
			removeElement(TargetDefinitionPersistenceHelper.ARGUMENTS, TargetDefinitionPersistenceHelper.VM_ARGS);
		}
	}

	@Override
	public void setWS(String ws) {
		fWS = ws;
		if (fRoot != null && ws != null && !ws.isEmpty()) {
			Element nlNode = TargetDefinitionDocumentTools.getChildElement(fRoot,
					TargetDefinitionPersistenceHelper.ENVIRONMENT,
					TargetDefinitionPersistenceHelper.WS);
			nlNode.setTextContent(ws);
		} else {
			removeElement(TargetDefinitionPersistenceHelper.ENVIRONMENT, TargetDefinitionPersistenceHelper.WS);
		}
	}

	@Override
	public void setTargetLocations(ITargetLocation[] locations) {
		// Clear the feature model cache as it is based on the bundle container locations
		fFeatures = null;
		fOtherBundles = null;

		if (locations != null && locations.length == 0) {
			locations = null;
		}

		fContainers = locations;

		if (locations == null) {
			setIncluded(null);
		} else {
			for (ITargetLocation location : locations) {
				if (location instanceof AbstractBundleContainer) {
					((AbstractBundleContainer) location).associateWithTarget(this);
				}
			}
		}
		try {
			if (fRoot != null && locations != null) {
				Element containersElement = TargetDefinitionDocumentTools.getChildElement(fRoot,
						TargetDefinitionPersistenceHelper.LOCATIONS);
				serializeBundleContainers(locations, containersElement);
			} else {
				removeElement(TargetDefinitionPersistenceHelper.LOCATIONS);
			}
		} catch (CoreException | DOMException | SAXException | IOException | ParserConfigurationException e) {
			PDECore.log(e);
		}
	}

	/**
	 * Clears the any models that are cached for the given container location.
	 *
	 * @param location location to clear cache for or <code>null</code> to clear all cached models
	 */
	public void flushCaches(String location) {
		// Clear the feature model cache as it is based on the bundle container locations
		fFeatures = null;
		fOtherBundles = null;
		if (location == null) {
			fFeaturesInLocation.clear();
		} else {
			fFeaturesInLocation.remove(location);
		}
		if (fContainers == null) {
			setIncluded(null);
		}
	}

	@Override
	public IStatus resolve(IProgressMonitor monitor) {
		ITargetLocation[] targetLocations = getTargetLocations();
		if (targetLocations == null || targetLocations.length == 0) {
			// if we do not do anything, we can't do anything wrong...
			return fResolutionStatus = Status.OK_STATUS;
		}
		fResolutionStatus = null;
		SubMonitor subMonitor = SubMonitor.convert(monitor, Messages.TargetDefinition_1, targetLocations.length * 100);
		try {
			MultiStatus status = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.TargetDefinition_2);
			Map<P2TargetUtils, List<ITargetLocation>> synchronizers = new HashMap<>();
			// clear all previous maps
			P2TargetUtils.fgTargetArtifactRepo.clear();
			P2TargetUtils.fgArtifactKeyRepoFile.clear();
			for (ITargetLocation location : targetLocations) {
				subMonitor.checkCanceled();
				subMonitor.subTask(Messages.TargetDefinition_4);
				P2TargetUtils synchronizer = location.getAdapter(P2TargetUtils.class);
				if (synchronizer == null) {
					// a usual target definition location
					IStatus s = location.resolve(this, subMonitor.split(100));
					if (!s.isOK()) {
						status.add(s);
					}
				} else {
					// has to be performed later on in a separate batch
					synchronizers.computeIfAbsent(synchronizer, nil -> new ArrayList<>()).add(location);
				}
			}
			if (!synchronizers.isEmpty()) {
				List<ITargetLocation> delayedLocations = synchronizers.values().stream().flatMap(Collection::stream)
						.toList();
				subMonitor.setWorkRemaining(synchronizers.size() * 100 + delayedLocations.size());
				synchronizers.forEach((synchronizer, locations) -> {
					subMonitor.checkCanceled();
					try {
						synchronizer.synchronize(this, subMonitor.split(100));
						locations.stream().map(ITargetLocation::getStatus).filter(s -> s != null && !s.isOK())
								.forEach(status::add);
					} catch (CoreException e) {
						PDECore.log(e.getStatus());
						status.add(e.getStatus());
					}
				});
				for (ITargetLocation location : delayedLocations) {
					subMonitor.checkCanceled();
					IStatus s = location.resolve(this, subMonitor.split(1));
					if (!s.isOK()) {
						status.add(s);
					}
				}
			}
			if (status.isOK()) {
				return fResolutionStatus = Status.OK_STATUS;
			}
			return fResolutionStatus = status;
		} catch (OperationCanceledException e) {
			return Status.CANCEL_STATUS;
		} finally {
			// keep a list of resolved targets with key as handle
			TargetPlatformHelper.addTargetDefinitionMap(this);
			subMonitor.done();
		}
	}

	@Override
	public boolean isResolved() {
		ITargetLocation[] containers = getTargetLocations();
		if (containers != null) {
			for (ITargetLocation targetLocation : containers) {
				if (!targetLocation.isResolved()) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public IStatus getStatus() {
		if (fResolutionStatus != null && !fResolutionStatus.isOK()) {
			return fResolutionStatus;
		}
		if (isResolved()) {
			ITargetLocation[] containers = getTargetLocations();
			if (containers != null) {
				// Check if the containers have any resolution problems
				MultiStatus result = new MultiStatus(PDECore.PLUGIN_ID, 0, Messages.TargetDefinition_5);
				for (ITargetLocation container : containers) {
					IStatus containerStatus = container.getStatus();
					if (containerStatus != null && !containerStatus.isOK()) {
						result.add(containerStatus);
					}
				}

				// Check if any of the included bundles have problems
				// build status from bundle list
				TargetBundle[] bundles = getBundles();
				for (int i = 0; i < bundles.length; i++) {
					if (!bundles[i].getStatus().isOK()) {
						result.add(bundles[i].getStatus());
					}
				}

				if (result.isOK()) {
					// Return generic ok status instead of problem multi-status with no children
					return Status.OK_STATUS;
				}
				return result;
			}
			return Status.OK_STATUS;
		}
		return null;
	}

	@Override
	public void setIncluded(NameVersionDescriptor[] included) {
		fIncluded = included;
		if (included != null && fRoot != null) {
			Arrays.sort(included, (o1, o2) -> {
				int compareType = o1.getType().compareTo(o2.getType());
				if (compareType != 0) {
					return compareType;
				}
				return o1.getId().compareTo(o2.getId());
			});
			Element includedElement = TargetDefinitionDocumentTools.getChildElement(fRoot,
					TargetDefinitionPersistenceHelper.INCLUDE_BUNDLES);
			serializeBundles(includedElement, included);
		} else {
			removeElement(TargetDefinitionPersistenceHelper.INCLUDE_BUNDLES);
		}
	}

	@Override
	public NameVersionDescriptor[] getIncluded() {
		return fIncluded;
	}

	@Override
	public TargetBundle[] getBundles() {
		return getBundles(false);
	}

	@Override
	public TargetBundle[] getAllBundles() {
		return getBundles(true);
	}

	/**
	 * Gathers and returns all or included bundles in this target or <code>null</code> if
	 * not resolved.
	 *
	 * @param allBundles whether to consider all bundles, or just those included/optional
	 * @return bundles or <code>null</code>
	 */
	private TargetBundle[] getBundles(boolean allBundles) {
		if (isResolved()) {
			ITargetLocation[] containers = getTargetLocations();
			if (containers != null) {
				List<TargetBundle> all = new ArrayList<>();
				for (ITargetLocation container : containers) {
					TargetBundle[] bundles = container.getBundles();
					if (bundles != null) {
						Collections.addAll(all, bundles);
					}
				}

				TargetBundle[] allResolvedBundles = all.toArray(new TargetBundle[all.size()]);
				if (allBundles) {
					return allResolvedBundles;
				}
				return filterBundles(allResolvedBundles, getIncluded());
			}
			return new TargetBundle[0];
		}
		return null;
	}

	private TargetBundle[] filterBundles(TargetBundle[] bundles, NameVersionDescriptor[] filter) {
		if (filter == null) {
			// No filtering to do
			return bundles;
		}
		if (filter.length == 0) {
			return new TargetBundle[0];
		}

		// If there are features, don't set errors for missing bundles as they are caused by missing OS specific fragments
		boolean containsFeatures = false;

		// If there are any included features that are missing, add errors as resolved bundles (the same thing we would do for missing bundles)
		List<NameVersionDescriptor> missingFeatures = new ArrayList<>();

		List<NameVersionDescriptor> included = new ArrayList<>();
		// For feature filters, get the list of included bundles, for bundle filters just add them to the list
		for (NameVersionDescriptor element : filter) {
			if (element.getType() == NameVersionDescriptor.TYPE_PLUGIN) {
				included.add(element);
			} else if (element.getType() == NameVersionDescriptor.TYPE_FEATURE) {
				containsFeatures = true;
				TargetFeature[] features = getAllFeatures();
				TargetFeature bestMatch = null;
				for (TargetFeature feature : features) {
					if (feature.getId().equals(element.getId())) {
						if (element.getVersion() != null) {
							// Try to find an exact feature match
							if (element.getVersion().equals(feature.getVersion())) {
								// Exact match
								bestMatch = feature;
								break;
							}
						} else if (bestMatch != null) {
							// If no version specified take the highest version
							Version v1 = Version.parseVersion(feature.getVersion());
							Version v2 = Version.parseVersion(bestMatch.getVersion());
							if (v1.compareTo(v2) > 0) {
								bestMatch = feature;
							}
						}

						if (bestMatch == null) {
							// If we can't find a version match, just take any name match
							bestMatch = feature;
						}
					}
				}

				// Add the required plugins from the feature to the list of includes
				if (bestMatch != null) {
					NameVersionDescriptor[] plugins = bestMatch.getPlugins();
					Collections.addAll(included, plugins);
				} else {
					missingFeatures.add(element);
				}
			}
		}

		// Return matching bundles, if we are organizing by feature, do not create invalid target bundles for missing bundle includes
		List<TargetBundle> result = getMatchingBundles(bundles, included.toArray(new NameVersionDescriptor[included.size()]), !containsFeatures);

		// Add in missing features as resolved bundles with error statuses
		if (containsFeatures && !missingFeatures.isEmpty()) {
			for (NameVersionDescriptor missing : missingFeatures) {
				BundleInfo info = new BundleInfo(missing.getId(), missing.getVersion(), null, BundleInfo.NO_LEVEL, false);
				String message = NLS.bind(Messages.TargetDefinition_RequiredFeatureCouldNotBeFound, missing.getId());
				Status status = new Status(IStatus.ERROR, PDECore.PLUGIN_ID, TargetBundle.STATUS_FEATURE_DOES_NOT_EXIST, message, null);
				result.add(new InvalidTargetBundle(info, status));
			}
		}

		return result.toArray(new TargetBundle[result.size()]);
	}

	/**
	 * Returns bundles from the specified collection that match the symbolic names
	 * and/or version in the specified criteria. When no version is specified
	 * the newest version (if any) is selected.
	 * <p>
	 * If handleMissingBundles is <code>true</code>, the returned list will contain {@link InvalidTargetBundle}s
	 * for any included filters that do not have a matching bundle in the collection. The invalid bundles
	 * will contain statuses describing what couldn't be matched.
	 * </p>
	 * @param collection bundles to resolve against match criteria
	 * @param included bundles to include or <code>null</code> if no restrictions
	 * @param handleMissingBundles whether to create {@link InvalidTargetBundle}s for missing includes
	 *
	 * @return list of IResolvedBundle bundles that match this container's restrictions
	 */
	static List<TargetBundle> getMatchingBundles(TargetBundle[] collection, NameVersionDescriptor[] included, boolean handleMissingBundles) {
		if (included == null) {
			ArrayList<TargetBundle> result = new ArrayList<>();
			result.addAll(Arrays.asList(collection));
			return result;
		}
		// map bundles names to available versions
		Map<String, List<TargetBundle>> bundleMap = new HashMap<>(collection.length);
		for (TargetBundle resolved : collection) {
			String name = resolved.getBundleInfo().getSymbolicName();
			List<TargetBundle> list = bundleMap.computeIfAbsent(name, n -> new ArrayList<>(3));
			list.add(resolved);
		}
		List<TargetBundle> resolved = new ArrayList<>();

		for (NameVersionDescriptor element : included) {
			BundleInfo info = new BundleInfo(element.getId(), element.getVersion(), null, BundleInfo.NO_LEVEL, false);
			TargetBundle bundle = resolveBundle(bundleMap, info, handleMissingBundles);
			if (bundle != null) {
				resolved.add(bundle);
			}
		}

		return resolved;
	}

	/**
	 * Resolves a bundle for the given info from the given map. The map contains
	 * keys of symbolic names and values are lists of {@link TargetBundle}'s available
	 * that match the names.
	 * <p>
	 * If handleMissingBundles is <code>true</code>, a {@link InvalidTargetBundle} will be created and
	 * returned if the give info does not match up with a map entry. The returned bundle will have
	 * a status giving more details on what is missing. If handleMissingBundles is <code>false</code>,
	 * <code>null</code> will be returned.
	 * </p>
	 *
	 * @param bundleMap available bundles to resolve against
	 * @param info name and version to match against
	 * @param handleMissingBundles whether to return an {@link InvalidTargetBundle} for a info that does not match with a map entry or <code>null</code>
	 * @return resolved bundle or <code>null</code>
	 */
	private static TargetBundle resolveBundle(Map<String, List<TargetBundle>> bundleMap, BundleInfo info, boolean handleMissingBundles) {
		List<TargetBundle> list = bundleMap.get(info.getSymbolicName());
		if (list != null) {
			String version = info.getVersion();
			if (version == null || version.equals(BundleInfo.EMPTY_VERSION)) {
				// select newest
				if (list.size() > 1) {
					// sort the list
					Collections.sort(list, (o1, o2) -> {
						BundleInfo b1 = o1.getBundleInfo();
						BundleInfo b2 = o2.getBundleInfo();
						try {
							Version v1 = Version.create(b1.getVersion());
							Version v2 = Version.create(b2.getVersion());
							return v1.compareTo(v2);
						} catch (IllegalArgumentException e) {
							// If one of the bundles has a bad version
							PDECore.log(e);
							return b1.getVersion().compareTo(b2.getVersion());
						}
					});
				}
				// select the last one
				TargetBundle rb = list.get(list.size() - 1);
				return rb;
			}
			Iterator<TargetBundle> iterator = list.iterator();
			while (iterator.hasNext()) {
				TargetBundle bundle = iterator.next();
				if (bundle.getBundleInfo().getVersion().equals(version)) {
					return bundle;
				}
			}
			// If major, minor and micro components of the version match, return
			// that bundle
			iterator = list.iterator();
			while (iterator.hasNext()) {
				TargetBundle bundle = iterator.next();
				try{
					org.osgi.framework.Version bundleVersion = new org.osgi.framework.Version(bundle.getBundleInfo().getVersion());
					org.osgi.framework.Version infoVersion = new org.osgi.framework.Version(version);
					if (bundleVersion.getMajor() == infoVersion.getMajor()
							&& bundleVersion.getMinor() == infoVersion.getMinor()
							&& bundleVersion.getMicro() == infoVersion.getMicro()) {
						return bundle;
					}
				}
				catch (IllegalArgumentException e) {
					// invalid version, do nothing, check the next bundle.
				}
			}
			// VERSION DOES NOT EXIST
			if (!handleMissingBundles) {
				return null;
			}
			int sev = IStatus.ERROR;
			String message = NLS.bind(Messages.AbstractBundleContainer_1, new Object[] {info.getVersion(), info.getSymbolicName()});
			IStatus status = new Status(sev, PDECore.PLUGIN_ID, TargetBundle.STATUS_VERSION_DOES_NOT_EXIST, message, null);
			return new InvalidTargetBundle(info, status);
		}
		// DOES NOT EXIST
		if (!handleMissingBundles) {
			return null;
		}
		int sev = IStatus.ERROR;
		String message = NLS.bind(Messages.AbstractBundleContainer_3, info.getSymbolicName());
		IStatus status = new Status(sev, PDECore.PLUGIN_ID, TargetBundle.STATUS_PLUGIN_DOES_NOT_EXIST, message, null);
		return new InvalidTargetBundle(info, status);
	}

	@Override
	public ITargetHandle getHandle() {
		return fHandle;
	}

	/**
	 * Build contents from the given stream.
	 *
	 * @param stream
	 *            input stream
	 * @throws CoreException
	 *             if an error occurs
	 */
	void setContents(InputStream stream) throws CoreException {
		try {
			fArch = null;
			fContainers = null;
			fImplicit = null;
			fJREContainer = null;
			fIncluded = null;
			fName = null;
			fNL = null;
			fOS = null;
			fProgramArgs = null;
			fVMArgs = null;
			fWS = null;
			fDocument = null;
			fRoot = null;
			TargetDefinitionPersistenceHelper.initFromXML(this, stream);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			setDocument(createNewDocument());
			abort(Messages.TargetDefinition_0, e);
		}
	}

	/**
	 * Persists contents to the given stream.
	 *
	 * @param stream
	 *            output stream
	 * @throws CoreException
	 *             if an error occurs
	 */
	void write(OutputStream stream) throws CoreException {
		try {
			if (fContainers != null && fContainers.length != 0) {
				Element containersElement = TargetDefinitionDocumentTools.getChildElement(fRoot,
						TargetDefinitionPersistenceHelper.LOCATIONS);
				serializeBundleContainers(fContainers, containersElement);
			}
			TargetDefinitionPersistenceHelper.persistXML(this, stream);
		} catch (IOException | ParserConfigurationException | TransformerException | SAXException e) {
			abort(Messages.TargetDefinition_3, e);
		}
	}

	/**
	 * Throws a core exception with the given message and underlying exception (possibly
	 * <code>null</code>).
	 *
	 * @param message message
	 * @param e underlying cause of the exception or <code>null</code>
	 */
	private void abort(String message, Exception e) throws CoreException {
		throw new CoreException(Status.error(message, e));
	}

	@Override
	public NameVersionDescriptor[] getImplicitDependencies() {
		return fImplicit;
	}

	@Override
	public void setImplicitDependencies(NameVersionDescriptor[] bundles) {
		if (bundles != null && bundles.length == 0) {
			bundles = null;
		}
		fImplicit = bundles;
		if (fRoot != null && bundles != null && bundles.length > 0) {
			Element implicitDependenciesElement = TargetDefinitionDocumentTools.getChildElement(fRoot,
					TargetDefinitionPersistenceHelper.IMPLICIT);
			List<Element> descriptorElements = new ArrayList<>();
			for (NameVersionDescriptor descriptor : bundles) {
				Element plugin = fDocument.createElement(TargetDefinitionPersistenceHelper.PLUGIN);
				plugin.setAttribute(TargetDefinitionPersistenceHelper.ATTR_ID, descriptor.getId());
				if (descriptor.getVersion() != null) {
					plugin.setAttribute(TargetDefinitionPersistenceHelper.ATTR_VERSION, descriptor.getVersion());
				}
				descriptorElements.add(plugin);
			}

			TargetDefinitionDocumentTools.updateElements(implicitDependenciesElement, null, descriptorElements, BY_ID);
		} else {
			removeElement(TargetDefinitionPersistenceHelper.IMPLICIT);
		}
	}

	@Override
	public IPath getJREContainer() {
		return fJREContainer;
	}

	@Override
	public void setJREContainer(IPath containerPath) {
		fJREContainer = containerPath;
		if (fRoot != null && containerPath != null) {
			Element jreElement = TargetDefinitionDocumentTools.getChildElement(fRoot,
					TargetDefinitionPersistenceHelper.TARGET_JRE);
			jreElement.setAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_PATH,
					containerPath.toPortableString());
		} else {
			removeElement(TargetDefinitionPersistenceHelper.TARGET_JRE);
		}
	}

	/**
	 * Returns whether the content of this definition is equal to the content of the specified definition.
	 *
	 * @return whether the content of this definition is equal to the content of the specified definition
	 */
	public boolean isContentEqual(ITargetDefinition definition) {
		return Objects.equals(getName(), definition.getName()) && //
				Objects.equals(getArch(), definition.getArch()) && //
				Objects.equals(getNL(), definition.getNL()) && //
				Objects.equals(getOS(), definition.getOS()) && //
				Objects.equals(getWS(), definition.getWS()) && //
				Objects.equals(getProgramArguments(), definition.getProgramArguments()) && //
				Objects.equals(getVMArguments(), definition.getVMArguments()) && //
				Objects.equals(getJREContainer(), definition.getJREContainer()) && //
				Arrays.equals(getIncluded(), definition.getIncluded()) && //
				unorderedArraysEqualsSerialized(getTargetLocations(), definition.getTargetLocations()) && //
				Arrays.equals(getImplicitDependencies(), definition.getImplicitDependencies());
	}

	/**
	 * Returns whether the content of this definition is equivalent to the content of the
	 * specified definition (excluding name/description).
	 *
	 * @return whether the content of this definition is equivalent to the content of the
	 * specified definition
	 */
	public boolean isContentEquivalent(ITargetDefinition definition) {
		return Objects.equals(getArch(), definition.getArch()) && //
				Objects.equals(getNL(), definition.getNL()) && //
				Objects.equals(getOS(), definition.getOS()) && //
				Objects.equals(getWS(), definition.getWS()) && //
				isArgsNullOrEqual(getProgramArguments(), definition.getProgramArguments()) && //
				isArgsNullOrEqual(getVMArguments(), definition.getVMArguments()) && //
				Objects.equals(getJREContainer(), definition.getJREContainer()) && //
				Arrays.equals(getIncluded(), definition.getIncluded()) && //
				unorderedArraysEqualsSerialized(getTargetLocations(), definition.getTargetLocations()) && //
				Arrays.equals(getImplicitDependencies(), definition.getImplicitDependencies());
	}

	private static boolean unorderedArraysEqualsSerialized(ITargetLocation[] one, ITargetLocation[] two) {
		if (one == two) {
			return true;
		}
		if (one == null || two == null || one.length != two.length) {
			return false;
		}
		Set<Object> oneXml = Arrays.stream(one).filter(Objects::nonNull)
				.map(l -> Objects.requireNonNullElse(l.serialize(), l)).collect(Collectors.toSet());
		Set<Object> twoXml = Arrays.stream(two).filter(Objects::nonNull)
				.map(l -> Objects.requireNonNullElse(l.serialize(), l)).collect(Collectors.toSet());
		return oneXml.equals(twoXml);
	}

	private boolean isArgsNullOrEqual(String args1, String args2) {
		return (args1 == null && args2 == null)
				|| Arrays.equals(DebugPlugin.parseArguments(args1), DebugPlugin.parseArguments(args2));
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(fName != null ? fName : "No Name"); //$NON-NLS-1$
		if (fContainers == null) {
			buf.append("\n\tNo containers"); //$NON-NLS-1$
		} else {
			for (ITargetLocation fContainer : fContainers) {
				buf.append("\n\t").append(fContainer.toString()); //$NON-NLS-1$
			}
		}
		buf.append("\nEnv: ").append(fOS).append("/").append(fWS).append("/").append(fArch).append("/").append(fNL); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		buf.append("\nJRE: ").append(fJREContainer); //$NON-NLS-1$
		buf.append("\nArgs: ").append(fProgramArgs).append("/").append(fVMArgs); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("\nImplicit: ").append(fImplicit == null ? "null" : Integer.toString(fImplicit.length)); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("\nHandle: ").append(fHandle.toString()); //$NON-NLS-1$
		return buf.toString();
	}

	/**
	 * Returns a set of feature models that exist in the provided location.  If
	 * the locationPath is <code>null</code> the default target platform location
	 * will be used.  The locationPath string may container string variables which
	 * will be resolved.  This target definition may cache the feature models for
	 * faster retrieval.
	 *
	 * TODO When to clear the cache
	 *
	 * @param locationPath string path to the directory containing features.  May container string variables or be <code>null</code>
	 * @return list of feature models found in the location, possible empty
	 * @param monitor progress monitor
	 * @throws CoreException if there is a problem substituting a string variable
	 */
	public TargetFeature[] resolveFeatures(String locationPath, IProgressMonitor monitor) throws CoreException {
		String path = locationPath;
		if (path == null) {
			path = TargetPlatform.getDefaultLocation();
		} else {
			IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
			path = manager.performStringSubstitution(path);
		}

		TargetFeature[] models = null;
		if (fFeaturesInLocation != null) {
			models = fFeaturesInLocation.get(path);
		}

		if (models != null) {
			return models; /*(IFeatureModel[])models.toArray(new IFeatureModel[models.size()]);*/
		}

		models = ExternalFeatureModelManager.createFeatures(path, new ArrayList<>(), monitor);
		fFeaturesInLocation.put(path, models);
		return models;
	}

	@Override
	public TargetFeature[] getAllFeatures() {
		if (!isResolved()) {
			return null;
		}

		if (fFeatures != null) {
			return fFeatures;
		}

		ITargetLocation[] containers = getTargetLocations();

		// collect up all features from all containers and remove duplicates.
		Map<NameVersionDescriptor, TargetFeature> result = new HashMap<>();
		if (containers != null && containers.length > 0) {
			for (ITargetLocation container : containers) {
				TargetFeature[] currentFeatures = container.getFeatures();
				if (currentFeatures != null && currentFeatures.length > 0) {
					for (TargetFeature feature : currentFeatures) {
						if (feature.getId() != null) {
							// Don't allow features with null ids, Bug 377563
							NameVersionDescriptor key = new NameVersionDescriptor(feature.getId(), feature.getVersion());
							result.put(key, feature);
						}
					}
				}
			}
		}

		fFeatures = result.values().toArray(new TargetFeature[result.size()]);
		return fFeatures;
	}

	/**
	 * Returns the set of IResolvedBundle available in this target that are not part of any features, will return a cached copy if available
	 *
	 * @see #getAllFeatures()
	 * @return set of resolved bundles available in this target that don't belong to any features, possibly empty
	 */
	public TargetBundle[] getOtherBundles() {
		if (!isResolved()) {
			return null;
		}

		if (fOtherBundles != null) {
			return fOtherBundles;
		}

		TargetBundle[] allBundles = getAllBundles();
		Map<String, TargetBundle> remaining = new HashMap<>();
		for (TargetBundle allBundle : allBundles) {
			remaining.put(allBundle.getBundleInfo().getSymbolicName(), allBundle);
		}

		TargetFeature[] features = getAllFeatures();
		for (TargetFeature feature : features) {
			NameVersionDescriptor[] plugins = feature.getPlugins();
			for (NameVersionDescriptor plugin : plugins) {
				remaining.remove(plugin.getId());
			}
		}

		fOtherBundles = remaining.values().toArray(TargetBundle[]::new);
		return fOtherBundles;
	}

	/**
	 * Convenience method to return the set of {@link TargetFeature}s that are included in this
	 * target as well as any other included plug-ins as {@link TargetBundle}s (that are not part
	 * of the features). Also returns any bundles with error statuses.  Will return <code>null</code>
	 * if this target has not been resolved.
	 *
	 * @see #getAllFeatures()
	 * @see #getOtherBundles()
	 * @return set of {@link TargetFeature}s and {@link TargetBundle}s or <code>null</code>
	 */
	public Set<Object> getFeaturesAndBundles() {
		if (!isResolved()) {
			return null;
		}

		TargetFeature[] allFeatures = getAllFeatures();
		TargetBundle[] allExtraBundles = getOtherBundles();

		NameVersionDescriptor[] included = getIncluded();

		if (included == null) {
			Set<Object> result = new HashSet<>();
			result.addAll(Arrays.asList(allFeatures));
			result.addAll(Arrays.asList(allExtraBundles));
			return result;
		}

		Set<Object> result = new HashSet<>();
		for (NameVersionDescriptor element : included) {
			if (element.getType() == NameVersionDescriptor.TYPE_PLUGIN) {
				for (TargetBundle allExtraBundle : allExtraBundles) {
					if (allExtraBundle.getBundleInfo().getSymbolicName().equals(element.getId())) {
						result.add(allExtraBundle);
					}
				}
			} else if (element.getType() == NameVersionDescriptor.TYPE_FEATURE) {
				for (TargetFeature allFeature : allFeatures) {
					if (allFeature.getId().equals(element.getId())) {
						result.add(allFeature);
					}
				}
			}
		}

		return result;
	}

	/**
	 * @return the current UI style one of {@link #MODE_FEATURE} or {@link #MODE_PLUGIN}
	 */
	public int getUIMode() {
		return fUIMode;
	}

	/**
	 * @param mode new UI style to use, one of {@link #MODE_FEATURE} or {@link #MODE_PLUGIN}
	 */
	public void setUIMode(int mode) {
		fUIMode = mode;
		if (fRoot != null && mode == TargetDefinition.MODE_FEATURE) {
			fRoot.setAttribute(TargetDefinitionPersistenceHelper.ATTR_INCLUDE_MODE,
					TargetDefinitionPersistenceHelper.FEATURE);
		}
	}

	private void removeElement(String... childNames) {
		if (fRoot != null) {
			TargetDefinitionDocumentTools.removeElement(fRoot, childNames);
		}
	}

	private void serializeBundleContainers(ITargetLocation[] targetLocations, Element containersElement)
			throws DOMException, CoreException, SAXException, IOException, ParserConfigurationException {

		List<Element> newContainers = new ArrayList<>();
		List<Element> newIUContainers = new ArrayList<>();
		List<Element> newGenericContainers = new ArrayList<>();
		List<Element> oldContainers = new ArrayList<>();
		List<Element> oldIUContainers = new ArrayList<>();
		List<Element> oldGenericContainers = new ArrayList<>();

		@SuppressWarnings("restriction")
		DocumentBuilder docBuilder = org.eclipse.core.internal.runtime.XmlProcessorFactory
				.createDocumentBuilderWithErrorOnDOCTYPE();
		for (ITargetLocation targetLocation : targetLocations) {
			String type = targetLocation.getType();
			if (targetLocation instanceof DirectoryBundleContainer) {
				Element containerElement = createContainerElement(type, targetLocation.getLocation(false));
				newContainers.add(containerElement);
			} else if (targetLocation instanceof FeatureBundleContainer) {
				Element containerElement = createContainerElement(type, targetLocation.getLocation(false));
				String version = ((FeatureBundleContainer) targetLocation).getFeatureVersion();
				String id = ((FeatureBundleContainer) targetLocation).getFeatureId();
				if (version != null) {
					containerElement.setAttribute(TargetDefinitionPersistenceHelper.ATTR_VERSION, version);
				}
				if (id != null) {
					containerElement.setAttribute(TargetDefinitionPersistenceHelper.ATTR_ID, id);
				}
				newContainers.add(containerElement);
			} else if (targetLocation instanceof ProfileBundleContainer) {
				Element containerElement = createContainerElement(type, targetLocation.getLocation(false));
				String configurationArea = ((ProfileBundleContainer) targetLocation).getConfigurationLocation();
				if (configurationArea != null) {
					containerElement.setAttribute(TargetDefinitionPersistenceHelper.ATTR_CONFIGURATION,
							configurationArea);
				}
				newContainers.add(containerElement);
			} else {
				String xml = targetLocation.serialize();
				if (xml != null) {
					Document document = docBuilder
							.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
					Element root = document.getDocumentElement();
					if (!root.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.LOCATION)) {
						throw new CoreException(Status.error(
								NLS.bind(Messages.TargetDefinitionPersistenceHelper_WrongRootElementInXML, type, xml)));
					}
					root.setAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_TYPE, type);
					if (IUBundleContainer.TYPE.equals(type)) {
						newIUContainers.add(root);
					} else {
						newGenericContainers.add(root);
					}
				}
			}
		}

		NodeList nodes = containersElement.getChildNodes();
		for (int j = 0; j < nodes.getLength(); j++) {
			if (nodes.item(j) instanceof Element element
					&& element.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.LOCATION)) {
				String type = element.getAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_TYPE);
				switch (type) {
				case IUBundleContainer.TYPE:
					oldIUContainers.add(element);
					break;
				case DirectoryBundleContainer.TYPE:
				case FeatureBundleContainer.TYPE:
				case ProfileBundleContainer.TYPE:
					oldContainers.add(element);
					break;
				default:
					oldGenericContainers.add(element);
					break;
				}
			}
		}

		updateContainerElements(containersElement, oldContainers, newContainers);
		updateGenericContainerElements(containersElement, oldGenericContainers, newGenericContainers);
		updateIUContainerElements(containersElement, oldIUContainers, newIUContainers);
	}

	private Element createContainerElement(String type, String path) {
		Element containerElement = fDocument.createElement(TargetDefinitionPersistenceHelper.LOCATION);
		containerElement.setAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_TYPE, type);
		containerElement.setAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_PATH, path);
		return containerElement;
	}

	private void updateGenericContainerElements(Element containersElement, List<Element> oldContainers,
			List<Element> newContainers) {
		TargetDefinitionDocumentTools.updateElements(containersElement, oldContainers, newContainers, null);
	}

	private void updateContainerElements(Element containersElement, List<Element> oldContainers,
			List<Element> newContainers) {
		TargetDefinitionDocumentTools.updateElements(containersElement, oldContainers, newContainers, (o1, o2) -> {
			int typeCompare = o1.getAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_TYPE)
					.compareTo(o2.getAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_TYPE));
			int pathCompare = o1.getAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_PATH)
					.compareTo(o2.getAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_PATH));
			int idCompare = 0;
			if (o1 instanceof FeatureBundleContainer) {
				idCompare = BY_ID.compare(o1, o2);
			}
			return typeCompare == 0 && pathCompare == 0 && idCompare == 0 ? 0 : 1;
		});
	}

	private static final Comparator<Element> BY_ID = Comparator
			.comparing(o -> o.getAttribute(TargetDefinitionPersistenceHelper.ATTR_ID));

	private void updateIUContainerElements(Element containersElement, List<Element> oldContainers,
			List<Element> newContainers) {
		Map<String, List<Element>> oldContainersByRepo = new HashMap<>();
		Map<Element, List<Element>> oldUnitsByContainer = new HashMap<>();
		for (Element container : oldContainers) {
			NodeList nodes = container.getChildNodes();
			List<Element> units = new ArrayList<>();
			String repoURL = null;
			for (int i = 0; i < nodes.getLength(); i++) {
				if (nodes.item(i) instanceof Element element) {
					if (repoURL == null
							&& element.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.REPOSITORY)) {
						repoURL = element.getAttribute(TargetDefinitionPersistenceHelper.LOCATION);

						oldContainersByRepo.computeIfAbsent(repoURL, u -> new ArrayList<>()).add(container);
					} else if (element.getNodeName()
							.equalsIgnoreCase(TargetDefinitionPersistenceHelper.INSTALLABLE_UNIT)) {
						units.add(element);
					}
				}
			}
			if (repoURL != null) {
				oldUnitsByContainer.put(container, units);
			} else {
				TargetDefinitionDocumentTools.removeChildAndWhitespace(container);
			}
		}

		for (Element container : newContainers) {
			NodeList nodes = container.getChildNodes();
			List<Element> units = new ArrayList<>();
			String repoURL = null;
			for (int i = 0; i < nodes.getLength(); i++) {
				if (nodes.item(i) instanceof Element element) {
					if (repoURL == null
							&& element.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.REPOSITORY)) {
						repoURL = element.getAttribute(TargetDefinitionPersistenceHelper.LOCATION);
					} else if (element.getNodeName()
							.equalsIgnoreCase(TargetDefinitionPersistenceHelper.INSTALLABLE_UNIT)) {
						units.add(element);
					}
				}
			}
			if (repoURL != null) {
				if (oldContainersByRepo.containsKey(repoURL)) {
					Element oldContainer = oldContainersByRepo.get(repoURL).get(0);
					TargetDefinitionDocumentTools.updateElements(oldContainer, oldUnitsByContainer.get(oldContainer),
							units, BY_ID);
					if (oldContainersByRepo.get(repoURL).size() == 1) {
						oldContainersByRepo.remove(repoURL);
					} else {
						oldContainersByRepo.get(repoURL).remove(0);
					}
				} else {
					Node movedContainer = fDocument.importNode(container, true);
					TargetDefinitionDocumentTools.addChildWithIndent(containersElement, movedContainer);
				}
			}
		}

		for (List<Element> containers : oldContainersByRepo.values()) {
			containers.forEach(TargetDefinitionDocumentTools::removeChildAndWhitespace);
		}
	}

	private void serializeBundles(Element parent, NameVersionDescriptor[] bundles) {
		List<Element> bundlElements = new ArrayList<>();
		for (NameVersionDescriptor bundle : bundles) {
			if (bundle.getType() == NameVersionDescriptor.TYPE_FEATURE) {
				Element includedBundle = fDocument.createElement(TargetDefinitionPersistenceHelper.FEATURE);
				includedBundle.setAttribute(TargetDefinitionPersistenceHelper.ATTR_ID, bundle.getId());
				String version = bundle.getVersion();
				if (version != null) {
					includedBundle.setAttribute(TargetDefinitionPersistenceHelper.ATTR_VERSION, version);
				}
				bundlElements.add(includedBundle);
			} else {
				Element includedBundle = fDocument.createElement(TargetDefinitionPersistenceHelper.PLUGIN);
				includedBundle.setAttribute(TargetDefinitionPersistenceHelper.ATTR_ID, bundle.getId());
				String version = bundle.getVersion();
				if (version != null) {
					includedBundle.setAttribute(TargetDefinitionPersistenceHelper.ATTR_VERSION, version);
				}
				bundlElements.add(includedBundle);
			}
		}
		TargetDefinitionDocumentTools.updateElements(parent, null, bundlElements, BY_ID);
	}
}
