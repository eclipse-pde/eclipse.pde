## What are Execution Environments?

Execution environments (EEs) are symbolic representations of JREs. 
For example, rather than talking about a specific JRE, with a specific name at a specific location on your disk, you can talk about the JavaSE-21 execution environment.
The system can then be configured to use a specific JRE to implement that execution environment.

Execution environments are relevant both to development (compile) time and runtime.

## So what does this mean to me and my bundles?

Good bundles have minimal dependencies. 
This allows them to be used in a wider range of situations and results in a smaller overall footprint for applications built of these bundles. 
The execution environment needed by your bundle is key in both situations.

Consider the different outcomes for some bundle A that requires just JavaSE-17 and a bundle B that requires JavaSE-21. 
First, A can be used by applications using Java version 17. 
Second, an application written entirely of bundles such as A can ship on a Java 17 JRE. 

## How can this be configured?

[OSGi](https://www.osgi.org/) allows bundles to be marked with the minimum execution environment they require 
(see the [ Setting the Execution Environment](#setting-the-execution-environment)).
Setting this value has two effects.

1.  The compiler attempts to compile your bundle against the JRE
    associated with the EE you choose. For example, if you set your EE
    to be JavaSE-21, then the compiler will help you stick to that and
    not let you use APIs which exist in other class library versions. If
    you choose to increase your EE level, then you are forced to
    explicitly do so, rather than finding out later that you did it
    accidentally by referencing new APIs.
2.  The Equinox runtime will refuse to resolve/run your bundle if the
    current running JRE does not meet the minimum standard you
    specified. For example, if you attempt to install bundle B from
    above (requires JavaSE-21) on a system running JavaSE-17, B will
    not resolve or run.

This page describes how Execution Environments are defined within the PDE environment.

## Which Execution Environment should I use?

**This section is outdated. 
The general consensus today with more frequent Java releases is to keep EE more up to date for developer productivity.
See also [SimRel Release Requirements for Execution Environment](https://github.com/eclipse-simrel/.github/blob/main/wiki/SimRel/Simultaneous_Release_Requirements.md#execution-environment)**

As discussed above, you should seek to use the smallest EE which give you all the features you require. 
Practically speaking, when creating a new OSGi bundle JavaSE-17 is a reasonable starting point. 
Lower EE settings are only practical in very specialized applications. 
If your particular bundle requires capabilities from a later EE, then specify a higher EE, but keep in mind this may limit adoption of your bundle for applications using an older Java runtime.

Once a particular EE has been chosen, it should be left alone unless there is a clear advantage to moving up. 
Increasing your EE can create a lot of work with no real value, such as exposing your code to new warnings, deprecations, etc.

Projects should not leave these choices to chance. 
Dependency structures are key parts of an architecture. 
For example, the [Eclipse Project](https://eclipse.dev/eclipse) has explicitly identified EEs for all of their bundles. 
These choices are documented in the [project plan](https://eclipse.dev/eclipse/development/plans.html).
The execution environment listed in the table is based on the needs of the bundle and the expected use scenarios for the bundle.

## I have prerequisites that require version X to run, so shouldn't I require version X too?

This is not necessary and was not recommended in the past.
However, with the the fast evolving Java runtime environment, you may want to update your BREE if your pre-requisites require a higher Java version so that you can take advantages of the new features in Java.

## Setting the Execution Environment

1.  Use Eclipse-SDK in its latest version.
2.  Right click on your bundle's `MANIFEST.MF` and select **Open With...
    \> Plug-in Manifest Editor**.
3.  Select the **Overview** tab.
4.  Note the section in the lower left corner entitled **Execution
    Environments**.
5.  Add your appropriate environment(s) noting the [special
6.  Save the file.
7.  Select the link "update the classpath and compiler compliance
    settings".
8.  Ensure you have no compiling errors in your workspace.  
9.  Release your changes to the repository.

## Special cases


### Compiling against more than is required

In some cases, a bundle may optionally use function from newer execution
environments if available but fall back to some other behaviour if not.
Such bundles must be compiled against the maximum EE actually used in
their code. 

In these cases you must list both the EE required for compilation and
the absolute minimum EE for runtime in the **Execution Environment**
section of the bundle **Overview**. The EE needed for compilation
against must appear **first** in the list. 


## Managing Execution Environments

For the most part the Eclipse IDE manages the execution environments for
you. You do however have to have the relevant JREs installed on your
machine and configured into your IDE.

When you install a new JRE, the IDE analyzes the function it provides
and identifies the set of EEs it can support. The IDE distinguishes
between **exact matches** for an EE and compatible matches. You can see
all the execution environments and the JREs they match by looking at
**Window \> Preferences \> Java \> Installed JREs \> Execution
Environments**.

If you do not have an exact match for an EE that is specified in one of
your bundles but do have a compatible match, you get a warning.
Continuing development runs the risk of using API which will not be
present at runtime. If you have neither an exact or compatible match,
your bundle's project will fail to compile.

## Standard Execution Environments

The set of execution environments is extensible but the Eclipse IDE
includes support for the environments set out in the OSGi R4
specification. Some examples are listed in the table below.

| EE Name                    | Description                                                                                                                                                       |
|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **JavaSE-11**             | Java 11 release                                                                                                                                                    |
| **JavaSE-17**             | Java 17 release                                                                                                                                                    |
| **JavaSE-11**             | Java 17 release                                                                                                                                                    |

