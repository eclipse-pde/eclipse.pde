package org.eclipse.pde.internal.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import java.net.*;
import java.util.*;
import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.internal.plugins.PluginParser;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.core.Feature;
import org.eclipse.update.internal.core.FeatureExecutableFactory;
import org.xml.sax.InputSource;

public class FeatureBuildScriptGenerator extends PluginTool {
	private PluginModel plugins[] = null;
	private PluginModel fragments[] = null;
	private boolean generateChildren = false;
	private String featureLocation = null;
	private Feature feature = null;
	private boolean merge = false;
	
	// constants
	private static final String SWITCH_FEATURE = "-feature";
	
	// output filenames
	private static final String DEFAULT_FILENAME_SRC = "source.jar";
	private static final String DEFAULT_FILENAME_LOG = "logs.zip";
	private static final String DEFAULT_FILENAME_DOC = "docs.zip";
	private static final String DEFAULT_FILENAME_MAIN = "build.xml";
	
	// targets
	private static final String TARGET_ALL_TEMPLATE = "all-template";
	private static final String TARGET_PLUGIN_TEMPLATE = "plugin-template";
	private static final String TARGET_FRAGMENT_TEMPLATE = "fragment-template";

public FeatureBuildScriptGenerator() {
	super();
}
public FeatureBuildScriptGenerator(String featureLocation, String installPath, PluginRegistryModel registry) {
	super();
	this.featureLocation = featureLocation;
	setRegistry(registry);
	setInstall(installPath);
}
protected PluginModel[] determineFragments() {
	if (fragments == null) {
		if (feature != null)
			fragments = readElements(true);
		else
			plugins = new PluginModel[0];
	}
	return fragments;
}
protected String determineFulllId() {
	return feature == null ? "" : feature.getFeatureIdentifier();
}
protected PluginModel[] determinePlugins() {
	if (plugins == null) {
		if (feature != null)
			plugins = readElements(false);
		else
			plugins = new PluginModel[0];
	}
	return plugins;
}

public IStatus execute() {
	if (!readFeature())
		return getProblems();
	if (generateChildren) {
		PluginModel plugins[] = determinePlugins();
		PluginBuildScriptGenerator pluginGenerator = new PluginBuildScriptGenerator(plugins,getRegistry());
		pluginGenerator.setDevEntries(getDevEntries());
		addProblems(pluginGenerator.execute());
		
		PluginModel fragments[] = determineFragments();
		FragmentBuildScriptGenerator fragmentGenerator = new FragmentBuildScriptGenerator(fragments,getRegistry());
		fragmentGenerator.setDevEntries(getDevEntries());
		addProblems(fragmentGenerator.execute());
	}

	try {
		PrintWriter output = openMainOutput();
		try {
			generateBuildScript(output);
		} finally {
			output.flush();
			output.close();
		}
	} catch (IOException e) {
		getPluginLog().log(new Status(IStatus.ERROR,PI_PDECORE,EXCEPTION_OUTPUT,Policy.bind("exception.output"),e));
	}
	return getProblems();
}
protected void generateAllTarget(PrintWriter output) {
	ArrayList targets = new ArrayList(5);
	targets.add("init");
//	targets.add(TARGET_JAR);
//	targets.add(TARGET_BIN);
// 	targets.add(TARGET_SRC);
//	targets.add(TARGET_LOG);
//	targets.add(TARGET_DOC);
	output.println();
	output.println("  <target name=\"" + TARGET_ALL + "\" depends=\"" + getStringFromCollection(targets, "", "", ",") + "\">");
	output.println("  </target>");
}
protected void generateAllTemplateTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + TARGET_ALL_TEMPLATE + "\" depends=\"init," + TARGET_PLUGIN_TEMPLATE + "," + TARGET_FRAGMENT_TEMPLATE + "\">");
	output.println("  </target>");
}
protected void generateBinTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + TARGET_BIN + "\">");
	output.println("  </target>");
}

protected void generateBinGatherDataTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + "bin.gather.data" + "\" depends=\"init\" if=\"feature.base\">");
	String explode = getSubstitution(feature, "explode");
	if (explode != null) {
		for (Enumeration i = new StringTokenizer(explode, ","); i.hasMoreElements();) {
			String zipfile = (String) i.nextElement();
			output.println("    <ant antfile=\"${template}\" target=\"" + "mapperCopy" + "\">");
			String inclusions = getSubstitution(feature, zipfile);
			String mapping = inclusions;
			output.println("      <property name=\"includes\" value=\"" + inclusions + "\"/>");
			String exclusions = "";
			output.println("      <property name=\"excludes\" value=\"" + exclusions + "\"/>");
			output.println("      <property name=\"dest\" value=\"${feature.base}/" + "" + "\"/>");
			output.println("      <property name=\"mapping\" value=\"" + mapping + "\"/>");
			output.println("    </ant>");
		}
	}
	output.println("  </target>");
}
protected void generateBinGatherPartsTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + "bin.gather.parts" + "\" depends=\"init\" if=\"feature.base\">");
	output.println("    <antcall target=\"" + "children" + "\">");
	output.println("      <param name=\"target\" value=\"" + "bin.gather.parts" +"\"/>");
	output.println("      <param name=\"destroot\" value=\"${feature.base}\"/>");
	output.println("    </antcall>");

	output.println("    <property name=\"feature.auto.includes\" value=\"feature.xml\"/>");
	output.println("    <property name=\"feature.auto.excludes\" value=\"\"/>");
	output.println("    <ant antfile=\"${template}\" target=\"includesExcludesCopy\">");
	String inclusions = getSubstitution(feature, BIN_INCLUDES);
	if (inclusions == null)
		inclusions = "${feature.auto.includes}";
	output.println("      <property name=\"includes\" value=\"" + inclusions + "\"/>");
	String exclusions = getSubstitution(feature, BIN_EXCLUDES);
	if (exclusions == null)
		exclusions = "${feature.auto.excludes}";
	output.println("      <property name=\"excludes\" value=\"" + exclusions + "\"/>");
	output.println("      <property name=\"dest\" value=\"${feature.base}/install/features/${feature}\"/>");
	output.println("    </ant>");
	output.println("  </target>");
}
protected void generateBinGatherWholeTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + "bin.gather.whole" + "\" depends=\"init\" if=\"feature.base\">");
	output.println("    <antcall target=\"" + "children" + "\">");
	output.println("      <param name=\"target\" value=\"" + "bin.gather.whole" + "\"/>");
	output.println("      <param name=\"destroot\" value=\"${feature.base}/plugins/\"/>");
	output.println("    </antcall>");
	output.println("    <copy file=\"${feature}_${featureVersion}.jar\" todir=\"${feature.base}/features\"/>");
	Map properties = getProperties(feature);
	for (Iterator i = properties.keySet().iterator(); i.hasNext();) {
		String element = (String) i.next();
		if (element.toLowerCase().endsWith(".zip"))
			output.println("    <copy file=\"" + element + "\" todir=\"${feature.base}/features\"/>");
	}
	output.println("  </target>");
}
protected void generateBinZipTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + TARGET_BIN_ZIP + "\" depends=\"init\">");
	
	output.println("    <antcall target=\"" + "children" + "\">");
	output.println("      <param name=\"target\" value=\"" + TARGET_BIN_ZIP +"\"/>");
	output.println("    </antcall>");

	output.println("    <property name=\"feature.base\" value=\"${basedir}/bin.zip.pdetemp\"/>");
	output.println("    <delete dir=\"${feature.base}\"/>");
	output.println("    <mkdir dir=\"${feature.base}\"/>");

	// be sure to call the gather with children turned off.  The only way to do this is 
	// to clear all inherited values.  Must remember to setup anything that is really expected.
	output.println("    <antcall target=\"bin.gather.parts\" inheritAll=\"false\">");
	output.println("      <param name=\"feature.base\" value=\"${feature.base}\"/>");
	output.println("    </antcall>");
	
	// call runJar on template.xml in case we want to use an external program
	output.println("    <ant antfile=\"${template}\" target=\"runJar\">");
	output.println("      <property name=\"resultingFile\" value=\"${feature}_${featureVersion}.jar\"/>");
	output.println("      <property name=\"targetDir\" value=\"${feature.base}/install/features/${feature}\"/>");
	output.println("    </ant>");
	
	output.println("    <delete dir=\"${feature.base}\"/>");
	output.println("  </target>");
}
protected void generateBinDistPartsTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + "bin.dist.parts" + "\" depends=\"init\">");

	output.println("    <property name=\"feature.base\" value=\"${basedir}/bin.zip.pdetemp\"/>");
	output.println("    <delete dir=\"${feature.base}\"/>");
	output.println("    <mkdir dir=\"${feature.base}\"/>");

	output.println("    <antcall target=\"bin.gather.parts\">");
	output.println("      <param name=\"includeChildren\" value=\"true\"/>");
	output.println("    </antcall>");
	output.println("    <antcall target=\"bin.gather.data\"/>");
	
	// call runJar on template.xml in case we want to use an external program
	output.println("    <ant antfile=\"${template}\" target=\"runZip\">");
	output.println("      <property name=\"resultingFile\" value=\"${feature}_${featureVersion}.bin.dist.zip\"/>");
	output.println("      <property name=\"targetDir\" value=\"${feature.base}\"/>");
	output.println("    </ant>");
	
	output.println("    <delete dir=\"${feature.base}\"/>");
	output.println("  </target>");
}
protected void generateBinDistWholeTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + "bin.dist.whole" + "\" depends=\"init\">");

	output.println("    <property name=\"feature.base\" value=\"${basedir}/bin.zip.pdetemp\"/>");
	output.println("    <delete dir=\"${feature.base}\"/>");
	output.println("    <mkdir dir=\"${feature.base}\"/>");

	output.println("    <antcall target=\"bin.gather.whole\">");
	output.println("      <param name=\"includeChildren\" value=\"true\"/>");
	output.println("    </antcall>");
	
	// call runJar on template.xml in case we want to use an external program
	output.println("    <ant antfile=\"${template}\" target=\"runZip\">");
	output.println("      <property name=\"resultingFile\" value=\"${feature}_${featureVersion}.bin.dist.zip\"/>");
	output.println("      <property name=\"targetDir\" value=\"${feature.base}\"/>");
	output.println("    </ant>");
	
	output.println("    <delete dir=\"${feature.base}\"/>");
	output.println("  </target>");
}
protected void generateBuildScript(PrintWriter output) {
	generatePrologue(output);
	generatePluginTemplateTarget(output);
	generateFragmentTemplateTarget(output);
	generateAllTemplateTarget(output);
	generateChildrenTarget(output);
	generateCompileTarget(output);
//	generateTemplateCallTarget(output, "compile", true);
	generateTemplateCallTarget(output, "src", true);
	
	generateBinDistWholeTarget(output);
	generateBinDistPartsTarget(output);
	generateBinZipTarget(output);
	generateBinGatherWholeTarget(output);
	generateBinGatherPartsTarget(output);
	generateBinGatherDataTarget(output);
//	generateTemplateTargetCall(output, TARGET_JAVADOC);
//	generateGatherTemplateCall(output, TARGET_DOC, true);

	generateSrcZipTarget(output);
	generateSrcGatherPartsTarget(output);
	generateSrcGatherWholeTarget(output);

	generateLogTarget(output);
	generateCleanTarget(output);
	generateAllTarget(output);
	// generate a bin target to simplify -dev bin testing.  This should be removed
	// when the platform stops passing through all command line args.
	generateBinTarget(output);
	generateEpilogue(output);
}

protected void generateCompileTarget(PrintWriter output) {
	StringBuffer zips = new StringBuffer();
	Map properties = getProperties(feature);
	for (Iterator i = properties.keySet().iterator(); i.hasNext();) {
		String element = (String) i.next();
		if (element.toLowerCase().endsWith(".zip")) {
			zips.append("," + element);
			generateZipTarget(output, element);
		}
	}
	output.println();
	output.println("  <target name=\"" + "compile" + "\" depends=\"init" + zips.toString() + "\">");
	output.println("    <antcall target=\"" + "children" + "\">");
	output.println("      <param name=\"target\" value=\"" + "compile" +"\"/>");
	output.println("    </antcall>");
	output.println("  </target>");
}
protected void generateChildrenTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + "children" + "\" if=\"includeChildren\">");
	output.println("    <antcall target=\"" + TARGET_ALL_TEMPLATE + "\"/>");
	output.println("  </target>");
}
protected void generateCleanTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + TARGET_CLEAN + "\" depends=\"init\">");
	output.println("    <antcall target=\"" + TARGET_ALL_TEMPLATE + "\">");
	output.println("      <param name=\"target\" value=\"clean\"/>");
	output.println("    </antcall>");
	output.println("    <delete>");
	output.println("      <fileset dir=\".\" includes=\"*.pdetemp\"/>");
	output.println("      <fileset dir=\".\" includes=\"${feature}*.jar\"/>");
	output.println("      <fileset dir=\".\" includes=\"${feature}*.zip\"/>");
	output.println("    </delete>");
	output.println("  </target>");
}
protected void generateDocTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + DEFAULT_FILENAME_DOC + "\" depends=\"init\">");
	output.println("    <property name=\"tempdir\" value=\"docs\"/>");
	output.println("    <delete dir=\"${tempdir}\"/>");
	output.println("    <antcall target=\"" + TARGET_ALL_TEMPLATE + "\">");
	output.println("      <param name=\"target\" value=\"doc\"/>");
	output.println("      <param name=\"docdir\" value=\"${tempdir}\"/>");
	output.println("    </antcall>");
	output.println("    <delete>");
	output.println("      <fileset dir=\".\" includes=\"${feature}.doc*.zip\"/>");
	output.println("    </delete>");
	
	// call runZip on template.xml in case we want to use an external program
	output.println("    <ant antfile=\"${template}\" target=\"runZip\">");
	output.println("      <property name=\"resultingFile\" value=\"../${feature}.doc${stamp}.zip\"/>");
	output.println("      <property name=\"targetDir\" value=\"${basedir}/_temp_\"/>");
	output.println("    </ant>");

	output.println("  </target>");
}
protected void generateEpilogue(PrintWriter output) {
	output.println("</project>");
}
protected void generateFragmentTemplateTarget(PrintWriter output) {
	PluginModel[] list = determineFragments();
	IPath base = new Path(featureLocation);
	output.println();
	output.println("  <target name=\"" + TARGET_FRAGMENT_TEMPLATE + "\" depends=\"init\">");
	for (int i = 0; i < list.length; i++) {
		String location = makeRelative(getLocation(list[i]), base);
		output.println("    <ant dir=\"" + location + "\" target=\"${target}\"/>");
	}
	output.println("  </target>");
}
protected void generateTemplateCallTarget(PrintWriter output, String target, boolean outputTerminatingTag) {
	output.println();
	output.println("  <target name=\"" + target + "\" depends=\"init\">");
	output.println("    <antcall target=\"" + TARGET_ALL_TEMPLATE + "\">");
	output.println("      <param name=\"target\" value=\"" + target + "\"/>");
	output.println("    </antcall>");
	if (outputTerminatingTag)
		output.println("  </target>");
}

protected void generateZipTarget(PrintWriter output, String zipfile) {
	output.println();
	output.println("  <target name=\"" + zipfile + "\" depends=\"init\">");
	output.println("    <property name=\"feature.base\" value=\"${basedir}/zip.pdetemp\"/>");
	output.println("    <delete dir=\"${feature.base}\"/>");
	output.println("    <mkdir dir=\"${feature.base}\"/>");

	output.println("    <ant antfile=\"${template}\" target=\"" + "mapperCopy" + "\">");
	String inclusions = getSubstitution(feature, zipfile);
	String mapping = inclusions;
	output.println("      <property name=\"includes\" value=\"" + inclusions + "\"/>");
	String exclusions = "";
	output.println("      <property name=\"excludes\" value=\"" + exclusions + "\"/>");
	output.println("      <property name=\"dest\" value=\"${feature.base}/" + "" + "\"/>");
	output.println("      <property name=\"mapping\" value=\"" + mapping + "\"/>");
	output.println("    </ant>");
		
	// call runJar on template.xml in case we want to use an external program
	output.println("    <ant antfile=\"${template}\" target=\"runZip\">");
	output.println("      <property name=\"resultingFile\" value=\"" + zipfile + "\"/>");
	output.println("      <property name=\"targetDir\" value=\"${feature.base}\"/>");
	output.println("    </ant>");
	
	output.println("    <delete dir=\"${feature.base}\"/>");
	output.println("  </target>");
}
protected void generateLogTarget(PrintWriter output) {
	generateTemplateCallTarget(output,TARGET_LOG,false);
	
	// call runZip on template.xml in case we want to use an external program
	output.println("    <ant antfile=\"${template}\" target=\"runZip\">");
	output.println("      <property name=\"resultingFile\" value=\"../${feature}.log${stamp}.zip\"/>");
	output.println("      <property name=\"targetDir\" value=\"${basedir}/_temp_\"/>");
	output.println("    </ant>");

	output.println("  </target>");
}
protected void generatePrologue(PrintWriter output) {
	output.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	output.println("<project name=\"main\" default=\"" + TARGET_ALL + "\" basedir=\".\">");
	output.println();
	output.println("  <target name=\"init\">");
	output.println("    <initTemplate/>");
	output.println("    <property name=\"feature\" value=\"" + feature.getFeatureIdentifier() + "\"/>");
	output.println("    <property name=\"featureVersion\" value=\"" + feature.getFeatureVersion() + "\"/>");
	String stampString = stamp.length() == 0 ? stamp : "-" + stamp;
	output.println("    <property name=\"stamp\" value=\"" + stampString + "\"/>");
	Map map = getPropertyAssignments(feature);
	Iterator keys = map.keySet().iterator();
	while (keys.hasNext()) {
		String key = (String)keys.next();
		output.println("    <property name=\"" + key + "\" value=\"" + (String)map.get(key) + "\"/>");
	}
	output.println("  </target>");
}
protected void generateSrcZipTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + TARGET_SRC_ZIP + "\" depends=\"init\">");
	output.println("    <antcall target=\"" + TARGET_ALL_TEMPLATE + "\">");
	output.println("      <param name=\"target\" value=\"" + TARGET_SRC_ZIP + "\"/>");
	output.println("    </antcall>");

	output.println("    <property name=\"feature.base\" value=\"${basedir}/src.zip.pdetemp\"/>");
	output.println("    <delete dir=\"${feature.base}\"/>");
	output.println("    <mkdir dir=\"${feature.base}\"/>");

	output.println("    <antcall target=\"src.gather.parts\"/>");
	
	// call runJar on template.xml in case we want to use an external program
	output.println("    <ant antfile=\"${template}\" target=\"runZip\">");
	output.println("      <property name=\"resultingFile\" value=\"${feature}_src_${featureVersion}.zip\"/>");
	output.println("      <property name=\"targetDir\" value=\"${feature.base}/install/features/${feature}_${featureVersion}\"/>");
	output.println("    </ant>");
	
	output.println("    <delete dir=\"${feature.base}\"/>");
	output.println("  </target>");
}
protected void generateSrcTarget(PrintWriter output) {
	generateTemplateCallTarget(output,TARGET_SRC, false);
	
	// call runJar on template.xml in case we want to use an external program
	output.println("    <ant antfile=\"${template}\" target=\"runJar\">");
	output.println("      <property name=\"resultingFile\" value=\"../${feature}.src${stamp}.jar\"/>");
	output.println("      <property name=\"targetDir\" value=\"${basedir}/_temp_\"/>");
	output.println("    </ant>");
	output.println("    <delete dir=\"${basedir}/_temp_\"/>");
	output.println("  </target>");
}
protected void generateSrcGatherWholeTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + "src.gather.whole" + "\" depends=\"init\">");
	output.println("    <property name=\"feature.base\" value=\"${basedir}/src.assemble.pdetemp\"/>");
	output.println("    <delete dir=\"${feature.base}\"/>");
	output.println("    <mkdir dir=\"${feature.base}\"/>");
	output.println("    <antcall target=\"" + "children" + "\">");
	output.println("      <param name=\"target\" value=\"" + "src.gather.whole" + "\"/>");
	output.println("      <param name=\"destroot\" value=\"${feature.base}/plugins/\"/>");
	output.println("    </antcall>");
	output.println("    <copy file=\"${feature}_src_${featureVersion}.zip\" todir=\"${feature.base}/features\"/>");
		
	// call runZip on template.xml in case we want to use an external program
	output.println("    <property name=\"zipfile\" value=\"${feature}_${featureVersion}.src.zip\"/>");
	output.println("    <ant antfile=\"${template}\" target=\"runZip\">");
	output.println("      <property name=\"resultingFile\" value=\"${zipfile}\"/>");
	output.println("      <property name=\"targetDir\" value=\"${feature.base}\"/>");
	output.println("    </ant>");
	
	output.println("    <delete dir=\"${feature.base}\"/>");
	output.println("  </target>");
}
protected void generateSrcGatherPartsTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + "src.gather.parts" + "\" depends=\"init\">");
	output.println("    <antcall target=\"" + "children" + "\">");
	output.println("      <param name=\"target\" value=\"" + "src.gather.parts" + "\"/>");
	output.println("      <param name=\"destroot\" value=\"${feature.base}\"/>");
	output.println("    </antcall>");

	output.println("    <property name=\"feature.auto.includes\" value=\"feature.xml\"/>");
	output.println("    <property name=\"feature.auto.excludes\" value=\"\"/>");
	output.println("    <ant antfile=\"${template}\" target=\"includesExcludesCopy\">");
	String inclusions = getSubstitution(feature, SRC_INCLUDES);
	if (inclusions == null)
		inclusions = "${feature.auto.includes}";
	output.println("      <property name=\"includes\" value=\"" + inclusions + "\"/>");
	String exclusions = getSubstitution(feature, SRC_EXCLUDES);
	if (exclusions == null)
		exclusions = "${feature.auto.excludes}";
	output.println("      <property name=\"excludes\" value=\"" + exclusions + "\"/>");
	output.println("      <property name=\"dest\" value=\"${feature.base}/install/features/${feature}\"/>");
	output.println("    </ant>");
	output.println("  </target>");
}
protected void generatePluginTemplateTarget(PrintWriter output) {
	PluginModel[] plugins = determinePlugins();
	IPath base = new Path(featureLocation).removeLastSegments(1);
	String[][] sortedPlugins = computePrerequisiteOrder(plugins);
	output.println();
	output.println("  <target name=\"" + TARGET_PLUGIN_TEMPLATE + "\" depends=\"init\">");
	for (int list = 0; list < 2; list++) {
		for (int i = 0; i < sortedPlugins[list].length; i++) {
			PluginModel plugin = getRegistry().getPlugin(sortedPlugins[list][i]);
			String location = makeRelative(getLocation(plugin), base);
			output.println("      <ant antfile=\"build.xml\" dir=\"" + location + "\" target=\"${target}\"/>");
		}
	}
	output.println("  </target>");
}
protected String getLocation(PluginModel descriptor) {
	try {
		return new URL(descriptor.getLocation()).getFile();
	} catch (MalformedURLException e) {
		return "../" + descriptor.getId() + "/";
	}
}
protected PrintWriter openMainOutput() throws IOException {
	return new PrintWriter(new FileOutputStream(new File(new File(featureLocation).getParentFile(), DEFAULT_FILENAME_MAIN).getAbsoluteFile()));
}

protected void printUsage(PrintWriter out) {
	out.println("\tjava FeatureBuildScriptGenerator -install <targetDir> -feature <featureLocation> [-nochildren] [-dev <devEntries>]");
}
protected String[] processCommandLine(String[] args) {
	super.processCommandLine(args);
	for (int i = 0; i < args.length; i++) {
		String currentArg = args[i];
		if (currentArg.equalsIgnoreCase("-children"))
			generateChildren = true;
		if (currentArg.equalsIgnoreCase("-merge"))
			merge = true;
		if (i == args.length - 1 || args[i + 1].startsWith(SWITCH_DELIMITER))
			continue;
		String previousArg = currentArg;
		currentArg = args[++i];
		if (previousArg.equalsIgnoreCase(SWITCH_FEATURE))
			featureLocation = currentArg;
		if (previousArg.equalsIgnoreCase("-elements")) 
			featureLocation = getInstall() + "/install/features/" + currentArg + "/feature.xml";
	}
	return new String[0];
}
protected boolean readFeature() {
	if (featureLocation == null) {
		addProblem(new Status(IStatus.ERROR, PluginTool.PI_PDECORE, ScriptGeneratorConstants.EXCEPTION_FEATURE_MISSING, Policy.bind("error.missingComponentId"), null));
		return false;
	}
	
	try {
		FeatureExecutableFactory factory = new FeatureExecutableFactory();
		File file = new File(featureLocation).getParentFile();
		feature = (Feature) factory.createFeature(file.toURL(), null);
		return true;
	} catch (Exception e) {
		e.printStackTrace();
		return false;
	}
}
protected PluginModel[] readElements(boolean fragments) {
	ArrayList result = new ArrayList();
	IPluginEntry[] pluginList = feature.getPluginEntries();
	for (int i = 0; i < pluginList.length; i++) {
		IPluginEntry entry = pluginList[i];
		if (fragments ==  entry.isFragment()) {
			PluginModel model = getRegistry().getPlugin(entry.getVersionIdentifier().getIdentifier(), entry.getVersionIdentifier().getVersion().toString());
			if (model == null)
				addProblem(new Status(IStatus.ERROR, PluginTool.PI_PDECORE, ScriptGeneratorConstants.EXCEPTION_PLUGIN_MISSING, Policy.bind("exception.missingPlugin", entry.getVersionIdentifier().toString()), null));
			else
				result.add(model);
		}
	}
	return (PluginModel[])result.toArray(new PluginModel[result.size()]);
}

public Object run(Object args) throws Exception {
	super.run(args);
	return execute();
}
public void setFeatureLocation(String value) {
	featureLocation = value;
}
public void setGenerateChildren(boolean value) {
	generateChildren = value;
}
protected String[][] computePrerequisiteOrder(PluginModel[] plugins) {
	List prereqs = new ArrayList(9);
	Set pluginList = new HashSet(plugins.length);
	for (int i = 0; i < plugins.length; i++) 
		pluginList.add(plugins[i].getId());
	// create a collection of directed edges from plugin to prereq
	for (int i = 0; i < plugins.length; i++) {
		boolean boot = false;
		boolean runtime = false;
		boolean found = false;
		PluginPrerequisiteModel[] prereqList = plugins[i].getRequires();
		if (prereqList != null) {
			for (int j = 0; j < prereqList.length; j++) {
				// ensure that we only include values from the original set.
				String prereq = prereqList[j].getPlugin();
				boot = boot || prereq.equals(BootLoader.PI_BOOT);
				runtime = runtime || prereq.equals(Platform.PI_RUNTIME);
				if (pluginList.contains(prereq)) {
					found = true;
					prereqs.add(new String[] { plugins[i].getId(), prereq });
				}
			}
		}
		// if we didn't find any prereqs for this plugin, add a null prereq 
		// to ensure the value is in the output
		if (!found)
			prereqs.add(new String[] { plugins[i].getId(), null });
		// if we didn't find the boot or runtime plugins as prereqs and they are in the list
		// of plugins to build, add prereq relations for them.  This is required since the 
		// boot and runtime are implicitly added to a plugin's requires list by the platform runtime.
		// Note that we should skip the xerces plugin as this would cause a circularity.
		if (plugins[i].getId().equals("org.apache.xerces"))
			continue;
		if (!boot && pluginList.contains(BootLoader.PI_BOOT) && !plugins[i].getId().equals(BootLoader.PI_BOOT))
			prereqs.add(new String[] { plugins[i].getId(), BootLoader.PI_BOOT});
		if (!runtime && pluginList.contains(Platform.PI_RUNTIME) && !plugins[i].getId().equals(Platform.PI_RUNTIME) && !plugins[i].getId().equals(BootLoader.PI_BOOT))
			prereqs.add(new String[] { plugins[i].getId(), Platform.PI_RUNTIME});
	}
	// do a topological sort and return the prereqs
	String[][] prereqArray = (String[][]) prereqs.toArray(new String[prereqs.size()][]);
	return computeNodeOrder(prereqArray);
}
protected static String[][] computeNodeOrder(String[][] specs) {
	HashMap counts = computeCounts(specs);
	List nodes = new ArrayList(counts.size());
	while (!counts.isEmpty()) {
		List roots = findRootNodes(counts);
		if (roots.isEmpty())
			break;
		for (Iterator i = roots.iterator(); i.hasNext();)
			counts.remove(i.next());
		nodes.addAll(roots);
		removeArcs(specs, roots, counts);
	}
	String[][] result = new String[2][];
	result[0] = (String[]) nodes.toArray(new String[nodes.size()]);
	result[1] = (String[]) counts.keySet().toArray(new String[counts.size()]);
	return result;
}
protected static HashMap computeCounts(String[][] mappings) {
	HashMap counts = new HashMap(5);
	for (int i = 0; i < mappings.length; i++) {
		String from = mappings[i][0];
		Integer fromCount = (Integer) counts.get(from);
		String to = mappings[i][1];
		if (to == null)
			counts.put(from, new Integer(0));
		else {
			if (((Integer) counts.get(to)) == null)
				counts.put(to, new Integer(0));
			fromCount = fromCount == null ? new Integer(1) : new Integer(fromCount.intValue() + 1);
			counts.put(from, fromCount);
		}
	}
	return counts;
}
protected static List findRootNodes(HashMap counts) {
	List result = new ArrayList(5);
	for (Iterator i = counts.keySet().iterator(); i.hasNext();) {
		String node = (String) i.next();
		int count = ((Integer) counts.get(node)).intValue();
		if (count == 0)
			result.add(node);
	}
	return result;
}
protected static void removeArcs(String[][] mappings, List roots, HashMap counts) {
	for (Iterator j = roots.iterator(); j.hasNext();) {
		String root = (String) j.next();
		for (int i = 0; i < mappings.length; i++) {
			if (root.equals(mappings[i][1])) {
				String input = mappings[i][0];
				Integer count = (Integer) counts.get(input);
				if (count != null)
					counts.put(input, new Integer(count.intValue() - 1));
			}
		}
	}
}
}
