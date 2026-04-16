package at.techbee.spectacled.screens.core.domain

/**
 * Defines the most important CalDAV/WebDAV privileges.
 *
 * The raw value is the 'localName' of the privilege tag (e.g., "read" for <D:read/>).
 */
enum class CalDavPrivilege(val tag: String) {
    // --- Data Access Privileges (Most Common) ---

    /** The ability to retrieve the resource's contents and properties. */
    READ("read"),

    /** The ability to modify the resource's contents (content-only). */
    WRITE_CONTENT("write-content"),

    /** The ability to modify the resource's properties (metadata-only). */
    WRITE_PROPERTIES("write-properties"),

    /** The full write access: write-content, write-properties, creation, and deletion. */
    WRITE("write"),

    /** The CalDAV-specific privilege to retrieve free/busy information. */
    READ_FREE_BUSY("read-free-busy"),


    // --- Structural Privileges (Creation/Deletion) ---

    /** The ability to add a new calendar to the collection. */
    BIND("bind"),

    /** The ability to delete a calendar from the collection. */
    UNBIND("unbind"),

    /** Grants all privileges on the resource. */
    ALL("all"),


    // --- Permission/ACL Privileges (Less Common) ---

    /** The ability to view the Access Control List (sharing permissions). */
    READ_ACL("read-acl"),

    /** The ability to retrieve the current-user-privilege-set property itself. */
    READ_CURRENT_USER_PRIVILEGE_SET("read-current-user-privilege-set");


    companion object {
        /**
         * Converts the raw privilege tag name string from the parser into the enum.
         * Returns null if the privilege is not one of the mapped types.
         */
        fun fromTag(tag: String): CalDavPrivilege? {
            return entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
        }
    }
}