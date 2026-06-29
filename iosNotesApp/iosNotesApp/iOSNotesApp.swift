import SwiftUI
import ComposeNotesApp

@main
struct iOSNotesApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self)
    var appDelegate

    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
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
            DeepLinkHandler.shared.onDeepLinkReceived(
                calendarId: nil,
                entryId: 0,
                description: desc
            )
        }
    }
}
