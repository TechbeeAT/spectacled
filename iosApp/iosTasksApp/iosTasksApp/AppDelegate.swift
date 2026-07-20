import UIKit
import BackgroundTasks
import ComposeTasksApp
import os

private let log = Logger(subsystem: "at.techbee.spectacled.tasks", category: "AppDelegate")

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    private let iOSSyncEntryPoint = IOSSyncEntryPoint.shared
    private let bGAppRefreshTaskRequestIdentifier = "at.techbee.spectacled.tasks.sync"

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        SpectacledAppKt.doInitKoin(spectacledVariant: .tasks)
        log.info("App did finish launching")

        // Registration must happen before didFinishLaunchingWithOptions returns
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: bGAppRefreshTaskRequestIdentifier,
            using: nil
        ) { task in
            log.debug("Received BG task: \(task.identifier, privacy: .public)")
            self.handle(task: task as! BGAppRefreshTask)
        }
        return true
    }

    // MARK: UISceneSession Lifecycle

    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        let configuration = UISceneConfiguration(
            name: "Default Configuration",
            sessionRole: connectingSceneSession.role
        )
        configuration.delegateClass = SceneDelegate.self
        return configuration
    }

    func scheduleBackgroundSync() {
        // Called when the app moves to the background
        schedule()
    }

    private func handle(task: BGAppRefreshTask) {
        log.info("Handling background refresh task")

        task.expirationHandler = {
            self.iOSSyncEntryPoint.cancelBackgroundSync()
            task.setTaskCompleted(success: false)
        }

        iOSSyncEntryPoint.runBackgroundSync {
            log.info("Kotlin sync finished")

            self.schedule()   // Schedule the next one
            task.setTaskCompleted(success: true)
        }
    }


    private func schedule() {
        // Removing cancel() as it can sometimes cause Code 1 errors if followed immediately by submit()
        // BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: bGAppRefreshTaskRequestIdentifier)

        log.info("Scheduling next sync")
        let request = BGAppRefreshTaskRequest(identifier: bGAppRefreshTaskRequestIdentifier)
        log.info("Request created")
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        log.info("Request next execution set")
        do {
            try BGTaskScheduler.shared.submit(request)
            log.debug("Scheduled BG refresh for earliest \(String(describing: request.earliestBeginDate), privacy: .public)")
        } catch {
            // Note: If you still see Code 1 here on a Simulator, it's a known Simulator limitation.
            log.error("Failed to schedule BG refresh: \(String(describing: error), privacy: .public)")
        }
    }
}


class SceneDelegate: UIResponder, UIWindowSceneDelegate {

    var window: UIWindow?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = (scene as? UIWindowScene) else { return }
        let window = UIWindow(windowScene: windowScene)
        // Host Compose as the window's root view controller directly instead of embedding it in a
        // SwiftUI UIViewControllerRepresentable. SwiftUI was compounding the safe-area insets on
        // every rotation; as the root VC, ComposeUIViewController reads the real window insets and
        // the insets no longer grow.
        window.rootViewController = MainViewControllerKt.MainViewController()
        self.window = window
        window.makeKeyAndVisible()

        // Handle a URL the app was launched with
        if let url = connectionOptions.urlContexts.first?.url {
            handleURL(url)
        }
    }

    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        guard let url = URLContexts.first?.url else { return }
        handleURL(url)
    }

    func sceneDidEnterBackground(_ scene: UIScene) {
        (UIApplication.shared.delegate as? AppDelegate)?.scheduleBackgroundSync()
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
