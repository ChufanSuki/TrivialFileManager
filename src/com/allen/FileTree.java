package com.allen;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.io.File;
import java.util.Arrays;

public class FileTree extends JTree {

    private FileManager fileManager;

    // Top Container
    private JScrollPane treeScroll;
    private DefaultTreeModel treeModel;

    FileTree(FileManager fileManager) {
        this.fileManager = fileManager;
        DetailView detail = fileManager.getDetailView();
        JPanel gui = fileManager.getGui();

        /* Provides nice icon and name for files **/
        FileSystemView fileSystemView = FileSystemView.getFileSystemView();

        /* file tree **/
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        treeModel = new DefaultTreeModel(root);
        getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);


        /* get root partition on system */
        File[] roots = fileSystemView.getRoots();
        for (File fileRoot : roots) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(fileRoot);
            root.add(node);
            File[] files = fileSystemView.getFiles(fileRoot, true);
            for (File file : files) {
                if (file.isDirectory()) {
                    DefaultMutableTreeNode parent = new DefaultMutableTreeNode(file);
                    node.add(parent);
                }
            }
        }
        setModel(treeModel);
        //Returns the last path element of the selection.
        //This method is useful only when the selection model allows a single selection.
        addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                    e.getPath().getLastPathComponent();
            if (node == null)
                return;
            showChildren(node);
            detail.setFileDetail((File)node.getUserObject());
        });

        setVisibleRowCount(15);
        setRootVisible(false);
        expandRow(0);
        setCellRenderer(new FileTreeCellRenderer());
        treeScroll = new JScrollPane(this);

        Dimension preferredSize = treeScroll.getPreferredSize();
        Dimension widePreferred = new Dimension(
                200,
                (int)preferredSize.getHeight());
        treeScroll.setPreferredSize( widePreferred );
    }

    public JScrollPane getTreeScroll() {
        return treeScroll;
    }

    class FileTreeCellRenderer extends DefaultTreeCellRenderer {
        private FileSystemView fileSystemView;
        private JLabel label;
        FileTreeCellRenderer() {
            fileSystemView = FileSystemView.getFileSystemView();
            label = new JLabel();
            label.setOpaque(true);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            File file = (File) ((DefaultMutableTreeNode)value).getUserObject();
            label.setIcon(fileSystemView.getSystemIcon(file));
            label.setText(fileSystemView.getSystemDisplayName(file));
            label.setToolTipText(file.getPath());
            if (selected) {
                label.setBackground(backgroundSelectionColor);
                label.setForeground(textSelectionColor);
            } else {
                label.setBackground(backgroundNonSelectionColor);
                label.setForeground(textNonSelectionColor);
            }
            return label;
        }
    }

//    /** Don't forget this because I hide the root. */
//    public void showRootFile() {
//        // ensure the main files are displayed
//        setSelectionInterval(0,0);
//    }

    public DefaultTreeModel getTreeModel() {
        return treeModel;
    }

    void showChildren(DefaultMutableTreeNode node) {
        // TODO:bug
        FileSystemView fileSystemView = FileSystemView.getFileSystemView();
        setEnabled(false);
        fileManager.getProgressBar().setVisible(true);
        fileManager.getProgressBar().setIndeterminate(true);

        SwingWorker<Void, File> worker = new SwingWorker<>() {
            @Override
            public Void doInBackground() {
                File file = (File) node.getUserObject();
                if (file.isDirectory()) {
                    File[] files = fileSystemView.getFiles(file, true); //!!
                    if (node.isLeaf()) {
                        for (File child : files) {
                            if (child.isDirectory()) {
                                publish(child);
                            }
                        }
                    }
                    fileManager.getDetailView().setTableData(Arrays.asList(files));
                }
                return null;
            }

            @Override
            protected void process(java.util.List<File> chunks) {
                for (File child : chunks) {
                    node.add(new DefaultMutableTreeNode(child));
                }
            }

            @Override
            protected void done() {
                fileManager.getProgressBar().setIndeterminate(false);
                fileManager.getProgressBar().setVisible(false);
                setEnabled(true);
            }
        };
        worker.execute();

    }

}
