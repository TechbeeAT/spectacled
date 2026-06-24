import UIKit
import BackgroundTasks
import ComposeNotesApp
import os

private let log = Logger(subsystem: "at.techbee.spectacled.notes", category: "AppDelegate")

class AppDelegate: NSObject, UIApplicationDelegate {

    private let iOSSyncEntryPoint = IOSSyncEntryPoint.shared
    private let bGAppRefreshTaskRequestIdentifier = "at.techbee.spectacled.notes.sync"

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        KoinIOSKt.initKoinIos(spectacledVariant: .notes)
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

    func scheduleFromSwiftUI() {
        // Called when app moves to background
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
