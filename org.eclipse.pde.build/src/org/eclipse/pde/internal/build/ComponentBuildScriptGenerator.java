package org.eclipse.pde.internal.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import java.net.*;
import java.util.*;
import org.eclipse.core.internal.plugins.PluginParser;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.xml.sax.InputSource;

public class ComponentBuildScriptGenerator extends PluginTool {
	private ComponentModel componentModel = null;
	private String componentId = null;
	private PluginModel plugins[] = null;
	private PluginModel fragments[] = null;
	private boolean generateChildren = true;
	
	// constants
	private static final String TEMPLATE_LOCATION = "../org.eclipse.pde.core/template.xml";
	private static final String SWITCH_COMPONENT = "-component";
	
	// output filenames
	private static final String DEFAULT_FILENAME_SRC = "source.jar";
	private static final String DEFAULT_FILENAME_LOG = "logs.zip";
	private static final String DEFAULT_FILENAME_DOC = "docs.zip";
	private static final String DEFAULT_FILENAME_MAIN = "build.xml";
	
	// targets
	private static final String TARGET_ALL_TEMPLATE = "all-template";
	private static final String TARGET_PLUGIN_TEMPLATE = "plugin-template";
	private static final String TARGET_FRAGMENT_TEMPLATE = "fragment-template";

public ComponentBuildScriptGenerator() {
	super();
}
public ComponentBuildScriptGenerator(String componentId,String installPath,PluginRegistryModel registry) {
	super();
	this.componentId = componentId;
	setRegistry(registry);
	setInstall(installPath);
}
protected PluginModel[] determineFragments() {
	if (fragments == null) {
		if (componentModel != null)
			fragments = readFragmentsFromComponentModel();
		else
			plugins = new PluginModel[0];
	}

	return fragments;
}
protected String determineFullComponentModelId() {
	if (componentModel == null)
		return "";

	return componentModel.getId() + SEPARATOR_VERSION + componentModel.getVersion();
}
protected PluginModel[] determinePlugins() {
	if (plugins == null) {
		if (componentModel != null)
			plugins = readPluginsFromComponentModel();
		else
			plugins = new PluginModel[0];
	}
		
	return plugins;
}

public IStatus execute() {
	if (!readComponentModel())
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
	targets.add(TARGET_JAR);
	targets.add(TARGET_BIN);
 	targets.add(TARGET_SRC);
	targets.add(TARGET_LOG);
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
	output.println("  <target name=\"" + TARGET_BIN + "\" depends=\"init\">");
	output.println("    <antcall target=\"" + TARGET_ALL_TEMPLATE + "\">");
	output.println("      <param name=\"target\" value=\"" + TARGET_BIN + "\"/>");
	output.println("      <param name=\"destroot\" value=\"${basedir}/_temp___/\"/>");
	output.println("    </antcall>");

	output.println("    <property name=\"comp.auto.includes\" value=\"install.xml\"/>");
	output.println("    <property name=\"comp.auto.excludes\" value=\"\"/>");
	output.println("    <ant antfile=\"${template}\" target=\"bin\">");
	
	String inclusions = getSubstitution(componentModel,BIN_INCLUDES);
	if (inclusions == null)
		inclusions = "${comp.auto.includes}";
	output.println("      <property name=\"includes\" value=\"" + inclusions + "\"/>");

	String exclusions = getSubstitution(componentModel,BIN_EXCLUDES);
	if (exclusions == null)
		exclusions = "${comp.auto.excludes}";
	output.println("      <property name=\"excludes\" value=\"" + exclusions + "\"/>");

	output.println("      <property name=\"dest\" value=\"${basedir}/_temp___/install/components/${component}_${compVersion}\"/>");
	output.println("    </ant>");

	output.println("    <jar jarfile=\"${component}_${compVersion}.jar\" basedir=\"${basedir}/_temp___\"/>");
	output.println("    <delete dir=\"${basedir}/_temp___\"/>");
	output.println("  </target>");
}
protected void generateBuildScript(PrintWriter output) {
	generatePrologue(output);
	generatePluginTemplateTarget(output);
	generateFragmentTemplateTarget(output);
	generateAllTemplateTarget(output);
	generateTemplateTargetCall(output, TARGET_JAR);
			
	generateBinTarget(output);
	
	generateTemplateTargetCall(output, TARGET_JAVADOC);
	generateGatherTemplateCall(output, TARGET_DOC,true);
		
	generateSrcTarget(output);
	generateLogTarget(output);
				
	generateCleanTarget(output);
	generateAllTarget(output);
	generateEpilogue(output);
}
protected void generateCleanTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + TARGET_CLEAN + "\" depends=\"init\">");
	output.println("    <antcall target=\"" + TARGET_ALL_TEMPLATE + "\">");
	output.println("      <param name=\"target\" value=\"clean\"/>");
	output.println("    </antcall>");

	output.println("    <delete file=\"${component}_${compVersion}.jar\"/>");
	output.println("    <delete file=\"" + DEFAULT_FILENAME_LOG + "\"/>");
	output.println("    <delete file=\"" + DEFAULT_FILENAME_DOC + "\"/>");
	output.println("    <delete file=\"" + DEFAULT_FILENAME_SRC + "\"/>");

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
	output.println("    <delete file=\"" + DEFAULT_FILENAME_DOC + "\"/>");
	output.println("    <zip zipfile=\"" + DEFAULT_FILENAME_DOC + "\" basedir=\"${tempdir}\"/>");
	output.println("    <delete dir=\"${tempdir}\"/>");
	output.println("  </target>");
}
protected void generateEpilogue(PrintWriter output) {
	output.println("</project>");
}
protected void generateFragmentTemplateTarget(PrintWriter output) {
	PluginModel[] list = determineFragments();
	IPath base = new Path(componentModel.getLocation());

	output.println();
	output.println("  <target name=\"" + TARGET_FRAGMENT_TEMPLATE + "\" depends=\"init\">");
	for (int i = 0; i < list.length; i++) {
		String location = makeRelative(getLocation(list[i]), base);
		output.println("      <ant dir=\"" + location + "\" target=\"${target}\"/>");
	}
	output.println("  </target>");
}
protected void generateGatherTemplateCall(PrintWriter output, String targetName, boolean outputTerminatingTag) {
	output.println();
	output.println("  <target name=\"" + targetName + "\" depends=\"init\">");
	output.println("    <antcall target=\"" + TARGET_ALL_TEMPLATE + "\">");
	output.println("      <param name=\"target\" value=\"" + targetName + "\"/>");
	output.println("      <param name=\"destroot\" value=\"${basedir}/_temp___/\"/>");
	output.println("    </antcall>");
	
	if (outputTerminatingTag)
		output.println("  </target>");
}
protected void generateLogTarget(PrintWriter output) {
	generateGatherTemplateCall(output,TARGET_LOG,false);
	output.println("    <zip zipfile=\"" + DEFAULT_FILENAME_LOG + "\" basedir=\"${basedir}/_temp___/\"/>");
	output.println("    <delete dir=\"${basedir}/_temp___\"/>");
	output.println("  </target>");
}
protected void generatePrologue(PrintWriter output) {
	output.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	output.println("<project name=\"main\" default=\"" + TARGET_ALL + "\" basedir=\".\">");
	output.println();
	output.println("  <target name=\"init\">");
	output.println("    <initTemplate/>");
	output.println("    <property name=\"component\" value=\"" + componentModel.getId() + "\"/>");
	output.println("    <property name=\"compVersion\" value=\"" + componentModel.getVersion() + "\"/>");
	
	Map map = getPropertyAssignments(componentModel);
	Iterator keys = map.keySet().iterator();
	while (keys.hasNext()) {
		String key = (String)keys.next();
		output.println("    <property name=\"" + key + "\" value=\"" + (String)map.get(key) + "\"/>");
	}

	output.println("  </target>");
}
protected void generateSrcTarget(PrintWriter output) {
	generateGatherTemplateCall(output,TARGET_SRC,false);
	output.println("    <jar jarfile=\"" + DEFAULT_FILENAME_SRC + "\" basedir=\"${basedir}/_temp___/\"/>");
	output.println("    <delete dir=\"${basedir}/_temp___\"/>");
	output.println("  </target>");
}
protected void generatePluginTemplateTarget(PrintWriter output) {
	PluginModel[] plugins = determinePlugins();
	IPath base = new Path(componentModel.getLocation());
	String[][] sortedPlugins = computePrerequisiteOrder(plugins);

	output.println();
	output.println("  <target name=\"" + TARGET_PLUGIN_TEMPLATE + "\" depends=\"init\">");
	for (int list = 0; list < 2; list++) {
		for (int i = 0; i < sortedPlugins[list].length; i++) {
			String location = makeRelative(getLocation(getRegistry().getPlugin(sortedPlugins[list][i])), base);
			output.println("      <ant dir=\"" + location + "\" target=\"${target}\"/>");
		}
	}
	output.println("  </target>");
}
protected void generateTemplateTargetCall(PrintWriter output, String target) {
	output.println();
	output.println("  <target name=\"" + target + "\" depends=\"init\">");
	output.println("    <antcall target=\"" + TARGET_ALL_TEMPLATE + "\">");
	output.println("      <param name=\"target\" value=\"" + target + "\"/>");
	output.println("      <param name=\"destroot\" value=\"${basedir}/_temp___/\"/>");
	output.println("    </antcall>");
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
	return new PrintWriter(new FileOutputStream(new File(componentModel.getLocation(),DEFAULT_FILENAME_MAIN).getAbsoluteFile()));
}

protected void printUsage(PrintWriter out) {
	out.println("\tjava ComponentBuildScriptGenerator -install <targetDir> -component <componentId> [-nochildren] [-dev <devEntries>]");
}
protected String[] processCommandLine(String[] args) {
	super.processCommandLine(args);
	
	for (int i = 0; i < args.length; i++) {
		String currentArg = args[i];
		if (currentArg.equalsIgnoreCase(SWITCH_NOCHILDREN))
			generateChildren = false;

		if (i == args.length - 1 || args[i + 1].startsWith(SWITCH_DELIMITER))
			continue;

		String previousArg = currentArg;
		currentArg = args[++i];

		if (previousArg.equalsIgnoreCase(SWITCH_COMPONENT))
			componentId = currentArg;
	}
	
	return new String[0];
}
protected boolean readComponentModel() {
	if (componentId == null) {
		addProblem(new Status(
			IStatus.ERROR,
			PluginTool.PI_PDECORE,
			ScriptGeneratorConstants.EXCEPTION_COMPONENT_MISSING,
			Policy.bind("error.missingComponentId"),
			null));
		return false;
	}
	
	ModelRegistry modelRegistry = new ModelRegistry();
	modelRegistry.seekComponents(getInstall());
	ComponentModel component = modelRegistry.getComponent(componentId);
	if (component != null) {
		componentModel = component;
		return true;
	}
	
	return false;
}
protected PluginModel[] readFragmentsFromComponentModel() {
	Vector accumulatingResult = new Vector();
	PluginModel componentFragments[] = componentModel.getFragments();

	for (int i = 0; i < componentFragments.length; i++) {
		PluginModel currentReadFragment = componentFragments[i];
		PluginModel resultingFragment = getRegistry().getFragment(currentReadFragment.getId());
		if (resultingFragment == null) {
			addProblem(new Status(
				IStatus.ERROR,
				PluginTool.PI_PDECORE,
				ScriptGeneratorConstants.EXCEPTION_FRAGMENT_MISSING,
				Policy.bind("exception.missingFragment",currentReadFragment.getId()),
				null));
			continue;
		}
		if (!currentReadFragment.getVersion().equals(resultingFragment.getVersion())) {
			addProblem(new Status(
				IStatus.WARNING,
				PluginTool.PI_PDECORE,
				ScriptGeneratorConstants.WARNING_FRAGMENT_INCORRECTVERSION,
				Policy.bind("warning.usingIncorrectFragmentVersion",currentReadFragment.getId()),
				null));
		}

		accumulatingResult.addElement(resultingFragment);
	}
	
	PluginModel result[] = new PluginModel[accumulatingResult.size()];
	accumulatingResult.copyInto(result);
	
	return result;	
}
protected PluginModel[] readPluginsFromComponentModel() {
	Vector accumulatingResult = new Vector();
	PluginModel componentPlugins[] = componentModel.getPlugins();

	for (int i = 0; i < componentPlugins.length; i++) {
		PluginModel currentReadPlugin = componentPlugins[i];
		PluginModel resultingPlugin = getRegistry().getPlugin(currentReadPlugin.getId());
		if (resultingPlugin == null) {
			addProblem(new Status(
				IStatus.ERROR,
				PluginTool.PI_PDECORE,
				ScriptGeneratorConstants.EXCEPTION_PLUGIN_MISSING,
				Policy.bind("exception.missingPlugin",currentReadPlugin.getId()),
				null));
			continue;
		}
		if (!currentReadPlugin.getVersion().equals(resultingPlugin.getVersion())) {
			addProblem(new Status(
				IStatus.WARNING,
				PluginTool.PI_PDECORE,
				ScriptGeneratorConstants.WARNING_PLUGIN_INCORRECTVERSION,
				Policy.bind("warning.usingIncorrectPluginVersion",currentReadPlugin.getId()),
				null));
		}
		
		accumulatingResult.addElement(resultingPlugin);
	}
	
	PluginModel result[] = new PluginModel[accumulatingResult.size()];
	accumulatingResult.copyInto(result);
	
	return result;	
}
public Object run(Object args) throws Exception {
	super.run(args);
	return execute();
}
public void setComponentId(String value) {
	componentId = value;
}
public void setGenerateChildren(boolean value) {
	generateChildren = value;
}
public static void main(String[] args) throws Exception {
	new ComponentBuildScriptGenerator().run(args);
}
public static void main(String argString) throws Exception {
	main(tokenizeArgs(argString));
}
protected String[][] computePrerequisiteOrder(PluginModel[] plugins) {
	List prereqs = new ArrayList(9);
	List pluginList = new ArrayList(plugins.length);
	for (int i = 0; i < plugins.length; i++) 
		pluginList.add(plugins[i].getId());
	for (int i = 0; i < plugins.length; i++) {
		prereqs.add(new String[] { plugins[i].getId(), null });
		PluginPrerequisiteModel[] prereqList = plugins[i].getRequires();
		if (prereqList != null) {
			for (int j = 0; j < prereqList.length; j++) {
				// ensure that we only include values from the original set.
				String prereq = prereqList[j].getPlugin();
				if (pluginList.contains(prereq))
					prereqs.add(new String[] { plugins[i].getId(), prereq });
			}
		}
	}

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
