package at.techbee.spectacled

expect object DeepLinkHandler {
    var initialCalendarId: Long?
    var initialIcalEntryId: Long?
    var initialIcalEntryDescription: String?

    fun onDeepLinkReceived(calendarId: Long?, entryId: Long?, description: String?)
    
    fun setupDesktopHandler()
    fun parseArgs(args: Array<String>)
}
