package org.eclipse.pde.internal.launching;

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.BundleValidationOperation;
import org.eclipse.pde.internal.launching.launcher.EclipsePluginValidationOperation;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

public final class JUnitEclipsePluginValidationOperation extends EclipsePluginValidationOperation implements ResolverHook {

	private static final VersionRange JUNIT5_VERSION_RANGE = new VersionRange("[1.0.0,6.0.0)"); //$NON-NLS-1$

	private static final String JUNIT_BUNDLE_PREFIX = "junit"; //$NON-NLS-1$

	private final Map<BundleDescription, IStatus[]> errors;
	private final VersionRange junitVersionRange;
	private final int junitVersion;

	@SuppressWarnings("restriction")
	public JUnitEclipsePluginValidationOperation(ILaunchConfiguration configuration, Set<IPluginModelBase> models, String launchMode) {
		super(configuration, models, launchMode);
		errors = new HashMap<>(2);
		org.eclipse.jdt.internal.junit.launcher.ITestKind testKind = org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants.getTestRunnerKind(configuration);
		switch (testKind.getId()) {
			case org.eclipse.jdt.internal.junit.launcher.TestKindRegistry.JUNIT5_TEST_KIND_ID -> {
				junitVersionRange = JUNIT5_VERSION_RANGE;
				junitVersion = 5;
			}
			default -> {
				junitVersionRange = null;
				junitVersion = -1;
			}
		}
	}

	protected BundleValidationOperation createOperation(Dictionary<String, String>[] properties) {
		ResolverHook hook = junitVersionRange != null ? this : null;
		BundleValidationOperation op = new BundleValidationOperation(fModels, properties, hook);
		return op;
	}

	@Override
	public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
		if (!isOptional(requirement)) {
			BundleRevision requirementRevision = requirement.getRevision();
			String requirementName = requirementRevision.getSymbolicName();
			if (!requirementName.startsWith(JUNIT_BUNDLE_PREFIX)) {
				Iterator<BundleCapability> iterator = candidates.iterator();
				while (iterator.hasNext()) {
					BundleCapability candidate = iterator.next();
					BundleRevision candidateRevision = candidate.getRevision();
					String name = candidateRevision.getSymbolicName();
					Version version = candidateRevision.getVersion();
					if (!junitVersionRange.includes(version) && name.startsWith(JUNIT_BUNDLE_PREFIX)) {
						Version requirementVersion = requirementRevision.getVersion();
						BundleDescription bundle = getState().getBundle(requirementName, requirementVersion);
						if (bundle != null) {
							String error = NLS.bind(PDEMessages.JUnitLaunchConfiguration_error_invalidJunitVersion, new Object[] {name, version, Integer.valueOf(junitVersion)});
							IStatus[] bundleErrors = errors.computeIfAbsent(bundle, b -> new Status[0]);
							if (!Arrays.stream(bundleErrors).map(IStatus::getMessage).anyMatch(m -> error.equals(m))) {
								IStatus[] newBundleErrors = Arrays.copyOf(bundleErrors, bundleErrors.length + 1);
								newBundleErrors[bundleErrors.length] = Status.error(error);
								errors.put(bundle, newBundleErrors);
							}
						} else {
							PDELaunchingPlugin.log(Status.error("Bundle not found: " + requirementName + " " + requirementVersion, new IllegalStateException())); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
				}
			}
		}
	}

	private boolean isOptional(BundleRequirement requirement) {
		return Constants.RESOLUTION_OPTIONAL.equals(requirement.getDirectives().get(Constants.RESOLUTION_DIRECTIVE));
	}

	@Override
	public void filterResolvable(Collection<BundleRevision> candidates) {
	}

	@Override
	public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
	}

	@Override
	public void end() {
	}

	@Override
	public boolean hasErrors() {
		return super.hasErrors() || !errors.isEmpty();
	}

	@Override
	public Map<Object, Object[]> getInput() {
		Map<Object, Object[]> map = super.getInput();
		map.putAll(errors);
		return map;
	}

}
