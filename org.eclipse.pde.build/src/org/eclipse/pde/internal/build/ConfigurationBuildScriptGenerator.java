package org.eclipse.pde.internal.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.io.*;
import java.util.*;
import org.eclipse.core.internal.plugins.PluginParser;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.xml.sax.InputSource;

public class ConfigurationBuildScriptGenerator extends PluginTool implements ScriptGeneratorConstants {
	private ConfigurationModel configurationModel = null;
	private String configurationId = null;
	private boolean generateChildren = true;
	private ComponentModel components[] = null;
	private Properties properties = null;
		
	// constants
	private static final String SWITCH_CONFIGURATION = "-configuration";
	private static final String FILENAME_OUTPUT = "build.xml";
	private static final String DIRECTORY_BIN = "bin";
	
	// targets
	private static final String TARGET_COMPONENT_TEMPLATE = "component-template";
	private static final String TARGET_BINCOPY = "bin-copy";

protected ComponentModel[] determineComponents() {
	if (components == null) {
		if (configurationModel != null)
			components = readComponentsFromConfigurationModel();
		else
			components = new ComponentModel[0];
	}
		
	return components;
}
protected String determineFullConfigurationModelId() {
	if (configurationModel == null)
		return "";

	return configurationModel.getId() + "_" + configurationModel.getVersion();
}
public void execute() {
	if (!readConfigurationModel())
		return;
		
	if (generateChildren) {
		ComponentModel components[] = determineComponents();
		for (int i = 0; i < components.length; i++) {
			ComponentBuildScriptGenerator generator = new ComponentBuildScriptGenerator(components[i].getId(),getInstall(),getRegistry());
			generator.setDevEntries(getDevEntries());
			generator.execute();
		}
	}
	
	try {
		PrintWriter output = openConfigurationOutput();
		try {
			generateBuildScript(output);
		} finally {
			output.flush();
			output.close();
		}
	} catch (IOException e) {
		e.printStackTrace(System.out);
	}
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
protected void generateBinTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + TARGET_BIN + "\" depends=\"init\">");
	output.println("    <antcall target=\"" + TARGET_COMPONENT_TEMPLATE + "\">");
	output.println("      <param name=\"target\" value=\"" + TARGET_BIN + "\"/>");
	output.println("    </antcall>");
	
	output.println("    <property name=\"auto.includes\" value=\"install.xml\"/>");
	output.println("    <property name=\"auto.excludes\" value=\"\"/>");
	output.println("    <ant antfile=\"${template}\" target=\"bin\">");

	String inclusions = getSubstitution(configurationModel,BIN_INCLUDES);
	if (inclusions == null)
		inclusions = "${auto.includes}";
	output.println("      <property name=\"includes\" value=\"" + inclusions + "\"/>");

	String exclusions = getSubstitution(configurationModel,BIN_EXCLUDES);
	if (exclusions == null)
		exclusions = "${auto.excludes}";
	output.println("      <property name=\"excludes\" value=\"" + exclusions + "\"/>");

	output.println("      <property name=\"dest\" value=\"${basedir}/temp/install/configurations/${configuration}_${configVersion}\"/>");
	output.println("    </ant>");

	IPath base = new Path(configurationModel.getLocation());
	String binLocation = makeRelative(new Path(getInstall()).append(DIRECTORY_BIN).toString(),base);
	
	output.println("    <property name=\"binSource\" value=\"" + binLocation + "\"/>");
	output.println("    <property name=\"binDest\" value=\"${basedir}/temp/bin\"/>");
	output.println("    <available file=\"${binSource}\" property=\"binSource.exists\"/>");
	output.println("    <antcall target=\"bin-copy\"/>");

	output.println("    <jar jarfile=\"${configuration}_${configVersion}.jar\" basedir=\"${basedir}/temp\"/>");
	output.println("    <delete dir=\"${basedir}/temp\"/>");
	output.println("  </target>");
	
	generateBinCopyTarget(output);
}
protected void generateBinCopyTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + TARGET_BINCOPY + "\" depends=\"init\" if=\"binSource.exists\">");
	output.println("    <mkdir dir=\"${binDest}\"/>");
	output.println("    <copydir src=\"${binSource}\" dest=\"${binDest}\"/>");
	output.println("  </target>");
}
protected void generateBuildScript(PrintWriter output) {
	System.out.println("Generating configuration " + configurationModel.getId());
	
	generatePrologue(output);
	generateComponentTemplateTarget(output);
	generateBinTarget(output);
	generateTemplateTargetCall(output,TARGET_JAR);
	generateTemplateTargetCall(output,TARGET_SRC);
	generateTemplateTargetCall(output,TARGET_LOG);
	generateCleanTarget(output);
	generateAllTarget(output);
	generateEpilogue(output);
}
protected void generateCleanTarget(PrintWriter output) {
	output.println();
	output.println("  <target name=\"" + TARGET_CLEAN + "\" depends=\"init\">");
	output.println("    <antcall target=\"" + TARGET_COMPONENT_TEMPLATE + "\">");
	output.println("      <param name=\"target\" value=\"" + TARGET_CLEAN + "\"/>");
	output.println("    </antcall>");
	output.println("    <delete file=\"${configuration}_${configVersion}.jar\"/>");
	output.println("  </target>");
}
protected void generateComponentTemplateTarget(PrintWriter output) {
	ComponentModel[] components = determineComponents();

	output.println();
	output.println("  <target name=\"" + TARGET_COMPONENT_TEMPLATE + "\" depends=\"init\">");
	IPath base = new Path(configurationModel.getLocation());
	for (int i = 0; i < components.length; i++) {
		String location = makeRelative(components[i].getLocation(),base);
		output.println("      <ant dir=\"" + location + "\" target=\"${target}\"/>");
	}
	output.println("  </target>");
}
protected void generateEpilogue(PrintWriter output) {
	output.println("</project>");
}
protected void generatePrologue(PrintWriter output) {
	output.println("<?xml version=\"1.0\"?>");
	output.println("<project name=\"main\" default=\"" + TARGET_ALL + "\" basedir=\".\">");
	output.println("  <target name=\"init\">");
	output.println("    <initTemplate/>");
	output.println("    <property name=\"configuration\" value=\"" + configurationModel.getId() + "\"/>");
	output.println("    <property name=\"configVersion\" value=\"" + configurationModel.getVersion() + "\"/>");
	
	Map map = getPropertyAssignments(configurationModel);
	Iterator keys = map.keySet().iterator();
	while (keys.hasNext()) {
		String key = (String)keys.next();
		output.println("    <property name=\"" + key + "\" value=\"" + (String)map.get(key) + "\"/>");
	}

	output.println("  </target>");
}
protected void generateTemplateTargetCall(PrintWriter output, String target) {
	output.println();
	output.println("  <target name=\"" + target + "\" depends=\"init\">");
	output.println("    <antcall target=\"" + TARGET_COMPONENT_TEMPLATE + "\">");
	output.println("      <param name=\"target\" value=\"" + target + "\"/>");
	output.println("    </antcall>");
	output.println("  </target>");
}
protected String makeRelative(String location, IPath base) {
	IPath path = new Path(location);
	if (!path.getDevice().equalsIgnoreCase(base.getDevice()))
		return location.toString();
	int baseCount = base.segmentCount();
	int count = base.matchingFirstSegments(path);
	if (count > 0) {
		String temp = "";
		for (int j = 0; j < baseCount - count; j++)
			temp += "../";
		path = new Path(temp).append(path.removeFirstSegments(count));
	}
	return path.toString();
}
protected PrintWriter openConfigurationOutput() throws IOException {
	return new PrintWriter(new FileOutputStream(
		new File(configurationModel.getLocation(),FILENAME_OUTPUT).getAbsoluteFile()));
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

		if (previousArg.equalsIgnoreCase(SWITCH_CONFIGURATION))
			configurationId = currentArg;
	}
	
	return new String[0];
}
protected ComponentModel[] readComponentsFromConfigurationModel() {
	ModelRegistry fullComponentRegistry = new ModelRegistry();
	fullComponentRegistry.seekComponents(getInstall());

	Vector accumulatingResult = new Vector();
	ComponentModel configurationComponents[] = configurationModel.getComponents();

	for (int i = 0; i < configurationComponents.length; i++) {
		ComponentModel currentReadComponent = configurationComponents[i];
		ComponentModel resultingComponent = fullComponentRegistry.getComponent(currentReadComponent.getId());
		if (resultingComponent == null) {
			System.out.println("could not read component " + currentReadComponent.getId());
			continue;
		}
		if (!currentReadComponent.getVersion().equals(resultingComponent.getVersion()))
			System.out.println("note: using incorrect version of component " + currentReadComponent.getId());
		accumulatingResult.addElement(resultingComponent);
	}
	
	ComponentModel result[] = new ComponentModel[accumulatingResult.size()];
	accumulatingResult.copyInto(result);
	
	return result;	
}
protected boolean readConfigurationModel() {
	if (configurationId == null) {
		System.out.println("Configuration id must be specified.");
		return false;
	}
		
	ModelRegistry modelRegistry = new ModelRegistry();
	modelRegistry.seekConfigurations(getInstall());
	ConfigurationModel configuration = modelRegistry.getConfiguration(configurationId);
	if (configuration != null) {
		configurationModel = configuration;
		return true;
	}
	
	return false;
}
public Object run(Object args) throws Exception {
	super.run(args);
	execute();
	return null;
}
public static void main(String[] args) throws Exception {
	new ConfigurationBuildScriptGenerator().run(args);
}
public static void main(String argString) throws Exception {
	main(tokenizeArgs(argString));
}


}
