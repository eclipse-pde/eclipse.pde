PDE/FAQ
=======

Frequently Asked Questions for PDE. 
Community members are encouraged to maintain the page, and make sure the information is accurate.


Contents
--------

*   [1 General](#General)
    *   [1.1 What is PDE](#What-is-PDE)
    *   [1.2 How do I test my plug-ins](#How-do-I-test-my-plug-ins)
*   [2 PDE Build](#PDE-Build)
*   [3 Target Management](#Target-Management)
    *   [3.1 Why do I get an popup dialog saying I have selected a target with a newer version than your current Eclipse installation?](#Why-do-I-get-an-popup-dialog-saying-I-have-selected-a-target-with-a-newer-version-than-your-current-Eclipse-installation.3F)
*   [4 Products](#Products)
    *   [4.1 Why doesn't my JRE get included in a headless build?](#Why-doesnt-my-JRE-get-included-in-a-headless-build)
*   [5 Classpath](#Classpath)
    *   [5.1 How do I find a plug-in (bundle) given a class](#How-do-I-find-a-plug-in-bundle-given-a-class)
    *   [5.2 How do I get access to a plug-in (bundle) in my workspace or target](#How-do-I-get-access-to-a-plug-in-bundle-in-my-workspace-or-target)
    *   [5.3 I have an error that says some package (e.g., com.sun.misc) isn't accessible but it's on my classpath](#i-have-an-error-that-says-some-package-eg-comsunmisc-isnt-accessible-but-its-on-my-classpath)
*   [6 API Tooling](#API-Tooling)
    *   [6.1 How do I enable API Tooling for my projects](#How-do-I-enable-API-Tooling-for-my-projects)
    *   [6.2 How do I enable API Tooling for headless builds](#How-do-I-enable-API-Tooling-for-headless-builds)
*   [7 Build and Deployment Errors](#Build-and-Deployment-Errors)
    *   [7.1 My product fails to start](#My-product-fails-to-start)
    *   [7.2 My bundles aren't loading](#My-bundles-arent-loading)
    *   [7.3 Why aren't my launcher icons being found?](#Why-arent-my-launcher-icons-being-found)
    *   [7.4 Why aren't my class files being bundled?](#Why-arent-my-class-files-being-bundled)
    *   [7.5 Why aren't my class files being resolved? (ClassNotFoundException and NoClassDefFoundError)](#why-arent-my-class-files-being-resolved-classnotfoundexception-and-noclassdeffounderror)
    *   [7.6 Why am I getting a "package uses conflict"?](#Why-am-I-getting-a-package-uses-conflict)
*   [8 Misc](#Misc)
    *   [8.1 How do I add my own template to the New Plug-in Project Wizard](#How-do-I-add-my-own-template-to-the-New-Plug-in-Project-Wizard)
    *   [8.2 How do I know if my project is a plug-in project](#How-do-I-know-if-my-project-is-a-plug-in-project)
    *   [8.3 How do source attachments for bundles work](#How-do-source-attachments-for-bundles-work)

General
-------

### What is PDE

The Plug-in Development Environment (PDE) provides tools to create, develop, test, debug, build and deploy OSGi bundles and RCP applications.

### How do I test my plug-ins

The Eclipse PDE component provides a great way to test and debug your Eclipse plug-ins in a runtime environment. This is typically called **self-hosting**. The Eclipse you launch from is your **host** Eclipse, while the Eclipse instance you start is the **runtime workbench**.

PDE provides highly customizable launch configurations for self-hosting, but the default options will be enough for many users. There are also actions to run and debug your code in a runtime workbench in the top right corner of the PDE manifest editor, plug-in editor, product editor and more.

To create a new launch configuration, open the launch configuration dialog from the main menu, selecting **Run > Run Configurations**. From there you must create a new **Eclipse Application**. Pressing Run will launch the runtime workbench using code from your workspace and target platform.

Build
-----

For building PDE artifacts it is recommended to use [Maven Tycho](https://github.com/eclipse-tycho/tycho).

Target Management
-----------------

### Why do I get an popup dialog saying I have selected a target with a newer version than your current Eclipse installation?

This happens because PDE detects a newer version of the OSGi runtime in your target platform than in your host installation. To resolve this problem, don't point to a target that may contain future versions of Eclipse or just upgrade your current host to a newer version of Eclipse. In the end, this message is trying to convey that PDE is backwards compatible only, not forward compatible.

Also, see our [Target Definitions](https://github.com/eclipse-pde/eclipse.pde/blob/master/docs/Target_Definitions.md) page.

Products
--------

### Why doesn't my JRE get included in a headless build?

At the moment, there is a disparity between PDE UI and Build when it comes to JREs and product definitions. The best way to get your JRE to be included in a headless and UI build is to use [root files](https://help.eclipse.org/galileo/index.jsp?topic=/org.eclipse.pde.doc.user/tasks/pde_rootfiles.htm).

Classpath
---------

### How do I find a plug-in (bundle) given a class

If you're interested in finding a class at runtime, please use the PackageAdmin service from OSGi

### How do I get access to a plug-in (bundle) in my workspace or target

Use the PluginRegistry API to acquire models from your workspace or target

### I have an error that says some package (e.g., com.sun.misc) isn't accessible but it's on my classpath

In most cases, people get this error by accessing the Base64 class from the Sun VM. We generally don't recommend using the Base64 class from the VM because your bundle will be tired to only VMs that have that specific class. However, if this isn't an issue, you can get around the access restriction error by adding an access rule to your system library to make the package accessible. You can do this on the **Libraries** tab of the **Java Build Path** project properties. Add an **Accessible** rule for **com/sun/misc/***

Also, in most cases, this package needs to be visible during runtime. To have a package visible during runtime, you will need to add the **org.osgi.framework.system.packages.extra** system property. For example:

org.osgi.framework.system.packages.extra==com.sun.misc,com.sun.java.swing.plaf.windows 

API Tooling
-----------

### How do I enable API Tooling for my projects

Enabling API Tooling on a project is described [here](https://github.com/eclipse-pde/eclipse.pde/blob/master/docs/User_Guide.md).

Build and Deployment Errors
---------------------------

There are a few differences between the environment provided by PDE and the actual deployment environment. PDE's training wheels are a great help when you're developing, but losing them is painful when finally putting together a product!

The first step in diagnosing deployment errors is to examine your _workspace_/.metadata/.log. The workspace is specified by the osgi.instance.area.default and osgi.instance.area system properties.

### My product fails to start

Typical startup problems are:

1.  Missing necessary files (e.g., plugin.xml, Application.e4xmi) in your build.properties. As a result, these files are not being exported. Such errors are typically revealed in your .log.
2.  Unspecified start levels for key bundles like org.eclipse.equinox.ds. Start levels are generally specified in your .product file, though it is possible to provide hints through \[[p2 metadata](https://help.eclipse.org/latest/index.jsp?topic=/org.eclipse.platform.doc.isv/guide/p2_customizing_metadata.html)\] for a bundle.

A typical .product file might include the following configuration stanza:

        <configurations>
            <plugin id="org.eclipse.core.runtime" autoStart="true" startLevel="4" />
            <plugin id="org.eclipse.equinox.common" autoStart="true" startLevel="2" />
            <plugin id="org.eclipse.equinox.ds" autoStart="true" startLevel="2" />
            <plugin id="org.eclipse.equinox.p2.reconciler.dropins" autoStart="true" startLevel="4" />
            <plugin id="org.eclipse.equinox.simpleconfigurator" autoStart="true" startLevel="1" />
        </configurations>

### My bundles aren't loading

See [Where Is My Bundle?](https://github.com/eclipse-equinox/equinox/blob/master/docs/Where_Is_My_Bundle.md) for an overview of how to use the OSGi console for diagnosing problems.

### Why aren't my launcher icons being found?

Different build tools interpret the file paths in a .product differently. See [\[1\]](https://bugs.eclipse.org/bugs/show_bug.cgi?id=331974#c28%7Cthis) for ome details.

### Why aren't my class files being bundled?

Occasionally you will find that a built bundle seems to be missing the generated .class files. This problem generally occurs because the build.properties file is missing a reference to ".". A typical build.properties file looks like:

        source.. = src/
        output.. = bin/
        bin.includes = META-INF/,\
                    plugin.xml,\
                    about.html,\
                    .

Note the last line is a simple "."

### Why aren't my class files being resolved? (ClassNotFoundException and NoClassDefFoundError)

OSGi uses the Bundle-Classpath entry in a bundle's MANIFEST.MF to decide how to resolve classes within the bundle. This entry is a comma-separated list of directories and jar files from which classfiles are resolved. An absent Bundle-Classpath is interpreted as ".", meaning the root of the bundle.

Earlier versions of PDE did not automatically add a "." when a Bundle-Classpath is first added to a MANIFEST.MF. The workaround is to either explicitly add "." to the MANIFEST.MF, or use the PDE Manifest editor to Runtime > New (in the Classpath section) and add ".". (Note that this advice is specific to the default setup from PDE where classes are directly placed in the bundle rather than in a separate jar file.)

  

### Why am I getting a "package uses conflict"?

["Package uses"](http://blog.springsource.com/2008/10/20/understanding-the-osgi-uses-directive/) are akin to a bundle's require-bundles: they specify a package's dependent packages.  Package uses conflicts  are fortunately somewhat rare, but are obscure and difficult-to-diagnose problems. Basically they occur when a bundle's imports causes two different versions of a package to be available within the same bundle — there is no way for the framework to decide which package to allow. The articles linked to earlier in this paragraph provide more detail.

Package uses conflicts often occur with system-level packages from the JVM's execution environment (e.g., javax.transaction) are made available, either via system bundle extensions or the org.osgi.framework.system.packages and org.osgi.framework.system.packages.extra properties (and possibly the Equinox Launcher's [compatibility boot-delegation](/Equinox_Boot_Delegation "Equinox Boot Delegation") too). For example, many modern JREs now include javax.transaction, javax.annotation, javax.inject, and javax.vecmath, which many apps previously brought in through other bundles. When these packages are exposed through the system bundle, any bundle that re-exports the system bundle (notably org.eclipse.core.runtime) will also expose those classes. Conflicts arise when also including a bundle using a "Dynamic-ImportPackage" as the import set has to be verified against the packages available.

Diagnosing package-uses conflicts can be painful, and Equinox's diagnostics unfortunately omit the offending package names. The Eclipse Virgo project apparently has [some enhanced diagnostics](http://blog.springsource.com/2008/10/20/understanding-the-osgi-uses-directive/). Given a bundle, use the Equinox Console to examine the bundles providing that package using the packages command. Otherwise set a breakpoint in Equinox's org.eclipse.osgi.internal.module.ResolverImpl#findBestCombination(ResolverBundle\[\],Dictionary) to examine the local packageConstraints variable.

Misc
----

### How do I add my own template to the New Plug-in Project Wizard

The _org.eclipse.pde.ui.pluginContent_ extension point is what you need. There's a fantastic [article](http://www.ibm.com/developerworks/library/os-eclipse-pde/) on this topic at developerWorks.

### How do I know if my project is a plug-in project

In PDE, projects have a specific nature (see the .project file) associated with them if they are a plug-in. The PDE nature is:

org.eclipse.pde.PluginNature

A typical .project in a plug-in project will look like this:

        <?xml version="1.0" encoding="UTF-8"?>
        <projectDescription>
            <name>org.eclipse.ui.views.log</name>
            <comment></comment>
            <projects>
            </projects>
            <buildSpec>
                <buildCommand>
                    <name>org.eclipse.jdt.core.javabuilder</name>
                    <arguments>
                    </arguments>
                </buildCommand>
                <buildCommand>
                    <name>org.eclipse.pde.ManifestBuilder</name>
                    <arguments>
                    </arguments>
                </buildCommand>
                <buildCommand>
                    <name>org.eclipse.pde.SchemaBuilder</name>
                    <arguments>
                    </arguments>
                </buildCommand>
                <buildCommand>
                    <name>org.eclipse.pde.api.tools.apiAnalysisBuilder</name>
                    <arguments>
                    </arguments>
                </buildCommand>
            </buildSpec>
            <natures>
                <nature>org.eclipse.pde.PluginNature</nature>
                <nature>org.eclipse.jdt.core.javanature</nature>
                <nature>org.eclipse.pde.api.tools.apiAnalysisNature</nature>
            </natures>
        </projectDescription>

### How do source attachments for bundles work

Since Eclipse 3.4 Eclipse Bundle JARs are accompanied by separate source bundles (see [Bug 202462 simplify the way source is contributed](http://bugs.eclipse.org/202462)):

        javax.servlet_2.5.0.v200910301333
        javax.servlet.source_2.5.0.v200910301333

The source bundle has to contain the [Eclipse-SourceBundle](https://help.eclipse.org/galileo/index.jsp?topic=/org.eclipse.pde.doc.user/tasks/pde_individual_source.htm) header in its MANIFEST.MF, attaching the sources to the actual bundle:

        Bundle-SymbolicName: javax.servlet.source
        Eclipse-SourceBundle: javax.servlet;version="2.5.0.v200910301333"

When you build bundles using PDE build, you can choose to generate these source bundles.

Please note that it is not possible to attach sources manually for bundles because the classpath is created from the MANIFEST.MF content automatically. Manual changes to the classpath container are not allowed because they would be overwritten.

