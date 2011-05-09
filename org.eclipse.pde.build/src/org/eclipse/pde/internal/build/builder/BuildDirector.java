/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * GmbH - internationalization implementation (bug 150933)
 *******************************************************************************/
package org.eclipse.pde.internal.build.builder;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.equinox.p2.publisher.eclipse.FeatureEntry;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.build.builder.ClasspathComputer3_0.ClasspathElement;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.osgi.framework.Version;

/**
 * Generates build.xml script for features.
 */
public class BuildDirector extends AbstractBuildScriptGenerator {

	private static final int QUALIFIER_SUFFIX_VERSION = 2;

	// GENERATION FLAGS
	/**
	 * Indicates whether scripts for this feature included features should be
	 * generated.
	 */
	protected boolean analyseIncludedFeatures = false;
	/**
	 * Indicates whether scripts for this feature children' should be
	 * generated.
	 */
	protected boolean analysePlugins = true;
	/** Indicates whether the feature is binary */
	protected boolean binaryFeature = true;
	/** Indicates if the build scripts files should be produced or not */
	private boolean scriptGeneration = true;

	//FEATURE RELATED INFORMATION
	/** The identifier of the feature that the build script is being generated for. */
	protected String featureIdentifier;
	protected String searchedVersion;

	protected SourceFeatureInformation sourceToGather = new SourceFeatureInformation();
	private boolean generateVersionSuffix = false;
	protected boolean signJars = false;
	protected String product = null;
	protected boolean generateJnlp = false;
	protected boolean workspaceBinaries = false;
	private boolean sourceReferences = false;

	public static boolean p2Gathering = false;

	public BuildDirector() {
		super();
	}

	/**
	 * Constructor FeatureBuildScriptGenerator.
	 */
	public BuildDirector(String featureId, String versionId, AssemblyInformation informationGathering) throws CoreException {
		if (featureId == null) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Messages.error_missingFeatureId, null));
		}
		this.featureIdentifier = featureId;
		this.searchedVersion = versionId;
		assemblyData = informationGathering;
	}

	public BuildDirector(AssemblyInformation assemblageInformation) {
		this.assemblyData = assemblageInformation;
	}

	private final Map extractedLocations = new HashMap();

	public String getExtractedRoot(ClasspathElement element) {
		if (element.getSubPath() == null)
			return element.getPath();

		String absolute = element.getAbsolutePath();
		if (extractedLocations.containsKey(absolute)) {
			return (String) extractedLocations.get(absolute);
		}

		//Use the jar name, append a suffix if that name is already taken
		String name = new File(absolute).getName();
		if (name.endsWith(".jar")) //$NON-NLS-1$
			name = name.substring(0, name.length() - 4);
		String destination = name;
		while (extractedLocations.containsValue(destination)) {
			destination = name + '_' + Integer.toHexString(destination.hashCode());
		}

		extractedLocations.put(absolute, destination);
		return destination;
	}

	/**
	 * Returns a list of BundleDescription objects representing the elements delivered by the feature. 
	 *  
	 * @return List of BundleDescription
	 * @throws CoreException
	 */
	protected Set computeElements(BuildTimeFeature feature) throws CoreException {
		Set computedElements = new LinkedHashSet(5);
		Properties featureProperties = getBuildProperties(feature);
		FeatureEntry[] pluginList = feature.getPluginEntries();
		for (int i = 0; i < pluginList.length; i++) {
			FeatureEntry entry = pluginList[i];
			BundleDescription model;
			if (selectConfigs(entry).size() == 0)
				continue;

			String versionRequested = entry.getVersion();
			model = getSite(false).getRegistry().getResolvedBundle(entry.getId(), versionRequested);
			//we prefer a newly generated source plugin over a preexisting binary one. 
			if ((model == null || Utils.isBinary(model)) && featureProperties.containsKey(GENERATION_SOURCE_PLUGIN_PREFIX + entry.getId())) {
				boolean individual = useIndividualSource(featureProperties);
				String[] extraEntries = Utils.getArrayFromString(featureProperties.getProperty(GENERATION_SOURCE_PLUGIN_PREFIX + entry.getId()));
				if (individual) {
					BundleDescription originalBundle = getSite(false).getRegistry().getResolvedBundle(extraEntries[0]);
					if (originalBundle != null) {
						if (!Utils.isBinary(originalBundle))
							generateEmbeddedSource(entry, extraEntries, individual);
						else if (model == null) {
							String message = NLS.bind(Messages.exception_unableToGenerateSourceFromBinary, entry.getId(), originalBundle.getSymbolicName() + "_" + originalBundle.getVersion()); //$NON-NLS-1$
							IStatus status = new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, message, null);
							throw new CoreException(status);
						}
					}
				} else {
					generateEmbeddedSource(entry, extraEntries, individual);
				}
				model = getSite(false).getRegistry().getResolvedBundle(entry.getId(), versionRequested);
			}
			if (model == null) {
				getSite(false).missingPlugin(entry.getId(), versionRequested, feature, true);
			}

			associateModelAndEntry(model, entry);

			computedElements.add(model);
			collectElementToAssemble(pluginList[i]);
		}
		return computedElements;
	}

	private boolean useIndividualSource(Properties featureProperties) {
		boolean individual = Boolean.valueOf(featureProperties.getProperty(PROPERTY_INDIVIDUAL_SOURCE)).booleanValue();
		return individual || AbstractScriptGenerator.getPropertyAsBoolean(PROPERTY_INDIVIDUAL_SOURCE);
	}

	private void associateModelAndEntry(BundleDescription model, FeatureEntry entry) {
		Properties bundleProperties = ((Properties) model.getUserObject());
		if (bundleProperties == null) {
			bundleProperties = new Properties();
			model.setUserObject(bundleProperties);
		}
		Set entries = (Set) bundleProperties.get(PLUGIN_ENTRY);
		if (entries == null) {
			entries = new HashSet();
			bundleProperties.put(PLUGIN_ENTRY, entries);
		}
		entries.add(entry);
	}

	private void generateEmbeddedSource(FeatureEntry pluginEntry, String[] extraEntries, boolean individual) throws CoreException {
		if (individual) {
			BundleDescription originalBundle = getSite(false).getRegistry().getResolvedBundle(extraEntries[0]);
			if (originalBundle != null) {
				SourceGenerator sourceGenerator = new SourceGenerator();
				sourceGenerator.setExtraEntries(extraEntries);
				sourceGenerator.setDirector(this);
				sourceGenerator.setIndividual(individual);
				sourceGenerator.generateSourcePlugin(pluginEntry, originalBundle);
				return;
			}
		}
		/* else do it the old way */
		BuildTimeFeature baseFeature = getSite(false).findFeature(extraEntries[0], null, true);
		if (baseFeature != null)
			generateSourceFeature(baseFeature, pluginEntry.getId(), extraEntries, false);
	}

	/**
	 * Set the boolean for whether or not children scripts should be generated.
	 * 
	 * @param generate
	 *                   <code>true</code> if the children scripts should be
	 *                   generated, <code>false</code> otherwise
	 */
	public void setAnalyseChildren(boolean generate) {
		analysePlugins = generate;
	}

	public void generate() throws CoreException {
		if (workingDirectory == null) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_BUILDDIRECTORY_LOCATION_MISSING, Messages.error_missingInstallLocation, null));
		}

		BuildTimeFeature feature = getSite(false).findFeature(featureIdentifier, searchedVersion, true);
		generate(feature);
	}

	/**
	 * @see AbstractScriptGenerator#generate()
	 */
	public void generate(BuildTimeFeature feature) throws CoreException {
		generate(feature, true);
	}

	protected void generate(BuildTimeFeature feature, boolean generateProductFiles) throws CoreException {
		if (analyseIncludedFeatures)
			generateIncludedFeatureBuildFile(feature);
		if (analysePlugins)
			generateChildrenScripts(feature);

		collectElementToAssemble(feature);

		if (scriptGeneration) {
			FeatureBuildScriptGenerator featureScriptGenerator = new FeatureBuildScriptGenerator(feature);
			featureScriptGenerator.setDirector(this);
			featureScriptGenerator.setBuildSiteFactory(siteFactory);
			featureScriptGenerator.setGenerateProductFiles(generateProductFiles);
			featureScriptGenerator.generate();
		}
	}

	protected void generateSourceFeature(BuildTimeFeature baseFeature, String sourceFeatureName, String[] extraEntries, boolean individual) throws CoreException {
		SourceGenerator sourceGenerator = new SourceGenerator();
		sourceGenerator.setExtraEntries(extraEntries);
		sourceGenerator.setDirector(this);
		sourceGenerator.setIndividual(individual);
		sourceGenerator.generateSourceFeature(baseFeature, sourceFeatureName);
	}

	protected void generateIncludedFeatureBuildFile(BuildTimeFeature feature) throws CoreException {
		FeatureEntry[] referencedFeatures = feature.getIncludedFeatureReferences();
		for (int i = 0; i < referencedFeatures.length; i++) {
			String featureId = referencedFeatures[i].getId();
			String featureVersion = referencedFeatures[i].getVersion();

			BuildTimeFeature nestedFeature = null;
			Properties featureProperties = getBuildProperties(feature);
			boolean doSourceFeatureGeneration = featureProperties.containsKey(GENERATION_SOURCE_FEATURE_PREFIX + featureId);
			if (doSourceFeatureGeneration) {
				String[] extraEntries = Utils.getArrayFromString(featureProperties.getProperty(GENERATION_SOURCE_FEATURE_PREFIX + featureId));
				nestedFeature = getSite(false).findFeature(extraEntries[0], featureVersion, true);
				generateSourceFeature(nestedFeature, featureId, extraEntries, useIndividualSource(featureProperties));
			}

			try {
				nestedFeature = getSite(false).findFeature(featureId, featureVersion, true);
				generate(nestedFeature, false);
			} catch (CoreException exception) {
				absorbExceptionIfOptionalFeature(referencedFeatures[i], exception);
			}
		}
	}

	private void absorbExceptionIfOptionalFeature(FeatureEntry entry, CoreException toAbsorb) throws CoreException {
		if (toAbsorb.getStatus().getCode() != EXCEPTION_FEATURE_MISSING || (toAbsorb.getStatus().getCode() == EXCEPTION_FEATURE_MISSING && !entry.isOptional()))
			throw toAbsorb;
	}

	/**
	 * @throws CoreException
	 */
	private void generateChildrenScripts(BuildTimeFeature feature) throws CoreException {
		Set plugins = computeElements(feature);
		String suffix = generateFeatureVersionSuffix(feature);
		if (suffix != null) {
			Version versionId = new Version(feature.getVersion());
			String qualifier = versionId.getQualifier();
			qualifier = qualifier.substring(0, feature.getContextQualifierLength());
			qualifier = qualifier + '-' + suffix;
			versionId = new Version(versionId.getMajor(), versionId.getMinor(), versionId.getMicro(), qualifier);
			String newVersion = versionId.toString();
			feature.setVersion(newVersion);
			//initializeFeatureNames(); //reset our variables
		}
		generateModels(Utils.extractPlugins(getSite(false).getRegistry().getSortedBundles(), plugins));
	}

	// Encode a non-negative number as a variable length string, with the
	// property that if X > Y then the encoding of X is lexicographically
	// greater than the enocding of Y.  This is accomplished by encoding the
	// length of the string at the beginning of the string.  The string is a
	// series of base 64 (6-bit) characters.  The first three bits of the first
	// character indicate the number of additional characters in the string.
	// The last three bits of the first character and all of the rest of the
	// characters encode the actual value of the number.  Examples:
	//     0 --> 000 000 --> "-"
	//     7 --> 000 111 --> "6"
	//     8 --> 001 000 001000 --> "77"
	//    63 --> 001 000 111111 --> "7z"
	//    64 --> 001 001 000000 --> "8-"
	//   511 --> 001 111 111111 --> "Dz"
	//   512 --> 010 000 001000 000000 --> "E7-"
	//   2^32 - 1 --> 101 011 111111 ... 111111 --> "fzzzzz"
	//   2^45 - 1 --> 111 111 111111 ... 111111 --> "zzzzzzzz"
	// (There are some wasted values in this encoding.  For example,
	// "7-" through "76" and "E--" through "E6z" are not legal encodings of
	// any number.  But the benefit of filling in those wasted ranges would not
	// be worth the added complexity.)
	private static String lengthPrefixBase64(long number) {
		int length = 7;
		for (int i = 0; i < 7; ++i) {
			if (number < (1L << ((i * 6) + 3))) {
				length = i;
				break;
			}
		}
		StringBuffer result = new StringBuffer(length + 1);
		result.append(Utils.base64Character((length << 3) + (int) ((number >> (6 * length)) & 0x7)));
		while (--length >= 0) {
			result.append(Utils.base64Character((int) ((number >> (6 * length)) & 0x3f)));
		}
		return result.toString();
	}

	private static void appendEncodedCharacter(StringBuffer buffer, int c) {
		while (c > 62) {
			buffer.append('z');
			c -= 63;
		}
		buffer.append(Utils.base64Character(c));
	}

	private static int getIntProperty(String property, int defaultValue) {
		int result = defaultValue;
		if (property != null) {
			try {
				result = Integer.parseInt(property);
				if (result < 1) {
					// It has to be a positive integer.  Use the default.
					result = defaultValue;
				}
			} catch (NumberFormatException e) {
				// Leave as default value
			}
		}
		return result;
	}

	protected String generateFeatureVersionSuffix(BuildTimeFeature buildFeature) throws CoreException {
		if (!generateVersionSuffix || buildFeature.getContextQualifierLength() == -1) {
			return null; // do nothing
		}

		Properties properties = getBuildProperties(buildFeature);
		int significantDigits = getIntProperty((String) properties.get(PROPERTY_SIGNIFICANT_VERSION_DIGITS), -1);
		if (significantDigits == -1)
			significantDigits = getIntProperty(AbstractScriptGenerator.getImmutableAntProperty(PROPERTY_SIGNIFICANT_VERSION_DIGITS), Integer.MAX_VALUE);
		int maxGeneratedLength = getIntProperty((String) properties.get(PROPERTY_GENERATED_VERSION_LENGTH), -1);
		if (maxGeneratedLength == -1)
			maxGeneratedLength = getIntProperty(AbstractScriptGenerator.getImmutableAntProperty(PROPERTY_GENERATED_VERSION_LENGTH), 28);

		long majorSum = 0L;
		long minorSum = 0L;
		long serviceSum = 0L;

		// Include the version of this algorithm as part of the suffix, so that
		// we have a way to make sure all suffixes increase when the algorithm
		// changes.
		majorSum += QUALIFIER_SUFFIX_VERSION;

		FeatureEntry[] referencedFeatures = buildFeature.getIncludedFeatureReferences();
		FeatureEntry[] pluginList = buildFeature.getRawPluginEntries();
		int numElements = pluginList.length + referencedFeatures.length;
		if (numElements == 0) {
			// Empty feature.
			return null;
		}
		String[] qualifiers = new String[numElements];
		int idx = -1;

		// Loop through the included features, adding the version number parts
		// to the running totals and storing the qualifier suffixes.
		for (int i = 0; i < referencedFeatures.length; i++) {
			BuildTimeFeature refFeature = getSite(false).findFeature(referencedFeatures[i].getId(), null, false);
			if (refFeature == null) {
				qualifiers[++idx] = ""; //$NON-NLS-1$
				continue;
			}

			Version version = new Version(refFeature.getVersion());
			//PluginVersionIdentifier version = refFeature.getVersion();
			majorSum += version.getMajor();
			minorSum += version.getMinor();
			serviceSum += version.getMicro();
			int contextLength = refFeature.getContextQualifierLength();
			++contextLength; //account for the '-' separating the context qualifier and suffix
			String qualifier = version.getQualifier();
			// The entire qualifier of the nested feature is often too long to
			// include in the suffix computation for the containing feature,
			// and using it would result in extremely long qualifiers for
			// umbrella features.  So instead we want to use just the suffix
			// part of the qualifier, or just the context part (if there is no
			// suffix part).  See bug #162022.
			if (qualifier.length() > contextLength) {
				// Use the suffix part
				qualifiers[++idx] = qualifier.substring(contextLength);
			} else {
				// Use the context part
				qualifiers[++idx] = qualifier;
			}
		}

		// Loop through the included plug-ins and fragments, adding the version
		// number parts to the running totals and storing the qualifiers.

		for (int i = 0; i < pluginList.length; i++) {
			FeatureEntry entry = pluginList[i];

			String versionRequested = entry.getVersion();
			BundleDescription model = getSite(false).getRegistry().getBundle(entry.getId(), versionRequested, false);
			Version version = null;
			if (model != null) {
				version = model.getVersion();
			} else {
				if (versionRequested.endsWith(PROPERTY_QUALIFIER)) {
					int resultingLength = versionRequested.length() - PROPERTY_QUALIFIER.length();
					if (versionRequested.charAt(resultingLength - 1) == '.')
						resultingLength--;
					versionRequested = versionRequested.substring(0, resultingLength);
				}
				version = new Version(versionRequested);
			}

			majorSum += version.getMajor();
			minorSum += version.getMinor();
			serviceSum += version.getMicro();
			qualifiers[++idx] = version.getQualifier();
		}

		// Limit the qualifiers to the specified number of significant digits,
		// and figure out what the longest qualifier is.
		int longestQualifier = 0;
		for (int i = 0; i < numElements; ++i) {
			if (qualifiers[i].length() > significantDigits) {
				qualifiers[i] = qualifiers[i].substring(0, significantDigits);
			}
			if (qualifiers[i].length() > longestQualifier) {
				longestQualifier = qualifiers[i].length();
			}
		}

		StringBuffer result = new StringBuffer();

		// Encode the sums of the first three parts of the version numbers.
		result.append(lengthPrefixBase64(majorSum));
		result.append(lengthPrefixBase64(minorSum));
		result.append(lengthPrefixBase64(serviceSum));

		if (longestQualifier > 0) {
			// Calculate the sum at each position of the qualifiers.
			int[] qualifierSums = new int[longestQualifier];
			for (int i = 0; i < numElements; ++i) {
				for (int j = 0; j < qualifiers[i].length(); ++j) {
					qualifierSums[j] += Utils.qualifierCharValue(qualifiers[i].charAt(j));
				}
			}
			// Normalize the sums to be base 65.
			int carry = 0;
			for (int k = longestQualifier - 1; k >= 1; --k) {
				qualifierSums[k] += carry;
				carry = qualifierSums[k] / 65;
				qualifierSums[k] = qualifierSums[k] % 65;
			}
			qualifierSums[0] += carry;

			// Always use one character for overflow.  This will be handled
			// correctly even when the overflow character itself overflows.
			result.append(lengthPrefixBase64(qualifierSums[0]));
			for (int m = 1; m < longestQualifier; ++m) {
				appendEncodedCharacter(result, qualifierSums[m]);
			}
		}
		// It is safe to strip any '-' characters from the end of the suffix.
		// (This won't happen very often, but it will save us a character or
		// two when it does.)
		while (result.length() > 0 && result.charAt(result.length() - 1) == '-') {
			result.deleteCharAt(result.length() - 1);
		}

		// If the resulting suffix is too long, shorten it to the designed length.
		if (maxGeneratedLength > result.length()) {
			return result.toString();
		}
		return result.substring(0, maxGeneratedLength);
	}

	/**
	 * @param models
	 * @throws CoreException
	 */
	private void generateModels(List models) throws CoreException {
		if (scriptGeneration == false)
			return;
		if (binaryFeature == false || models.isEmpty())
			return;

		Set generatedScripts = new HashSet(models.size());
		for (Iterator iterator = models.iterator(); iterator.hasNext();) {
			BundleDescription model = (BundleDescription) iterator.next();
			if (generatedScripts.contains(model))
				continue;
			generatedScripts.add(model);

			//Get the corresponding plug-in entries (from a feature object) associated with the model
			//and generate the script if one the configuration is being built. The generated scripts
			//are configuration agnostic so we only generate once.
			Set matchingEntries = (Set) ((Properties) model.getUserObject()).get(PLUGIN_ENTRY);
			if (matchingEntries == null || matchingEntries.isEmpty())
				return;

			Iterator entryIter = matchingEntries.iterator();
			FeatureEntry correspondingEntry = (FeatureEntry) entryIter.next();
			List list = selectConfigs(correspondingEntry);
			if (list.size() == 0)
				continue;

			ModelBuildScriptGenerator generator = new ModelBuildScriptGenerator();
			generator.setBuildSiteFactory(siteFactory);
			generator.setCompiledElements(getCompiledElements());
			generator.setIgnoreMissingPropertiesFile(isIgnoreMissingPropertiesFile());
			generator.setModel(model); // setModel has to be called before configurePersistentProperties because it reads the model's properties
			generator.setFeatureGenerator(this);
			generator.setPluginPath(getPluginPath());
			generator.setBuildingOSGi(isBuildingOSGi());
			generator.setDevEntries(devEntries);
			generator.includePlatformIndependent(isPlatformIndependentIncluded());
			generator.setSignJars(signJars);
			generator.setAssociatedEntry(correspondingEntry);
			generator.setGenerateSourceReferences(sourceReferences);
			generator.generate();
		}

	}

	/**
	 * Set this object's feature id to be the given value.
	 * 
	 * @param featureID the feature id
	 * @throws CoreException if the given feature id is <code>null</code>
	 */
	public void setFeature(String featureID) throws CoreException {
		if (featureID == null) {
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Messages.error_missingFeatureId, null));
		}
		this.featureIdentifier = featureID;
	}

	/**
	 * Return a properties object constructed from the build.properties file
	 * for the given feature. If no file exists, then an empty properties
	 * object is returned.
	 * 
	 * @return Properties the feature's build.properties
	 * @see Feature
	 */
	protected Properties getBuildProperties() {
		throw new UnsupportedOperationException();
	}

	protected Properties getBuildProperties(BuildTimeFeature feature) throws CoreException {
		return readProperties(feature.getRootLocation(), PROPERTIES_FILE, isIgnoreMissingPropertiesFile() ? IStatus.OK : IStatus.WARNING);
	}

	public void setGenerateIncludedFeatures(boolean recursiveGeneration) {
		analyseIncludedFeatures = recursiveGeneration;
	}

	protected void collectElementToAssemble(BuildTimeFeature featureToCollect) throws CoreException {
		if (assemblyData == null || featureToCollect == null)
			return;

		String binIncludes = getBuildProperties(featureToCollect).getProperty(PROPERTY_BIN_INCLUDES);

		/* collect binary features, and any feature defining bin.includes */
		if (featureToCollect.isBinary() || (binIncludes != null && binIncludes.length() > 0)) {
			basicCollectElementToAssemble(featureToCollect);
			return;
		}

		//at this point, we have a non-binary feature with empty bin includes
		//when building p2, containers (features without bin.includes) need to be collected, 
		//with the exception of the pde generated containers.
		if (!BuildDirector.p2Gathering)
			return;

		if (featureToCollect.getId().equals(CONTAINER_FEATURE) || featureToCollect.getId().equals(UI_CONTAINER_FEATURE))
			return;

		basicCollectElementToAssemble(featureToCollect);
	}

	private void basicCollectElementToAssemble(BuildTimeFeature featureToCollect) {
		if (assemblyData == null)
			return;
		List correctConfigs = selectConfigs(featureToCollect);
		// Here, we could sort if the feature is a common one or not by
		// comparing the size of correctConfigs
		for (Iterator iter = correctConfigs.iterator(); iter.hasNext();) {
			Config config = (Config) iter.next();
			assemblyData.addFeature(config, featureToCollect);
		}
	}

	/**
	 * Method setSourceToGather.
	 * 
	 * @param sourceToGather
	 */
	public void setSourceToGather(SourceFeatureInformation sourceToGather) {
		this.sourceToGather = sourceToGather;
	}

	/**
	 * Sets the binaryFeatureGeneration.
	 * 
	 * @param binaryFeatureGeneration
	 *                   The binaryFeatureGeneration to set
	 */
	public void setBinaryFeatureGeneration(boolean binaryFeatureGeneration) {
		this.binaryFeature = binaryFeatureGeneration;
	}

	/**
	 * Sets the scriptGeneration.
	 * 
	 * @param scriptGeneration
	 *                   The scriptGeneration to set
	 */
	public void setScriptGeneration(boolean scriptGeneration) {
		this.scriptGeneration = scriptGeneration;
	}

	/**
	 * Sets whether or not to generate JNLP manifests
	 * 
	 * @param value whether or not to generate JNLP manifests
	 */
	public void setGenerateJnlp(boolean value) {
		generateJnlp = value;
	}

	/**
	 * Sets whether or not to sign any constructed jars.
	 * 
	 * @param value whether or not to sign any constructed JARs
	 */
	public void setSignJars(boolean value) {
		signJars = value;
	}

	public void setGenerateSourceReferences(boolean generateSourceRef) {
		this.sourceReferences = generateSourceRef;
	}

	/**
	 * Sets whether or not to generate the feature version suffix
	 * 
	 * @param value whether or not to generate the feature version suffix
	 */
	public void setGenerateVersionSuffix(boolean value) {
		generateVersionSuffix = value;
	}

	/**
	 * Set the location of the .product file
	 * @param product the location of the .product file
	 */
	public void setProduct(String product) {
		this.product = product;
	}

	protected void collectElementToAssemble(FeatureEntry entryToCollect) throws CoreException {
		if (assemblyData == null)
			return;
		List correctConfigs = selectConfigs(entryToCollect);
		String versionRequested = entryToCollect.getVersion();
		BundleDescription effectivePlugin = null;
		effectivePlugin = getSite(false).getRegistry().getResolvedBundle(entryToCollect.getId(), versionRequested);
		for (Iterator iter = correctConfigs.iterator(); iter.hasNext();) {
			assemblyData.addPlugin((Config) iter.next(), effectivePlugin);
		}
	}

	public String getProduct() {
		return product;
	}

	public boolean getSignJars() {
		return signJars;
	}

	public boolean getGenerateJnlp() {
		return generateJnlp;
	}

	public AssemblyInformation getAssemblyData() {
		return assemblyData;
	}

	public boolean useWorkspaceBinaries() {
		return workspaceBinaries;
	}

	public void setUseWorkspaceBinaries(boolean workspaceBinaries) {
		this.workspaceBinaries = workspaceBinaries;
	}
}