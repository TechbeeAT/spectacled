import UIKit
import BackgroundTasks
import ComposeApp
import os

private let log = Logger(subsystem: "at.techbee.spectacled", category: "AppDelegate")

class AppDelegate: NSObject, UIApplicationDelegate {

    private let iOSSyncEntryPoint = IOSSyncEntryPoint.shared
    private let bGAppRefreshTaskRequestIdentifier = "at.techbee.spectacled.sync"

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        log.info("App did finish launching")

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
        // scheduling in SwiftUI should only be done when the app scene moved to background
        // this function is called from iOSApp.swift when exactly this happens
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

            self.schedule()   // ✅ schedule NEXT here only
            task.setTaskCompleted(success: true)
        }
    }


    private func schedule() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: bGAppRefreshTaskRequestIdentifier)

        log.info("Scheduling next sync")
        let request = BGAppRefreshTaskRequest(identifier: bGAppRefreshTaskRequestIdentifier)
        log.info("Request created")
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        log.info("Request next execution set")
        do {
            try BGTaskScheduler.shared.submit(request)
            log.debug("Scheduled BG refresh for earliest \(String(describing: request.earliestBeginDate), privacy: .public)")
        } catch {
            log.error("Failed to schedule BG refresh: \(String(describing: error), privacy: .public)")
        }
    }
}
