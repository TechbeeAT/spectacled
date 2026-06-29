import SwiftUI
import ComposeNotesApp

@main
struct iOSNotesApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self)
    var appDelegate

    @Environment(\.scenePhase) private var scenePhase

    @State private var initialCalendarId: Int64? = nil
    @State private var initialIcalEntryId: Int64? = nil
    @State private var initialIcalEntryDescription: String? = nil

    var body: some Scene {
        WindowGroup {
            ContentView(
                initialCalendarId: initialCalendarId,
                initialIcalEntryId: initialIcalEntryId,
                initialIcalEntryDescription: initialIcalEntryDescription
            )
            .onOpenURL { url in
                handleURL(url)
            }
        }
        .onChange(of: scenePhase) { oldPhase, newPhase in
            if newPhase == .background {
                appDelegate.scheduleFromSwiftUI()
            }
        }
    }

    private func handleURL(_ url: URL) {
        guard url.host == "add" else { return }
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: true) else { return }

        if let desc = components.queryItems?.first(where: { $0.name == "description" })?.value {
            self.initialIcalEntryDescription = desc
            self.initialIcalEntryId = 0
            
            // Also push to the shared handler for when the app is already open
            DeepLinkHandler.shared.onDeepLinkReceived(
                calendarId: nil,
                entryId: 0,
                description: desc
            )
        }
    }
}
