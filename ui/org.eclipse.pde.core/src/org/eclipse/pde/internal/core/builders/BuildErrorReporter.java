/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - ongoing enhancements
 *     Brock Janiczak <brockj@tpg.com.au> - bug 191545
 *     Jacek Pospychala <jacek.pospychala@pl.ibm.com> - bug 221998
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.text.build.BuildEntry;
import org.eclipse.pde.internal.core.text.build.BuildModel;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.osgi.framework.Constants;

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

	private class BuildProblem {
		String fEntryToken;
		String fEntryName;
		String fMessage;
		String fCategory;
		int fFixId;
		int fSeverity;
		HashMap attributes;
		int fExtraBuntryEntryIndex;

		BuildProblem(String name, String token, String message, int fixId, int severity, String category) {
			fEntryName = name;
			fEntryToken = token;
			fMessage = message;
			fFixId = fixId;
			fSeverity = severity;
			fCategory = category;
			fExtraBuntryEntryIndex = 0;
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof BuildProblem))
				return false;
			BuildProblem bp = (BuildProblem) obj;
			if (!fEntryName.equals(bp.fEntryName))
				return false;
			if (fEntryToken != null && !fEntryToken.equals(bp.fEntryToken))
				return false;
			if (fFixId != bp.fFixId)
				return false;
			return true;
		}

		public void addExtraBuildEntryTokenAttribute(String entry, String value) {
			addAttribute(PDEMarkerFactory.BK_BUILD_ENTRY + '.' + fExtraBuntryEntryIndex, entry);
			addAttribute(PDEMarkerFactory.BK_BUILD_TOKEN + '.' + fExtraBuntryEntryIndex, value);
			fExtraBuntryEntryIndex++;
		}

		public void addAttribute(String attributeName, String value) {
			if (attributes == null)
				attributes = new HashMap(1);
			attributes.put(attributeName, value);
		}

		public void addAttributes(HashMap attributes) {
			if (attributes == null)
				attributes = new HashMap(1);
			attributes.putAll(attributes);
		}
	}

	class WildcardFilenameFilter implements FilenameFilter {

		private Pattern pattern;

		public WildcardFilenameFilter(String file) {
			pattern = PatternConstructor.createPattern(file, false);
		}

		public boolean accept(File dir, String name) {
			Matcher matcher = pattern.matcher(name);
			return matcher.matches();
		}

	}

	protected ArrayList fProblemList = new ArrayList();
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

	public void validate(IProgressMonitor monitor) {
		/*if (fBuildSeverity == CompilerFlags.IGNORE && fClasspathSeverity == CompilerFlags.IGNORE)
			return;*/
		WorkspaceBuildModel wbm = new WorkspaceBuildModel(fFile);
		wbm.load();
		if (!wbm.isLoaded())
			return;
		// check build and store all found errors
		validateBuild(wbm.getBuild(true));

		// if there are any errors report using the text model
		if (fProblemList.size() > 0)
			reportErrors(prepareTextBuildModel(monitor));
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
		ArrayList javacWarnings = new ArrayList();
		ArrayList javacErrors = new ArrayList();
		ArrayList sourceEntries = new ArrayList(1);
		ArrayList sourceEntryKeys = new ArrayList(1);
		ArrayList outputEntries = new ArrayList(1);
		Map encodingEntries = new HashMap();
		IBuildEntry[] entries = build.getBuildEntries();
		for (int i = 0; i < entries.length; i++) {
			String name = entries[i].getName();
			if (entries[i].getTokens().length == 0)
				prepareError(name, null, PDECoreMessages.BuildErrorReporter_emptyEntry, PDEMarkerFactory.B_REMOVAL, PDEMarkerFactory.CAT_FATAL);
			else if (name.equals(PROPERTY_BIN_INCLUDES))
				binIncludes = entries[i];
			else if (name.equals(PROPERTY_BIN_EXCLUDES))
				binExcludes = entries[i];
			else if (name.equals(PROPERTY_SRC_INCLUDES))
				srcIncludes = entries[i];
			else if (name.equals(PROPERTY_SRC_EXCLUDES))
				srcExcludes = entries[i];
			else if (name.equals(PROPERTY_JAVAC_SOURCE))
				javacSource = entries[i];
			else if (name.equals(PROPERTY_JAVAC_TARGET))
				javacTarget = entries[i];
			else if (name.equals(PROPERTY_PROJECT_SETTINGS))
				javaProjectWarnings = entries[i];
			else if (name.equals(PROPERTY_JRE_COMPILATION_PROFILE))
				jreCompilationProfile = entries[i];
			else if (name.startsWith(PROPERTY_JAVAC_WARNINGS_PREFIX))
				javacWarnings.add(entries[i]);
			else if (name.startsWith(PROPERTY_JAVAC_ERRORS_PREFIX))
				javacErrors.add(entries[i]);
			else if (name.startsWith(PROPERTY_SOURCE_PREFIX))
				sourceEntries.add(entries[i]);
			else if (name.startsWith(PROPERTY_OUTPUT_PREFIX))
				outputEntries.add(entries[i]);
			else if (name.startsWith(PROPERTY_JAVAC_DEFAULT_ENCODING_PREFIX))
				encodingEntries.put(entries[i].getName(), entries[i].getTokens()[0]);
			else if (name.equals(PROPERTY_JAR_EXTRA_CLASSPATH))
				jarsExtra = entries[i];
			else if (name.equals(IBuildEntry.SECONDARY_DEPENDENCIES))
				bundleList = entries[i];
			else if (name.equals(PROPERTY_CUSTOM)) {
				String[] tokens = entries[i].getTokens();
				if (tokens.length == 1 && tokens[0].equalsIgnoreCase("true")) //$NON-NLS-1$
					// nothing to validate in custom builds
					return;
			}

			// non else if statement to catch all names
			if (name.startsWith(PROPERTY_SOURCE_PREFIX))
				sourceEntryKeys.add(entries[i].getName());
		}

		// validation not relying on build flag
		if (fClasspathSeverity != CompilerFlags.IGNORE) {
			if (bundleList != null)
				validateDependencyManagement(bundleList);
		}

		if (jarsExtra != null)
			validateJarsExtraClasspath(jarsExtra);
		validateIncludes(binIncludes, sourceEntryKeys, fBinInclSeverity);
		validateIncludes(binExcludes, sourceEntryKeys, fBinInclSeverity);
		validateIncludes(srcIncludes, sourceEntryKeys, fSrcInclSeverity);
		validateIncludes(srcExcludes, sourceEntryKeys, fSrcInclSeverity);
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
				ArrayList problems = srcEntryErrReporter.getProblemList();
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
	private List getSourceLibraries(List sourceEntries) {
		List libraries = new ArrayList();
		for (Iterator iterator = sourceEntries.iterator(); iterator.hasNext();) {
			IBuildEntry sourceEntry = (IBuildEntry) iterator.next();
			String libName = sourceEntry.getName().substring(PROPERTY_SOURCE_PREFIX.length());
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
	private void validateExecutionEnvironment(IBuildEntry javacSourceEntry, IBuildEntry javacTargetEntry, IBuildEntry jreCompilationProfileEntry, ArrayList javacWarningsEntries, ArrayList javacErrorsEntries, List libraryNames) {
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
			for (int i = 0; i < classpath.length; i++) {
				IClasspathEntry cpe = classpath[i];
				if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
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

				Map defaultComplianceOptions = new HashMap();
				JavaCore.setComplianceOptions(projectComplianceLevel, defaultComplianceOptions);

				//project compliance does not match the BREE
				String projectJavaCompatibility = findMatchingEE(projectSourceCompatibility, projectClassCompatibility, true);
				String message = null;
				if (projectJavaCompatibility != null) {
					if (jreCompilationProfileEntry == null) {
						message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceMissingEntry, PROPERTY_JRE_COMPILATION_PROFILE, PDECoreMessages.BuildErrorReporter_CompilercomplianceLevel);
						prepareError(PROPERTY_JRE_COMPILATION_PROFILE, projectJavaCompatibility, message, PDEMarkerFactory.B_JAVA_ADDDITION, fJavaComplianceSeverity, PDEMarkerFactory.CAT_EE);
					} else {
						if (!projectJavaCompatibility.equalsIgnoreCase(jreCompilationProfileEntry.getTokens()[0])) {
							message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceDifferentToken, PROPERTY_JRE_COMPILATION_PROFILE, PDECoreMessages.BuildErrorReporter_CompilercomplianceLevel);
							prepareError(PROPERTY_JRE_COMPILATION_PROFILE, projectJavaCompatibility, message, PDEMarkerFactory.B_REPLACE, fJavaComplianceSeverity, PDEMarkerFactory.CAT_EE);
						}
					}
				} else {
					// Check source level setting
					if (projectSourceCompatibility.equals(defaultComplianceOptions.get(JavaCore.COMPILER_SOURCE))) {
						if (javacSourceEntry != null) {
							message = NLS.bind(PDECoreMessages.BuildErrorReporter_BuildEntryNotRequiredMatchesDefault, PROPERTY_JAVAC_SOURCE, PDECoreMessages.BuildErrorReporter_SourceCompatibility);
							prepareError(PROPERTY_JAVAC_SOURCE, null, message, PDEMarkerFactory.B_REMOVAL, fJavaComplianceSeverity, PDEMarkerFactory.CAT_EE);
						}
					} else {
						if (javacSourceEntry == null) {
							message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceMissingEntry, PROPERTY_JAVAC_SOURCE, PDECoreMessages.BuildErrorReporter_SourceCompatibility);
							prepareError(PROPERTY_JAVAC_SOURCE, projectSourceCompatibility, message, PDEMarkerFactory.B_JAVA_ADDDITION, fJavaComplianceSeverity, PDEMarkerFactory.CAT_EE);
						} else {
							if (!projectSourceCompatibility.equalsIgnoreCase(javacSourceEntry.getTokens()[0])) {
								message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceDifferentToken, PROPERTY_JAVAC_SOURCE, PDECoreMessages.BuildErrorReporter_SourceCompatibility);
								prepareError(PROPERTY_JAVAC_SOURCE, projectSourceCompatibility, message, PDEMarkerFactory.B_REPLACE, fJavaComplianceSeverity, PDEMarkerFactory.CAT_EE);
							}
						}
					}

					// Check target level setting
					if (projectClassCompatibility.equals(defaultComplianceOptions.get(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM))) {
						if (javacTargetEntry != null) {
							message = NLS.bind(PDECoreMessages.BuildErrorReporter_BuildEntryNotRequiredMatchesDefault, PROPERTY_JAVAC_TARGET, PDECoreMessages.BuildErrorReporter_GeneratedClassFilesCompatibility);
							prepareError(PROPERTY_JAVAC_TARGET, null, message, PDEMarkerFactory.B_REMOVAL, fJavaComplianceSeverity, PDEMarkerFactory.CAT_EE);
						}
					} else {
						if (javacTargetEntry == null) {
							message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceMissingEntry, PROPERTY_JAVAC_TARGET, PDECoreMessages.BuildErrorReporter_GeneratedClassFilesCompatibility);
							prepareError(PROPERTY_JAVAC_TARGET, projectClassCompatibility, message, PDEMarkerFactory.B_JAVA_ADDDITION, fJavaComplianceSeverity, PDEMarkerFactory.CAT_EE);
						} else {
							if (!projectClassCompatibility.equalsIgnoreCase(javacTargetEntry.getTokens()[0])) {
								message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceDifferentToken, PROPERTY_JAVAC_TARGET, PDECoreMessages.BuildErrorReporter_GeneratedClassFilesCompatibility);
								prepareError(PROPERTY_JAVAC_TARGET, projectClassCompatibility, message, PDEMarkerFactory.B_REPLACE, fJavaComplianceSeverity, PDEMarkerFactory.CAT_EE);
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
	private void checkJavaComplianceSettings(String complianceLevel, ArrayList javacWarningsEntries, ArrayList javacErrorsEntries, List libraryNames) {
		List complianceWarnSettings = new ArrayList(3);
		List complianceErrorSettings = new ArrayList(3);

		IJavaProject project = JavaCore.create(fProject);
		if (project.exists()) {

			Map defaultComplianceOptions = new HashMap();
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
			if (complianceWarnSettings.size() > 0) {
				for (Iterator iterator = libraryNames.iterator(); iterator.hasNext();) {
					String libName = (String) iterator.next();
					IBuildEntry matchingEntry = null;
					for (Iterator iterator2 = javacWarningsEntries.iterator(); iterator2.hasNext();) {
						IBuildEntry candidate = (IBuildEntry) iterator2.next();
						if (candidate.getName().equals(PROPERTY_JAVAC_WARNINGS_PREFIX + libName)) {
							matchingEntry = candidate;
							break;
						}
					}
					if (matchingEntry == null) {
						String missingTokens = ""; //$NON-NLS-1$
						for (Iterator iterator2 = complianceWarnSettings.iterator(); iterator2.hasNext();) {
							String currentIdentifier = (String) iterator2.next();
							missingTokens = join(missingTokens, '-' + currentIdentifier);
						}
						String message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceMissingEntry, PROPERTY_JAVAC_WARNINGS_PREFIX + libName);
						prepareError(PROPERTY_JAVAC_WARNINGS_PREFIX + libName, missingTokens, message, PDEMarkerFactory.B_JAVA_ADDDITION, fJavaComplianceSeverity, PDEMarkerFactory.CAT_EE);
					} else {
						String missingTokens = ""; //$NON-NLS-1$
						for (Iterator iterator2 = complianceWarnSettings.iterator(); iterator2.hasNext();) {
							String currentIdentifier = (String) iterator2.next();
							if (!matchingEntry.contains(currentIdentifier) && !matchingEntry.contains('+' + currentIdentifier) && !matchingEntry.contains('-' + currentIdentifier)) {
								join(missingTokens, '-' + currentIdentifier);
							}
						}
						if (missingTokens.length() > 0) {
							String message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceDifferentToken, PROPERTY_JAVAC_WARNINGS_PREFIX + libName);
							prepareError(PROPERTY_JAVAC_WARNINGS_PREFIX + libName, missingTokens, message, PDEMarkerFactory.B_JAVA_ADDDITION, fJavaComplianceSeverity, PDEMarkerFactory.CAT_EE);
						}
					}
				}
			}

			// If a warnings entry is required, make sure there is one for each library with the correct content
			if (complianceErrorSettings.size() > 0) {
				for (Iterator iterator = libraryNames.iterator(); iterator.hasNext();) {
					String libName = (String) iterator.next();
					IBuildEntry matchingEntry = null;
					for (Iterator iterator2 = javacErrorsEntries.iterator(); iterator2.hasNext();) {
						IBuildEntry candidate = (IBuildEntry) iterator2.next();
						if (candidate.getName().equals(PROPERTY_JAVAC_ERRORS_PREFIX + libName)) {
							matchingEntry = candidate;
							break;
						}
					}
					if (matchingEntry == null) {
						String missingTokens = ""; //$NON-NLS-1$
						for (Iterator iterator2 = complianceErrorSettings.iterator(); iterator2.hasNext();) {
							String currentIdentifier = (String) iterator2.next();
							missingTokens = join(missingTokens, '-' + currentIdentifier);
						}
						String message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceMissingEntry, PROPERTY_JAVAC_ERRORS_PREFIX + libName);
						prepareError(PROPERTY_JAVAC_ERRORS_PREFIX + libName, missingTokens, message, PDEMarkerFactory.B_JAVA_ADDDITION, fJavaComplianceSeverity, PDEMarkerFactory.CAT_EE);
					} else {
						String missingTokens = ""; //$NON-NLS-1$
						for (Iterator iterator2 = complianceErrorSettings.iterator(); iterator2.hasNext();) {
							String currentIdentifier = (String) iterator2.next();
							if (!matchingEntry.contains(currentIdentifier) && !matchingEntry.contains('+' + currentIdentifier) && !matchingEntry.contains('-' + currentIdentifier)) {
								missingTokens = join(missingTokens, '-' + currentIdentifier);
							}
						}
						if (missingTokens.length() > 0) {
							String message = NLS.bind(PDECoreMessages.BuildErrorReporter_ProjectSpecificJavaComplianceDifferentToken, PROPERTY_JAVAC_ERRORS_PREFIX + libName);
							prepareError(PROPERTY_JAVAC_ERRORS_PREFIX + libName, missingTokens, message, PDEMarkerFactory.B_JAVA_ADDDITION, fJavaComplianceSeverity, PDEMarkerFactory.CAT_EE);
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

		// make sure if we're a plug-in, we have a plugin.xml entry
		if (PDEProject.getPluginXml(fProject).exists()) {
			validateBinIncludes(binIncludes, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
		}

		// validate for bundle localization
		IPluginModelBase model = PluginRegistry.findModel(fProject);
		if (model == null)
			return;
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
			if (resourcePath != null && entry != null) {
				if (PDEProject.getBundleRoot(fProject).exists(resourcePath)) {
					validateBinIncludes(binIncludes, entry);
				}
			}
		}

	}

	private void validateBinIncludes(IBuildEntry binIncludes, String key) {
		if (binIncludes == null)
			return;
		String[] tokens = binIncludes.getTokens();
		boolean exists = false;
		for (int i = 0; i < tokens.length; i++) {
			if (key.startsWith(tokens[i])) {
				exists = true;
				break;
			}

			// check for wildcards
			IPath project = fFile.getProject().getLocation();
			if (project != null && tokens[i] != null) {
				File file = project.toFile();
				File[] files = file.listFiles(new WildcardFilenameFilter(tokens[i]));
				for (int j = 0; j < files.length; j++) {
					if (files[j].toString().endsWith(key)) {
						exists = true;
						break;
					}
				}
			}
		}

		if (!exists) {
			prepareError(PROPERTY_BIN_INCLUDES, key, NLS.bind(PDECoreMessages.BuildErrorReporter_binIncludesMissing, key), PDEMarkerFactory.B_ADDITION, fBinInclSeverity, PDEMarkerFactory.CAT_FATAL);
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
					if (model == null)
						exists = false;
					else {
						IResource resource = model.getUnderlyingResource();
						path = path.substring(sep + 1);
						if (resource == null) {
							String location = model.getInstallLocation();
							File external = new File(location);
							if (external.isDirectory()) {
								IPath p = new Path(location).addTrailingSeparator().append(path);
								exists = new File(p.toOSString()).exists();
							} else
								// compiler will not recognize nested jars, if external location is not
								// a directory this reference "does not exist"
								exists = false;
						} else
							exists = resource.getProject().findMember(path) != null;
					}
				}
			} else
				exists = projectPath.append(tokens[i]).toFile().exists();

			if (!exists && !startsWithAntVariable(tokens[i])) {
				prepareError(PROPERTY_JAR_EXTRA_CLASSPATH, tokens[i], NLS.bind(PDECoreMessages.BuildErrorReporter_cannotFindJar, tokens[i]), PDEMarkerFactory.NO_RESOLUTION, fBuildSeverity, PDEMarkerFactory.CAT_OTHER);
			}
		}
	}

	private void validateMissingSourceInBinIncludes(IBuildEntry binIncludes, ArrayList sourceEntryKeys, IBuild build) {
		if (binIncludes == null)
			return;
		List pluginLibraryNames = new ArrayList(1);
		IPluginModelBase pluginModel = PluginRegistry.findModel(fProject);
		if (pluginModel != null) {
			IPluginLibrary[] pluginLibraries = pluginModel.getPluginBase().getLibraries();
			for (int i = 0; i < pluginLibraries.length; i++) {
				pluginLibraryNames.add(pluginLibraries[i].getName());
			}
		}
		if (!pluginLibraryNames.contains(".")) { //$NON-NLS-1$
			pluginLibraryNames.add("."); //$NON-NLS-1$)
		}
		for (int i = 0; i < sourceEntryKeys.size(); i++) {
			String key = (String) sourceEntryKeys.get(i);
			if (!pluginLibraryNames.contains(key)) {
				return; // do not report error for folders if the library itself does not exists on plug-in classpath
			}
			// We don't want to flag source.. = . as in  bug 146042 comment 1
			if (DEF_SOURCE_ENTRY.equals(key)) {
				IBuildEntry entry = build.getEntry(DEF_SOURCE_ENTRY);
				String[] tokens = entry.getTokens();
				if (tokens.length == 1 && tokens[0].equals(".")) //$NON-NLS-1$
					continue;
			}
			key = key.substring(PROPERTY_SOURCE_PREFIX.length());
			boolean found = false;
			String[] binIncludesTokens = binIncludes.getTokens();
			for (int j = 0; j < binIncludesTokens.length; j++) {
				Pattern pattern = PatternConstructor.createPattern(binIncludesTokens[j], false);
				if (pattern.matcher(key).matches())
					found = true;
			}
			// account for trailing slash on class file folders
			if (!found) {
				IPath path = new Path(key);
				if (path.getFileExtension() == null) {
					if (!key.endsWith("/")) { //$NON-NLS-1$
						key = key + "/"; //$NON-NLS-1$
						for (int j = 0; j < binIncludesTokens.length; j++) {
							Pattern pattern = PatternConstructor.createPattern(binIncludesTokens[j], false);
							if (pattern.matcher(key).matches())
								found = true;
						}
					}
				}
			}
			if (!found)
				prepareError(PROPERTY_BIN_INCLUDES, key, NLS.bind(PDECoreMessages.BuildErrorReporter_binIncludesMissing, key), PDEMarkerFactory.B_ADDITION, fBinInclSeverity, PDEMarkerFactory.CAT_FATAL);
		}
	}

	private void validateMissingLibraries(ArrayList sourceEntryKeys, IClasspathEntry[] cpes) {
		boolean srcFolderExists = false;
		// no need to flag anything if the project contains no source folders.
		for (int j = 0; j < cpes.length; j++) {
			if (cpes[j].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				srcFolderExists = true;
				break;
			}
		}
		if (!srcFolderExists)
			return;

		IPluginModelBase model = PluginRegistry.findModel(fProject);
		if (model == null)
			return;
		if (model instanceof IBundlePluginModelBase && !(model instanceof IBundleFragmentModel)) {
			IBundleModel bm = ((IBundlePluginModelBase) model).getBundleModel();
			IManifestHeader mh = bm.getBundle().getManifestHeader(Constants.BUNDLE_CLASSPATH);
			if ((mh == null || mh.getValue() == null)) {
				if (!sourceEntryKeys.contains(DEF_SOURCE_ENTRY)) {
					prepareError(DEF_SOURCE_ENTRY, null, PDECoreMessages.BuildErrorReporter_sourceMissing, PDEMarkerFactory.NO_RESOLUTION, fSrcLibSeverity, PDEMarkerFactory.CAT_OTHER);
				}
			}
		}
		IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
		for (int i = 0; i < libraries.length; i++) {
			String libname = libraries[i].getName();
			if (libname.equals(".")) { //$NON-NLS-1$
				if (!sourceEntryKeys.contains(DEF_SOURCE_ENTRY)) {
					prepareError(DEF_SOURCE_ENTRY, null, PDECoreMessages.BuildErrorReporter_sourceMissing, PDEMarkerFactory.NO_RESOLUTION, fSrcLibSeverity, PDEMarkerFactory.CAT_OTHER);
					continue;
				}
			} else if (fProject.findMember(libname) != null) {
				// non "." library entries that exist in the workspace
				// don't have to be referenced in the build properties
				continue;
			}
			String sourceEntryKey = PROPERTY_SOURCE_PREFIX + libname;
			if (!sourceEntryKeys.contains(sourceEntryKey) && !containedInFragment(model.getBundleDescription(), libname))
				prepareError(sourceEntryKey, null, NLS.bind(PDECoreMessages.BuildErrorReporter_missingEntry, sourceEntryKey), PDEMarkerFactory.B_ADDITION, PDEMarkerFactory.CAT_OTHER);
		}
	}

	private boolean containedInFragment(BundleDescription description, String libname) {
		if (description == null)
			return false;

		BundleDescription[] fragments = description.getFragments();

		if (fragments == null)
			return false;
		for (int j = 0; j < fragments.length; j++) {
			IPluginModelBase fragmentModel = PluginRegistry.findModel(fragments[j]);
			if (fragmentModel != null && fragmentModel.getUnderlyingResource() != null) {
				IProject project = fragmentModel.getUnderlyingResource().getProject();
				if (project.findMember(libname) != null)
					return true;
				try {
					IBuild build = ClasspathUtilCore.getBuild(fragmentModel);
					if (build != null) {
						IBuildEntry[] entries = build.getBuildEntries();
						for (int i = 0; i < entries.length; i++)
							if (entries[i].getName().equals(PROPERTY_SOURCE_PREFIX + libname))
								return true;
						return false;
					}
				} catch (CoreException e) {
				}
			} else {
				String location = fragments[j].getLocation();
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

	private void validateSourceEntries(ArrayList sourceEntries, IBuildEntry srcExcludes, IClasspathEntry[] cpes) {
		if (sourceEntries == null || sourceEntries.size() == 0)
			return;
		String[] unlisted = PDEBuilderHelper.getUnlistedClasspaths(sourceEntries, fProject, cpes);
		List excludeList = new ArrayList(0);
		if (srcExcludes != null && srcExcludes.getTokens().length > 0) {
			excludeList = Arrays.asList(srcExcludes.getTokens());
		}
		String name = ((IBuildEntry) sourceEntries.get(0)).getName();
		String message = PDECoreMessages.BuildErrorReporter_classpathEntryMissing1;
		if (sourceEntries.size() > 1) {
			name = DEF_SOURCE_ENTRY;
			message = PDECoreMessages.BuildErrorReporter_classpathEntryMissing;
		}
		for (int i = 0; i < unlisted.length; i++) {
			if (unlisted[i] == null || excludeList.contains(unlisted[i]))
				continue;
			BuildProblem error = prepareError(name, unlisted[i], NLS.bind(message, unlisted[i], name), PDEMarkerFactory.B_ADDITION, fSrcLibSeverity, PDEMarkerFactory.CAT_OTHER);
			error.addExtraBuildEntryTokenAttribute(PROPERTY_SRC_EXCLUDES, unlisted[i]);
		}
	}

	// bug 286808
	private void validateSourceFoldersInSrcIncludes(IBuildEntry includes) {
		if (includes == null)
			return;

		List sourceFolderList = new ArrayList(0);
		try {
			IJavaProject javaProject = JavaCore.create(fProject);
			if (javaProject.exists()) {
				IClasspathEntry[] classPathEntries = javaProject.getResolvedClasspath(true);

				for (int index = 0; index < classPathEntries.length; index++) {
					if (classPathEntries[index].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						sourceFolderList.add(classPathEntries[index].getPath());
					}
				}
			}
		} catch (JavaModelException e) { //do nothing
		}

		List reservedTokens = Arrays.asList(RESERVED_NAMES);

		String[] tokens = includes.getTokens();
		for (int i = 0; i < tokens.length; i++) {
			IResource res = fProject.findMember(tokens[i]);
			if (res == null)
				continue;
			String errorMessage = null;
			if (sourceFolderList.contains(res.getFullPath())) {
				errorMessage = PDECoreMessages.BuildErrorReporter_srcIncludesSourceFolder;
			} else if (tokens[i].startsWith(".") || reservedTokens.contains(res.getName().toString().toLowerCase())) { //$NON-NLS-1$
				errorMessage = NLS.bind(PDECoreMessages.BuildErrorReporter_srcIncludesSourceFolder1, res.getName());
			}

			if (errorMessage != null) {
				prepareError(includes.getName(), tokens[i], errorMessage, PDEMarkerFactory.B_REMOVAL, fSrcInclSeverity, PDEMarkerFactory.CAT_OTHER);
			}
		}

	}

	private void validateIncludes(IBuildEntry includes, ArrayList sourceIncludes, int severity) {
		if (includes == null)
			return;
		String[] tokens = includes.getTokens();
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i].trim();
			if (token.indexOf("*") != -1) //$NON-NLS-1$
				// skip entries with wildcards
				continue;
			if (token.equals(".")) //$NON-NLS-1$
				// skip . since we know it exists
				continue;
			if (startsWithAntVariable(token))
				// skip '${x}' variables
				continue;
			IResource member = PDEProject.getBundleRoot(fProject).findMember(token);
			String message = null;
			int fixId = PDEMarkerFactory.NO_RESOLUTION;
			if (member == null) {
				if (sourceIncludes.contains(PROPERTY_SOURCE_PREFIX + token))
					continue;
				if (token.endsWith("/")) //$NON-NLS-1$
					message = NLS.bind(PDECoreMessages.BuildErrorReporter_missingFolder, token);
				else
					message = NLS.bind(PDECoreMessages.BuildErrorReporter_missingFile, token);
				fixId = PDEMarkerFactory.B_REMOVAL;
			} else if (token.endsWith("/") && !(member instanceof IFolder)) { //$NON-NLS-1$
				message = NLS.bind(PDECoreMessages.BuildErrorReporter_entiresMustRefDirs, token);
				fixId = PDEMarkerFactory.B_REMOVE_SLASH_FILE_ENTRY;
			} else if (!token.endsWith("/") && !(member instanceof IFile)) { //$NON-NLS-1$
				message = NLS.bind(PDECoreMessages.BuildErrorReporter_dirsMustEndSlash, token);
				fixId = PDEMarkerFactory.B_APPEND_SLASH_FOLDER_ENTRY;
			}

			if (message != null)
				prepareError(includes.getName(), token, message, fixId, severity, PDEMarkerFactory.CAT_OTHER);
		}
	}

	private boolean startsWithAntVariable(String token) {
		int varStart = token.indexOf("${"); //$NON-NLS-1$
		return varStart != -1 && varStart < token.indexOf("}"); //$NON-NLS-1$
	}

	private void validateDependencyManagement(IBuildEntry bundleList) {
		String[] bundles = bundleList.getTokens();
		for (int i = 0; i < bundles.length; i++) {
			if (PluginRegistry.findModel(bundles[i]) == null)
				prepareError(IBuildEntry.SECONDARY_DEPENDENCIES, bundles[i], NLS.bind(PDECoreMessages.BuildErrorReporter_cannotFindBundle, bundles[i]), PDEMarkerFactory.NO_RESOLUTION, fClasspathSeverity, PDEMarkerFactory.CAT_OTHER);
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
			Map options = project.getOptions(false);
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
						prepareError(PROPERTY_PROJECT_SETTINGS, token, message, PDEMarkerFactory.B_REPLACE, fJavaCompilerSeverity, PDEMarkerFactory.CAT_EE);
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
					prepareError(PROPERTY_PROJECT_SETTINGS, token, message, PDEMarkerFactory.B_JAVA_ADDDITION, fJavaCompilerSeverity, PDEMarkerFactory.CAT_EE);
				}
			} else if (useJavaProjectSettings != null) {
				String message = NLS.bind(PDECoreMessages.BuildErrorReporter_buildEntryInvalidWhenNoProjectSettings, PROPERTY_PROJECT_SETTINGS);
				prepareError(PROPERTY_PROJECT_SETTINGS, null, message, PDEMarkerFactory.B_REMOVAL, fJavaCompilerSeverity, PDEMarkerFactory.CAT_EE);
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
		StringBuffer result = new StringBuffer();
		if (token1 != null && token1.length() > 0) {
			result.append(token1);
		}
		if (token2 != null && token2.length() > 0) {
			if (result.length() > 0)
				result.append(',');
			result.append(token2);
		}
		return result.toString();
	}

	private BuildModel prepareTextBuildModel(IProgressMonitor monitor) {
		try {
			IDocument doc = createDocument(fFile);
			if (doc == null)
				return null;
			BuildModel bm = new BuildModel(doc, true);
			bm.load();
			if (!bm.isLoaded())
				return null;
			return bm;
		} catch (CoreException e) {
			PDECore.log(e);
			return null;
		}
	}

	private void reportErrors(BuildModel bm) {
		if (bm == null)
			return;

		for (int i = 0; i < fProblemList.size(); i++) {
			BuildProblem bp = (BuildProblem) fProblemList.get(i);

			int lineNum;
			IBuildEntry buildEntry = bm.getBuild().getEntry(bp.fEntryName);
			if (buildEntry == null || bp.fEntryName == null)
				// general file case (eg. missing source.* entry)
				lineNum = 1;
			else
				// issue with a particular entry
				lineNum = getLineNumber(buildEntry, bp.fEntryToken);

			if (lineNum > 0) {
				IMarker marker = report(bp.fMessage, lineNum, bp.fFixId, bp.fEntryName, bp.fEntryToken, bp.fSeverity, bp.fCategory);
				if (marker != null && bp.attributes != null) {
					for (Iterator iterator = bp.attributes.keySet().iterator(); iterator.hasNext();) {
						String attribute = (String) iterator.next();
						try {
							marker.setAttribute(attribute, bp.attributes.get(attribute));
						} catch (CoreException e) {
						}
					}
				}
			}
		}
	}

	private int getLineNumber(IBuildEntry ibe, String tokenString) {
		if (!(ibe instanceof BuildEntry))
			return 0;
		BuildEntry be = (BuildEntry) ibe;
		IDocument doc = ((BuildModel) be.getModel()).getDocument();
		try {
			int buildEntryLineNumber = doc.getLineOfOffset(be.getOffset()) + 1;
			if (tokenString == null)
				// we are interested in the build entry name
				// (getLineOfOffset is 0-indexed, need 1-indexed)
				return buildEntryLineNumber;

			// extract the full entry
			String entry = doc.get(be.getOffset(), be.getLength());

			int valueIndex = entry.indexOf('=') + 1;
			if (valueIndex == 0 || valueIndex == entry.length())
				return buildEntryLineNumber;

			// remove the entry name			
			entry = entry.substring(valueIndex);

			int entryTokenOffset = entry.indexOf(tokenString);
			if (entryTokenOffset == -1)
				return buildEntryLineNumber;

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
					if (entry.trim().equals(tokenString))
						return doc.getLineOfOffset(currOffset + entry.indexOf(tokenString)) + 1;
					return buildEntryLineNumber;
				}

				String ct = entry.substring(0, cci).trim();
				if (ct.equals(tokenString))
					return doc.getLineOfOffset(currOffset) + 1;

				entry = entry.substring(++cci);
				currOffset += cci;
			}

		} catch (BadLocationException e) {
		}
		return 0;
	}

	protected BuildProblem prepareError(String name, String token, String message, int fixId, String category) {
		return prepareError(name, token, message, fixId, fBuildSeverity, category);
	}

	protected BuildProblem prepareError(String name, String token, String message, int fixId, int severity, String category) {
		BuildProblem bp = new BuildProblem(name, token, message, fixId, severity, category);
		for (int i = 0; i < fProblemList.size(); i++) {
			BuildProblem listed = (BuildProblem) fProblemList.get(i);
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
	private IMarker report(String message, int line, int problemID, String buildEntry, String buildToken, int severity, String category) {
		IMarker marker = report(message, line, severity, problemID, category);
		if (marker != null) {
			try {
				marker.setAttribute(PDEMarkerFactory.BK_BUILD_ENTRY, buildEntry);
				marker.setAttribute(PDEMarkerFactory.BK_BUILD_TOKEN, buildToken);
			} catch (CoreException e) {
			}
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
}
