import SwiftUI
import ComposeTasksApp

@main
struct iOSTasksApp: App {

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

        let description = components.queryItems?.first(where: { $0.name == "description" })?.value
        
        if let desc = description {
            DeepLinkHandler.shared.onDeepLinkReceived(
                calendarId: nil,
                entryId: KotlinLong(value: 0),
                description: desc
            )
        }
    }
}
