/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

import java.net.URI;


import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;
import junit.framework.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.target.*;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.pde.internal.ui.tests.macro.MacroPlugin;
import org.osgi.framework.ServiceReference;

/**
 * Tests the persistence of target definitions.  Tests memento creation, reading of old target files, and writing of the model.
 * 
 * @since 3.5 
 */
public class TargetDefinitionPersistenceTests extends TestCase {
	
	public static Test suite() {
		return new TestSuite(TargetDefinitionPersistenceTests.class);
	}
	
	protected void assertTargetDefinitionsEqual(ITargetDefinition targetA, ITargetDefinition targetB) {
		assertTrue("Target content not equal",((TargetDefinition)targetA).isContentEqual(targetB));
	}
	
	/**
	 * Returns the target platform service or <code>null</code> if none
	 * 
	 * @return target platform service
	 */
	protected ITargetPlatformService getTargetService() {
		ServiceReference reference = MacroPlugin.getBundleContext().getServiceReference(ITargetPlatformService.class.getName());
		assertNotNull("Missing target platform service", reference);
		if (reference == null)
			return null;
		return (ITargetPlatformService) MacroPlugin.getBundleContext().getService(reference);
	}
	
	/**
	 * Returns the resolved location of the specified bundle container.
	 * 
	 * @param container bundle container
	 * @return resolved location
	 * @throws CoreException 
	 */
	protected String getResolvedLocation(IBundleContainer container) throws CoreException {
		return ((AbstractBundleContainer)container).getLocation(true);
	}
	
	/**
	 * Returns the location of the JDT feature in the running host as
	 * a path in the local file system.
	 * 
	 * @return path to JDT feature
	 */
	protected IPath getJdtFeatureLocation() {
		IPath path = new Path(TargetPlatform.getDefaultLocation());
		path = path.append("features");
		File dir = path.toFile();
		assertTrue("Missing features directory", dir.exists() && !dir.isFile());
		String[] files = dir.list();
		String location = null;
		for (int i = 0; i < files.length; i++) {
			if (files[i].startsWith("org.eclipse.jdt_")) {
				location = path.append(files[i]).toOSString();
				break;
			}
		}
		assertNotNull("Missing JDT feature", location);
		return new Path(location);
	}
	
	/**
	 * Tests restoration of a handle to target definition in an IFile 
	 * @throws CoreException 
	 */
	public void testWorkspaceTargetHandleMemento() throws CoreException {
		ITargetPlatformService service = getTargetService();
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("does/not/exist"));
		ITargetHandle handle = service.getTarget(file);
		assertFalse("Target should not exist", handle.exists());
		String memento = handle.getMemento();
		assertNotNull("Missing memento", memento);
		ITargetHandle handle2 = service.getTarget(memento);
		assertEquals("Restore failed", handle, handle2);
		IFile file2 = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("does/not/exist/either"));
		ITargetHandle handle3 = service.getTarget(file2);
		assertFalse("Should be different targets", handle.equals(handle3));
	}
	
	/**
	 * Tests restoration of a handle to target definition in local metadata 
	 * 
	 * @throws CoreException 
	 * @throws InterruptedException 
	 */
	public void testLocalTargetHandleMemento() throws CoreException, InterruptedException {
		ITargetPlatformService service = getTargetService();
		ITargetHandle handle = service.newTarget().getHandle();
		assertFalse("Target should not exist", handle.exists());
		String memento = handle.getMemento();
		assertNotNull("Missing memento", memento);
		ITargetHandle handle2 = service.getTarget(memento);
		assertEquals("Restore failed", handle, handle2);
		ITargetHandle handle3 = service.newTarget().getHandle();
		assertFalse("Should be different targets", handle.equals(handle3));
	}
	
	/**
	 * Tests restoration of a handle to target definition in external URI
	 * 
	 * @throws CoreException 
	 * @throws InterruptedException 
	 */
	public void testExternalFileTargetHandleMemento() throws CoreException, InterruptedException {
		ITargetPlatformService service = getTargetService();
		URI uri = null;
		try {
			uri = new URI("file:///does/not/exist");
		} catch (URISyntaxException e) {
		}
		//IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("does/not/exist"));
		ITargetHandle handle = service.getTarget(uri);
		assertFalse("Target should not exist", handle.exists());
		String memento = handle.getMemento();
		assertNotNull("Missing memento", memento);
		ITargetHandle handle2 = service.getTarget(memento);
		assertEquals("Restore failed", handle, handle2);
		//IFile file2 = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("does/not/exist/either"));
		URI uri2 = null;
		try {
			uri2 = new URI("file://even/this/file/does/not/exist");
		} catch (URISyntaxException e) {
		}
		ITargetHandle handle3 = service.getTarget(uri2);
		assertFalse("Should be different targets", handle.equals(handle3));
	}
	
	/**
	 * Tests that a complex metadata based target definition can be serialized to xml, 
	 * then deserialized without any loss of data.
	 * 
	 * @throws Exception
	 */
	public void testPersistComplexMetadataDefinition() throws Exception {
		ITargetDefinition definitionA = getTargetService().newTarget();
		initComplexDefiniton(definitionA);
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		TargetDefinitionPersistenceHelper.persistXML(definitionA, outputStream);
		ITargetDefinition definitionB = getTargetService().newTarget();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		TargetDefinitionPersistenceHelper.initFromXML(definitionB, inputStream);
		
		assertTargetDefinitionsEqual(definitionA, definitionB);
	}
	
	/**
	 * Tests that a complex workspace file based target definition can be serialized to xml, 
	 * then deserialized without any loss of data.
	 * 
	 * @throws Exception
	 */
	public void testPersistComplexWorkspaceDefinition() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("TargetDefinitionPersistenceTests");
		try{
			if (!project.exists()){
				project.create(null);
			}
			assertTrue("Could not create test project",project.exists());
			project.open(null);
			assertTrue("Could not open test project", project.isOpen());

			IFile target = project.getFile(new Long(System.currentTimeMillis()).toString() + "A.target");
			ITargetDefinition definitionA = getTargetService().getTarget(target).getTargetDefinition();
			initComplexDefiniton(definitionA);
			getTargetService().saveTargetDefinition(definitionA);
			ITargetDefinition definitionB = getTargetService().getTarget(target).getTargetDefinition();
							
			assertTargetDefinitionsEqual(definitionA, definitionB);
		} finally {
			if (project.exists()){
				project.delete(true, null);
			}
			assertFalse("Could not delete test project",project.exists());
		}
	}
	
	
	protected void initComplexDefiniton(ITargetDefinition definition) throws URISyntaxException {
		definition.setName("name");
		definition.setOS("os");
		definition.setWS("ws");
		definition.setArch("arch");
		definition.setNL("nl");
		definition.setProgramArguments("program\nargs");
		definition.setVMArguments("vm\nargs");
		definition.setJREContainer(JavaRuntime.newDefaultJREContainerPath());
		
		BundleInfo[] implicit = new BundleInfo[]{
				new BundleInfo("org.eclipse.jdt.launching", null, null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.jdt.debug", null, null, BundleInfo.NO_LEVEL, false)
		};		
		definition.setImplicitDependencies(implicit);
		
		// Directory container
		IBundleContainer dirContainer = getTargetService().newDirectoryContainer(TargetPlatform.getDefaultLocation() + "/plugins");
		// Profile container with specific config area
		IBundleContainer profileContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), new File(Platform.getConfigurationLocation().getURL().getFile()).getAbsolutePath());
		// Feature container with specific version
		IPath location = getJdtFeatureLocation();
		String segment = location.lastSegment();
		int index = segment.indexOf('_');
		assertTrue("Missing version id", index > 0);
		String version = segment.substring(index + 1);
		IBundleContainer featureContainer = getTargetService().newFeatureContainer("${eclipse_home}", "org.eclipse.jdt", version);
		// Profile container restricted to just two bundles
		IBundleContainer restrictedProfileContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		BundleInfo[] restrictions = new BundleInfo[]{
				new BundleInfo("org.eclipse.jdt.launching", null, null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.jdt.debug", null, null, BundleInfo.NO_LEVEL, false)
		};
		restrictedProfileContainer.setIncludedBundles(restrictions);
		// Add some optional bundles
		BundleInfo[] optional = new BundleInfo[]{
				new BundleInfo("org.eclipse.debug.examples.core", null, null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.debug.examples.ui", null, null, BundleInfo.NO_LEVEL, false)
		};
		restrictedProfileContainer.setOptionalBundles(optional);
		// Profile container restrict to zero bundles
		IBundleContainer emptyProfileContainer = getTargetService().newProfileContainer(TargetPlatform.getDefaultLocation(), null);
		BundleInfo[] completeRestrictions = new BundleInfo[0];
		emptyProfileContainer.setIncludedBundles(completeRestrictions);
		
		// Site bundle containers with different settings
		IUBundleContainer siteContainer = (IUBundleContainer)getTargetService().newIUContainer(new IInstallableUnit[]{}, new URI[]{new URI("TESTURI"), new URI("TESTURI2")});
		siteContainer.setIncludeAllRequired(false, null);
		siteContainer.setIncludeAllEnvironments(true, null);
		IUBundleContainer siteContainer2 = (IUBundleContainer)getTargetService().newIUContainer(new String[]{"unit1","unit2"},new String[]{"1.0", "2.0"}, new URI[]{new URI("TESTURI"), new URI("TESTURI2")});
		siteContainer2.setIncludeAllRequired(true, null);
		siteContainer2.setIncludeAllEnvironments(false, null);
		
		definition.setBundleContainers(new IBundleContainer[]{dirContainer, profileContainer, featureContainer, restrictedProfileContainer, emptyProfileContainer, siteContainer, siteContainer2});
	}
	
	/**
	 * Tests that an empty target definition can be serialized to xml, then deserialized without
	 * any loss of data.
	 * 
	 * @throws Exception
	 */
	public void testPersistEmptyDefinition() throws Exception {
		ITargetDefinition definitionA = getTargetService().newTarget();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		TargetDefinitionPersistenceHelper.persistXML(definitionA, outputStream);
		ITargetDefinition definitionB = getTargetService().newTarget();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
		TargetDefinitionPersistenceHelper.initFromXML(definitionB, inputStream);
		assertTargetDefinitionsEqual(definitionA, definitionB);
	}
	
	/**
	 * Reads a target definition file from the tests/targets/target-files location
	 * with the given name. Note that ".target" will be appended.
	 * 
	 * @param name
	 * @return target definition
	 * @throws Exception
	 */
	protected ITargetDefinition readOldTarget(String name) throws Exception {
		URL url = MacroPlugin.getBundleContext().getBundle().getEntry("/tests/targets/target-files/" + name + ".trgt");
		File file = new File(FileLocator.toFileURL(url).getFile());
		ITargetDefinition target = getTargetService().newTarget();
		FileInputStream stream = new FileInputStream(file);
		TargetDefinitionPersistenceHelper.initFromXML(target, stream);
		stream.close();
		return target;
	}
	
	/**
	 * Tests that we can de-serialize an old style target definition file (version 3.2) and retrieve the correct
	 * contents.
	 * 
	 * @throws Exception
	 */
	public void testReadOldBasicTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("basic");
		
		assertEquals("Wrong name", "Basic", target.getName());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getImplicitDependencies());
		assertNull(target.getJREContainer());
		
		IBundleContainer[] containers = target.getBundleContainers();
		assertEquals("Wrong number of bundles", 1, containers.length);
		assertTrue("Container should be a profile container", containers[0] instanceof ProfileBundleContainer);
		assertEquals("Wrong home location", new Path(TargetPlatform.getDefaultLocation()),
				new Path(getResolvedLocation(containers[0])));
	}
	
	/**
	 * Tests that we can de-serialize an old style target definition file (version 3.2) and retrieve the correct
	 * contents.
	 * 
	 * @throws Exception
	 */
	public void testReadOldBasicDirectoryTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("directory");
		
		assertEquals("Wrong name", "Directory", target.getName());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getImplicitDependencies());
		assertNull(target.getJREContainer());
		
		IBundleContainer[] containers = target.getBundleContainers();
		assertEquals("Wrong number of bundles", 1, containers.length);
		assertTrue("Container should be a directory container", containers[0] instanceof DirectoryBundleContainer);
		assertEquals("Wrong home location", new Path(TargetPlatform.getDefaultLocation()).append("plugins"),
				new Path(getResolvedLocation(containers[0])));
	}	
	
	/**
	 * Tests that we can de-serialize an old style target definition file (version 3.2) and retrieve the correct
	 * contents.
	 * 
	 * @throws Exception
	 */
	public void testReadOldSpecificTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("specific");
		
		assertEquals("Wrong name", "Specific Settings", target.getName());
		assertEquals("x86", target.getArch());
		assertEquals("linux", target.getOS());
		assertEquals("en_US", target.getNL());
		assertEquals("gtk", target.getWS());
		assertEquals("pgm1 pgm2", target.getProgramArguments());
		assertEquals("-Dfoo=\"bar\"", target.getVMArguments());
		assertEquals(JavaRuntime.newJREContainerPath(JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("J2SE-1.4")), target.getJREContainer());
		
		BundleInfo[] infos = target.getImplicitDependencies();
		assertEquals("Wrong number of implicit dependencies", 2, infos.length);
		Set set = new HashSet();
		for (int i = 0; i < infos.length; i++) {
			set.add(infos[i].getSymbolicName());
		}
		assertTrue("Missing ", set.remove("org.eclipse.jdt.debug"));
		assertTrue("Missing ", set.remove("org.eclipse.debug.core"));
		assertTrue(set.isEmpty());
		
		IBundleContainer[] containers = target.getBundleContainers();
		assertEquals("Wrong number of bundles", 1, containers.length);
		assertTrue("Container should be a directory container", containers[0] instanceof DirectoryBundleContainer);
		assertEquals("Wrong home location", new Path(TargetPlatform.getDefaultLocation()).append("plugins"),
				new Path(getResolvedLocation(containers[0])));
	}	
	
	/**
	 * Tests that we can de-serialize an old style target definition file (version 3.2) and retrieve the correct
	 * contents.
	 * 
	 * @throws Exception
	 */
	public void testReadOldAdditionLocationsTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("additionalLocations");
		
		assertEquals("Wrong name", "Additional Locations", target.getName());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getJREContainer());
		assertNull(target.getImplicitDependencies());
		
		IBundleContainer[] containers = target.getBundleContainers();
		assertEquals("Wrong number of bundles", 3, containers.length);
		assertTrue(containers[0] instanceof ProfileBundleContainer);
		assertTrue(containers[1] instanceof DirectoryBundleContainer);
		assertTrue(containers[2] instanceof DirectoryBundleContainer);
		
		assertEquals("Wrong home location", new Path(TargetPlatform.getDefaultLocation()),
				new Path(getResolvedLocation(containers[0])));
		
		String string = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution("${workspace_loc}");
		assertEquals("Wrong 1st additional location", new Path(string).append("stuff"),
				new Path(getResolvedLocation(containers[1])));
		
		assertEquals("Wrong 2nd additional location", new Path(TargetPlatform.getDefaultLocation()).append("dropins"),
				new Path(getResolvedLocation(containers[2])));
	}		
	
	/**
	 * Tests that we can de-serialize an old style target definition file (version 3.2) and retrieve the correct
	 * contents.
	 * 
	 * @throws Exception
	 */
	public void testReadOldFeaturesTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("featureLocations");
		
		assertEquals("Wrong name", "Features", target.getName());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getJREContainer());
		assertNull(target.getImplicitDependencies());
		
		IBundleContainer[] containers = target.getBundleContainers();
		assertEquals("Wrong number of bundles", 2, containers.length);
		assertTrue(containers[0] instanceof FeatureBundleContainer);
		assertTrue(containers[1] instanceof FeatureBundleContainer);

		assertEquals("Wrong feature location", "org.eclipse.jdt", ((FeatureBundleContainer)containers[0]).getFeatureId());
		assertEquals("Wrong feature location", "org.eclipse.platform", ((FeatureBundleContainer)containers[1]).getFeatureId());
	}
	
	/**
	 * Tests that we can de-serialize an old style target definition file (version 3.2) and retrieve the correct
	 * contents.
	 * 
	 * @throws Exception
	 */
	public void testReadOldRestrictionsTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("restrictions");
		
		assertEquals("Wrong name", "Restrictions", target.getName());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getJREContainer());
		assertNull(target.getImplicitDependencies());
		
		BundleInfo[] restrictions = new BundleInfo[]{
			new BundleInfo("org.eclipse.debug.core", null, null, BundleInfo.NO_LEVEL, false),
			new BundleInfo("org.eclipse.debug.ui", null, null, BundleInfo.NO_LEVEL, false),
			new BundleInfo("org.eclipse.jdt.debug", null, null, BundleInfo.NO_LEVEL, false),
			new BundleInfo("org.eclipse.jdt.debug.ui", null, null, BundleInfo.NO_LEVEL, false),
			new BundleInfo("org.eclipse.jdt.launching", null, null, BundleInfo.NO_LEVEL, false)
		};
		
		IBundleContainer[] containers = target.getBundleContainers();
		assertEquals("Wrong number of bundles", 3, containers.length);
		assertTrue(containers[0] instanceof ProfileBundleContainer);
		assertTrue(containers[1] instanceof FeatureBundleContainer);
		assertTrue(containers[2] instanceof DirectoryBundleContainer);
		
		assertEquals("Wrong home location", new Path(TargetPlatform.getDefaultLocation()), new Path(getResolvedLocation(containers[0])));
		assertEquals("Wrong 1st additional location", "org.eclipse.jdt",((FeatureBundleContainer)containers[1]).getFeatureId());
		assertEquals("Wrong 2nd additional location", new Path(TargetPlatform.getDefaultLocation()).append("dropins"),
				new Path(getResolvedLocation(containers[2])));
		
		for (int i = 0; i < containers.length; i++) {
			IBundleContainer container = containers[i];
			BundleInfo[] actual = container.getIncludedBundles();
			if (container instanceof FeatureBundleContainer) {
				assertNull(actual);
			} else {
				assertNotNull(actual);
				assertEquals("Wrong number of restrictions", restrictions.length, actual.length);
				for (int j = 0; j < actual.length; j++) {
					assertEquals("Wrong restriction", restrictions[j], actual[j]);
				}
			}
		}
	}		
	
	/**
	 * Tests that we can de-serialize an old style target definition file (version 3.2)
	 * that has extra/unsupported tags and retrieve the correct contents.
	 * 
	 * @throws Exception
	 */
	public void testReadOldTargetFileWithUnknownTags() throws Exception {
		ITargetDefinition target = readOldTarget("extratags");
		
		assertEquals("Wrong name", "Restrictions", target.getName());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getJREContainer());
		assertNull(target.getImplicitDependencies());
		
		BundleInfo[] restrictions = new BundleInfo[]{
			new BundleInfo("org.eclipse.debug.core", null, null, BundleInfo.NO_LEVEL, false),
			new BundleInfo("org.eclipse.debug.ui", null, null, BundleInfo.NO_LEVEL, false),
			new BundleInfo("org.eclipse.jdt.debug", null, null, BundleInfo.NO_LEVEL, false),
			new BundleInfo("org.eclipse.jdt.debug.ui", null, null, BundleInfo.NO_LEVEL, false),
			new BundleInfo("org.eclipse.jdt.launching", null, null, BundleInfo.NO_LEVEL, false)
		};
		
		IBundleContainer[] containers = target.getBundleContainers();
		assertEquals("Wrong number of bundles", 3, containers.length);
		assertTrue(containers[0] instanceof ProfileBundleContainer);
		assertTrue(containers[1] instanceof FeatureBundleContainer);
		assertTrue(containers[2] instanceof DirectoryBundleContainer);
		
		assertEquals("Wrong home location", new Path(TargetPlatform.getDefaultLocation()), new Path(getResolvedLocation(containers[0])));
		assertEquals("Wrong 1st additional location", "org.eclipse.jdt",((FeatureBundleContainer)containers[1]).getFeatureId());
		assertEquals("Wrong 2nd additional location", new Path(TargetPlatform.getDefaultLocation()).append("dropins"),
				new Path(getResolvedLocation(containers[2])));
		
		for (int i = 0; i < containers.length; i++) {
			IBundleContainer container = containers[i];
			BundleInfo[] actual = container.getIncludedBundles();
			if (container instanceof FeatureBundleContainer) {
				assertNull(actual);
			} else {
				assertNotNull(actual);
				assertEquals("Wrong number of restrictions", restrictions.length, actual.length);
				for (int j = 0; j < actual.length; j++) {
					assertEquals("Wrong restriction", restrictions[j], actual[j]);
				}
			}
		}
	}			
	
	/**
	 * Tests that we can de-serialize an old style target definition file (version 3.2) and retrieve
	 * the correct contents with optional bundles.
	 * 
	 * @throws Exception
	 */
	public void testReadOldOptionalTargetFile() throws Exception {
		ITargetDefinition target = readOldTarget("optional");
		
		assertEquals("Wrong name", "Optional", target.getName());
		assertNull(target.getArch());
		assertNull(target.getOS());
		assertNull(target.getNL());
		assertNull(target.getWS());
		assertNull(target.getProgramArguments());
		assertNull(target.getVMArguments());
		assertNull(target.getImplicitDependencies());
		assertNull(target.getJREContainer());
		
		IBundleContainer[] containers = target.getBundleContainers();
		assertEquals("Wrong number of bundles", 2, containers.length);
		assertTrue("Container should be a profile container", containers[0] instanceof ProfileBundleContainer);
		assertTrue("Container should be a profile container", containers[1] instanceof FeatureBundleContainer);
		assertEquals("Wrong home location", new Path(TargetPlatform.getDefaultLocation()),
				new Path(getResolvedLocation(containers[0])));
		assertEquals("Wrong feature location", "org.eclipse.jdt", ((FeatureBundleContainer)containers[1]).getFeatureId());
		
		BundleInfo[] included = new BundleInfo[]{
				new BundleInfo("org.eclipse.debug.core", null, null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.debug.ui", null, null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.jdt.debug", null, null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.jdt.debug.ui", null, null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.jdt.launching", null, null, BundleInfo.NO_LEVEL, false)
			};
		BundleInfo[] optional = new BundleInfo[]{
				new BundleInfo("org.eclipse.debug.examples.core", null, null, BundleInfo.NO_LEVEL, false),
				new BundleInfo("org.eclipse.debug.examples.ui", null, null, BundleInfo.NO_LEVEL, false)
			};
		
		for (int i = 0; i < containers.length; i++) {
			IBundleContainer container = containers[i];
			BundleInfo[] actual = container.getIncludedBundles();
			if (container instanceof FeatureBundleContainer) {
				assertNull(actual);
			} else {
				assertNotNull(actual);
				assertEquals("Wrong number of inclusions", included.length, actual.length);
				for (int j = 0; j < actual.length; j++) {
					assertEquals("Wrong restriction", included[j], actual[j]);
				}
				actual = container.getOptionalBundles();
				assertNotNull(actual);
				assertEquals("Wrong number of optionals", optional.length, actual.length);
				for (int j = 0; j < actual.length; j++) {
					assertEquals("Wrong restriction", optional[j], actual[j]);
				}
			}
		}
	}
	
	/**
	 * Test for bug 268709, if the content section is included in the xml, but there are no specific
	 * plug-ins or features as well as no useAllPlugins=true setting, treat the file as though it did
	 * include all plug-ins from the directory.
	 */
	public void testEmptyContentSection() throws Exception {
		ITargetDefinition target = readOldTarget("emptycontent");
			
		IBundleContainer[] containers = target.getBundleContainers();
		assertEquals("Wrong number of bundles", 1, containers.length);
		assertTrue("Container should be a directory container", containers[0] instanceof DirectoryBundleContainer);
		assertEquals("Wrong home location", new Path(TargetPlatform.getDefaultLocation()).append("plugins"),
				new Path(getResolvedLocation(containers[0])));
		
		target.resolve(null);
		
		assertTrue("Should have resolved bundles", target.getBundles().length > 0);
		
	}
	
	/**
	 * Test for bug 264139. Tests that when a target definition specifies "useAllPlugins=true"
	 * that we ignore specific plug-ins/features specified in the file during migration.
	 * 
	 * @throws Exception
	 */
	public void testMigrationOfUseAllWithRestrictions() throws Exception {
		ITargetDefinition target = readOldTarget("eclipse-serverside");
		IBundleContainer[] containers = target.getBundleContainers();
		assertEquals(6, containers.length);
		validateTypeAndLocation((AbstractBundleContainer) containers[0], ProfileBundleContainer.class, "${resource_loc:/target-platforms/eclipse-equinox-SDK-3.5M5/eclipse}");
		validateTypeAndLocation((AbstractBundleContainer) containers[1], DirectoryBundleContainer.class, "${resource_loc:/target-platforms/eclipse-3.5M5-delta-pack/eclipse}");
		validateTypeAndLocation((AbstractBundleContainer) containers[2], DirectoryBundleContainer.class, "${resource_loc:/target-platforms/eclipse-pde-headless-3.5M5}");
		validateTypeAndLocation((AbstractBundleContainer) containers[3], DirectoryBundleContainer.class, "${resource_loc:/target-platforms/eclipse-test-framework-3.5M5/eclipse}");
		validateTypeAndLocation((AbstractBundleContainer) containers[4], DirectoryBundleContainer.class, "${resource_loc:/target-platforms/eclipse-core-plugins-3.5M5}");
		validateTypeAndLocation((AbstractBundleContainer) containers[5], DirectoryBundleContainer.class, "${resource_loc:/target-platforms/3rdparty-bundles}");
	}	
	
	/**
	 * Validates the type and location of a bundle container.
	 * 
	 * @param container container to validate
	 * @param clazz the type of container expected
	 * @param rawLocation its unresolved location
	 * @throws CoreException if something goes wrong
	 */
	protected void validateTypeAndLocation(AbstractBundleContainer container, Class clazz, String rawLocation) throws CoreException {
		assertTrue(clazz.isInstance(container));
		assertEquals(rawLocation, container.getLocation(false));
		assertNull(container.getIncludedBundles());
	}
}