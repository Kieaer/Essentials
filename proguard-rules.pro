# Essentials ProGuard configuration
# Entry point: essential.core.Main (Mindustry Plugin)
# Safe defaults: we disable obfuscation to avoid breaking reflection-heavy frameworks.
# You can enable obfuscation later by removing -dontobfuscate and relying on the keep rules below.

# 1) Global settings
# Ignore all warnings to unblock optimization and preverification.
-ignorewarnings
# Ensure ProGuard analyzes all library classes fully (JDK modules, etc.)
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
# Keep obfuscation disabled (safer for reflection-heavy Mindustry/Kotlin)
-dontobfuscate
# Enable shrinking and optimization/preverification (default when not disabled).

# Keep important class and parameter/annotation metadata used by Kotlin, serialization, and reflection.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions,SourceFile,LineNumberTable,Record
# Keep Kotlin metadata classes broadly without naming a specific class (avoids config-time validation issues)
-keep class kotlin.** { *; }

# Provide Java runtime modules to ProGuard for analysis (required on JDK 9+)
# Using <java.home> makes this portable across machines and CI agents
-libraryjars '<java.home>/jmods/java.base.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.logging.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.xml.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.sql.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.desktop.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/jdk.unsupported.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.management.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.naming.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/jdk.jfr.jmod'(!**.jar;!module-info.class)

# 2) Mindustry plugin entry points
# Keep Essentials core package and any classes that extend Mindustry's Plugin (wildcard to avoid config-time validation)
-keep class essential.core.** { *; }
-keep class ** extends mindustry.mod.** { *; }

# 2b) Stabilize optimizer: don’t optimize our own code (but still allow shrinking)
# This avoids rare evaluator crashes in methods like essential.core.Main#init when
# host-provided supertypes (e.g., mindustry.mod.Plugin) are only provided as library jars.
-keep,allowshrinking class essential.** { *; }

# Optionally, avoid optimizing known reflection-heavy third-party libs while still shrinking them.
-keep,allowshrinking class com.fasterxml.jackson.** { *; }
-keep,allowshrinking class com.auth0.jwk.** { *; }
-keep,allowshrinking class it.krzeminski.snakeyaml.** { *; }
-keep,allowshrinking class com.charleskorn.kaml.** { *; }
-keep,allowshrinking class org.apache.logging.log4j.** { *; }
-keep,allowshrinking class kotlinx.datetime.** { *; }

# Mindustry/Arc live on the host, not inside our shaded jar; suppress missing-type warnings
-dontwarn mindustry.**
-dontwarn arc.**

# 3) Kotlinx Serialization
# Keep generated serializers
-keep class **$$serializer { *; }
# Serializer discovery helpers (Companion.serializer() and top-level serializer(...) functions)
-keepclassmembers class ** {
    public static ** Companion;
    public static ** serializer(...);
}
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# 4) Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 5) Generated code by KSP (command/event registrations, etc.)
-keep class essential.**.generated.** { *; }

# 6) Web server stack (Ktor Netty) and JSON
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# 6b) Reactor / Reactor Netty metrics: avoid optimizing classes that depend on optional Micrometer
# Reactor Netty has handlers that reference io.micrometer.observation.* when metrics are enabled.
# On the analysis classpath these may be absent, which can crash optimizer with IncompleteClassHierarchyException.
# Keep them from being optimized (but still allow shrink if unused), and suppress warnings about optional types.
-keep,allowshrinking class reactor.netty.** { *; }
-keep,allowshrinking class io.projectreactor.** { *; }
-dontwarn reactor.netty.**
-dontwarn io.projectreactor.**
-dontwarn io.micrometer.**

# 7) Database stack (Exposed R2DBC)
-keep class org.jetbrains.exposed.** { *; }
-dontwarn org.jetbrains.exposed.**
-dontwarn io.r2dbc.**

# Keep R2DBC providers to ensure ServiceLoader discovery works
-keep class io.r2dbc.** { *; }
-keep class org.postgresql.** { *; }

# 8) (Optional) Resource handling
# ProGuard doesn't remove or rename resources unless configured with -adaptresourcefilenames/-adaptresourcefilecontents.
# Since we don't use those options here, no explicit resource keep rules are required.

# 9) If you enable obfuscation later, you may also want to preserve names in these packages to be extra safe:
# -keepnames class essential.** { *; }
# -keepnames class io.ktor.** { *; }
# -keepnames class org.jetbrains.exposed.** { *; }