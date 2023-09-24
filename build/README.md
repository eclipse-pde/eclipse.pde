# PDE buld is in deep maintenance mode

## Compatibility with newer Eclipse versions functionality
Enhancements to make use of new functionality implemented in latest versions of other components are most likely not implemented in pde.build.

## Bugs 
Bugs affecting in IDE Export functionality may still be worked on (until replacement functionality is provided) although with low priority. If you experience issues with headless builds it is highly likely that you would have to investigate and provide a patch fixing it.

## Alternative headless/standalone builds recommendation
[Tycho project](https://github.com/eclipse-tycho/tycho) is the most common and active project for building Eclipse RCP apps and thus is recommended as a replacement for pde.build.

If you need support in migration see https://github.com/eclipse-tycho/tycho#getting-support for details.