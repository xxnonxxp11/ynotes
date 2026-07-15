package app.uamo.ynotes.data

enum class SortOrder(val label: String) {
    UPDATED_DESC("Modificación ↓"),
    UPDATED_ASC("Modificación ↑"),
    CREATED_DESC("Creación ↓"),
    CREATED_ASC("Creación ↑"),
    NAME_ASC("Nombre A→Z"),
    NAME_DESC("Nombre Z→A"),
    SIZE_DESC("Tamaño ↓"),
    SIZE_ASC("Tamaño ↑");

    fun comparator(): Comparator<NoteEntity> = when (this) {
        UPDATED_DESC -> compareByDescending { it.updatedAt }
        UPDATED_ASC -> compareBy { it.updatedAt }
        CREATED_DESC -> compareByDescending { it.createdAt }
        CREATED_ASC -> compareBy { it.createdAt }
        NAME_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        NAME_DESC -> compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title }
        SIZE_DESC -> compareByDescending { it.title.length + it.body.length }
        SIZE_ASC -> compareBy { it.title.length + it.body.length }
    }
}
