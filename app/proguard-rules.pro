# Pantry release R8 / ProGuard rules.
# Most rules are inherited from library consumer rules (Hilt, Firebase BoM,
# kotlinx-coroutines, Compose). The rules below cover what those don't.

# Domain model: Firestore (and the manual toMap()/toX() converters in the
# repos) rely on field names. R8 normally renames everything; keep the
# entire domain.model package as-is.
-keep class app.pantry.domain.model.** { *; }

# BuildConfig: Crashlytics' build-tool reflection reads versionName / versionCode
# off this class. Without this, the Crashlytics dashboard shows blank versions.
-keep class app.pantry.BuildConfig { *; }

# Compose preview & tooling classes that the release variant doesn't need but
# that R8 sees referenced from debug-only paths. Silence the warnings.
-dontwarn androidx.compose.ui.tooling.preview.**

# Coroutines uses ServiceLoader; the consumer rule covers this but we restate
# it defensively because broken coroutines is a particularly nasty crash mode.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler

# Firebase Functions: the callable contract relies on Map<String, *> shapes
# that R8 sees as Any. The consumer rule in firebase-functions handles this,
# but we add an explicit -dontwarn for the okhttp transitive dep that R8
# sometimes flags noisily.
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
