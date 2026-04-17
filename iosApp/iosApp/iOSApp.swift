import SwiftUI

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self)
    var appDelegate

    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView().onAppear() {
                appDelegate.scheduleFromSwiftUI()   // schedule for the first time
            }
        }
    }
}