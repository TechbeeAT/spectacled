package at.techbee.spectacled.screens.core.data.webdav


/*
actual suspend fun getCalDavCalendarsDavX5(
    client: HttpClient,
    location: Url
): List<Principal> {

    val userGivenServer = DavCollection(client, location)

    val principals = mutableSetOf<Principal>()

    //val userPrincipals = mutableSetOf<String>()

    // Find all calendar collections (folders) in the given location (e.g., calendar home set)
    try {
        userGivenServer.propfind(
            1,
            WebDAV.CurrentUserPrincipal, WebDAV.DisplayName
        ) { response, _ ->

            val currentUserPrincipal = response[CurrentUserPrincipal::class.java]?.href ?: throw DavException("CurrentUserPrincipal was null")
            println("Response $response")
            println("CurrentUserPrincipal $currentUserPrincipal")

            //getHomeCollections(client, Url(currentUserPrincipal))

            val currentPrincipal = Principal(
                id = 0L,
                principalUrl = URLBuilder(location).takeFrom(currentUserPrincipal).build(),
                displayName = null,
                calendarUserAddressSet = emptyList(),
                homeCollections = emptyList()
            )
            principals.add(currentPrincipal)

        }

        principals.forEach { principal ->
            populateHomeCollections(client, location, principal)
            populateCalendars(client, location, principal)
        }

    } catch (e: UnauthorizedException) {
        println(e.toString())
        //returnResponse =  "Invalid username or password"
    } catch (e: IOException) {
        println(e.toString())
        //returnResponse = e.localizedMessage ?: "IO Error"
    } catch (e: DavException) {
        println(e.toString())
        //returnResponse = e.localizedMessage ?: "DAV Error ${e.statusCode}"
    } catch (e: HttpException) {
        //returnResponse = e.localizedMessage ?: "Http Error ${e.statusCode}"
        println(e.toString())
    }
    return principals.toList()
    //return collections.joinToString(separator = "\n")
}


private suspend fun populateHomeCollections(client: HttpClient, location: Url, principal: Principal) {

    //val calendarHomeSets = mutableSetOf<String>()
    val homeCollections = mutableSetOf<HomeCollection>()

    DavCollection(client, URLBuilder(location).takeFrom(principal.principalUrl).build()).propfind(
        0,
        CalDAV.CalendarHomeSet, WebDAV.DisplayName
    ) { response, relation ->

        val calendarHomeSets = response[CalendarHomeSet::class.java]?.hrefs

        if(calendarHomeSets.isNullOrEmpty())
            throw (DavException("CalendarHomeSet was null"))

        println("Response $response")
        println("CalendarHomeSets " + response[CalendarHomeSet::class.java]?.hrefs?.joinToString(separator = ", "))

        calendarHomeSets.forEach { calendarHomeSet ->
            homeCollections.add(
                HomeCollection(
                    id = 0L,
                    principalId = 0L,
                    url = URLBuilder(location).takeFrom(calendarHomeSet).build(),
                    calDavPrivileges = emptyList(),  // populated later
                    calendars = emptyList() // populated later
                )
            )
        }
    }

    principal.homeCollections = homeCollections.toList()
}


private suspend fun populateCalendars(client: HttpClient, location: Url, principal: Principal) {

    val calendars = mutableListOf<Calendar>()

    //println("CalendarHomeSet from Set: $homeCollectionUrl")

    principal.homeCollections.forEach { homeCollection ->
        DavCollection(client, URLBuilder(location).takeFrom(homeCollection.url).build()).propfind(
            1,
            WebDAV.DisplayName,
            WebDAV.ResourceType,
            CalDAV.CalendarColor,
            CalDAV.CalendarDescription,
            //CalendarTimezone.Factory.getName(),
            WebDAV.CurrentUserPrincipal,
            WebDAV.CurrentUserPrivilegeSet,
            CalDAV.SupportedCalendarComponentSet,
            CalDAV.GetCTag
        ) { response, relation ->

            val calendarDisplayName = response[DisplayName::class.java]?.displayName
            val calendarResourceType = response[ResourceType::class.java]
            val calendarColor = response[CalendarColor::class.java]?.color?.let { Color(it) }
            val calendarDescription = response[CalendarDescription::class.java]?.description
            //val calendarTimezone = response[CalendarTimezone::class.java]
            val currentUserPrivilegeSet = response[CurrentUserPrivilegeSet::class.java]
            val supportedCalendarComponentSet = response[SupportedCalendarComponentSet::class.java]?.let { componentSet ->
                val calendarComponents = mutableListOf<CalendarComponent>().apply {
                    if(componentSet.supportsJournal) add(CalendarComponent.VJOURNAL)
                    if(componentSet.supportsTasks) add(CalendarComponent.VTODO)
                    if(componentSet.supportsEvents) add(CalendarComponent.VEVENT)
                }
                return@let calendarComponents
            } ?: emptyList()
            val cTag = response[GetCTag::class.java]?.cTag

            // skip calendars that are NOT of resource type calendar
            if(calendarResourceType?.types?.none { it.name == CalDAV.Calendar.name } == true)
                return@propfind

            println("============================")
            println("Response $response")
            println("DisplayName $calendarDisplayName")
            println("ResourceType " + calendarResourceType?.types?.joinToString(transform = { "CalendarResourceType: Name: ${it.name}, Namespace: ${it.namespace}; " }))
            println("CalendarColor $calendarColor")
            println("CalendarDescription $calendarDescription")

            println("CurrentUserPrivilegeSet " + currentUserPrivilegeSet?.toString())
            println("SupportedCalendarComponents $supportedCalendarComponentSet")

            calendars.add(
                Calendar(
                    id = 0L,
                    homeCollectionId = 0L,
                    url = response.href,
                    displayName = calendarDisplayName,
                    calendarDescription = calendarDescription,
                    color = calendarColor,
                    ctag = cTag,
                    supportedComponents = supportedCalendarComponentSet,
                    calDavPrivileges = emptyList(),  // TODO!!!
                    calendarSyncStatus = null,
                    notes = emptyList()
                )
            )
        }

        homeCollection.calendars = calendars
    }
}



actual suspend fun getCalendarEntriesDavX5(
    client: HttpClient,
    calendar: Calendar
): List<Note> {

    val calendarHrefs = mutableListOf<Url>()
    val notes = mutableListOf<Note>()


    val davCalendar = DavCalendar(client, calendar.url)

    davCalendar.calendarQuery(
        CalendarComponent.VJOURNAL.name,
        null,
        null
    ) { response, relation ->
        println("calendarQueryResponse $response")
        calendarHrefs.add(response.href)
    }

    davCalendar.multiget(calendarHrefs) { response, relation ->
        val calendarData = response[CalendarData::class.java]?.iCalendar
        /*
        println("multigetResponse $response")
        println("multigetResponse CalendarData: $calendarData")
         */
        // 3. Access and print the full, untruncated iCalendar string
        println("--- Full Calendar Data for ${response.href} ---")
        println(calendarData)
        calendarData
            ?.let { parseVJournals(it) }
            ?.let { notes.addAll(it) }

        println("--- End of Calendar Data ---")
    }

    return notes
}


 */