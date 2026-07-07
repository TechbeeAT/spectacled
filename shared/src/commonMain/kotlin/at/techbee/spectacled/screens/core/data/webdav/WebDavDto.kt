package at.techbee.spectacled.screens.core.data.webdav

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlSerializer
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.UnknownChildHandler
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlParsingException
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import nl.adaptivity.xmlutil.skipElement
import nl.adaptivity.xmlutil.xml
import nl.adaptivity.xmlutil.xmlCollapseWhitespace
import kotlin.math.roundToInt


private const val NAMESPACE_DAV = "DAV:"
private const val NAMESPACE_CALDAV = "urn:ietf:params:xml:ns:caldav"
private const val NAMESPACE_CARDDAV = "urn:ietf:params:xml:ns:carddav"
private const val NAMESPACE_APPLE_ICAL = "http://apple.com/ns/ical/"
private const val NAMESPACE_CALENDARSERVER = "http://calendarserver.org/ns/"
private const val PREFIX_WILDCARD = ""
private const val PREFIX_CALDAV = "cal"
private const val PREFIX_CARDDAV = "card"
private const val PREFIX_APPLE_ICAL = "apple"
private const val PREFIX_CALENDARSERVER = "cserver"




@OptIn(ExperimentalXmlUtilApi::class)
val calDavXml = XML {
    xmlVersion = XmlVersion.XML10
    isCollectingNSAttributes = true
    autoPolymorphic = true // Good practice for complex XML
    policy = DefaultXmlSerializationPolicy.Builder().apply {
        unknownChildHandler = UnknownChildHandler { _, _, _, name, _->
            // Return an empty list to signify "do nothing and continue parsing"
            println("Unknown children found: $name")
            emptyList()
        }
    }.build()
}

@Serializable
@XmlSerialName("multistatus", NAMESPACE_DAV, PREFIX_WILDCARD)
data class WebDavMultiStatus(

    @XmlElement
    @XmlSerialName("sync-token", NAMESPACE_DAV, PREFIX_WILDCARD)
    val syncToken: String? = null,

    @XmlElement
    @XmlSerialName("response", NAMESPACE_DAV, PREFIX_WILDCARD)
    val responses: List<WebDavResponse>
)

@Serializable
@XmlSerialName("response", NAMESPACE_DAV, PREFIX_WILDCARD)
data class WebDavResponse(

    @XmlElement
    @XmlSerialName("href", NAMESPACE_DAV, PREFIX_WILDCARD)
    val href: String,

    @XmlElement
    @XmlSerialName("propstat", NAMESPACE_DAV, PREFIX_WILDCARD)
    val propstat: List<WebDavPropStat>
)

@Serializable
@XmlSerialName("propstat", NAMESPACE_DAV, PREFIX_WILDCARD)
data class WebDavPropStat(

    @XmlElement
    @XmlSerialName("prop", NAMESPACE_DAV, PREFIX_WILDCARD)
    val prop: WebDavProp,

    @XmlElement
    @XmlSerialName("status", NAMESPACE_DAV, PREFIX_WILDCARD)
    val status: String

) {

    fun isDirectory() =
        prop.resourceType?.collection != null
}

@Serializable
@XmlSerialName("current-user-principal", NAMESPACE_DAV, PREFIX_WILDCARD)
data class CurrentUserPrincipal(
    @XmlElement
    @XmlSerialName("href", NAMESPACE_DAV, PREFIX_WILDCARD)
    val href: String? = null
)

@Serializable
@XmlSerialName("calendar-home-set",  NAMESPACE_CALDAV, PREFIX_CALDAV)
data class CalendarHomeSet(
    @XmlElement
    @XmlSerialName("href", NAMESPACE_DAV, PREFIX_WILDCARD)
    val hrefs: List<String> = emptyList()
)


@Serializable
@XmlSerialName("calendar-user-address-set",  NAMESPACE_CALDAV, PREFIX_CALDAV)
data class CalendarUserAddressSet(
    @XmlElement
    @XmlSerialName("href", NAMESPACE_DAV, PREFIX_WILDCARD)
    val hrefs: List<String> = emptyList()
)




@Serializable
@XmlSerialName("prop", NAMESPACE_DAV, PREFIX_WILDCARD)
data class WebDavProp(

    @XmlElement
    @XmlSerialName("getlastmodified", NAMESPACE_DAV, PREFIX_WILDCARD)
    val lastModified: String? = null,

    @XmlElement
    @XmlSerialName("getcontentlength", NAMESPACE_DAV, PREFIX_WILDCARD)
    @Serializable(LongOrNullSerializer::class)
    val contentLength: Long? = null,

    @XmlElement
    @XmlSerialName("getcontenttype", NAMESPACE_DAV, PREFIX_WILDCARD)
    val contentType: String? = null,

    @XmlElement
    @XmlSerialName("quota-used-bytes", NAMESPACE_DAV, PREFIX_WILDCARD)
    @Serializable(LongOrNullSerializer::class)
    val quotaUsedBytes: Long? = null,

    @XmlElement
    @XmlSerialName("quota-available-bytes", NAMESPACE_DAV, PREFIX_WILDCARD)
    @Serializable(LongOrNullSerializer::class)
    val quotaAvailableBytes: Long? = null,

    @XmlElement
    @XmlSerialName("getetag", NAMESPACE_DAV, PREFIX_WILDCARD)
    val getETag: String? = null,


    @XmlElement
    @XmlSerialName("displayname", NAMESPACE_DAV, PREFIX_WILDCARD)
    val displayName: String? = null,

    @XmlElement
    @XmlSerialName("current-user-principal", NAMESPACE_DAV, PREFIX_WILDCARD)
    val currentUserPrincipal: CurrentUserPrincipal? = null,

    @XmlElement
    @XmlSerialName("calendar-home-set", NAMESPACE_CALDAV, PREFIX_CALDAV)
    val calendarHomeSet: CalendarHomeSet? = null,

    @XmlElement
    @XmlSerialName("calendar-user-address-set", NAMESPACE_CALDAV, PREFIX_CALDAV)
    val calendarUserAddressSet: CalendarUserAddressSet? = null,

    @XmlElement
    @XmlSerialName("calendar-description",  NAMESPACE_CALDAV, PREFIX_CALDAV)
    val calendarDescription: String? = null,

    @XmlElement
    @XmlSerialName("getctag", NAMESPACE_CALENDARSERVER, PREFIX_CALENDARSERVER)
    val getCTag: String? = null,

    @XmlElement
    @XmlSerialName("calendar-data",  NAMESPACE_CALDAV, PREFIX_CALDAV)
    val calendarData: String? = null,

    @XmlElement
    @XmlSerialName("schedule-tag",  NAMESPACE_CALDAV, PREFIX_CALDAV)
    val scheduleTag: String? = null,

    @XmlElement
    @XmlSerialName("calendar-color",  NAMESPACE_APPLE_ICAL, PREFIX_APPLE_ICAL)
    @Serializable(ColorSerializer::class)
    val calendarColor: Color? = null,

    @XmlElement
    @XmlSerialName("attachment-collection", NAMESPACE_CALDAV, PREFIX_CALDAV)
    val attachmentCollection: HrefProperty? = null,

    @XmlElement
    @XmlSerialName("calendar-dropbox", NAMESPACE_APPLE_ICAL, PREFIX_APPLE_ICAL)
    val calendarDropbox: HrefProperty? = null,

    @XmlElement
    @XmlSerialName("dropbox-home-set", NAMESPACE_CALDAV, PREFIX_CALDAV)
    val dropboxHomeSet: HrefProperty? = null,

    @XmlElement(true)
    @XmlSerialName("supported-calendar-component-set", NAMESPACE_CALDAV, PREFIX_CALDAV)
    val supportedCalendarComponentSet: SupportedCalendarComponentSet? = null,

    @XmlElement
    @XmlSerialName("resourcetype", NAMESPACE_DAV, PREFIX_WILDCARD)
    val resourceType: ResourceType? = null,


    @XmlElement
    @XmlSerialName("current-user-privilege-set", NAMESPACE_DAV, PREFIX_WILDCARD)
    val currentUserPrivilegeSet: CurrentUserPrivilegeSet? = null
)


/**
 * Represents the <cal:comp> tag, which has a "name" attribute.
 * e.g., <cal:comp name="VJOURNAL"/>
 */
@Serializable
@XmlSerialName("comp", NAMESPACE_CALDAV, PREFIX_CALDAV)
data class CalendarComp(
    @XmlElement(false)
    val name: String
)

/**
 * Represents the <cal:supported-calendar-component-set> tag,
 * which contains a list of <cal:comp> tags.
 */
@Serializable
@XmlSerialName("supported-calendar-component-set", NAMESPACE_CALDAV, PREFIX_CALDAV)
data class SupportedCalendarComponentSet(
    @XmlElement
    @XmlSerialName("comp", NAMESPACE_CALDAV, PREFIX_CALDAV)
    val components: List<CalendarComp> = emptyList()
)


/**
 * Represents the <d:current-user-privilege-set> tag which contains a list of privileges.
 */
@Serializable
@XmlSerialName("current-user-privilege-set", NAMESPACE_DAV, PREFIX_WILDCARD)
data class CurrentUserPrivilegeSet(
    @XmlElement
    @XmlSerialName("privilege", NAMESPACE_DAV, PREFIX_WILDCARD)
    val privileges: List<PrivilegeName> = emptyList(),
)

/**
 * Represents the actual privilege tag (e.g., <D:read/>, <D:write/>).
 * This class uses a custom serializer to extract the tag name.
 */
@Serializable(PrivilegeNameSerializer::class)
data class PrivilegeName(
    val name: String
)


@OptIn(ExperimentalSerializationApi::class, ExperimentalXmlUtilApi::class)
object PrivilegeNameSerializer : XmlSerializer<PrivilegeName> {

    // Define a simple descriptor, as we are manually handling the structure
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("PrivilegeName", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): PrivilegeName {
        throw NotImplementedError("Non-XML deserialization not supported")
    }

    override fun serialize(encoder: Encoder, value: PrivilegeName) {
        throw NotImplementedError("Serialization not supported for this type")
    }

    override fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: PrivilegeName?,
        isValueChild: Boolean
    ): PrivilegeName {
        // Start reading the children of <D:privilege>
        input.require(EventType.START_ELEMENT, NAMESPACE_DAV, "privilege")

        var privilegeName: String? = null

        // Loop through the content inside <D:privilege>
        while (input.hasNext()) {
            input.next()
            when (input.eventType) {
                EventType.START_ELEMENT -> {
                    // This is the actual privilege tag (e.g., <D:read> or <CAL:read-free-busy>)
                    privilegeName = input.localName
                    // Important: Skip the content of this element (it's an empty tag anyway)
                    // This moves the reader past the END_ELEMENT of the privilege tag
                    input.skipElement()
                }
                EventType.END_ELEMENT -> {
                    // End of the <D:privilege> wrapper
                    input.require(EventType.END_ELEMENT, NAMESPACE_DAV, "privilege")
                    break
                }
                // Ignore all other events (comments, whitespace, processing instructions, etc.)
                else -> {}
            }
        }

        // If no privilege name was found, it means <D:privilege> was empty or malformed.
        val name = privilegeName ?: input.extLocationInfo?.let {
            throw XmlParsingException(
                "Expected a child element inside <D:privilege>",
                it.toString()
            )
        }

        return PrivilegeName(name!!)
    }

    override fun serializeXML(encoder: Encoder, output: XmlWriter, value: PrivilegeName, isValueChild: Boolean) {
        // Since we are only focusing on parsing, we don't need to implement XML serialization.
        throw NotImplementedError("Serialization not supported for this type")
    }
}


/**
 * Represents the <d:resourcetype> tag
 */
@Serializable
@XmlSerialName("resourcetype", NAMESPACE_DAV, PREFIX_WILDCARD)
data class ResourceType(

    @XmlElement(true)
    @XmlSerialName("collection", NAMESPACE_DAV, PREFIX_WILDCARD)
    val collection: DavCollection? = null,

    @XmlElement(true)
    @XmlSerialName("principal", NAMESPACE_DAV, PREFIX_WILDCARD)
    val principal: DavPrincipal? = null,

    @XmlElement(true)
    @XmlSerialName("calendar", NAMESPACE_CALDAV, PREFIX_CALDAV)
    val calendar: CaldavCalendar? = null,

    @XmlElement(true)
    @XmlSerialName("addressbook", NAMESPACE_CARDDAV, PREFIX_CARDDAV)
    val addressBook: AddressBook? = null,

    @XmlElement(true)
    @XmlSerialName("calendar-proxy-read", NAMESPACE_CALENDARSERVER, PREFIX_CALENDARSERVER)
    val calendarProxyRead: CalendarProxyRead? = null,

    @XmlElement(true)
    @XmlSerialName("calendar-proxy-write", NAMESPACE_CALENDARSERVER, PREFIX_CALENDARSERVER)
    val calendarProxyWrite: CalendarProxyWrite? = null,

    @XmlElement(true)
    @XmlSerialName("subscribed", NAMESPACE_CALENDARSERVER, PREFIX_CALENDARSERVER)
    val subscribed: Subscribed? = null,

    @XmlElement(true)
    @XmlSerialName("shared-owner", NAMESPACE_CALENDARSERVER, PREFIX_CALENDARSERVER)
    val sharedOwner: SharedOwner? = null,

    @XmlElement(true)
    @XmlSerialName("schedule-inbox", NAMESPACE_CALDAV, PREFIX_CALDAV)
    val scheduleInbox: ScheduleInbox? = null,

    @XmlElement(true)
    @XmlSerialName("schedule-outbox", NAMESPACE_CALDAV, PREFIX_CALDAV)
    val scheduleOutbox: ScheduleOutbox? = null
)

// Use empty data classes for tags that simply exist, like <D:collection/>

@Serializable
@XmlSerialName("collection", NAMESPACE_DAV, PREFIX_WILDCARD)
class DavCollection

@Serializable
@XmlSerialName("principal", NAMESPACE_DAV, PREFIX_WILDCARD)
class DavPrincipal

@Serializable
@XmlSerialName("calendar", NAMESPACE_CALDAV, PREFIX_CALDAV)
class CaldavCalendar


@Serializable
data class HrefProperty(
    @XmlElement
    @XmlSerialName("href", NAMESPACE_DAV, PREFIX_WILDCARD)
    val href: String? = null
)

@Serializable
@XmlSerialName("addressbook", NAMESPACE_CARDDAV, PREFIX_CARDDAV)
class AddressBook

@Serializable
@XmlSerialName("calendar-proxy-read", NAMESPACE_CALENDARSERVER, PREFIX_CALENDARSERVER)
class CalendarProxyRead

@Serializable
@XmlSerialName("calendar-proxy-write", NAMESPACE_CALENDARSERVER, PREFIX_CALENDARSERVER)
class CalendarProxyWrite

@Serializable
@XmlSerialName("subscribed", NAMESPACE_CALENDARSERVER, PREFIX_CALENDARSERVER)
class Subscribed

@Serializable
@XmlSerialName("shared-owner", NAMESPACE_CALENDARSERVER, PREFIX_CALENDARSERVER)
class SharedOwner

@Serializable
@XmlSerialName("schedule-inbox", NAMESPACE_CALDAV, PREFIX_CALDAV)
class ScheduleInbox

@Serializable
@XmlSerialName("schedule-outbox", NAMESPACE_CALDAV, PREFIX_CALDAV)
class ScheduleOutbox






@OptIn(ExperimentalSerializationApi::class, ExperimentalXmlUtilApi::class)
object LongOrNullSerializer: XmlSerializer<Long?> {
    @OptIn(XmlUtilInternal::class)
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("longOrNull", PrimitiveKind.LONG).nullable
            .xml(PrimitiveSerialDescriptor("longOrNull", PrimitiveKind.STRING).nullable)

    override fun serialize(encoder: Encoder, value: Long?) {
        when (value) {
            null -> encoder.encodeNull()
            else -> {
                encoder.encodeNotNullMark()
                encoder.encodeLong(value)
            }
        }
    }

    override fun deserialize(decoder: Decoder): Long? = when {
        decoder.decodeNotNullMark() -> decoder.decodeLong()
        else -> decoder.decodeNull()
    }

    override fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: Long?,
        isValueChild: Boolean
    ): Long? {
        if (!decoder.decodeNotNullMark()) return decoder.decodeNull()
        return when (val s = xmlCollapseWhitespace(decoder.decodeString())) {
            "" -> null
            else -> s.toLong()
        }
    }

    override fun serializeXML(encoder: Encoder, output: XmlWriter, value: Long?, isValueChild: Boolean) {
        // Only encode a value if it is not null. Otherwise, do nothing.
        value?.let {
            encoder.encodeString(it.toString())
        }
    }
}

@OptIn(ExperimentalSerializationApi::class, ExperimentalXmlUtilApi::class)
object ColorSerializer: XmlSerializer<Color?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color?) {
        when (value) {
            null -> encoder.encodeNull()
            Color.Unspecified -> encoder.encodeString("")
            else -> encoder.encodeString(colorToHex(value))
        }
    }

    override fun deserialize(decoder: Decoder): Color? {
        if (!decoder.decodeNotNullMark()) {
            decoder.decodeNull()
            return null
        }

        val text = decoder.decodeString()
        if (text.isBlank()) return Color.Unspecified

        return hexToColor(text)
    }



    override fun serializeXML(
        encoder: Encoder,
        output: XmlWriter,
        value: Color?,
        isValueChild: Boolean
    ) {
        when(value) {
            null -> return
            Color.Unspecified -> encoder.encodeString("")
            else -> encoder.encodeString(colorToHex(value))
        }
    }

    override fun deserializeXML(
        decoder: Decoder,
        input: XmlReader,
        previousValue: Color?,
        isValueChild: Boolean
    ): Color? {
        // xmlCollapseWhitespace reads the text inside the tag and trims it.
        val hexString = xmlCollapseWhitespace(decoder.decodeString())

        // If the string is not empty, try to parse it. Otherwise, return null.
        return if (hexString.isNotEmpty()) {
            try {
                hexToColor(hexString)
            } catch (_: IllegalArgumentException) {
                // The string was not a valid color format
                null
            }
        } else {
            null
        }
    }


    private fun colorToHex(color: Color): String {
        val a = (color.alpha * 255).roundToInt().coerceIn(0, 255)
        val r = (color.red * 255).roundToInt().coerceIn(0, 255)
        val g = (color.green * 255).roundToInt().coerceIn(0, 255)
        val b = (color.blue * 255).roundToInt().coerceIn(0, 255)

        return buildString(9) {
            append('#')
            append(byteToHex(a))
            append(byteToHex(r))
            append(byteToHex(g))
            append(byteToHex(b))
        }
    }

    private const val HEX_CHARS = "0123456789ABCDEF"

    private fun byteToHex(value: Int): String {
        val v = value.coerceIn(0, 255)
        return buildString(2) {
            append(HEX_CHARS[v ushr 4])
            append(HEX_CHARS[v and 0x0F])
        }
    }

    private fun hexToColor(hex: String): Color {
        val clean = hex.trim().removePrefix("#")

        return when (clean.length) {
            6 -> {
                // #RRGGBB → assume fully opaque
                val r = clean.substring(0, 2).toInt(16) / 255f
                val g = clean.substring(2, 4).toInt(16) / 255f
                val b = clean.substring(4, 6).toInt(16) / 255f
                Color(r, g, b, 1f)
            }
            8 -> {
                // #AARRGGBB
                val a = clean.substring(0, 2).toInt(16) / 255f
                val r = clean.substring(2, 4).toInt(16) / 255f
                val g = clean.substring(4, 6).toInt(16) / 255f
                val b = clean.substring(6, 8).toInt(16) / 255f
                Color(r, g, b, a)
            }
            else -> {
                throw IllegalArgumentException(
                    "Invalid color format '$hex'. Expected #RRGGBB or #AARRGGBB."
                )
            }
        }
    }


}



@Serializable
@XmlSerialName("propfind", NAMESPACE_DAV, PREFIX_WILDCARD)
data class Propfind(

    @XmlElement
    @XmlSerialName("prop", NAMESPACE_DAV, PREFIX_WILDCARD)
    val prop: WebDavProp

)


@Serializable
// Note: The root element in your example is CAL:calendar-query, not DAV:
@XmlSerialName("calendar-query", NAMESPACE_CALDAV, PREFIX_CALDAV)
data class CalendarQuery(
    // The xmlns="DAV:" is tricky. By convention, elements without a prefix belong to the
    // default namespace. We'll set the default namespace during serialization.
    @XmlElement
    @XmlSerialName("prop", NAMESPACE_DAV, PREFIX_WILDCARD) // Use an empty prefix for the default namespace
    val prop: WebDavProp,

    @XmlElement
    @XmlSerialName("filter", NAMESPACE_CALDAV, PREFIX_CALDAV)
    val filter: CalFilter
)

@Serializable
@XmlSerialName("comp-filter", NAMESPACE_CALDAV, PREFIX_CALDAV)
data class CompFilter(
    @XmlElement(false)
    @XmlSerialName("name")
    val name: String,

    // A comp-filter can contain multiple (nested) comp-filters.
    @XmlElement
    @XmlSerialName("comp-filter", NAMESPACE_CALDAV, PREFIX_CALDAV)
    val compFilters: List<CompFilter> = emptyList()
)

@Serializable
@XmlSerialName("filter", NAMESPACE_CALDAV, PREFIX_CALDAV)
data class CalFilter(
    @XmlElement
    @XmlSerialName("comp-filter", NAMESPACE_CALDAV, PREFIX_CALDAV)
    val compFilter: CompFilter
)



@Serializable
// Note: The root element in your example is CAL:calendar-query, not DAV:
@XmlSerialName("calendar-multiget", NAMESPACE_CALDAV, PREFIX_CALDAV)
data class CalendarMultiget(
    @XmlElement
    @XmlSerialName("prop", NAMESPACE_DAV, PREFIX_WILDCARD) // Use an empty prefix for the default namespace
    val prop: WebDavProp,

    @XmlElement
    @XmlSerialName("href", NAMESPACE_DAV, PREFIX_WILDCARD)
    val hrefs: List<String>
)


@Serializable
@XmlSerialName("mkcol", NAMESPACE_DAV, PREFIX_WILDCARD)
data class CalendarMkcol(
    @XmlElement
    @XmlSerialName("set", NAMESPACE_DAV, PREFIX_WILDCARD)
    val set: WebDavSet
)

@Serializable
@XmlSerialName("set", NAMESPACE_DAV, PREFIX_WILDCARD)
data class WebDavSet(
    @XmlElement
    @XmlSerialName("prop", NAMESPACE_DAV, PREFIX_WILDCARD) // Use an empty prefix for the default namespace
    val prop: WebDavProp,
)

@Serializable
@XmlSerialName("remove", NAMESPACE_DAV, PREFIX_WILDCARD)
data class WebDavRemove(
    @XmlElement
    @XmlSerialName("prop", NAMESPACE_DAV, PREFIX_WILDCARD) // Use an empty prefix for the default namespace
    val prop: WebDavProp,
)

@Serializable
@XmlSerialName("propertyupdate", NAMESPACE_DAV, PREFIX_WILDCARD)
data class CalendarPropertyupdate(
    @XmlElement
    @XmlSerialName("set", NAMESPACE_DAV, PREFIX_WILDCARD)
    val set: WebDavSet? = null,

    @XmlElement
    @XmlSerialName("remove", NAMESPACE_DAV, PREFIX_WILDCARD)
    val remove: WebDavRemove? = null
)

@Serializable
@XmlSerialName("sync-collection", NAMESPACE_DAV, PREFIX_WILDCARD)
data class SyncCollection(

    @XmlElement
    @XmlSerialName("sync-token", NAMESPACE_DAV, PREFIX_WILDCARD)
    val syncToken: String = "",

    @XmlElement
    @XmlSerialName("sync-level", NAMESPACE_DAV, PREFIX_WILDCARD)
    val syncLevel: Int = 1,

    @XmlElement
    @XmlSerialName("prop", NAMESPACE_DAV, PREFIX_WILDCARD)
    val prop: WebDavProp = WebDavProp(getETag = "")
)