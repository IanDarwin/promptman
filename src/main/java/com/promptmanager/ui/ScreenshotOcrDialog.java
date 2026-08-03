package com.promptmanager.ui;

import com.promptmanager.util.AppSettings;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Dialog that accepts a screenshot via three routes:
 *   1. Browse button  — standard file chooser
 *   2. Drag-and-drop  — drop an image file (or image data) onto the drop zone
 *   3. Clipboard paste — Ctrl/Cmd-V or the Paste button; accepts an image
 *      copied from any graphics application
 *
 * Once an image is loaded a preview is shown and the user can click
 * "Run OCR" to extract text.  The extracted text is returned to the
 * caller via {@link #getExtractedText()}.
 */
public class ScreenshotOcrDialog extends JDialog {

    // ---- UI ----
    private final JLabel     dropLabel    = new JLabel("Drop image here, paste (Ctrl+V), or use Browse", SwingConstants.CENTER);
    private final JLabel     previewLabel = new JLabel("", SwingConstants.CENTER);
    private final JTextArea  ocrOutput    = new JTextArea(8, 60);
    private final JButton    browseBtn    = new JButton("Browse…");
    private final JButton    pasteBtn     = new JButton("Paste from Clipboard");
    private final JButton    runOcrBtn    = new JButton("Run OCR");
    private final JButton    insertBtn    = new JButton("Insert into Wording");
    private final JButton    cancelBtn    = new JButton("Cancel");
    private final JProgressBar progressBar = new JProgressBar();

    // ---- State ----
    private BufferedImage loadedImage   = null;   // the image to OCR
    private String        extractedText = null;   // result handed back to caller

    public ScreenshotOcrDialog(Frame owner) {
        super(owner, "Load Screenshot — OCR", true);
        buildUI();
        wireActions();
        pack();
        setMinimumSize(new Dimension(640, 480));
        setLocationRelativeTo(owner);
    }

    /** Returns the OCR text the user accepted, or null if cancelled. */
    public String getExtractedText() { return extractedText; }

    // =========================================================
    //  UI construction
    // =========================================================

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ---- Top: drop zone + browse/paste buttons ----
        JPanel dropZone = new JPanel(new BorderLayout(6, 6));
        dropZone.setBorder(new TitledBorder("Image source"));
        dropZone.setPreferredSize(new Dimension(0, 120));

        dropLabel.setFont(dropLabel.getFont().deriveFont(Font.ITALIC));
        dropLabel.setForeground(Color.GRAY);
        dropLabel.setOpaque(true);
        dropLabel.setBackground(new Color(245, 245, 250));
        dropLabel.setBorder(BorderFactory.createDashedBorder(Color.LIGHT_GRAY, 4, 4));
        dropZone.add(dropLabel, BorderLayout.CENTER);

        JPanel sourceBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        sourceBtns.add(browseBtn);
        sourceBtns.add(pasteBtn);
        dropZone.add(sourceBtns, BorderLayout.SOUTH);

        // ---- Centre: preview + OCR output ----
        previewLabel.setBorder(new TitledBorder("Preview"));
        previewLabel.setPreferredSize(new Dimension(0, 160));
        previewLabel.setOpaque(true);
        previewLabel.setBackground(Color.DARK_GRAY);

        ocrOutput.setLineWrap(true);
        ocrOutput.setWrapStyleWord(true);
        ocrOutput.setEditable(true);   // user can tidy up before inserting
        JScrollPane ocrScroll = new JScrollPane(ocrOutput);
        ocrScroll.setBorder(new TitledBorder("OCR result (editable before inserting)"));

        progressBar.setIndeterminate(false);
        progressBar.setString("Ready");
        progressBar.setStringPainted(true);

        JSplitPane centreSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, previewLabel, ocrScroll);
        centreSplit.setDividerLocation(160);
        centreSplit.setResizeWeight(0.35);

        // ---- Bottom: action buttons ----
        JPanel bottomBar = new JPanel(new BorderLayout(6, 0));
        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        runOcrBtn.setEnabled(false);
        insertBtn.setEnabled(false);
        actionBtns.add(runOcrBtn);
        actionBtns.add(insertBtn);
        actionBtns.add(cancelBtn);
        bottomBar.add(progressBar, BorderLayout.CENTER);
        bottomBar.add(actionBtns, BorderLayout.EAST);

        add(dropZone,    BorderLayout.NORTH);
        add(centreSplit, BorderLayout.CENTER);
        add(bottomBar,   BorderLayout.SOUTH);
    }

    // =========================================================
    //  Wiring
    // =========================================================

    private void wireActions() {
        browseBtn.addActionListener(e -> onBrowse());
        pasteBtn .addActionListener(e -> onPaste());
        runOcrBtn.addActionListener(e -> onRunOcr());
        cancelBtn.addActionListener(e -> dispose());

        insertBtn.addActionListener(e -> {
            extractedText = ocrOutput.getText();
            dispose();
        });

        // Keyboard shortcut: Ctrl/Cmd-V pastes from clipboard anywhere in the dialog
        KeyStroke pasteKey = KeyStroke.getKeyStroke("control V");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(pasteKey, "paste-image");
        getRootPane().getActionMap().put("paste-image", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { onPaste(); }
        });

        installDropTarget();
    }

    private void installDropTarget() {
        new DropTarget(dropLabel, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent e) {
                dropLabel.setBackground(new Color(220, 235, 255));
            }
            @Override
            public void dragExit(DropTargetEvent e) {
                dropLabel.setBackground(new Color(245, 245, 250));
            }
            @Override
            public void drop(DropTargetDropEvent e) {
                dropLabel.setBackground(new Color(245, 245, 250));
                e.acceptDrop(DnDConstants.ACTION_COPY);
                Transferable t = e.getTransferable();
                try {
                    // Try as a file list first (most common case: drop from Finder/Explorer)
                    if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) {
                            loadImageFile(files.get(0));
                            e.dropComplete(true);
                            return;
                        }
                    }
                    // Try as raw image data (drop from a browser or graphics app)
                    if (t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                        Image img = (Image) t.getTransferData(DataFlavor.imageFlavor);
                        setImage(toBuffered(img));
                        e.dropComplete(true);
                        return;
                    }
                    showDropError("Dropped item is not a recognised image.");
                    e.dropComplete(false);
                } catch (Exception ex) {
                    showDropError("Could not read dropped item: " + ex.getMessage());
                    e.dropComplete(false);
                }
            }
        });
    }

    // =========================================================
    //  Image input handlers
    // =========================================================

    private void onBrowse() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select image file");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image files (PNG, JPG, BMP, GIF, TIFF)", "png", "jpg", "jpeg", "bmp", "gif", "tif", "tiff"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadImageFile(fc.getSelectedFile());
        }
    }

    private void onPaste() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents == null) {
            showInfo("The clipboard is empty.");
            return;
        }
        try {
            if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                Image img = (Image) contents.getTransferData(DataFlavor.imageFlavor);
                setImage(toBuffered(img));
            } else {
                showInfo("No image found in the clipboard.\n\nCopy an image in your graphics application first,\nthen click Paste.");
            }
        } catch (Exception ex) {
            showError("Could not read clipboard: " + ex.getMessage());
        }
    }

    private void loadImageFile(File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                showError("File does not appear to be a supported image:\n" + file.getName());
                return;
            }
            setImage(img);
        } catch (IOException ex) {
            showError("Could not read file: " + ex.getMessage());
        }
    }

    /** Accepts the loaded image, updates the preview, and enables the OCR button. */
    private void setImage(BufferedImage img) {
        loadedImage = img;
        ocrOutput.setText("");
        insertBtn.setEnabled(false);

        // Scale to fit the preview panel (keep aspect ratio)
        int pw = previewLabel.getWidth()  == 0 ? 600 : previewLabel.getWidth();
        int ph = previewLabel.getHeight() == 0 ? 150 : previewLabel.getHeight();
        Image scaled = img.getScaledInstance(pw, ph, Image.SCALE_SMOOTH);
        previewLabel.setIcon(new ImageIcon(scaled));
        previewLabel.setText("");

        runOcrBtn.setEnabled(true);
        progressBar.setString("Image loaded — click Run OCR");
        dropLabel.setText("Image loaded ✓  (load another by dropping / browsing / pasting)");
        dropLabel.setForeground(new Color(0, 120, 0));
    }

    // =========================================================
    //  OCR
    // =========================================================

    private void onRunOcr() {
        if (loadedImage == null) return;

        runOcrBtn.setEnabled(false);
        insertBtn.setEnabled(false);
        progressBar.setIndeterminate(true);
        progressBar.setString("Running OCR…");
        ocrOutput.setText("");

        // Run on a background thread so the UI stays responsive
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                Tesseract tess = new Tesseract();

                String dataPath = AppSettings.getInstance().getTesseractDataPath();
                if (dataPath != null && !dataPath.isBlank()) {
                    tess.setDatapath(dataPath);
                }
                // Default language is English; users can override via Settings → Tesseract data path
                tess.setLanguage("eng");

                return tess.doOCR(loadedImage);
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                try {
                    String text = get();
                    ocrOutput.setText(text != null ? text.trim() : "");
                    ocrOutput.setCaretPosition(0);
                    progressBar.setString("OCR complete — review and edit, then click Insert");
                    insertBtn.setEnabled(true);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause.getMessage();
                    progressBar.setString("OCR failed");
                    ocrOutput.setText("OCR failed: " + msg);

                    if (msg != null && msg.contains("tessdata")) {
                        showError("""
                                OCR failed — Tesseract could not find its language data.

                                Make sure Tesseract is installed and set the 'tessdata'
                                directory in Tools → Settings → OCR / Tesseract data path.

                                Detail: """ + msg);
                    } else {
                        showError("OCR failed: " + msg);
                    }
                }
                runOcrBtn.setEnabled(true);
            }
        };
        worker.execute();
    }

    // =========================================================
    //  Utilities
    // =========================================================

    private static BufferedImage toBuffered(Image img) {
        if (img instanceof BufferedImage bi) return bi;
        BufferedImage bi = new BufferedImage(
                img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bi.createGraphics();
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        return bi;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showDropError(String msg) {
        dropLabel.setText("⚠ " + msg);
        dropLabel.setForeground(Color.RED);
    }
}
