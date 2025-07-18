# Disable obfuscation (no random package names)
-dontobfuscate

# Keep the main entry points
-keep class essential.**.*Main { *; }
-keep class essential.**.Main { *; }

# Keep all classes in the essential package
-keep class essential.** { *; }

# Keep essential.web classes specifically
-keep class essential.web.** { *; }
-keepclassmembers class essential.web.** {
    <fields>;
    <methods>;
}
-keep class essential.web.Main { *; }
-keep class essential.web.WebServer { *; }
-keepclassmembers class essential.web.Main {
    <fields>;
    <methods>;
    public void init();
}
-keepclassmembers class essential.web.WebServer {
    <fields>;
    <methods>;
    public void start();
}

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Kotlin reflection classes
-keep class kotlin.jvm.internal.** { *; }
-keep class kotlin.jvm.** { *; }
-keep class kotlin.** { *; }
-dontwarn kotlin.**

# Keep Java reflection and enum classes
-keep class java.lang.reflect.** { *; }
-keep class java.util.EnumMap { *; }
-keep class java.util.EnumSet { *; }
-keep class java.lang.Enum { *; }
-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep enum fields and methods
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# Keep serialization-related classes
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Keep kotlinx.serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }

# Keep Arc and Mindustry classes
-keep class arc.** { *; }
-keep class mindustry.** { *; }

# Keep JDA (Discord) classes if used
-keep class net.dv8tion.jda.** { *; }

# Keep Exposed (database) classes
-keep class org.jetbrains.exposed.** { *; }
-keep class org.jetbrains.exposed.exceptions.** { *; }
-keep class org.jetbrains.exposed.sql.** { *; }
-keep class org.jetbrains.exposed.sql.transactions.** { *; }

# Specifically keep the problematic class and method
-keep class org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManagerKt {
    public static void handleSQLException(java.sql.SQLException, org.jetbrains.exposed.sql.Transaction, int);
}

# Don't optimize the problematic method to avoid stack manipulation issues
-keepclassmembers,allowshrinking class org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManagerKt {
    public static void handleSQLException(java.sql.SQLException, org.jetbrains.exposed.sql.Transaction, int);
}

# Disable optimization for this class
-optimizations !class/merging/*, !code/allocation/variable

# Completely disable optimization for the specific method
-keep,allowshrinking class org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManagerKt {
    public static void handleSQLException(java.sql.SQLException, org.jetbrains.exposed.sql.Transaction, int);
}
-dontoptimize

# Preserve the stack for this method
-keepclasseswithmembers,includedescriptorclasses class org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManagerKt {
    public static void handleSQLException(java.sql.SQLException, org.jetbrains.exposed.sql.Transaction, int);
}

# Keep SQL-related classes
-keep class java.sql.** { *; }
-dontwarn java.sql.**
-keep class javax.sql.** { *; }
-dontwarn javax.sql.**

# Keep java.util collections
-keep class java.util.** { *; }
-dontwarn java.util.**

# Keep Ktor classes if used
-keep class io.ktor.** { *; }

# Keep Netty classes used by Ktor
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# Keep specific Netty classes mentioned in the stack trace
-keep class io.netty.util.internal.PlatformDependent { *; }
-keep class io.netty.util.internal.PlatformDependent$* { *; }
-keep class io.netty.util.internal.UnsafeAccess { *; }
-keep class io.netty.util.internal.UnsafeAccess$* { *; }
-keep class io.netty.util.concurrent.** { *; }
-keep class io.netty.channel.** { *; }
-keep class io.netty.channel.nio.** { *; }
-keep class io.netty.channel.socket.** { *; }

# Keep Netty internal classes and their fields
-keep class io.netty.util.internal.** { *; }
-keepclassmembers class io.netty.util.internal.** {
    <fields>;
    <methods>;
}

# Keep Netty NIO event loop classes
-keep class io.netty.channel.nio.NioEventLoop { *; }
-keep class io.netty.channel.nio.NioEventLoopGroup { *; }
-keepclassmembers class io.netty.channel.nio.NioEventLoop {
    <fields>;
    <methods>;
}
-keepclassmembers class io.netty.channel.nio.NioEventLoopGroup {
    <fields>;
    <methods>;
}

# Keep Netty MultithreadEventExecutorGroup and related classes
-keep class io.netty.util.concurrent.MultithreadEventExecutorGroup { *; }
-keep class io.netty.util.concurrent.MultithreadEventLoopGroup { *; }
-keep class io.netty.channel.MultithreadEventLoopGroup { *; }
-keep class io.netty.channel.AbstractEventLoopGroup { *; }
-keepclassmembers class io.netty.util.concurrent.MultithreadEventExecutorGroup {
    <fields>;
    <methods>;
}

# Keep specific Netty queue classes
-keep class io.netty.util.internal.shaded.org.jctools.queues.** { *; }
-keep class io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueue { *; }
-keepclassmembers class io.netty.util.internal.shaded.org.jctools.queues.BaseMpscLinkedArrayQueue {
    <fields>;
    <methods>;
}

# Keep JCTools classes (might be used directly or shaded by Netty)
-keep class org.jctools.** { *; }
-dontwarn org.jctools.**
-keep class org.jctools.queues.** { *; }
-keep class org.jctools.queues.BaseMpscLinkedArrayQueue { *; }
-keepclassmembers class org.jctools.queues.BaseMpscLinkedArrayQueue {
    <fields>;
    <methods>;
}

# Keep specific fields in queue classes
-keepclassmembers class ** {
    ** producerIndex;
    ** consumerIndex;
    ** producerLimit;
    ** consumerLimit;
}

# Keep field names accessed via reflection
-keepclassmembers class ** {
    long producerIndex;
    long consumerIndex;
    java.lang.Object[] elements;
}

# Keep sun.misc.Unsafe and related classes
-keep class sun.misc.Unsafe { *; }
-dontwarn sun.misc.Unsafe
-keep class sun.misc.** { *; }
-dontwarn sun.misc.**

# Keep JDK internal classes that might be used by Netty
-keep class java.nio.** { *; }
-dontwarn java.nio.**
-keep class sun.nio.** { *; }
-dontwarn sun.nio.**

# Keep Ktor Netty server classes
-keep class io.ktor.server.netty.** { *; }
-keep class io.ktor.server.netty.NettyApplicationEngine { *; }
-keep class io.ktor.server.netty.EventLoopGroupProxy { *; }
-keep class io.ktor.server.netty.EventLoopGroupProxy$Companion { *; }
-keepclassmembers class io.ktor.server.netty.** {
    <fields>;
    <methods>;
}
-keepclassmembers class io.ktor.server.netty.NettyApplicationEngine {
    <fields>;
    <methods>;
}
-keepclassmembers class io.ktor.server.netty.EventLoopGroupProxy {
    <fields>;
    <methods>;
}
-keepclassmembers class io.ktor.server.netty.EventLoopGroupProxy$Companion {
    <fields>;
    <methods>;
}

# Keep Ktor server engine classes
-keep class io.ktor.server.engine.** { *; }
-keepclassmembers class io.ktor.server.engine.** {
    <fields>;
    <methods>;
}
-keep class io.ktor.server.engine.EmbeddedServer { *; }
-keepclassmembers class io.ktor.server.engine.EmbeddedServer {
    <fields>;
    <methods>;
}

# Specifically keep EmbeddedServer inner classes
-keep class io.ktor.server.engine.EmbeddedServer$* { *; }
-keepclassmembers class io.ktor.server.engine.EmbeddedServer$* {
    <fields>;
    <methods>;
}

# Specifically keep the applicationInstance inner class
-keep class io.ktor.server.engine.EmbeddedServer$applicationInstance$1 { *; }
-keepclassmembers class io.ktor.server.engine.EmbeddedServer$applicationInstance$1 {
    <fields>;
    <methods>;
}

# Keep reflection data
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep source file names for better debugging
-keepattributes SourceFile,LineNumberTable

# Specify that we want to create a single jar
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Ignore warnings about missing classes
-dontwarn **
-ignorewarnings

# Keep classes accessed via reflection
-keepclassmembers class * {
    ** valueOf(java.lang.String);
    ** values();
}

# Keep any classes that might be accessed via reflection
-keepclassmembers class * {
    public <init>(java.lang.reflect.InvocationHandler);
}

# Keep Kotlin classes that might be accessed via reflection
-keepclassmembers class kotlin.** {
    <fields>;
    <methods>;
}

# Keep classes with @Serializable annotation
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}

# Keep classes with constructors that might be used by serialization
-keepclassmembers class * {
    public <init>(...);
}

# Keep JDK internal classes that might be accessed
-keep class jdk.internal.** { *; }
-dontwarn jdk.internal.**

# Keep Rhino classes
-keep class rhino.** { *; }
-dontwarn rhino.**

# Keep Kotlin Serialization classes
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-keep class kotlinx.serialization.json.** { *; }
-keepclassmembers class kotlinx.serialization.json.** { *; }

# Keep KAML classes
-keep class com.charleskorn.kaml.** { *; }
-keepclassmembers class com.charleskorn.kaml.** { *; }

# Keep Permission classes
-keep class essential.permission.Permission { *; }
-keep class essential.permission.Permission$PermissionData { *; }
-keep class essential.permission.Permission$RoleConfig { *; }
-keepclassmembers class essential.permission.Permission {
    <fields>;
    <methods>;
}
-keepclassmembers class essential.permission.Permission$PermissionData {
    <fields>;
    <methods>;
}
-keepclassmembers class essential.permission.Permission$RoleConfig {
    <fields>;
    <methods>;
}

-keep class org.sqlite.** { *; }
