// Top-level build file. Individual modules configure their own plugins.
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}

// This project sometimes lives on a non-native (exFAT) volume where macOS keeps
// creating AppleDouble "._*" files. Those break AGP's resource and dex merge
// tasks. Set WORKPLANNER_BUILD_DIR to redirect all build output to a native
// filesystem (APFS) when building from such a volume.
val externalBuildDir: String? = providers.environmentVariable("WORKPLANNER_BUILD_DIR").orNull
if (!externalBuildDir.isNullOrBlank()) {
    layout.buildDirectory.set(file(externalBuildDir))
    subprojects {
        layout.buildDirectory.set(file("$externalBuildDir/${project.name}"))
    }
}
