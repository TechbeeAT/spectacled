import UIKit
import SwiftUI
import ComposeTasksApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            // Ignore only the container safe area (notch/home indicator) so Compose stays
            // edge-to-edge, but keep the keyboard safe area so SwiftUI performs keyboard
            // avoidance natively. Compose skips imePadding on iOS (see imeAwarePadding) to avoid
            // compensating twice; SwiftUI resizing resets reliably on rotation.
            .ignoresSafeArea(.container, edges: .all)
    }
}
