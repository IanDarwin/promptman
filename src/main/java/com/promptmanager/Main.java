package com.promptmanager;

import com.promptmanager.dao.DatabaseManager;
import com.promptmanager.ui.MainWindow;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Use system look-and-feel so it feels native on each OS
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            try {
                DatabaseManager db = DatabaseManager.getInstance();
                MainWindow window = new MainWindow(db);
                window.wireDirtyTracking();
                window.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Fatal error starting application:\n" + e.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
