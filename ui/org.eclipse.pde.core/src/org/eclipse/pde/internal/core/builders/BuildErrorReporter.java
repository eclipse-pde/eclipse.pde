/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
 *     Code 9 Corporation - ongoing enhancements
 *     Brock Janiczak <brockj@tpg.com.au> - bug 191545
 *     Jacek Pospychala <jacek.pospychala@pl.ibm.com> - bug 221998
 *     Steven Spungin <steven@spungin.tv> - Bug 408727
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.builders.IncrementalErrorReporter.VirtualMarker;
import org.eclipse.pde.internal.core.ibundle.IBundleFragmentModel;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.text.build.BuildEntry;
import org.eclipse.pde.internal.core.text.build.BuildModel;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.osgi.framework.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BuildErrorReporter extends ErrorReporter implements IBuildPropertiesConstants {

	private static final String DEF_SOURCE_ENTRY = PROPERTY_SOURCE_PREFIX + '.';
	private static final String[] RESERVED_NAMES = new String[] {"meta-inf", "osgi-inf", ICoreConstants.BUILD_FILENAME_DESCRIPTOR, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR, "plugin.properties"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	private static final String ASSERT_IDENTIFIER = "assertIdentifier"; //$NON-NLS-1$
	private static final String ENUM_IDENTIFIER = "enumIdentifier"; //$NON-NLS-1$

	//Execution Environments
	private static final String JRE_1_1 = "JRE-1.1"; //$NON-NLS-1$
	private static final String J2SE_1_2 = "J2SE-1.2"; //$NON-NLS-1$
	private static final String J2SE_1_3 = "J2SE-1.3"; //$NON-NLS-1$
	private static final String J2SE_1_4 = "J2SE-1.4"; //$NON-NLS-1$
	private static final String J2SE_1_5 = "J2SE-1.5"; //$NON-NLS-1$
	private static final String JavaSE_1_6 = "JavaSE-1.6"; //$NON-NLS-1$
	private static final String JavaSE_1_7 = "JavaSE-1.7"; //$NON-NLS-1$

	class BuildProblem {
		String fEntryToken;
		String fEntryName;
		String fMessage;
		String fCategory;
		int fFixId;
		int fSeverity;
		String fCompilerKey;
		HashMap<String, String> attributes;
		int fExtraBuntryEntryIndex;

		BuildProblem(String name, String token, String message, int fixId, int severity, String compilerKey, String category) {
			fEntryName = name;
			fEntryToken = token;
			fMessage = message;
			fFixId = fixId;
			fSeverity = severity;
			fCompilerKey = compilerKey;
			fCategory = category;
			fExtraBuntryEntryIndex = 0;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof BuildProblem)) {
				return false;
			}
			BuildProblem bp = (BuildProblem) obj;
			if (!fEntryName.equals(bp.fEntryName)) {
				return false;
			}
			if (fEntryToken != null && !fEntryToken.equals(bp.fEntryToken)) {
				return false;
			}
			if (fFixId != bp.fFixId) {
				return false;
			}
			return true;
		}

		public void addExtraBuildEntryTokenAttribute(String entry, String value) {
			addAttribute(PDEMarkerFactory.BK_BUILD_ENTRY + '.' + fExtraBuntryEntryIndex, entry);
			addAttribute(PDEMarkerFactory.BK_BUILD_TOKEN + '.' + fExtraBuntryEntryIndex, value);
			fExtraBuntryEntryIndex++;
		}

		public void addAttribute(String attributeName, String value) {
			if (attributes == null) {
				attributes = new HashMap<>(1);
			}
			attributes.put(attributeName, value);
		}

		public void addAttributes(HashMap<String, String> attributes) {
			if (attributes == null) {
				attributes = new HashMap<>(1);
			}
			attributes.putAll(attributes);
		}
	}

	class WildcardFilenameFilter implements FilenameFilter {

		private final Pattern pattern;

		public WildcardFilenameFilter(String file) {
			pattern = PatternConstructor.createPattern(file, false);
		}

		@Override
		public boolean accept(File dir, String name) {
			Matcher matcher = pattern.matcher(name);
			return matcher.matches();
		}

	}

	protected ArrayList<BuildProblem> fProblemList = new ArrayList<>();
	protected int fBuildSeverity;
	protected int fClasspathSeverity;
	protected int fJavaComplianceSeverity;
	protected int fJavaCompilerSeverity;
	protected int fSrcInclSeverity;
	protected int fBinInclSeverity;
	protected int fMissingOutputLibSeverity;
	protected int fSrcLibSeverity;
	protected int fOututLibSeverity;
	protected int fEncodingSeverity;

	public BuildErrorReporter(IFile buildFile) {
		super(buildFile);
		fBuildSeverity = CompilerFlags.getFlag(fFile.getProject(), CompilerFlags.P_BUILD);
		fClasspathSeverity = CompilerFlags.getFlag(fFile.getProject(), CompilerFlags.P_UNRESOLVED_IMPORTS);
		fMissingOutputLibSeverity = CompilerFlags.getFlag(fFile.getProject(), CompilerFlags.P_BUILD_MISSING_OUTPUT);
		fSrcLibSeverity = CompilerFlags.getFlag(fFile.getProject(), CompilerFlags.P_BUILD_SOURCE_LIBRARY);
		fOututLibSeverity = CompilerFlags.getFlag(fFile.getProject(), CompilerFlags.P_BUILD_OUTPUT_LIBRARY);
		fJavaComplianceSeverity = CompilerFlags.getFlag(fFile.getProject(), CompilerFlags.P_BUILD_JAVA_COMPLIANCE);
		fJavaCompilerSeverity = CompilerFlags.getFlag(fFile.getProject(), CompilerFlags.P_BUILD_JAVA_COMPILER);
		fSrcInclSeverity = CompilerFlags.getFlag(fFile.getProject(), CompilerFlags.P_BUILD_SRC_INCLUDES);
		fBinInclSeverity = CompilerFlags.getFlag(fFile.getProject(), CompilerFlags.P_BUILD_BIN_INCLUDES);
		fEncodingSeverity = CompilerFlags.getFlag(fFile.getProject(), CompilerFlags.P_BUILD_ENCODINGS);
	}

	@Override
	public void validate(IProgressMonitor monitor) {
		/*if (fBuildSeverity == CompilerFlags.IGNORE && fClasspathSeverity == CompilerFlags.IGNORE)
			return;*/
		WorkspaceBuildModel wbm = new WorkspaceBuildModel(fFile);
		wbm.load();
		if (!wbm.isLoaded()) {
			return;
		}
		// check build and store all found errors
		validateBuild(wbm.getBuild(true));

		// if there are any errors report using the text model
		if (!fProblemList.isEmpty()) {
			reportErrors(prepareTextBuildModel(monitor));
		}
	}

	private void validateBuild(IBuild build) {

		IBuildEntry binIncludes = null;
		IBuildEntry binExcludes = null;
		IBuildEntry srcIncludes = null;
		IBuildEntry srcExcludes = null;
		IBuildEntry jarsExtra = null;
		IBuildEntry bundleList = null;
		IBuildEntry javacSource = null;
		IBuildEntry javacTarget = null;
		IBuildEntry jreCompilationProfile = null;
		IBuildEntry javaProjectWarnings = null;
		ArrayList<IBuildEntry> javacWarnings = new ArrayList<>();
		ArrayList<IBuildEntry> javacErrors = new ArrayList<>();
		ArrayList<IBuildEntry> sourceEntries = new ArrayList<>(1);
		ArrayList<String> sourceEntryKeys = new ArrayList<>(1);
		ArrayList<IBuildEntry> outputEntries = new ArrayList<>(1);
		Map<String, String> encodingEntries = new HashMap<>();
		IBuildEntry[] entries = build.getBuildEntries();
		for (IBuildEntry entry : entries) {
			String name = entry.getName();
			if (entry.getTokens().length == 0) {
				prepareError(name, null, PDECoreMessages.BuildErrorReporter_emptyEntry, PDEMarkerFactory.B_REMOVAL, PDEMarkerFactory.CAT_FATAL);
			} else if (name.equals(PROPERTY_BIN_INCLUDES)) {
				binIncludes = entry;
			} else if (name.equals(PROPERTY_BIN_EXCLUDES)) {
				binExcludes = entry;
			} else if (name.equals(PROPERTY_SRC_INCLUDES)) {
				srcIncludes = entry;
			} else if (name.equals(PROPERTY_SRC_EXCLUDES)) {
				srcExcludes = entry;
			} else if (name.equals(PROPERTY_JAVAC_SOURCE)) {
				javacSource = entry;
			} else if (name.equals(PROPERTY_JAVAC_TARGET)) {
				javacTarget = entry;
			} else if (name.equals(PROPERTY_PROJECT_SETTINGS)) {
				javaProjectWarnings = entry;
			} else if (name.equals(PROPERTY_JRE_COMPILATION_PROFILE)) {
				jreCompilationProfile = entry;
			} else if (name.startsWith(PROPERTY_JAVAC_WARNINGS_PREFIX)) {
				javacWarnings.add(entry);
			} else if (name.startsWith(PROPERTY_JAVAC_ERRORS_PREFIX)) {
				javacErrors.add(entry);
			} else if (name.startsWith(PROPERTY_SOURCE_PREFIX)) {
				sourceEntries.add(entry);
			} else if (name.startsWith(PROPERTY_OUTPUT_PREFIX)) {
				outputEntries.add(entry);
			} else if (name.startsWith(PROPERTY_JAVAC_DEFAULT_ENCODING_PREFIX)) {
				encodingEntries.put(entry.getName(), entry.getTokens()[0]);
			} else if (name.equals(PROPERTY_JAR_EXTRA_CLASSPATH)) {
				jarsExtra = entry;
			} else if (name.equals(IBuildEntry.SECONDARY_DEPENDENCIES)) {
				bundleList = entry;
			} else if (name.equals(PROPERTY_CUSTOM)) {
				String[] tokens = entry.getTokens();
				if (tokens.length == 1 && tokens[0].equalsIgnoreCase("true")) { //$NON-NLS-1$
					// nothing to validate in custom builds
					return;
				}
			}

			// non else if statement to catch all names
			if (name.startsWith(PROPERTY_SOURCE_PREFIX)) {
				sourceEntryKeys.add(entry.getName());
			}
		}

		// validation not relying on build flag
		if (fClasspathSeverity != CompilerFlags.IGNORE) {
			if (bundleList != null) {
				validateDependencyManagement(bundleList);
			}
		}

		if (jarsExtra != null) {
			validateJarsExtraClasspath(jarsExtra);
		}
		validateIncludes(binIncludes, sourceEntryKeys, fBinInclSeverity,CompilerFlags.P_BUILD_BIN_INCLUDES);
		validateIncludes(binExcludes, sourceEntryKeys, fBinInclSeverity,CompilerFlags.P_BUILD_BIN_INCLUDES);
		validateIncludes(srcIncludes, sourceEntryKeys, fSrcInclSeverity,CompilerFlags.P_BUILD_SRC_INCLUDES);
		validateIncludes(srcExcludes, sourceEntryKeys, fSrcInclSeverity,CompilerFlags.P_BUILD_SRC_INCLUDES);
		validateSourceFoldersInSrcIncludes(srcIncludes);

		try {
			IJavaProject jp = JavaCore.create(fProject);
			if (jp.exists()) {
				IClasspathEntry[] cpes = jp.getRawClasspath();
				validateMissingLibraries(sourceEntryKeys, cpes);
				validateSourceEntries(sourceEntries, srcExcludes, cpes);
				SourceEntryErrorReporter srcEntryErrReporter = new SourceEntryErrorReporter(fFile, build);
				srcEntryErrReporter.initialize(sourceEntries, outputEntries, cpes, fProject);
				srcEntryErrReporter.validate();
				ArrayList<BuildProblem> problems = srcEntryErrReporter.getProblemList();
				for (int i = 0; i < problems.size(); i++) {
					if (!fProblemList.contains(problems.get(i))) {
						fProblemList.add(problems.get(i));
					}
				}

			}
		} catch (JavaModelException e) {
		}

		validateMissingSourceInBinIncludes(binIncludes, sourceEntryKeys, build);
		validateBinIncludes(binIncludes);
		validateExecutionEnvironment(javacSource, javacTarget, jreCompilationProfile, javacWarnings, javacErrors, getSourceLibraries(sourceEntries));
		validateJavaCompilerSettings(javaProjectWarnings);
	}

	/**
	 * Given a list of source library entries, returns the list of library names.
	 *
	 * @param sourceEntries list of IBuildEntry source entries
	 * @return list of library names
	 */
	private List<String> getSourceLibraries(List<IBuildEntry> sourceEntries) {
		List<String> libraries = new ArrayList<>();
		for (IBuildEntry entry : sourceEntries) {
			String libName = entry.getName().substring(PROPERTY_SOURCE_PREFIX.length());
			libraries.add(libName);
		}
		return libraries;
	}

	/**
	 * Matches the javacSource, javacTarget, javacWarnings, javacErrors and jre.compilation.prile entries in build.properties with the
	 * project specific Java Compiler properties and reports the errors found.
	 *
	 * @param javacSourceEntry
	 * @param javacTargetEntry
	 * @param jreCompilationProfileEntry
	 * @param javacWarningsEntries
	 * @param javacErrorsEntries
	 * @param libraryNames list of library names (javacWarnings/javacErrors require an entry for each source library)
	 */
	private void validateExecutionEnvironment(IBuildEntry javacSourceEntry, IBuildEntry javacTargetEntry, IBuildEntry jreCompilationProfileEntry, ArrayList<IBuildEntry> javacWarningsEntries, ArrayList<IBuildEntry> javacErrorsEntries, List<String> libraryNames) {
		// if there is no source to compile, don't worry about compiler settings
		IJavaProject project = JavaCore.create(fProject);
		if (project.exists()) {
			IClasspathEntry[] classpath = null;
			try {
				classpath = project.getRawClasspath();
			} catch (JavaModelException e) {
				PDECore.log(e);
				return;
			}
			boolean source = false;
			for (IClasspathEntry entry : classpath) {
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					source = true;
				}
			}
			if (!source) {
				return;
			}

			String projectComplianceLevel = project.getOption(JavaCore.COMPILER_COMPLIANCE, false);

			if (projectComplianceLevel != null) {

				IPluginModelBase model = PluginRegistry.findModel(fProject);
				String[] execEnvs = null;
				if (model != null) {
					BundleDescription bundleDesc = model.getBundleDescription();
					if (bundleDesc != null) {
						execEnvs = bundleDesc.getExecutionEnvironments();
					}
				}

				if (execEnvs == null || execEnvs.length == 0) {
					return;
				}

				//PDE Build uses top most entry to build the plug-in
				String execEnv = execEnvs[0];

				String projectSourceCompatibility = project.getOption(JavaCore.COMPILER_SOURCE, true);
				String projectClassCompatibility = project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true);
				if (projectComplianceLevel.equals(findMatchingEE(projectSourceCompatibility, projectClassCompatibility, false)) && execEnv.equals(findMatchingEE(projectSourceCompatibility, projectClassCompatibility, true))) {
					return; //The project compliance settings matches the BREE
				}

				Map<?, ?> defaultComplianceOptions = new HashMap<>();
				JavaCore.setComplianceOptions(projectComplianceLevel, defaultComplianceOptions);

				//project compliance does not match the BREE
				String projectJavaCompatibility = findMatchingEE(projectSourceCompatibility, projectClassCompatibility, true);
				String message = null;
				if (projectJavaCompatibility != null) {
					if (jreCompilationProfileEntry == null) {
						message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceMissingEntry, PROPERTY_JRE_COMPILATION_PROFILE, PDECoreMessages.BuildErrorReporter_CompilercomplianceLevel);
						prepareError(PROPERTY_JRE_COMPILATION_PROFILE, projectJavaCompatibility, message, PDEMarkerFactory.B_JAVA_ADDDITION, fJavaComplianceSeverity,CompilerFlags.P_BUILD_JAVA_COMPLIANCE, PDEMarkerFactory.CAT_EE);
					} else {
						if (!projectJavaCompatibility.equalsIgnoreCase(jreCompilationProfileEntry.getTokens()[0])) {
							message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceDifferentToken, PROPERTY_JRE_COMPILATION_PROFILE, PDECoreMessages.BuildErrorReporter_CompilercomplianceLevel);
							prepareError(PROPERTY_JRE_COMPILATION_PROFILE, projectJavaCompatibility, message, PDEMarkerFactory.B_REPLACE, fJavaComplianceSeverity,CompilerFlags.P_BUILD_JAVA_COMPLIANCE, PDEMarkerFactory.CAT_EE);
						}
					}
				} else {
					// Check source level setting
					if (projectSourceCompatibility.equals(defaultComplianceOptions.get(JavaCore.COMPILER_SOURCE))) {
						if (javacSourceEntry != null) {
							message = NLS.bind(PDECoreMessages.BuildErrorReporter_BuildEntryNotRequiredMatchesDefault, PROPERTY_JAVAC_SOURCE, PDECoreMessages.BuildErrorReporter_SourceCompatibility);
							prepareError(PROPERTY_JAVAC_SOURCE, null, message, PDEMarkerFactory.B_REMOVAL, fJavaComplianceSeverity,CompilerFlags.P_BUILD_JAVA_COMPLIANCE, PDEMarkerFactory.CAT_EE);
						}
					} else {
						if (javacSourceEntry == null) {
							message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceMissingEntry, PROPERTY_JAVAC_SOURCE, PDECoreMessages.BuildErrorReporter_SourceCompatibility);
							prepareError(PROPERTY_JAVAC_SOURCE, projectSourceCompatibility, message, PDEMarkerFactory.B_JAVA_ADDDITION, fJavaComplianceSeverity,CompilerFlags.P_BUILD_JAVA_COMPLIANCE, PDEMarkerFactory.CAT_EE);
						} else {
							if (!projectSourceCompatibility.equalsIgnoreCase(javacSourceEntry.getTokens()[0])) {
								message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceDifferentToken, PROPERTY_JAVAC_SOURCE, PDECoreMessages.BuildErrorReporter_SourceCompatibility);
								prepareError(PROPERTY_JAVAC_SOURCE, projectSourceCompatibility, message, PDEMarkerFactory.B_REPLACE, fJavaComplianceSeverity,CompilerFlags.P_BUILD_JAVA_COMPLIANCE, PDEMarkerFactory.CAT_EE);
							}
						}
					}

					// Check target level setting
					if (projectClassCompatibility.equals(defaultComplianceOptions.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM))) {
						if (javacTargetEntry != null) {
							message = NLS.bind(PDECoreMessages.BuildErrorReporter_BuildEntryNotRequiredMatchesDefault, PROPERTY_JAVAC_TARGET, PDECoreMessages.BuildErrorReporter_GeneratedClassFilesCompatibility);
							prepareError(PROPERTY_JAVAC_TARGET, null, message, PDEMarkerFactory.B_REMOVAL, fJavaComplianceSeverity,CompilerFlags.P_BUILD_JAVA_COMPLIANCE, PDEMarkerFactory.CAT_EE);
						}
					} else {
						if (javacTargetEntry == null) {
							message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceMissingEntry, PROPERTY_JAVAC_TARGET, PDECoreMessages.BuildErrorReporter_GeneratedClassFilesCompatibility);
							prepareError(PROPERTY_JAVAC_TARGET, projectClassCompatibility, message, PDEMarkerFactory.B_JAVA_ADDDITION, fJavaComplianceSeverity,CompilerFlags.P_BUILD_JAVA_COMPLIANCE, PDEMarkerFactory.CAT_EE);
						} else {
							if (!projectClassCompatibility.equalsIgnoreCase(javacTargetEntry.getTokens()[0])) {
								message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceDifferentToken, PROPERTY_JAVAC_TARGET, PDECoreMessages.BuildErrorReporter_GeneratedClassFilesCompatibility);
								prepareError(PROPERTY_JAVAC_TARGET, projectClassCompatibility, message, PDEMarkerFactory.B_REPLACE, fJavaComplianceSeverity,CompilerFlags.P_BUILD_JAVA_COMPLIANCE, PDEMarkerFactory.CAT_EE);
							}
						}
					}
				}

				boolean warnForJavacWarnings = message != null || javacSourceEntry != null || javacTargetEntry != null || jreCompilationProfileEntry != null;
				if (warnForJavacWarnings == false) {
					return;
				}

				checkJavaComplianceSettings(projectComplianceLevel, javacWarningsEntries, javacErrorsEntries, libraryNames);
			}
		}
	}

	/**
	 * Matches the javacWarnings and javacErrors entries in build.properties with the
	 * project specific Java compliance properties and reports the errors found.  Since java
	 * compiler settings are set on a per project basis, any special javacWarnings/javacErrors
	 * must be set for each library.
	 *
	 * @param complianceLevel the compliance level to check settings against, used to get default values
	 * @param javacWarningsEntries list of build entries with the java compiler warnings prefix javacWarnings.
	 * @param javacErrorsEntries list of build entries with the java compiler errors prefix javacErrors.
	 * @param libraryNames list of String library names
	 */
	private void checkJavaComplianceSettings(String complianceLevel, ArrayList<IBuildEntry> javacWarningsEntries, ArrayList<IBuildEntry> javacErrorsEntries, List<String> libraryNames) {
		List<String> complianceWarnSettings = new ArrayList<>(3);
		List<String> complianceErrorSettings = new ArrayList<>(3);

		IJavaProject project = JavaCore.create(fProject);
		if (project.exists()) {

			Map<?, ?> defaultComplianceOptions = new HashMap<>();
			JavaCore.setComplianceOptions(complianceLevel, defaultComplianceOptions);

			//look for assertIdentifier and enumIdentifier entries in javacWarnings. If any is present let it be, if not warn.
			String assertIdentifier = project.getOption(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, false);
			String defaultAssert = (String) defaultComplianceOptions.get(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER);
			if (assertIdentifier != null && !assertIdentifier.equalsIgnoreCase(defaultAssert)) {
				if (JavaCore.ERROR.equalsIgnoreCase(assertIdentifier)) {
					complianceErrorSettings.add(ASSERT_IDENTIFIER);
				} else if (JavaCore.WARNING.equalsIgnoreCase(assertIdentifier)) {
					complianceWarnSettings.add(ASSERT_IDENTIFIER);
				}
			}

			String enumIdentifier = project.getOption(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, false);
			String defaultEnum = (String) defaultComplianceOptions.get(JavaCore.COMPILER_PB_ENUM_IDENTIFIER);
			if (enumIdentifier != null && !enumIdentifier.equalsIgnoreCase(defaultEnum)) {
				if (JavaCore.ERROR.equalsIgnoreCase(enumIdentifier)) {
					complianceErrorSettings.add(ENUM_IDENTIFIER);
				} else if (JavaCore.WARNING.equalsIgnoreCase(enumIdentifier)) {
					complianceWarnSettings.add(ENUM_IDENTIFIER);
				}
			}

			// If a warnings entry is required, make sure there is one for each library with the correct content
			if (!complianceWarnSettings.isEmpty()) {
				for (String libName : libraryNames) {
					IBuildEntry matchingEntry = null;
					for (IBuildEntry candidate : javacWarningsEntries) {
						if (candidate.getName().equals(PROPERTY_JAVAC_WARNINGS_PREFIX + libName)) {
							matchingEntry = candidate;
							break;
						}
					}
					if (matchingEntry == null) {
						String missingTokens = ""; //$NON-NLS-1$
						for (String currentIdentifier : complianceWarnSettings) {
							missingTokens = join(missingTokens, '-' + currentIdentifier);
						}
						String message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceMissingEntry, PROPERTY_JAVAC_WARNINGS_PREFIX + libName);
						prepareError(PROPERTY_JAVAC_WARNINGS_PREFIX + libName, missingTokens, message, PDEMarkerFactory.B_JAVA_ADDDITION, fJavaComplianceSeverity,CompilerFlags.P_BUILD_JAVA_COMPLIANCE, PDEMarkerFactory.CAT_EE);
					} else {
						String missingTokens = ""; //$NON-NLS-1$
						for (String currentIdentifier : complianceWarnSettings) {
							if (!matchingEntry.contains(currentIdentifier) && !matchingEntry.contains('+' + currentIdentifier) && !matchingEntry.contains('-' + currentIdentifier)) {
								join(missingTokens, '-' + currentIdentifier);
							}
						}
						if (missingTokens.length() > 0) {
							String message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceDifferentToken, PROPERTY_JAVAC_WARNINGS_PREFIX + libName);
							prepareError(PROPERTY_JAVAC_WARNINGS_PREFIX + libName, missingTokens, message, PDEMarkerFactory.B_JAVA_ADDDITION, fJavaComplianceSeverity,CompilerFlags.P_BUILD_JAVA_COMPLIANCE, PDEMarkerFactory.CAT_EE);
						}
					}
				}
			}

			// If a warnings entry is required, make sure there is one for each library with the correct content
			if (!complianceErrorSettings.isEmpty()) {
				for (String libName : libraryNames) {
					IBuildEntry matchingEntry = null;
					for (IBuildEntry candidate : javacErrorsEntries) {
						if (candidate.getName().equals(PROPERTY_JAVAC_ERRORS_PREFIX + libName)) {
							matchingEntry = candidate;
							break;
						}
					}
					if (matchingEntry == null) {
						String missingTokens = ""; //$NON-NLS-1$
						for (String currentIdentifier : complianceErrorSettings) {
							missingTokens = join(missingTokens, '-' + currentIdentifier);
						}
						String message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceMissingEntry, PROPERTY_JAVAC_ERRORS_PREFIX + libName);
						prepareError(PROPERTY_JAVAC_ERRORS_PREFIX + libName, missingTokens, message, PDEMarkerFactory.B_JAVA_ADDDITION, fJavaComplianceSeverity,CompilerFlags.P_BUILD_JAVA_COMPLIANCE, PDEMarkerFactory.CAT_EE);
					} else {
						String missingTokens = ""; //$NON-NLS-1$
						for (String currentIdentifier : complianceErrorSettings) {
							if (!matchingEntry.contains(currentIdentifier) && !matchingEntry.contains('+' + currentIdentifier) && !matchingEntry.contains('-' + currentIdentifier)) {
								missingTokens = join(missingTokens, '-' + currentIdentifier);
							}
						}
						if (missingTokens.length() > 0) {
							String message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceDifferentToken, PROPERTY_JAVAC_ERRORS_PREFIX + libName);
							prepareError(PROPERTY_JAVAC_ERRORS_PREFIX + libName, missingTokens, message, PDEMarkerFactory.B_JAVA_ADDDITION, fJavaComplianceSeverity,CompilerFlags.P_BUILD_JAVA_COMPLIANCE, PDEMarkerFactory.CAT_EE);
						}
					}
				}
			}
		}
	}

	private String findMatchingEE(String srcCompatibility, String clsCompatibility, boolean ee) {
		String executionEnv = null;
		String complaince = null;
		if (JavaCore.VERSION_1_1.equals(srcCompatibility) && JavaCore.VERSION_1_1.equals(clsCompatibility)) {
			executionEnv = JRE_1_1;
			complaince = JavaCore.VERSION_1_1;
		} else if (JavaCore.VERSION_1_2.equals(srcCompatibility) && JavaCore.VERSION_1_1.equals(clsCompatibility)) {
			executionEnv = J2SE_1_2;
			complaince = JavaCore.VERSION_1_2;
		} else if (JavaCore.VERSION_1_3.equals(srcCompatibility) && JavaCore.VERSION_1_1.equals(clsCompatibility)) {
			executionEnv = J2SE_1_3;
			complaince = JavaCore.VERSION_1_3;
		} else if (JavaCore.VERSION_1_3.equals(srcCompatibility) && JavaCore.VERSION_1_2.equals(clsCompatibility)) {
			executionEnv = J2SE_1_4;
			complaince = JavaCore.VERSION_1_4;
		} else if (JavaCore.VERSION_1_5.equals(srcCompatibility) && JavaCore.VERSION_1_5.equals(clsCompatibility)) {
			executionEnv = J2SE_1_5;
			complaince = JavaCore.VERSION_1_5;
		} else if (JavaCore.VERSION_1_6.equals(srcCompatibility) && JavaCore.VERSION_1_6.equals(clsCompatibility)) {
			executionEnv = JavaSE_1_6;
			complaince = JavaCore.VERSION_1_6;
		} else if (JavaCore.VERSION_1_7.equals(srcCompatibility) && JavaCore.VERSION_1_7.equals(clsCompatibility)) {
			executionEnv = JavaSE_1_7;
			complaince = JavaCore.VERSION_1_7;
		}

		if (ee) {
			return executionEnv;
		}
		return complaince;
	}

	private void validateBinIncludes(IBuildEntry binIncludes) {
		// make sure we have a manifest entry
		if (PDEProject.getManifest(fProject).exists()) {
			validateBinIncludes(binIncludes, ICoreConstants.MANIFEST_FOLDER_NAME);
		}

		// if we have an OSGI_INF/ directory, let's do some validation
		IFolder OSGinf = PDEProject.getOSGiInf(fProject);
		if (OSGinf.exists()) {
			try {
				if (OSGinf.members().length > 0) { // only validate if we have something in it
					validateBinIncludes(binIncludes, ICoreConstants.OSGI_INF_FOLDER_NAME);
				}
			} catch (CoreException e) { // do nothing
			}
		}

		// make sure if we're a fragment, we have a fragment.xml entry
		if (PDEProject.getFragmentXml(fProject).exists()) {
			validateBinIncludes(binIncludes, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
		}


		if (PDEProject.getPluginXml(fProject).exists()) {
			// make sure if we're a plug-in, we have a plugin.xml entry
			validateBinIncludes(binIncludes, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
			// make sure that we include model fragments
			validateFragmentContributions(binIncludes);
			// make sure if we're an application, we are include Application entry
			validateApplicationContributions(binIncludes);

		}

		// validate for bundle localization
		IPluginModelBase model = PluginRegistry.findModel(fProject);
		if (model == null) {
			return;
		}
		if (model instanceof IBundlePluginModelBase && !(model instanceof IBundleFragmentModel)) {
			IBundleModel bm = ((IBundlePluginModelBase) model).getBundleModel();
			IManifestHeader mh = bm.getBundle().getManifestHeader(Constants.BUNDLE_LOCALIZATION);
			IPath resourcePath = null;
			String entry = null;
			if ((mh == null || mh.getValue() == null)) { // check for default location
				resourcePath = new Path(Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME);
				entry = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
			} else { // check for the real location
				String localization = mh.getValue();
				int index = localization.lastIndexOf('/');
				if (index != -1) { // if we're a folder
					entry = localization.substring(0, index + 1);
					resourcePath = new Path(entry);
				} else { // if we're just a file location
					entry = mh.getValue().concat(".properties"); //$NON-NLS-1$
					resourcePath = new Path(entry);
				}
			}
			if (entry != null) {
				if (PDEProject.getBundleRoot(fProject).exists(resourcePath)) {
					validateBinIncludes(binIncludes, entry);
				}
			}
		}

	}

	// if we're defining fragments, make sure they have entries in plugin.xml
	private void validateFragmentContributions(IBuildEntry binIncludes) {
		try {
			DocumentBuilder newDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			newDocumentBuilder.setErrorHandler(new PDEErrorHandler());
			Document doc = newDocumentBuilder.parse(PDEProject.getPluginXml(fProject).getContents());
			XPath xpath = XPathFactory.newInstance().newXPath();
			NodeList list = (NodeList) xpath.evaluate("/plugin/extension[@point='org.eclipse.e4.workbench.model']/fragment/@uri", doc, XPathConstants.NODESET); //$NON-NLS-1$
			for (int i = 0; i < list.getLength(); i++) {
				Node node = list.item(i);
				validateBinIncludes(binIncludes, node.getNodeValue());
			}
		} catch (Exception e) {
		}
	}

	// if we're defining an application, make sure it has entries in plugin.xml
	private void validateApplicationContributions(IBuildEntry binIncludes) {
		try {
			DocumentBuilder newDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			newDocumentBuilder.setErrorHandler(new PDEErrorHandler());
			Document doc = newDocumentBuilder.parse(PDEProject.getPluginXml(fProject).getContents());
			XPath xpath = XPathFactory.newInstance().newXPath();
			// are we an application?
			Node nodeProduct = (Node) xpath.evaluate("/plugin/extension[@point='org.eclipse.core.runtime.products']/product", doc, XPathConstants.NODE); //$NON-NLS-1$
			if (nodeProduct != null) {
				Node attValue = (Node) xpath.evaluate("property[@name='applicationXMI']/@value", nodeProduct, XPathConstants.NODE); //$NON-NLS-1$
				if (attValue != null) {
					if (attValue.getNodeValue().isEmpty()) {
						//Error: no URL defined but should already be reported.
					} else {
						validateBinIncludes(binIncludes, attValue.getNodeValue());
					}
				} else {
					if (fProject.exists(new Path("Application.e4xmi"))) { //$NON-NLS-1$
						// Default if not specified
						validateBinIncludes(binIncludes, "Application.e4xmi"); //$NON-NLS-1$
					}
				}
			}

		} catch (Exception e) {
		}
	}

	private void validateBinIncludes(IBuildEntry binIncludes, String key) {
		if (binIncludes == null) {
			return;
		}
		String[] tokens = binIncludes.getTokens();
		boolean exists = false;
		for (String token : tokens) {
			if (key.startsWith(token)) {
				exists = true;
				break;
			}

			// check for wildcards
			IPath project = fFile.getProject().getLocation();
			if (project != null && token != null) {
				File projectFile = project.toFile();
				File[] files = projectFile.listFiles(new WildcardFilenameFilter(token));
				for (File file : files) {
					if (file.toString().endsWith(key)) {
						exists = true;
						break;
					}
				}
			}
		}

		if (!exists) {
			prepareError(PROPERTY_BIN_INCLUDES, key, NLS.bind(PDECoreMessages.BuildErrorReporter_binIncludesMissing, key), PDEMarkerFactory.B_ADDITION, fBinInclSeverity,CompilerFlags.P_BUILD_BIN_INCLUDES, PDEMarkerFactory.CAT_FATAL);
		}
	}

	private void validateJarsExtraClasspath(IBuildEntry javaExtra) {
		String platform = "platform:/plugin/"; //$NON-NLS-1$
		String[] tokens = javaExtra.getTokens();
		IPath projectPath = javaExtra.getModel().getUnderlyingResource().getProject().getLocation();
		for (int i = 0; i < tokens.length; i++) {
			boolean exists = true;
			if (tokens[i].startsWith(platform)) {
				String path = tokens[i].substring(platform.length());
				int sep = path.indexOf(IPath.SEPARATOR);
				if (sep > -1) {
					IPluginModelBase model = PluginRegistry.findModel(path.substring(0, sep));
					if (model == null) {
						exists = false;
					} else {
						IResource resource = model.getUnderlyingResource();
						path = path.substring(sep + 1);
						if (resource == null) {
							IPath result = PDECore.getDefault().getModelManager().getExternalModelManager()
									.getNestedLibrary(model, path.toString());
							if (result == null) {
								exists = false;
							}
						} else {
							exists = resource.getProject().findMember(path) != null;
						}
					}
				}
			} else {
				exists = projectPath.append(tokens[i]).toFile().exists();
			}

			if (!exists && !startsWithAntVariable(tokens[i])) {
				prepareError(PROPERTY_JAR_EXTRA_CLASSPATH, tokens[i], NLS.bind(PDECoreMessages.BuildErrorReporter_cannotFindJar, tokens[i]), PDEMarkerFactory.M_ONLY_CONFIG_SEV, fBuildSeverity,CompilerFlags.P_BUILD ,PDEMarkerFactory.CAT_OTHER);
			}
		}
	}

	private void validateMissingSourceInBinIncludes(IBuildEntry binIncludes, ArrayList<String> sourceEntryKeys, IBuild build) {
		if (binIncludes == null) {
			return;
		}
		List<String> pluginLibraryNames = new ArrayList<>(1);
		IPluginModelBase pluginModel = PluginRegistry.findModel(fProject);
		if (pluginModel != null) {
			IPluginLibrary[] pluginLibraries = pluginModel.getPluginBase().getLibraries();
			for (IPluginLibrary library : pluginLibraries) {
				pluginLibraryNames.add(library.getName());
			}
		}
		if (!pluginLibraryNames.contains(".")) { //$NON-NLS-1$
			pluginLibraryNames.add("."); //$NON-NLS-1$)
		}
		for (int i = 0; i < sourceEntryKeys.size(); i++) {
			String key = sourceEntryKeys.get(i);
			if (!pluginLibraryNames.contains(key)) {
				return; // do not report error for folders if the library itself does not exists on plug-in classpath
			}
			// We don't want to flag source.. = . as in  bug 146042 comment 1
			if (DEF_SOURCE_ENTRY.equals(key)) {
				IBuildEntry entry = build.getEntry(DEF_SOURCE_ENTRY);
				String[] tokens = entry.getTokens();
				if (tokens.length == 1 && tokens[0].equals(".")) { //$NON-NLS-1$
					continue;
				}
			}
			key = key.substring(PROPERTY_SOURCE_PREFIX.length());
			boolean found = false;
			String[] binIncludesTokens = binIncludes.getTokens();
			for (String token : binIncludesTokens) {
				Pattern pattern = PatternConstructor.createPattern(token, false);
				if (pattern.matcher(key).matches()) {
					found = true;
				}
			}
			// account for trailing slash on class file folders
			if (!found) {
				IPath path = new Path(key);
				if (path.getFileExtension() == null) {
					if (!key.endsWith("/")) { //$NON-NLS-1$
						key = key + "/"; //$NON-NLS-1$
						for (String token : binIncludesTokens) {
							Pattern pattern = PatternConstructor.createPattern(token, false);
							if (pattern.matcher(key).matches()) {
								found = true;
							}
						}
					}
				}
			}
			if (!found) {
				prepareError(PROPERTY_BIN_INCLUDES, key, NLS.bind(PDECoreMessages.BuildErrorReporter_binIncludesMissing, key), PDEMarkerFactory.B_ADDITION, fBinInclSeverity,CompilerFlags.P_BUILD_BIN_INCLUDES, PDEMarkerFactory.CAT_FATAL);
			}
		}
	}

	private void validateMissingLibraries(ArrayList<String> sourceEntryKeys, IClasspathEntry[] cpes) {
		boolean srcFolderExists = false;
		// no need to flag anything if the project contains no source folders.
		for (IClasspathEntry entry : cpes) {
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				srcFolderExists = true;
				break;
			}
		}
		if (!srcFolderExists) {
			return;
		}

		IPluginModelBase model = PluginRegistry.findModel(fProject);
		if (model == null) {
			return;
		}
		if (model instanceof IBundlePluginModelBase && !(model instanceof IBundleFragmentModel)) {
			IBundleModel bm = ((IBundlePluginModelBase) model).getBundleModel();
			IManifestHeader mh = bm.getBundle().getManifestHeader(Constants.BUNDLE_CLASSPATH);
			if ((mh == null || mh.getValue() == null)) {
				if (!sourceEntryKeys.contains(DEF_SOURCE_ENTRY)) {
					prepareError(DEF_SOURCE_ENTRY, null, PDECoreMessages.BuildErrorReporter_sourceMissing, PDEMarkerFactory.M_ONLY_CONFIG_SEV, fSrcLibSeverity,CompilerFlags.P_BUILD_SOURCE_LIBRARY, PDEMarkerFactory.CAT_OTHER);
				}
			}
		}
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		for (IPluginLibrary library : libraries) {
			String libname = library.getName();
			if (libname.equals(".")) { //$NON-NLS-1$
				if (!sourceEntryKeys.contains(DEF_SOURCE_ENTRY)) {
					prepareError(DEF_SOURCE_ENTRY, null, PDECoreMessages.BuildErrorReporter_sourceMissing, PDEMarkerFactory.M_ONLY_CONFIG_SEV, fSrcLibSeverity,CompilerFlags.P_BUILD_SOURCE_LIBRARY, PDEMarkerFactory.CAT_OTHER);
					continue;
				}
			} else if (fProject.findMember(libname) != null) {
				// non "." library entries that exist in the workspace
				// don't have to be referenced in the build properties
				continue;
			}
			String sourceEntryKey = PROPERTY_SOURCE_PREFIX + libname;
			if (!sourceEntryKeys.contains(sourceEntryKey) && !containedInFragment(model.getBundleDescription(), libname)) {
				prepareError(sourceEntryKey, null, NLS.bind(PDECoreMessages.BuildErrorReporter_missingEntry, sourceEntryKey), PDEMarkerFactory.B_ADDITION, PDEMarkerFactory.CAT_OTHER);
			}
		}
	}

	private boolean containedInFragment(BundleDescription description, String libname) {
		if (description == null) {
			return false;
		}

		BundleDescription[] fragments = description.getFragments();

		if (fragments == null) {
			return false;
		}
		for (BundleDescription fragment : fragments) {
			IPluginModelBase fragmentModel = PluginRegistry.findModel(fragment);
			if (fragmentModel != null && fragmentModel.getUnderlyingResource() != null) {
				IProject project = fragmentModel.getUnderlyingResource().getProject();
				if (project.findMember(libname) != null) {
					return true;
				}
				try {
					IBuild build = ClasspathUtilCore.getBuild(fragmentModel);
					if (build != null) {
						IBuildEntry[] entries = build.getBuildEntries();
						for (IBuildEntry entrie : entries) {
							if (entrie.getName().equals(PROPERTY_SOURCE_PREFIX + libname)) {
								return true;
							}
						}
						return false;
					}
				} catch (CoreException e) {
				}
			} else {
				String location = fragment.getLocation();
				File external = new File(location);
				if (external.exists()) {
					if (external.isDirectory()) {
						IPath p = new Path(location).addTrailingSeparator().append(libname);
						return new File(p.toOSString()).exists();
					}
					return CoreUtility.jarContainsResource(external, libname, false);
				}
			}
		}
		return false;
	}

	private void validateSourceEntries(ArrayList<IBuildEntry> sourceEntries, IBuildEntry srcExcludes, IClasspathEntry[] cpes) {
		if (sourceEntries == null || sourceEntries.isEmpty()) {
			return;
		}
		String[] unlisted = PDEBuilderHelper.getUnlistedClasspaths(sourceEntries, fProject, cpes);
		List<String> excludeList = new ArrayList<>(0);
		if (srcExcludes != null && srcExcludes.getTokens().length > 0) {
			excludeList = Arrays.asList(srcExcludes.getTokens());
		}
		String name = sourceEntries.get(0).getName();
		String message = PDECoreMessages.BuildErrorReporter_classpathEntryMissing1;
		if (sourceEntries.size() > 1) {
			name = DEF_SOURCE_ENTRY;
			message = PDECoreMessages.BuildErrorReporter_classpathEntryMissing;
		}
		for (String element : unlisted) {
			if (element == null || excludeList.contains(element)) {
				continue;
			}
			BuildProblem error = prepareError(name, element, NLS.bind(message, element, name), PDEMarkerFactory.B_ADDITION, fSrcLibSeverity,CompilerFlags.P_BUILD_SOURCE_LIBRARY, PDEMarkerFactory.CAT_OTHER);
			error.addExtraBuildEntryTokenAttribute(PROPERTY_SRC_EXCLUDES, element);
		}
	}

	// bug 286808
	private void validateSourceFoldersInSrcIncludes(IBuildEntry includes) {
		if (includes == null) {
			return;
		}

		List<IPath> sourceFolderList = new ArrayList<>(0);
		try {
			IJavaProject javaProject = JavaCore.create(fProject);
			if (javaProject.exists()) {
				IClasspathEntry[] classPathEntries = javaProject.getResolvedClasspath(true);

				for (IClasspathEntry entry : classPathEntries) {
					if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						sourceFolderList.add(entry.getPath());
					}
				}
			}
		} catch (JavaModelException e) { //do nothing
		}

		List<String> reservedTokens = Arrays.asList(RESERVED_NAMES);

		String[] tokens = includes.getTokens();
		for (String token : tokens) {
			IResource res = fProject.findMember(token);
			if (res == null) {
				continue;
			}
			String errorMessage = null;
			if (sourceFolderList.contains(res.getFullPath())) {
				errorMessage = PDECoreMessages.BuildErrorReporter_srcIncludesSourceFolder;
			} else if (token.startsWith(".") || reservedTokens.contains(res.getName().toString().toLowerCase())) { //$NON-NLS-1$
				errorMessage = NLS.bind(PDECoreMessages.BuildErrorReporter_srcIncludesSourceFolder1, res.getName());
			}

			if (errorMessage != null) {
				prepareError(includes.getName(), token, errorMessage, PDEMarkerFactory.B_REMOVAL, fSrcInclSeverity,CompilerFlags.P_BUILD_SRC_INCLUDES, PDEMarkerFactory.CAT_OTHER);
			}
		}

	}

	private void validateIncludes(IBuildEntry includes, ArrayList<String> sourceIncludes, int severity, String compilerKey) {
		if (includes == null) {
			return;
		}
		String[] tokens = includes.getTokens();
		for (String tokenBasic : tokens) {
			String token = tokenBasic.trim();
			if (token.indexOf("*") != -1) { //$NON-NLS-1$
				// skip entries with wildcards
				continue;
			}
			if (token.equals(".")) { //$NON-NLS-1$
				// skip . since we know it exists
				continue;
			}
			if (startsWithAntVariable(token)) {
				// skip '${x}' variables
				continue;
			}
			IResource member = PDEProject.getBundleRoot(fProject).findMember(token);
			String message = null;
			int fixId = PDEMarkerFactory.M_ONLY_CONFIG_SEV;
			if (member == null) {
				if (sourceIncludes.contains(PROPERTY_SOURCE_PREFIX + token)) {
					continue;
				}
				if (token.endsWith("/")) { //$NON-NLS-1$
					message = NLS.bind(PDECoreMessages.BuildErrorReporter_missingFolder, token);
				} else {
					message = NLS.bind(PDECoreMessages.BuildErrorReporter_missingFile, token);
				}
				fixId = PDEMarkerFactory.B_REMOVAL;
			} else if (token.endsWith("/") && !(member instanceof IFolder)) { //$NON-NLS-1$
				message = NLS.bind(PDECoreMessages.BuildErrorReporter_entiresMustRefDirs, token);
				fixId = PDEMarkerFactory.B_REMOVE_SLASH_FILE_ENTRY;
			} else if (!token.endsWith("/") && !(member instanceof IFile)) { //$NON-NLS-1$
				message = NLS.bind(PDECoreMessages.BuildErrorReporter_dirsMustEndSlash, token);
				fixId = PDEMarkerFactory.B_APPEND_SLASH_FOLDER_ENTRY;
			}

			if (message != null) {
				prepareError(includes.getName(), token, message, fixId, severity,compilerKey, PDEMarkerFactory.CAT_OTHER);
			}
		}
	}

	private boolean startsWithAntVariable(String token) {
		int varStart = token.indexOf("${"); //$NON-NLS-1$
		return varStart != -1 && varStart < token.indexOf("}"); //$NON-NLS-1$
	}

	private void validateDependencyManagement(IBuildEntry bundleList) {
		String[] bundles = bundleList.getTokens();
		for (String bundle : bundles) {
			if (PluginRegistry.findModel(bundle) == null) {
				prepareError(IBuildEntry.SECONDARY_DEPENDENCIES, bundle, NLS.bind(PDECoreMessages.BuildErrorReporter_cannotFindBundle, bundle), PDEMarkerFactory.M_ONLY_CONFIG_SEV, fClasspathSeverity,CompilerFlags.P_UNRESOLVED_IMPORTS, PDEMarkerFactory.CAT_OTHER);
			}
		}

	}

	/**
	 * Checks that if the project has java compiler settings that build.properties contains a use project settings
	 * entry so that the compiler picks up the settings using the .pref file.
	 *
	 * @param useJavaProjectSettings a build entry for using the project's compiler warning preferences file
	 */
	private void validateJavaCompilerSettings(IBuildEntry useJavaProjectSettings) {
		// Check if the project has compiler warnings set
		IJavaProject project = JavaCore.create(fProject);
		if (project.exists()) {
			Map<?, ?> options = project.getOptions(false);
			// If project specific options are turned on, all options will be stored.  Only need to check if at least one compiler option is set. Currently using the second option on the property page.
			if (options.containsKey(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS)) {
				if (useJavaProjectSettings != null) {
					boolean entryCorrect = false;
					String[] tokens = useJavaProjectSettings.getTokens();
					if (tokens != null && tokens.length == 1) {
						if (Boolean.TRUE.toString().equalsIgnoreCase(tokens[0])) {
							// True is valid if the bundle root is the default (the project)
							entryCorrect = fProject.equals(PDEProject.getBundleRoot(fProject));
						} else {
							IPath prefFile = null;
							prefFile = new Path(tokens[0]);
							if (prefFile.isAbsolute()) {
								entryCorrect = prefFile.toFile().exists();
							} else {
								IContainer root = PDEProject.getBundleRoot(fProject);
								entryCorrect = root.getFile(prefFile).exists();
							}
						}
					}
					if (!entryCorrect) {
						String token = null;
						String message = null;
						IContainer root = PDEProject.getBundleRoot(fProject);
						if (fProject.equals(root)) {
							// Default project root, just use 'true'
							token = Boolean.TRUE.toString();
							message = NLS.bind(PDECoreMessages.BuildErrorReporter_buildEntryMissingValidPath, PROPERTY_PROJECT_SETTINGS);
						} else {
							// Non default bundle root, make a relative path
							IPath prefFile = fProject.getFullPath().append(".settings").append(JavaCore.PLUGIN_ID + ".prefs"); //$NON-NLS-1$ //$NON-NLS-2$
							prefFile = prefFile.makeRelativeTo(root.getFullPath());
							token = prefFile.toString();
							message = NLS.bind(PDECoreMessages.BuildErrorReporter_buildEntryMissingValidRelativePath, PROPERTY_PROJECT_SETTINGS);
						}
						prepareError(PROPERTY_PROJECT_SETTINGS, token, message, PDEMarkerFactory.B_REPLACE, fJavaCompilerSeverity,CompilerFlags.P_BUILD_JAVA_COMPILER, PDEMarkerFactory.CAT_EE);
					}
				} else {
					String token = null;
					IContainer root = PDEProject.getBundleRoot(fProject);
					if (fProject.equals(root)) {
						// Default project root, just use 'true'
						token = Boolean.TRUE.toString();
					} else {
						// Non default bundle root, make a relative path
						IPath prefFile = fProject.getFullPath().append(".settings").append(JavaCore.PLUGIN_ID + ".prefs"); //$NON-NLS-1$ //$NON-NLS-2$
						prefFile = prefFile.makeRelativeTo(root.getFullPath());
						token = prefFile.toString();
					}
					String message = NLS.bind(PDECoreMessages.BuildErrorReporter_buildEntryMissingProjectSpecificSettings, PROPERTY_PROJECT_SETTINGS);
					prepareError(PROPERTY_PROJECT_SETTINGS, token, message, PDEMarkerFactory.B_JAVA_ADDDITION, fJavaCompilerSeverity,CompilerFlags.P_BUILD_JAVA_COMPILER, PDEMarkerFactory.CAT_EE);
				}
			} else if (useJavaProjectSettings != null) {
				String message = NLS.bind(PDECoreMessages.BuildErrorReporter_buildEntryInvalidWhenNoProjectSettings, PROPERTY_PROJECT_SETTINGS);
				prepareError(PROPERTY_PROJECT_SETTINGS, null, message, PDEMarkerFactory.B_REMOVAL, fJavaCompilerSeverity,CompilerFlags.P_BUILD_JAVA_COMPILER, PDEMarkerFactory.CAT_EE);
			}
		}
	}

	/**
	 * Joins the given tokens into a single string with a comma separator.  If either of
	 * the tokens are null or of length 0, the other string will be returned
	 *
	 * @param token1 first string
	 * @param token2 second string
	 * @return concatenated string
	 */
	private String join(String token1, String token2) {
		StringBuilder result = new StringBuilder();
		if (token1 != null && token1.length() > 0) {
			result.append(token1);
		}
		if (token2 != null && token2.length() > 0) {
			if (result.length() > 0) {
				result.append(',');
			}
			result.append(token2);
		}
		return result.toString();
	}

	private BuildModel prepareTextBuildModel(IProgressMonitor monitor) {
		try {
			IDocument doc = createDocument(fFile);
			if (doc == null) {
				return null;
			}
			BuildModel bm = new BuildModel(doc, true);
			bm.load();
			if (!bm.isLoaded()) {
				return null;
			}
			return bm;
		} catch (CoreException e) {
			PDECore.log(e);
			return null;
		}
	}

	private void reportErrors(BuildModel bm) {
		if (bm == null) {
			return;
		}

		for (int i = 0; i < fProblemList.size(); i++) {
			BuildProblem bp = fProblemList.get(i);

			int lineNum;
			IBuildEntry buildEntry = bm.getBuild().getEntry(bp.fEntryName);
			if (buildEntry == null || bp.fEntryName == null) {
				// general file case (eg. missing source.* entry)
				lineNum = 1;
			} else {
				// issue with a particular entry
				lineNum = getLineNumber(buildEntry, bp.fEntryToken);
			}

			if (lineNum > 0) {
				VirtualMarker marker = report(bp.fMessage, lineNum, bp.fFixId, bp.fEntryName, bp.fEntryToken,
						bp.fSeverity, bp.fCategory);
				addMarkerAttribute(marker,PDEMarkerFactory.compilerKey,bp.fCompilerKey);
				if (marker != null && bp.attributes != null) {
					for (String attribute : bp.attributes.keySet()) {
						marker.setAttribute(attribute, bp.attributes.get(attribute));
					}
				}
			}
		}
	}

	private int getLineNumber(IBuildEntry ibe, String tokenString) {
		if (!(ibe instanceof BuildEntry)) {
			return 0;
		}
		BuildEntry be = (BuildEntry) ibe;
		IDocument doc = ((BuildModel) be.getModel()).getDocument();
		try {
			int buildEntryLineNumber = doc.getLineOfOffset(be.getOffset()) + 1;
			if (tokenString == null) {
				// we are interested in the build entry name
				// (getLineOfOffset is 0-indexed, need 1-indexed)
				return buildEntryLineNumber;
			}

			// extract the full entry
			String entry = doc.get(be.getOffset(), be.getLength());

			int valueIndex = entry.indexOf('=') + 1;
			if (valueIndex == 0 || valueIndex == entry.length()) {
				return buildEntryLineNumber;
			}

			// remove the entry name
			entry = entry.substring(valueIndex);

			int entryTokenOffset = entry.indexOf(tokenString);
			if (entryTokenOffset == -1) {
				return buildEntryLineNumber;
			}

			// skip ahead to 1st occurence
			entry = entry.substring(entryTokenOffset);
			int currOffset = be.getOffset() + valueIndex + entryTokenOffset;
			while (true) {
				// tokenize string using ',' as a delimiter, trim out whitespace and '\' characters
				// during comparison
				if (entry.charAt(0) == '\\') {
					currOffset++;
					entry = entry.substring(1);
				}
				int cci = entry.indexOf(',');
				if (cci == -1) {
					if (entry.trim().equals(tokenString)) {
						return doc.getLineOfOffset(currOffset + entry.indexOf(tokenString)) + 1;
					}
					return buildEntryLineNumber;
				}

				String ct = entry.substring(0, cci).trim();
				if (ct.equals(tokenString)) {
					return doc.getLineOfOffset(currOffset + entry.indexOf(tokenString)) + 1;
				}

				entry = entry.substring(++cci);
				currOffset += cci;
			}

		} catch (BadLocationException e) {
		}
		return 0;
	}

	protected BuildProblem prepareError(String name, String token, String message, int fixId, String category) {
		return prepareError(name, token, message, fixId, fBuildSeverity,CompilerFlags.P_BUILD, category);
	}

	protected BuildProblem prepareError(String name, String token, String message, int fixId, int severity, String compilerKey, String category) {
		BuildProblem bp = new BuildProblem(name, token, message, fixId, severity,compilerKey, category);
		for (int i = 0; i < fProblemList.size(); i++) {
			BuildProblem listed = fProblemList.get(i);
			if (listed.equals(bp)) {
				if (bp.attributes != null) {
					listed.addAttributes(bp.attributes);
				}
				return listed;
			}
		}
		fProblemList.add(bp);
		return bp;
	}

	/**
	 * Creates a new marker with the given attributes.  May return <code>null</code> if no marker should be created because of severity level.
	 * @param message
	 * @param line
	 * @param problemID
	 * @param buildEntry
	 * @param buildToken
	 * @param severity
	 * @param category
	 * @return a new marker or <code>null</code>
	 */
	private VirtualMarker report(String message, int line, int problemID, String buildEntry, String buildToken, int severity, String category) {
		VirtualMarker marker = report(message, line, severity, problemID, category);
		if (marker != null) {
			marker.setAttribute(PDEMarkerFactory.BK_BUILD_ENTRY, buildEntry);
			marker.setAttribute(PDEMarkerFactory.BK_BUILD_TOKEN, buildToken);
		}
		return marker;
	}

	public boolean isCustomBuild() {
		WorkspaceBuildModel wbm = new WorkspaceBuildModel(fFile);
		IBuild build = wbm.getBuild();
		IBuildEntry entry = build.getEntry(PROPERTY_CUSTOM);
		if (entry != null) {
			String[] tokens = entry.getTokens();
			if (tokens.length == 1 && tokens[0].equalsIgnoreCase("true")) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}

	protected void addMarkerAttribute(VirtualMarker marker, String attr, String value) {
		if (marker != null) {
			marker.setAttribute(attr, value);
		}
	}
}
