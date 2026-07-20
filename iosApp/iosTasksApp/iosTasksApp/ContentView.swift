import UIKit
import SwiftUI
import ComposeTasksApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Stop SwiftUI from compounding safe-area insets into the embedded
        // ComposeUIViewController across rotations (insets otherwise grow each turn).
        uiViewController.additionalSafeAreaInsets = .zero
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard)
            .ignoresSafeArea()
    }
}
