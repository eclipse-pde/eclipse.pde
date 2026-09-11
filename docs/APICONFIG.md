# .apiconfig File Documentation

## Overview

The `.apiconfig` file provides a way to configure API Tools version increment rules and error handling at the project level. This file should be placed in the root of your project, similar to `.gitignore` or `.editorconfig` files.

## File Format

The `.apiconfig` file uses a simple key-value format:

```properties
# Comments start with #
key = value
```

## Configuration Options

### Version Increment Rules

Version increment rules control how version numbers are suggested when API changes are detected.

**Format:** `<segment>.version.increment = <target_segment>+<amount>`

**Parameters:**
- `segment`: The semantic change level detected (`major`, `minor`, or `micro`)
- `target_segment`: Which version segment to actually increment (`major`, `minor`, or `micro`)
- `amount`: How much to increment (must be positive integer)

**Default behavior:** Each segment increments itself by 1:
- `major.version.increment = major+1`
- `minor.version.increment = minor+1`
- `micro.version.increment = micro+1`

**Example:** Eclipse Platform pattern (no major version changes):
```properties
# When breaking changes detected, suggest minor increment instead
major.version.increment = minor+1

# Standard minor increment for compatible changes
minor.version.increment = minor+1

# Micro increments by 100 for service releases
micro.version.increment = micro+100
```

### Error Handling Mode

Error modes control how version problems are reported or suppressed.

**Format:** `<segment>.version.error = <mode>`

**Modes:**
- `error`: Report as error (default)
- `warning`: Report as warning
- `ignore`: Don't report
- `filter`: Automatically create an API filter with explanatory comment

**Example:**
```properties
# Auto-filter major version warnings (when not using major versions)
major.version.error = filter

# Report minor version issues as errors
minor.version.error = error

# Report micro version issues as warnings
micro.version.error = warning
```

## Use Cases

### Use Case 1: Eclipse Platform Pattern

Eclipse Platform doesn't use major version increments. Configure this with:

```properties
# Redirect major version changes to minor increment
major.version.increment = minor+1

# Auto-suppress major version warnings
major.version.error = filter

# Standard behavior for minor and micro
minor.version.increment = minor+1
micro.version.increment = micro+1
```

### Use Case 2: Service Release Pattern

For projects that increment micro version by 100 for each service release:

```properties
major.version.increment = major+1
minor.version.increment = minor+1

# Increment micro by 100
micro.version.increment = micro+100
```

### Use Case 3: Lenient Versioning

For projects in early development that want warnings instead of errors:

```properties
major.version.error = warning
minor.version.error = warning
micro.version.error = warning
```

## File Location

The `.apiconfig` file should be placed in the root of your project:

```
my-project/
├── .apiconfig          # Configuration file
├── .api_filters        # API filters (may be auto-generated)
├── META-INF/
│   └── MANIFEST.MF
├── src/
└── ...
```

## Interaction with Existing Settings

The `.apiconfig` file complements existing API Tools settings:

1. **IDE Preferences**: The `.apiconfig` file only affects version increment calculations and error modes. Other API Tools settings (like API baseline configuration) remain in IDE preferences.

2. **API Filters**: When `error = filter` is configured, filters are automatically added to `.api_filters` with explanatory comments. Manual filters in `.api_filters` continue to work as before.

3. **Baseline Checking**: The `.apiconfig` file doesn't affect baseline comparison itself, only how version problems are reported.

## Example Complete Configuration

```properties
# Eclipse Platform style .apiconfig
# Place this file in the root of your project

# We don't use major version increments at Platform
# When breaking changes are detected, increment minor instead
major.version.increment = minor+1
major.version.error = filter

# Standard minor version increment for compatible API additions
minor.version.increment = minor+1
minor.version.error = error

# Micro version increments by 100 for service releases
micro.version.increment = micro+100
micro.version.error = error
```

## Migration Guide

### From Manual Configuration

If you previously configured version increment rules manually in the IDE:

1. Create a `.apiconfig` file in your project root
2. Add your version increment rules
3. Commit the file to version control
4. The settings will now be shared across all developers

### From Existing Projects

For existing projects without `.apiconfig`:

1. API Tools will continue to work with default behavior (each segment increments by 1)
2. Create `.apiconfig` file when you want to customize behavior
3. No changes needed to existing `.api_filters` files

## Troubleshooting

### Config file not working

- Ensure the file is named exactly `.apiconfig` (with leading dot)
- Check that the file is in the project root (not in a subdirectory)
- Verify the syntax: `key = value` format with no typos
- Check the Error Log view for parsing errors

### Unexpected version suggestions

- Review your `*.version.increment` settings
- Ensure increment amounts are positive integers
- Verify target segments are spelled correctly: `major`, `minor`, or `micro`

### Filters not being created

- Ensure `*.version.error = filter` is set correctly
- Check that you have write permissions to create `.api_filters`
- Verify that API Tools is enabled for your project

## See Also

- [API Tools Documentation](API_Tools.md)
- [API Filters](https://wiki.eclipse.org/PDE/API_Tools/User_Guide#API_Filters)
- [OSGi Versioning](https://www.osgi.org/wp-content/uploads/SemanticVersioning1.pdf)
