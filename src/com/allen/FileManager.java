package com.allen;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class FileManager {
    private static FileManager fileManager;
    /** Main gui container **/
    private JPanel gui;
    /** Used to operate on files**/
    private Desktop desktop;
    /** File Tree view **/
    private JScrollPane scrollPane;
    private FileTree fileTree;
    /** File Detail View **/
    private DetailView detailView;
    /** Progress bar **/
    JPanel simpleOutput;
    JProgressBar progressBar;
    /** Get current selected file **/
    private File currentFile;

    public File getCurrentFile() {
        return currentFile;
    }

    public JPanel getGui() {
        return gui;
    }

    public FileTree getFileTree() {
        return fileTree;
    }

    public JComponent setupGui() {
        if (gui == null) {
            gui = new JPanel(new BorderLayout(3,3));
            gui.setBorder(BorderFactory.createEmptyBorder());
            desktop = Desktop.getDesktop();

            simpleOutput = new JPanel(new BorderLayout(3,3));
            progressBar = new JProgressBar();
            simpleOutput.add(progressBar, BorderLayout.EAST);
            progressBar.setVisible(false);
            gui.add(simpleOutput, BorderLayout.SOUTH);

            detailView = new DetailView(fileManager);
            fileTree = new FileTree(fileManager);

            JSplitPane splitPane = new JSplitPane(
                    JSplitPane.HORIZONTAL_SPLIT,
                    fileTree.getTreeScroll(),
                    detailView);
            gui.add(splitPane, BorderLayout.CENTER);
        }
        return gui;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public DetailView getDetailView() {
        return detailView;
    }

    public Desktop getDesktop() {
        return desktop;
    }

    public static void main(String[] args) {
        JFrame f = new JFrame("File Manager");
        fileManager = new FileManager();
        f.add(fileManager.setupGui());
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
        f.setVisible(true);
    }

}
