L2Tool
======
Lineage 2 texture viewer/replacer.

<div align="center">
  <img src="images/image.png" alt="L2Tool Screenshot">
</div>

Run
---
```bash
# Build the project
./gradlew build

# Run with JavaFX (extracts javafx-17.0.2.zip automatically)
./run.bat
```

Build
-----
```bash
# Using Gradle directly
./gradlew build

# Using build scripts
# Windows
build-w.bat

# Linux/macOS (requires execute permissions)
chmod +x build-l.sh
./build-l.sh
```

### Build Scripts
- **build-w.bat** (Windows): Build script for Windows that compiles the project and shows build status
- **build-l.sh** (Linux/macOS): Build script for Linux/macOS that compiles the project. Requires execute permissions (`chmod +x build-l.sh`) before running

Requirements
------------
Java 17 or later is required.

## Features & Updates by ak4n1
- **Modern Platform Support**: Updated to Java 17 compatibility, Gradle 7.6, and JavaFX 17.0.2 support with automaticExtraction.
- **Deployment Scripts**: Added `run.bat` for easy execution, plus `build-w.bat` (Windows) and `build-l.sh` (Linux/macOS) for simplified building.
- **Texture Viewing**: Enhanced viewer for Lineage 2 UTX files with support for multiple formats (DXT, RGBA8, P8, G16, etc.).
- **Premium Custom UI**: Fully redesigned dark-themed dialogs with technical details (Format, Dimensions, MipMap count, Export Index).
- **Enhanced Exporting**:
  - **Individual Export**: Save selected textures instantly to `output_selected/`.
  - **Batch Export**: Export entire packages to `output_all/`.
  - **Format Support**: Choose between PNG, JPEG, BMP, WEBP, and DDS (for DXT textures).
  - **Export Toggles**: "Keep folder structure" to maintain package hierarchy and "Clear output folder" to keep your workspace clean.
- **Selection Persistence**: Fixed UI bugs to ensure the selected texture remains visible and active during interaction.
- **Texture Replacement**: Easy replacement of individual textures within UTX packages.