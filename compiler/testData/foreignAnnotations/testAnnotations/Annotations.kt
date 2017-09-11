package kotlin.annotations.jvm


public enum class MigrationStatus {
    IGNORE,
    WARN,
    @Deprecated("experimental feature")
    STRICT
}

@Target(AnnotationTarget.ANNOTATION_CLASS)
public annotation class UnderMigration(val status: MigrationStatus)
