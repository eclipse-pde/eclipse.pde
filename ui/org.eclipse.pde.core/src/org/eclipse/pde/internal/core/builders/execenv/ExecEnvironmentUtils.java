package org.eclipse.pde.internal.core.builders.execenv;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
// import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
// import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.builders.JarManifestErrorReporter;

/**
 * @since 3.21
 */
public class ExecEnvironmentUtils extends JarManifestErrorReporter {

	public ExecEnvironmentUtils(IFile file) {
		super(file);
		// TODO Auto-generated constructor stub
	}

	private static final List<String> EXECUTION_ENVIRONMENT_NAMES = List.of("OSGi/Minimum", //$NON-NLS-1$
			"CDC-1.0/Foundation", //$NON-NLS-1$
			"CDC-1.1/Foundation", "JRE", "J2SE", "JavaSE"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	private static final Pattern EE_PATTERN = Pattern.compile("(.*)-(\\d+)\\.?(\\d+)?(.*)?"); //$NON-NLS-1$

	/**
	 * <p>
	 * Returns the highest Execution Environment between two given Execution
	 * Environments. An Execution Environment is <b>higher</b> than another
	 * Execution Environment if it's name occurs at a later index in
	 * {@link #EXECUTION_ENVIRONMENT_NAMES}, or if the names are equal and it's
	 * major version is greater, or if the names and major version are equal and
	 * it's minor version is greater.
	 * </p>
	 * <p>
	 * For example, the name component of JavaSE-1.8 is 'JavaSE', it's major
	 * version is 1 and it's minor version is 8. Thus JavaSE-1.8 is a higher
	 * Execution Environment than JRE-1.1 since it's name occurs at later index
	 * than 'JRE' in {@link #EXECUTION_ENVIRONMENT_NAMES}.
	 * </p>
	 *
	 * @param execEnv1
	 *            String representation of the first Execution Environment to
	 *            compare
	 * @param execEnv2
	 *            String representation of the second Execution Environment to
	 *            compare
	 * @return The string representation of the highest Execution Environment
	 *         between the two Execution Environments
	 */
	public static String getHighestEE(String execEnv1, String execEnv2) throws IllegalArgumentException {
		if (execEnv1 == null) {
			return execEnv2;
		} else if (execEnv2 == null) {
			return execEnv1;
		}

		Matcher eeMatcher1 = EE_PATTERN.matcher(execEnv1);
		Matcher eeMatcher2 = EE_PATTERN.matcher(execEnv2);

		if (!eeMatcher1.matches()) {
			throw new IllegalArgumentException(String.format("%s is not a valid Execution Environment", execEnv1)); //$NON-NLS-1$
		}
		if (!eeMatcher2.matches()) {
			throw new IllegalArgumentException(String.format("%s is not a valid Execution Environment", execEnv2)); //$NON-NLS-1$
		}

		String eeName1 = eeMatcher1.group(1);
		String eeName2 = eeMatcher2.group(1);
		int eeNameIndex1 = EXECUTION_ENVIRONMENT_NAMES.indexOf(eeName1);
		int eeNameIndex2 = EXECUTION_ENVIRONMENT_NAMES.indexOf(eeName2);
		int eeMajorVersion1 = Integer.parseInt(eeMatcher1.group(2));
		int eeMajorVersion2 = Integer.parseInt(eeMatcher2.group(2));
		Integer eeMinorVersion1 = null;
		Integer eeMinorVersion2 = null;

		if (eeMatcher1.groupCount() > 2 && eeMatcher1.group(3) != null) {
			eeMinorVersion1 = Integer.valueOf(eeMatcher1.group(3));
		}
		if (eeMatcher2.groupCount() > 2 && eeMatcher2.group(3) != null) {
			eeMinorVersion2 = Integer.valueOf(eeMatcher2.group(3));
		}

		if (eeNameIndex1 > eeNameIndex2) {
			return execEnv1;
		} else if (eeNameIndex1 < eeNameIndex2) {
			return execEnv2;
		}

		// EE1 and EE2 have the same EE name
		if (eeMajorVersion1 > eeMajorVersion2) {
			return execEnv1;
		} else if (eeMajorVersion1 < eeMajorVersion2) {
			return execEnv2;
		}

		// EE1 and EE2 have the same major version
		if (eeMinorVersion1 != null && eeMinorVersion2 != null) {
			if (eeMinorVersion1 > eeMinorVersion2) {
				return execEnv1;
			} else if (eeMinorVersion1 < eeMinorVersion2) {
				return execEnv2;
			}
		}

		// EE1 == EE2
		return execEnv1;
	}

	/**
	 * Compares all the Execution Environments in an array of Execution
	 * Environments strings and returns the highest one.
	 *
	 * @param executionEnvironments
	 *            Array of Execution Environment strings to compare
	 * @return The highest Execution Environment in the array of Execution
	 *         Environments, null if an error occurred or if an empty array is
	 *         given
	 */
	public static String getHighestBREE(String[] executionEnvironments) {
		if (executionEnvironments.length == 0) {
			return null;
		}
		String highestExecEnv = executionEnvironments[0];
		if (executionEnvironments.length > 1) {
			for (String execEnv : executionEnvironments) {
				try {
					highestExecEnv = getHighestEE(highestExecEnv, execEnv);
				} catch (Exception e) {
					PDECore.log(e);
					return null;
				}

			}
		}

		return highestExecEnv;
	}

	/**
	 * Gets the highest Execution Environment required by a bundle or any of
	 * it's transitive dependencies.
	 *
	 * @param desc
	 *            The bundle description of the bundle which we wish to check
	 *            for it's highest required Execution Environment
	 * @return List containing the highest Execution Environment and
	 *         BundleDescription with the highest BREE required by the bundle or
	 *         any of it's dependencies
	 */
	public static ArrayList<Object> checkBREE(BundleDescription desc) {
		System.out.println("checkBREE(), line 158"); //$NON-NLS-1$
		System.out.println("value of BundleDescription desc: " + desc); //$NON-NLS-1$
		ArrayList<Object> ret = new ArrayList<>();
		String highestBREE = getHighestBREE(desc.getExecutionEnvironments());
		ret.add(highestBREE);
		HashSet<BundleDescription> visitedBundles = new HashSet<>();
		Deque<BundleDescription> bundleDescriptions = new ArrayDeque<>();
		bundleDescriptions.push(desc);
		while (!bundleDescriptions.isEmpty()) {
			BundleDescription dependencyDesc = bundleDescriptions.pop();
			visitedBundles.add(dependencyDesc);
			for (BundleSpecification transitiveDependencyDesc : dependencyDesc.getRequiredBundles()) {
				if (transitiveDependencyDesc.isOptional()) {
					continue;
				}
				if (!visitedBundles.contains(transitiveDependencyDesc.getSupplier())) {
					if (transitiveDependencyDesc.getSupplier() instanceof BundleDescription) {
						bundleDescriptions.push((BundleDescription) transitiveDependencyDesc.getSupplier());
					}
				}
			}
			try {
				String high = getHighestEE(highestBREE, getHighestBREE(dependencyDesc.getExecutionEnvironments()));
				if (!high.equals(highestBREE)) {
					highestBREE = high;
					ret.clear();
					ret.add(highestBREE);
					ret.add(dependencyDesc);
				}
			} catch (Exception e) {
				PDECore.log(e);
			}
		}
		return ret;
	}

}
