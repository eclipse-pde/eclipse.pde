package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;

abstract class ModelBuildScriptGenerator extends PluginTool {

	private Map jarsCache = new HashMap(9);
	private Set requiredJars = null;
	private Hashtable devJars = null;
	private Hashtable trimmedDevJars = null;
	private ArrayList jarOrder = null;
	private PluginModel modelsToGenerate[] = null;
	private Vector commandLineModelNames = new Vector();
	
	// constant output filenames
	private static final String DEFAULT_FILENAME_SRC = "source.jar";
	private static final String DEFAULT_FILENAME_LOG = "logs.zip";
	private static final String DEFAULT_FILENAME_DOC = "docs.zip";
	private static final String OUTPUT_FILENAME = "build.xml";
	
	private static final String JAVADOC_EXTENSION = ".javadoc";
	private static final String PLUGIN_RUNTIME = "org.eclipse.core.runtime";
	private static final String SOURCE_EXTENSION = ".src";
	private static final String RUNTIME_FILENAME = "runtime.jar";
	private static final String BOOT_FILENAME = "org.eclipse.core.boot/boot.jar";
	private static final String SOURCE_PREFIX = "source.";
	private static final String WS = "$ws$";
	private static final String VARIABLE_WS = "${ws}";
		
public ModelBuildScriptGenerator() {
	super();
}
public ModelBuildScriptGenerator(PluginModel modelsToGenerate[],PluginRegistryModel registry) {
	this();
	setModelsToGenerate(modelsToGenerate);
	setRegistry(registry);
}
protected String computeCompilePathClause(PluginModel descriptor, String fullJar) {
	Set jars = new HashSet(9);
	List devEntries = getDevEntries();
	PluginModel desc = getRegistry().getPlugin(PLUGIN_RUNTIME);
	jars.add(getLocation(desc) + RUNTIME_FILENAME);
	if (devEntries != null)
		for (Iterator i = devEntries.iterator(); i.hasNext();) 
			jars.add(getLocation(desc) + i.next());
	
	// The boot jar must be located relative to the runtime jar.  This reflects the actual
	// runtime requirements.
	String location = new Path(getLocation(desc)).removeLastSegments(1).toString();
	jars.add(location + BOOT_FILENAME);
	if (devEntries != null)
		for (Iterator i = devEntries.iterator(); i.hasNext();) 
			jars.add(location + "org.eclipse.core.boot/" +  i.next());
			
	jars.addAll(requiredJars);
	jars.addAll(devJars.keySet());
	jars.remove(fullJar);
	if (devEntries != null)
		for (Iterator i = devEntries.iterator(); i.hasNext();) 
			jars.remove(getLocation(descriptor) + i.next());
	
	Set relativeJars = makeRelative(jars, new Path(getLocation(descriptor)));
	
	String result = getStringFromCollection(relativeJars, "", "", ";");
	result = replaceWsWithVariable(result);
	return result;
}
protected String computeCompleteSrc(PluginModel descriptor) {
	Set jars = new HashSet(9);
	for (Iterator i = devJars.values().iterator(); i.hasNext();)
		jars.addAll((Collection) i.next());
	return getStringFromCollection(jars, "", "/", ",");
}
public IStatus execute() {
	if (modelsToGenerate == null)
		retrieveCommandLineModels();
		
	for (int i = 0; i < modelsToGenerate.length; i++) {
		try {
			PrintWriter output = openOutput(modelsToGenerate[i]);
			try {
				generateBuildScript(output,modelsToGenerate[i]);
			} finally {
				output.flush();
			}
		} catch (IOException e) {
			e.printStackTrace(System.out);
		}
	}
	
	return getProblems();
}
public void generateBuildScript(PrintWriter output,PluginModel descriptor) {
	initializeFor(descriptor);
	generatePrologue(output, descriptor);

	generateJarsTarget(output, descriptor);

//	generateDocsTarget(output, descriptor);
//	generateJavadocsTarget(output, descriptor);
//	generateJavadocTargets(output, descriptor);
			
	generateSrcTargets(output, descriptor);

	generateBinTarget(output, descriptor);

	generateLogsTarget(output, descriptor);
				
	generateCleanTarget(output, descriptor);
	generateEpilogue(output, descriptor);
}
protected void generateBinTarget(PrintWriter output, PluginModel descriptor) {
	output.println();
	output.println("  <target name=\"" + TARGET_BIN + "\" depends=\"init\">");
	output.println("    <property name=\"auto.includes\" value=\"**\"/>");
	output.println("    <property name=\"auto.excludes\" value=\"" + computeCompleteSrc(descriptor) + "\"/>");

	output.println("    <ant antfile=\"${template}\" target=\"" + TARGET_BIN + "\">");

	String inclusions = getSubstitution(descriptor,BIN_INCLUDES);
	if (inclusions == null)
		inclusions = "${auto.includes}";
	output.println("      <property name=\"includes\" value=\"" + inclusions + "\"/>");

	String exclusions = getSubstitution(descriptor,BIN_EXCLUDES);
	if (exclusions == null)
		exclusions = "${auto.excludes}";
	output.println("      <property name=\"excludes\" value=\"" + exclusions + "\"/>");
		
	output.println("      <property name=\"dest\" value=\"${destroot}/" + getComponentDirectoryName() + "\"/>");
	output.println("    </ant>");
	output.println("  </target>");
}
protected void generateCleanTarget(PrintWriter output, PluginModel descriptor) {
	ArrayList jars = new ArrayList(9);
	ArrayList zips = new ArrayList(9);
	
	for (Iterator i = trimmedDevJars.keySet().iterator(); i.hasNext();) {
		String jar = new Path((String) i.next()).lastSegment();
		jars.add(jar);
		zips.add(jar.substring(0,jar.length() - 4) + DEFAULT_FILENAME_SRC);
	}
	
	String compiledJars = getStringFromCollection(jars, "", "", ",");
	String sourceZips = getStringFromCollection(zips, "", "", ",");
	output.println();
	output.println("  <target name=\"" + TARGET_CLEAN + "\" depends=\"init\">");
	
	if (compiledJars.length() > 0) {
		output.println("    <ant antfile=\"${template}\" target=\"" + TARGET_CLEAN + "\">");
		output.println("      <property name=\"" + TARGET_JAR + "\" value=\"" + compiledJars + "\"/>");
		output.println("      <property name=\"srczips\" value=\"" + sourceZips + "\"/>");
		output.println("    </ant>");
	}

	output.println("  </target>");
}
protected void generateCopyReference(PrintWriter output, PluginModel descriptor) {
	for (Iterator i = devJars.keySet().iterator(); i.hasNext();) {
		String fullJar = (String) i.next();
		String jar = fullJar.substring(fullJar.lastIndexOf('/') + 1);
		Collection sourceDirs = (Collection) trimmedDevJars.get(fullJar);
		output.println();
		output.println("  <patternset id=\"" + jar + ".ref\">");
		if (sourceDirs != null) {
			for (Iterator j = sourceDirs.iterator(); j.hasNext();) {
				String dir = (String) j.next();
				output.println("    <include name=\"" + dir + "/\"/>");
			}
		}
		output.println("  </patternset>");
	}
}
protected void generateDocsTarget(PrintWriter output, PluginModel descriptor) {
	output.println();
	output.println("  <target name=\"" + TARGET_DOC + "\" depends=\"init\">");
	output.println("    <ant antfile=\"${template}\" target=\"" + TARGET_DOC + "\">");
	output.println("      <property name=\"dest\" value=\"${destroot}/" + getComponentDirectoryName() + "\"/>");
	output.println("    </ant>");
	output.println("  </target>");
}
protected void generateEpilogue(PrintWriter output, PluginModel descriptor) {
	output.println("</project>");
}
protected void generateJarsTarget(PrintWriter output, PluginModel descriptor) {
	StringBuffer jars = new StringBuffer();
	for (Iterator i = jarOrder.iterator(); i.hasNext();) {
		jars.append(',');
		String currentJar = (String)i.next();
		jars.append(currentJar);
		generateJarTarget(output,descriptor,currentJar);
	}
	output.println();
	output.println("  <target name=\"" + TARGET_JAR + "\" depends=\"init" + jars.toString() + "\">");
	output.println("  </target>");
}
protected void generateJarTarget(PrintWriter output, PluginModel descriptor,String relativeJar) {
	String fullJar = null;
	try { 
		fullJar = new URL(descriptor.getLocation() + relativeJar).getFile();
	} catch (MalformedURLException e) {
		// should not happen
		e.printStackTrace();
	}
	
	String jar = fullJar.substring(fullJar.lastIndexOf('/') + 1);
	Collection sourceDirs = (Collection) trimmedDevJars.get(fullJar);
	String src = (sourceDirs == null || sourceDirs.isEmpty()) ? "" : getStringFromCollection(sourceDirs, "", "/", ",");
	String compilePath = computeCompilePathClause(descriptor, fullJar);
	output.println();
	output.println("  <target name=\"" + relativeJar + "\" depends=\"init\">");
	
	if (src.length() != 0) {
		output.println("    <property name=\"out\" value=\"${basedir}/" + jar + ".bin\"/>");
		output.println("    <delete dir=\"${out}\"/>");
		output.println("    <mkdir dir=\"${out}\"/>");
		output.println("    <ant antfile=\"${template}\" target=\"" + TARGET_JAR + "\">");
		output.println("      <property name=\"includes\" value=\"" + src + "\"/>");
		output.println("      <property name=\"excludes\" value=\"\"/>");
		output.println("      <property name=\"compilePath\" value=\"" + compilePath + "\"/>");
		output.println("    </ant>");
	}

	Iterator iterator = sourceDirs.iterator();
	while (iterator.hasNext()) {
		output.println("    <copy todir=\"${out}\">");
		output.println("      <fileset dir=\"" + (String)iterator.next() + "\">");
		output.println("        <include name=\"**\"/>");
		output.println("        <exclude name=\"**/*.java\"/>");
		output.println("      </fileset>");
		output.println("    </copy>");
	}

	output.println("    <property name=\"dest\" value=\"${destroot}/" + getComponentDirectoryName() + "\"/>");
	output.println("    <jar jarfile=\"" + relativeJar + "\" basedir=\"${out}\"/>");
	output.println("    <delete dir=\"${out}\"/>");

	output.println("  </target>");
}
protected void generateJavadocsTarget(PrintWriter output, PluginModel descriptor) {
	output.println();
	output.println("  <target name=\"" + TARGET_JAVADOC + "\" depends=\"init\">");
	output.println("    <delete dir=\"javadoc\"/>");
	output.println("    <mkdir dir=\"javadoc\"/>");
	
	for (Iterator i = jarOrder.iterator(); i.hasNext();) {
		output.print("    <antcall target=\"");
		// zip name is jar name without the ".jar" but with JAVADOC_EXTENSION appended
		String jar = (String) i.next();
		String zip = jar.substring(0, jar.length() - 4) + JAVADOC_EXTENSION;
		output.println(zip + "\"/>");
	}
	
	output.println("  </target>");
}
protected void generateJavadocTargets(PrintWriter output, PluginModel descriptor) {
	for (Iterator i = devJars.keySet().iterator(); i.hasNext();) {
		String fullJar = (String) i.next();
		// zip name is jar name without the ".jar" but with JAVADOC_EXTENSION appended
		String zip = fullJar.substring(fullJar.lastIndexOf('/') + 1, fullJar.length() - 4) + JAVADOC_EXTENSION;
		Collection sourceDirs = (Collection) trimmedDevJars.get(fullJar);
		String src = (sourceDirs == null || sourceDirs.isEmpty()) ? "" : getStringFromCollection(sourceDirs, "", "/", ";");
		String compilePath = computeCompilePathClause(descriptor, fullJar);
		output.println();
		output.println("  <target name=\"" + zip + "\" depends=\"init\">");

		if (src.length() != 0) {
			output.println("    <property name=\"auto.packages\" value=\"*\"/>");
			output.println("    <property name=\"auto.excludedpackages\" value=\"**.internal.*\"/>");
			output.println("    <ant antfile=\"${template}\" target=\"" + TARGET_JAVADOC + "\">");
			output.println("      <property name=\"sourcepath\" value=\"" + src + "\"/>");

			String inclusions = getSubstitution(descriptor,JAVADOC_PACKAGES);
			if (inclusions == null)
				inclusions = "${auto.packages}";
			output.println("      <property name=\"packages\" value=\"" + inclusions + "\"/>");
				
			String exclusions = getSubstitution(descriptor,JAVADOC_EXCLUDEDPACKAGES);
			if (exclusions == null)
				exclusions = "${auto.excludedpackages}";
			output.println("      <property name=\"excludedpackages\" value=\"" + exclusions + "\"/>");
			
			output.println("      <property name=\"out\" value=\"javadoc\"/>");
			output.println("      <property name=\"compilePath\" value=\"" + compilePath + "\"/>");
			output.println("    </ant>");
		}

		output.println("  </target>");
	}
}
protected void generateLogsTarget(PrintWriter output, PluginModel descriptor) {
	output.println();
	output.println("  <target name=\"" + TARGET_LOG + "\" depends=\"init\">");
	output.println("    <ant antfile=\"${template}\" target=\"" + TARGET_LOG + "\">");
	output.println("      <property name=\"dest\" value=\"${destroot}/" + getComponentDirectoryName() + "\"/>");
	output.println("    </ant>");
	output.println("  </target>");
}
protected void generatePrologue(PrintWriter output, PluginModel descriptor) {
	output.println("<?xml version=\"1.0\"?>");
	output.println("<project name=\"" + descriptor.getId() + "\" default=\"" + TARGET_JAR + "\" basedir=\".\">");
	output.println("  <target name=\"init\">");
	output.println("    <initTemplate/>");
	output.println("    <property name=\"" + getModelTypeName() + "\" value=\"" + descriptor.getId() + "\"/>");
	output.println("    <property name=\"version\" value=\"" + descriptor.getVersion() + "\"/>");

	Map map = getPropertyAssignments(descriptor);
	Iterator keys = map.keySet().iterator();
	while (keys.hasNext()) {
		String key = (String)keys.next();
		output.println("    <property name=\"" + key + "\" value=\"" + (String)map.get(key) + "\"/>");
	}

	output.println("  </target>");
}
protected void generateSrcTargets(PrintWriter output, PluginModel descriptor) {
	StringBuffer jars = new StringBuffer();
	for (Iterator i = jarOrder.iterator(); i.hasNext();) {
		jars.append(",");
		// zip name is jar name without the ".jar" but with SOURCE_EXTENSION appended
		String jar = (String) i.next();
		String zip = jar.substring(0, jar.length() - 4) + SOURCE_EXTENSION;
		jars.append(zip);
		generateSrcTarget(output,descriptor,jar,zip);
	}
	output.println();
	output.println("  <target name=\"" + TARGET_SRC + "\" depends=\"init" + jars.toString() + "\">");
	output.println("  </target>");
}
protected void generateSrcTarget(PrintWriter output,PluginModel descriptor,String relativeJar,String target) {
	String fullJar = null;
	try { 
		fullJar = new URL(descriptor.getLocation() + relativeJar).getFile();
	} catch (MalformedURLException e) {
		// should not happen
		e.printStackTrace();
	}
	
	// zip name is jar name without the ".jar" but with SOURCE_EXTENSION appended		
	String zip = fullJar.substring(fullJar.lastIndexOf('/') + 1, fullJar.length() - 4) + SOURCE_EXTENSION;
	Collection sourceDirs = (Collection) trimmedDevJars.get(fullJar);
	String src = (sourceDirs == null || sourceDirs.isEmpty()) ? "" : getStringFromCollection(sourceDirs, "", "/**/*.java", ",");
	output.println();
	output.println("  <target name=\"" + target + "\" depends=\"init\">");

	if (src.length() != 0) {
		output.println("    <property name=\"auto.includes\" value=\"" + src + "\"/>");
		output.println("    <property name=\"auto.excludes\" value=\"\"/>");
		output.println("    <ant antfile=\"${template}\" target=\"" + TARGET_SRC + "\">");
			
		String inclusions = getSubstitution(descriptor,SRC_INCLUDES);
		if (inclusions == null)
			inclusions = "${auto.includes}";
		output.println("      <property name=\"includes\" value=\"" + inclusions + "\"/>");
				
		String exclusions = getSubstitution(descriptor,SRC_EXCLUDES);
		if (exclusions == null)
			exclusions = "${auto.excludes}";
		output.println("      <property name=\"excludes\" value=\"" + exclusions + "\"/>");
			
		output.println("      <property name=\"dest\" value=\"${destroot}/" + getComponentDirectoryName() + "\"/>");
		output.println("    </ant>");
	}
		
	output.println("  </target>");
}

protected abstract String getComponentDirectoryName();

protected Set getJars(PluginModel descriptor) {
	Set result = (Set) jarsCache.get(descriptor.getId());
	
	if (result != null)
		return result;
		
	result = new HashSet(9);
	LibraryModel[] libs = descriptor.getRuntime();
	List devEntries = getDevEntries();
	
	if (libs != null) {
		if (devEntries != null)
			for (Iterator i = devEntries.iterator(); i.hasNext();) 
				result.add(getLocation(descriptor) + i.next());
		for (int i = 0; i < libs.length; i++)
			result.add(getLocation(descriptor) + libs[i].getName());
	}
	
	PluginPrerequisiteModel[] prereqs = descriptor.getRequires();
	if (prereqs != null) {
		for (int i = 0; i < prereqs.length; i++) {
			PluginModel prereq = getRegistry().getPlugin(prereqs[i].getPlugin());
			if (prereq != null)
				result.addAll(getJars(prereq));
		}
	}
	
	jarsCache.put(descriptor.getId(), result);
	return result;
}
protected List getListFromString(String prop) {
	if (prop == null || prop.trim().equals(""))
		return new ArrayList(0);
	ArrayList result = new ArrayList();
	for (StringTokenizer tokens = new StringTokenizer(prop, ","); tokens.hasMoreTokens();) {
		String token = tokens.nextToken().trim();
		if (!token.equals(""))
			result.add(token);
	}
	return result;
}
protected String getLocation(PluginModel descriptor) {
	try {
		return new URL(descriptor.getLocation()).getFile();
	} catch (MalformedURLException e) {
		return "../" + descriptor.getId() + "/";
	}
}
protected abstract String getModelTypeName();
protected void initializeFor(PluginModel descriptor) {
	devJars = loadJarDefinitions(descriptor);
	trimmedDevJars = trimDevJars(descriptor, devJars);
	requiredJars = getJars(descriptor);
}
protected Hashtable loadJarDefinitions(PluginModel descriptor) {
	jarOrder = new ArrayList();
	Properties props = new Properties() {
		public Object put(Object key, Object value) {
			if (!((String)key).startsWith(SOURCE_PREFIX))
				return value;
			key = ((String)key).substring(SOURCE_PREFIX.length());
			jarOrder.add(key);
			return super.put(key, value);
		}
	};
	
	URL location = null;
	try {
		location = new URL(descriptor.getLocation() + FILENAME_PROPERTIES);
		InputStream is = location.openStream();
		try {
			props.load(is);
		} finally {
			is.close();
		}
	} catch (IOException e) {
		addProblem(new Status(
			IStatus.ERROR,
			PluginTool.PI_PDECORE,
			ScriptGeneratorConstants.EXCEPTION_FILE_MISSING,
			Policy.bind("exception.missingFile",location.toString()),
			null));
				
		return new Hashtable(1);
	}
	
	String base = getLocation(descriptor);
	Hashtable result = new Hashtable(props.size());
	for (Iterator i = props.keySet().iterator(); i.hasNext();) {
		String key = (String) i.next();
		result.put(base + key, getListFromString(props.getProperty(key)));
	}
	
	return result;
}
protected Set makeRelative(Set jars, IPath base) {
	Set result = new HashSet(jars.size());
	for (Iterator i = jars.iterator(); i.hasNext();) 
		result.add(makeRelative((String) i.next(), base));
	return result;
}
protected PrintWriter openOutput(PluginModel descriptor) throws IOException {
	try {
		URL location = new URL(descriptor.getLocation());
		String file = location.getFile() + OUTPUT_FILENAME;
		return new PrintWriter(new FileOutputStream(new File(file).getAbsoluteFile()));
	} catch (MalformedURLException e) {
		e.printStackTrace(); // should never get here
		return null;
	}
}
protected String[] processCommandLine(String[] args) {
	super.processCommandLine(args);
	
	Vector modelsToGenerate = new Vector();
	for (int i = 0; i < args.length; i++) {
		String currentArg = args[i];
		
		if (i == args.length - 1)
			continue;

		String previousArg = currentArg;
		currentArg = args[++i];

		// accumulate the list of models to generate
		if (previousArg.substring(1).equalsIgnoreCase(getModelTypeName()))
			commandLineModelNames.addElement(currentArg);
	}
	
	return new String[0];
}
protected String replaceWsWithVariable(String sourceString) {
	int replacementIndex = sourceString.indexOf(WS);
	if (replacementIndex == -1)
		return sourceString;
		
	return
		sourceString.substring(0,replacementIndex) +
		VARIABLE_WS +
		sourceString.substring(replacementIndex + WS.length());
}			
protected void retrieveCommandLineModels() {
	Vector modelsToGenerate = new Vector();
	
	Enumeration models = commandLineModelNames.elements();
	while (models.hasMoreElements()) {
		String modelName = (String)models.nextElement();
		PluginModel model = retrieveModelNamed(modelName);
		if (model != null)
			modelsToGenerate.addElement(model);
		else {
			addProblem(new Status(
				IStatus.ERROR,
				PluginTool.PI_PDECORE,
				ScriptGeneratorConstants.EXCEPTION_PLUGIN_MISSING,
				Policy.bind("exception.missingPlugin",modelName),
				null));
		}
	}
	
	PluginModel result[] = new PluginModel[modelsToGenerate.size()];
	modelsToGenerate.copyInto(result);
	setModelsToGenerate(result);
}
protected abstract PluginModel retrieveModelNamed(String modelName);

public Object run(Object args) throws Exception {
	super.run(args);
	return execute();
}
public void setModelsToGenerate(PluginModel models[]) {
	modelsToGenerate = models;
}
protected Hashtable trimDevJars(PluginModel descriptor, Hashtable devJars) {
	Hashtable result = new Hashtable(9);
	for (Iterator it = devJars.keySet().iterator(); it.hasNext();) {
		String key = (String) it.next();
		Collection list = (Collection) devJars.get(key);			// projects
		File base = null;
		try {
			base = new File(new URL(descriptor.getLocation()).getFile());
		} catch (MalformedURLException e) {
		}
		ArrayList dirs = new ArrayList(list.size());
		for (Iterator i = list.iterator(); i.hasNext();) {
			String src = (String) i.next();
			if (new File(base, src).getAbsoluteFile().exists())		// the exported source files
				dirs.add(src);
		}
		if (!dirs.isEmpty())
			result.put(key, dirs);
	}
	return result;
}
}
