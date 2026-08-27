-dontobfuscate
-dontusemixedcaseclassnames
-dontnote **

-libraryjars '<java.home>/jmods/java.base.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.logging.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.xml.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.sql.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.desktop.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.management.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.naming.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.net.http.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.security.jgss.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.transaction.xa.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.scripting.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.instrument.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/java.compiler.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/jdk.unsupported.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/jdk.jfr.jmod'(!**.jar;!module-info.class)
-libraryjars '<java.home>/jmods/jdk.net.jmod'(!**.jar;!module-info.class)

-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod,Exceptions,Record
-keep class kotlin.Metadata { *; }

-keep class essential.core.Main { *; }

-keep,allowoptimization class essential.core.Commands { *; }

-keep class essential.core.service.**Service { *; }
-keep class essential.core.service.**Service$Companion { *; }
-keep class essential.core.service.effect.EffectSystem { *; }
-keep class essential.core.service.vote.VoteSystem { *; }
-keep class essential.core.service.achievements.AchievementHooks { *; }
-keep class essential.core.service.web.achievement.AchievementWebModule { *; }
-keep class essential.core.service.migration.FlywayMigration { *; }

-keepdirectories db/migration

-keep interface kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader
-keep class * implements kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader { public protected *; }
-keep interface kotlin.reflect.jvm.internal.impl.resolve.ExternalOverridabilityCondition
-keep class * implements kotlin.reflect.jvm.internal.impl.resolve.ExternalOverridabilityCondition { public protected *; }
-keep interface kotlin.reflect.jvm.internal.impl.km.internal.extensions.MetadataExtensions
-keep class * implements kotlin.reflect.jvm.internal.impl.km.internal.extensions.MetadataExtensions { public protected *; }

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static ** Companion;
}
-if @kotlinx.serialization.internal.NamedCompanion class *
-keepclassmembers class * {
    static <1> *;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers public class **$$serializer {
    private ** descriptor;
}

-keep,allowoptimization class **$$serializer { *; }
-keepclassmembers class **$Companion {
    public kotlinx.serialization.KSerializer serializer(...);
}

-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keepclassmembers class kotlin.coroutines.SafeContinuation { volatile <fields>; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-keepclassmembers class io.netty.** { volatile <fields>; }
-keepclassmembers class reactor.** { volatile <fields>; }

-keep,allowshrinking,allowobfuscation class kotlinx.coroutines.** { *; }

-keep,allowshrinking,allowobfuscation class okhttp3.** { *; }
-keep,allowshrinking,allowobfuscation class okio.** { *; }

-keep,allowshrinking,allowobfuscation class io.netty.util.internal.logging.Log4J2Logger* { *; }

-keep,allowshrinking,allowobfuscation class reactor.netty.**Micrometer** { *; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements org.flywaydb.core.extensibility.Plugin { public <init>(); }
-keep class * implements java.sql.Driver { public <init>(); }
-keep class * implements io.r2dbc.spi.ConnectionFactoryProvider { public <init>(); }
-keep class * implements org.mariadb.r2dbc.authentication.AuthenticationPlugin { public <init>(); }
-keep class * implements io.r2dbc.postgresql.extension.Extension { public <init>(); }
-keep class * implements org.jetbrains.exposed.v1.r2dbc.mappers.TypeMapper { public <init>(); }
-keep class * implements io.ktor.server.config.ConfigLoader { public <init>(); }
-keep class * implements io.ktor.serialization.kotlinx.KotlinxSerializationExtensionProvider { public <init>(); }
-keep class * implements kotlin.metadata.internal.extensions.MetadataExtensions { public <init>(); }
-keep class * implements com.fasterxml.jackson.databind.Module { public <init>(); }
-keep class * extends com.fasterxml.jackson.core.JsonFactory { public <init>(); }
-keep class * extends com.fasterxml.jackson.databind.ObjectMapper { public <init>(); }
-keep class * implements com.ongres.stringprep.Profile { public <init>(); }

-keep class org.h2.Driver { public <init>(); }
-keep class org.postgresql.Driver { public <init>(); }
-keep class org.mariadb.jdbc.Driver { public <init>(); }

-keepclassmembers class * implements org.flywaydb.core.extensibility.ConfigurationExtension {
    public <fields>;
    public <methods>;
}

-keep class * implements org.mariadb.jdbc.plugin.AuthenticationPluginFactory { public <init>(); }
-keep class * implements org.mariadb.jdbc.plugin.Codec { public <init>(); }
-keep class * implements org.mariadb.jdbc.plugin.CredentialPlugin { public <init>(); }
-keep class * implements org.mariadb.jdbc.plugin.TlsSocketPlugin { public <init>(); }

-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.JsonCreator <init>(...);
    @com.fasterxml.jackson.annotation.JsonCreator <methods>;
    @com.fasterxml.jackson.annotation.JsonProperty <fields>;
    @com.fasterxml.jackson.annotation.JsonProperty <methods>;
}

-dontwarn arc.**
-dontwarn mindustry.**
-dontwarn server.**
-dontwarn org.junit.**
-dontwarn org.mozilla.javascript.**
-dontwarn java.lang.instrument.**
-dontwarn sun.misc.**
-dontwarn jdk.internal.**
-dontwarn io.micrometer.**
-dontwarn reactor.blockhound.**
-dontwarn reactor.netty.**
-dontwarn io.netty.internal.tcnative.**
-dontwarn io.netty.incubator.channel.uring.**
-dontwarn io.netty.util.internal.logging.Log4J2Logger
-dontwarn io.netty.util.internal.**
-dontwarn io.netty.buffer.VarHandleByteBufferAccess
-dontwarn io.netty.handler.codec.compression.**
-dontwarn io.netty.handler.codec.protobuf.**
-dontwarn io.netty.handler.ssl.OpenSslParametersUtil
-dontwarn org.eclipse.jetty.alpn.**
-dontwarn org.eclipse.jetty.npn.**
-dontwarn io.netty.handler.ssl.util.**
-dontwarn io.netty.pkitesting.**
-dontwarn org.conscrypt.**
-dontwarn org.jboss.marshalling.**
-dontwarn org.jboss.vfs.**
-dontwarn org.osgi.**
-dontwarn javax.servlet.**
-dontwarn jakarta.servlet.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn com.google.common.hash.Hashing$Crc32cMethodHandles
-dontwarn org.locationtech.jts.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.lucene.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.graalvm.**
-dontwarn com.oracle.svm.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.aayushatharva.brotli4j.**
-dontwarn com.jcraft.jzlib.**
-dontwarn com.ning.compress.**
-dontwarn kotlinx.atomicfu.**
-dontwarn kotlin.concurrent.atomics.**
-dontwarn kotlin.jvm.internal.EnhancedNullability
-dontwarn kotlin.reflect.jvm.internal.impl.types.model.AnnotationMarker
-dontwarn io.asyncer.r2dbc.mysql.client.ZstdCompressor
-dontwarn org.h2.**
-dontwarn org.postgresql.osgi.**
-dontwarn org.postgresql.gss.**
-dontwarn org.postgresql.jdbc.PgConnection
-dontwarn okhttp3.internal.graal.**
-dontwarn org.openjsse.**
-dontwarn org.jspecify.annotations.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn android.annotation.**
-dontwarn org.apache.commons.text.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.flywaydb.core.internal.logging.apachecommons.**
-dontwarn com.p6spy.**
-dontwarn software.amazon.awssdk.**
-dontwarn com.amazonaws.**
-dontwarn com.google.cloud.**
-dontwarn waffle.**
-dontwarn com.sun.jna.**
-dontwarn org.mariadb.jdbc.client.socket.impl.UnixDomainSocket**
-dontwarn com.google.crypto.tink.**
-dontwarn org.slf4j.impl.**
-dontwarn javax.annotation.**
-dontwarn com.google.devtools.ksp.**
-dontwarn com.squareup.kotlinpoet.**
