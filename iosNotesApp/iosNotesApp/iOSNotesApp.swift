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
        let isAddPath = url.path.hasSuffix("/add")
        let isAddHost = url.host == DeepLinkData.companion.DEEPLINK_ADD_HOST
        
        guard isAddHost || isAddPath else { return }
        
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: true) else { return }

        let description = components.queryItems?.first(where: { $0.name == DeepLinkData.companion.DEEPLINK_DESCRIPTION_PARAM })?.value
        
        DeepLinkHandler.shared.onDeepLinkReceived(
            calendarId: nil,
            entryId: KotlinLong(value: 0),
            description: description
        )
    }
}
