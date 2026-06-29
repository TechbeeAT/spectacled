import UIKit
import SwiftUI
import ComposeTasksApp

struct ComposeView: UIViewControllerRepresentable {
    var initialCalendarId: Int64?
    var initialIcalEntryId: Int64?
    var initialIcalEntryDescription: String?

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            initialCalendarId: initialCalendarId != nil ? KotlinLong(value: initialCalendarId!) : nil,
            initialIcalEntryId: initialIcalEntryId != nil ? KotlinLong(value: initialIcalEntryId!) : nil,
            initialIcalEntryDescription: initialIcalEntryDescription
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var initialCalendarId: Int64?
    var initialIcalEntryId: Int64?
    var initialIcalEntryDescription: String?

    var body: some View {
        ComposeView(
            initialCalendarId: initialCalendarId,
            initialIcalEntryId: initialIcalEntryId,
            initialIcalEntryDescription: initialIcalEntryDescription
        )
        .ignoresSafeArea(.keyboard)
        .ignoresSafeArea()
    }
}
