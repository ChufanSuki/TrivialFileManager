package com.allen;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class DetailView extends JPanel {
    FileManager fileManager;
    /** system view **/
    private FileSystemView fileSystemView;
    /** File list **/
    private FileTableModel fileTableModel;
    private JTable table;
    private ListSelectionListener listSelectionListener;
    private int rowIconPadding = 6;
    private boolean cellSizesSet = false;
    /** File detail container **/
    JPanel fileView;
    /** File detail **/
    JPanel fileMainDetails;
    JPanel fileDetailsLabels;
    JPanel fileDetailsValues;
    JLabel fileIcon;
    JLabel path;
    JLabel date;
    JLabel size;
    JPanel flag;
    JRadioButton isDir;
    JRadioButton isFile;
    /** File operation **/
    File currentFile; // Current selected file
    JToolBar toolBar;
    JButton openButton;
    JButton editButton;
    JButton printButton;
    JButton newButton;
    JButton copyButton;
    JButton renameButton;
    JButton deleteButton;
    JCheckBox readCheckBox;
    JCheckBox writeCheckBox;
    JCheckBox executeCheckBox;
    /** GUI options/containers for new File/Directory creation.  Created lazily. */
    private JPanel newFilePanel;
    private JRadioButton newTypeFile;
    private JTextField name;

    DetailView(FileManager fileManager) {
//        There is a implicit call.So call super
//        if you want to send arguments if the parent's constructor takes parameters
//        super();
        this.fileManager = fileManager;
        fileSystemView = FileSystemView.getFileSystemView();
        setLayout(new BorderLayout(3,3));
        /* File list view **/
        table = new JTable();
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setShowVerticalLines(false);
//        TableModelListener modelListener = new TableModelListener() {
//            @Override
//            public void tableChanged(TableModelEvent e) {
//                int row = table.getSelectionModel().getLeadSelectionIndex();
//                setFileDetail( ((FileTableModel)table.getModel()).getFile(row) );
//        };
//        table.getModel().addTableModelListener(modelListener);
        listSelectionListener = e -> {
            int row = table.getSelectionModel().getLeadSelectionIndex();
            setFileDetail(((FileTableModel)table.getModel()).getFile(row));
        };
        table.getSelectionModel().addListSelectionListener(listSelectionListener);

        JScrollPane scrollPane = new JScrollPane(table);
        // TODO:fix
        Dimension d = scrollPane.getPreferredSize();
        scrollPane.setPreferredSize(new Dimension((int)d.getWidth(), (int)d.getHeight()/2));
        add(scrollPane, BorderLayout.CENTER);

        // details for a file
        fileMainDetails = new JPanel(new BorderLayout(4,2));
        fileMainDetails.setBorder(new EmptyBorder(0,6,0,6));

        fileDetailsLabels = new JPanel(new GridLayout(0,1,2,2));
        fileMainDetails.add(fileDetailsLabels, BorderLayout.WEST);

        fileDetailsValues = new JPanel(new GridLayout(0,1,2,2));
        fileMainDetails.add(fileDetailsValues, BorderLayout.CENTER);

        fileDetailsLabels.add(new JLabel("File"));
        fileIcon = new JLabel();
        fileDetailsValues.add(fileIcon);
        fileDetailsLabels.add(new JLabel("Path/Name"));
        path = new JLabel();
        fileDetailsValues.add(path);
        date = new JLabel();
        fileDetailsValues.add(date);
        fileDetailsLabels.add(new JLabel("Last Modified"));
        size = new JLabel();
        fileDetailsValues.add(size);
        fileDetailsLabels.add(new JLabel("File Size"));
        flag = new JPanel(new FlowLayout());
        isDir = new JRadioButton("Directory");
        isDir.setEnabled(false);
        isFile = new JRadioButton("File");
        isFile.setEnabled(false);
        flag.add(isDir);
        flag.add(isFile);
        fileDetailsLabels.add(new JLabel("Type"));
        fileDetailsValues.add(flag);
        add(fileMainDetails, BorderLayout.CENTER);

        int count = fileDetailsLabels.getComponentCount();
        for (int ii = 0; ii < count; ii++) {
            fileDetailsLabels.getComponent(ii).setEnabled(false);

        }

        /* toolbar section **/
        toolBar = new JToolBar();
        // mnemonics stop working in a floated toolbar
        toolBar.setFloatable(false);

        // Check the actions are supported on this platform!

        openButton = new JButton("Open");
        openButton.addActionListener(e -> {
            try {
                fileManager.getDesktop().open(currentFile);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        editButton = new JButton("Edit");
        editButton.addActionListener(e -> {
            try {
                fileManager.getDesktop().edit(currentFile);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        printButton = new JButton("Print");
        printButton.addActionListener(e -> {
            try {
                fileManager.getDesktop().print(currentFile);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        openButton.setEnabled(fileManager.getDesktop().isSupported(Desktop.Action.OPEN));
        editButton.setEnabled(fileManager.getDesktop().isSupported(Desktop.Action.EDIT));
        printButton.setEnabled(fileManager.getDesktop().isSupported(Desktop.Action.PRINT));

        newButton = new JButton("New");
        newButton.addActionListener(e -> {
            newFile();
        });
        copyButton = new JButton("Copy");
        copyButton.addActionListener(e -> {
            copyFile();
        });
        renameButton = new JButton("Rename");
        renameButton.addActionListener(e -> {
            renameFile();
        });
        deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> {
            deleteFile();
        });


        toolBar.add(openButton);
        toolBar.add(editButton);
        toolBar.add(printButton);
        toolBar.addSeparator();
        toolBar.add(newButton);
        toolBar.add(copyButton);
        toolBar.add(renameButton);
        toolBar.add(deleteButton);
        toolBar.addSeparator();
        readCheckBox = new JCheckBox("Read");
        readCheckBox.setMnemonic('a');
        writeCheckBox = new JCheckBox("Write");
        writeCheckBox.setMnemonic('w');
        executeCheckBox = new JCheckBox("Execute");
        executeCheckBox.setMnemonic('x');
        toolBar.add(readCheckBox);
        toolBar.add(writeCheckBox);
        toolBar.add(executeCheckBox);

        fileView = new JPanel(new BorderLayout(3,3));
        fileView.add(toolBar, BorderLayout.NORTH);
        fileView.add(fileMainDetails, BorderLayout.CENTER);
        add(fileView, BorderLayout.SOUTH);

    }

    private void showErrorMessage(String errorMessage, String errorTitle) {
        JOptionPane.showMessageDialog(
                fileManager.getGui(),
                errorMessage,
                errorTitle,
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void deleteFile() {
    }

    private void renameFile() {
    }

    private void copyFile() {
    }

    private void newFile() {
        //TODO: create a file
        if (currentFile == null) {
            showErrorMessage("No location selected for new file.","Select Location");
            return;
        }

        if (newFilePanel==null) {
            newFilePanel = new JPanel(new BorderLayout(3,3));
            JPanel southRadio = new JPanel(new GridLayout(1,0,2,2));
            newTypeFile = new JRadioButton("File", true);
            JRadioButton newTypeDirectory = new JRadioButton("Directory");
            ButtonGroup bg = new ButtonGroup();
            bg.add(newTypeFile);
            bg.add(newTypeDirectory);
            southRadio.add(newTypeFile);
            southRadio.add(newTypeDirectory);

            name = new JTextField(15);

            newFilePanel.add(new JLabel("Name"), BorderLayout.WEST);
            newFilePanel.add(name);
            newFilePanel.add(southRadio, BorderLayout.SOUTH);
        }

        int result = JOptionPane.showConfirmDialog(
                fileManager.getGui(),
                newFilePanel,
                "Create File",
                JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                boolean created;
                File parentFile = currentFile;
                if (!parentFile.isDirectory()) {
                    parentFile = parentFile.getParentFile();
                }
                File file = new File( parentFile, name.getText() );
                if (newTypeFile.isSelected()) {
                    created = file.createNewFile();
                } else {
                    created = file.mkdir();
                }
                if (created) {

                    TreePath parentPath = findTreePath(parentFile);
                    DefaultMutableTreeNode parentNode =
                            (DefaultMutableTreeNode)parentPath.getLastPathComponent();

                    if (file.isDirectory()) {
                        // add the new node..
                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(file);

                        TreePath currentPath = findTreePath(currentFile);
                        DefaultMutableTreeNode currentNode =
                                (DefaultMutableTreeNode)currentPath.getLastPathComponent();

                        fileManager.getFileTree().getTreeModel().insertNodeInto(newNode, parentNode, parentNode.getChildCount());
                    }

                    fileManager.getFileTree().showChildren(parentNode);
                } else {
                    String msg = "The file '" +
                            file +
                            "' could not be created.";
                    showErrorMessage(msg, "Create Failed");
                }
            } catch(Throwable t) {
                showThrowable(t);
            }
        }
        fileManager.getGui().repaint();
    }

    private void showThrowable(Throwable t) {
        t.printStackTrace();
        JOptionPane.showMessageDialog(
                fileManager.getGui(),
                t.toString(),
                t.getMessage(),
                JOptionPane.ERROR_MESSAGE
        );
        fileManager.getGui().repaint();
    }

    private TreePath findTreePath(File parentFile) {
        FileTree tree = fileManager.getFileTree();
        for (int ii = 0; ii < tree.getRowCount(); ii++) {
            TreePath treePath = tree.getPathForRow(ii);
            Object object = treePath.getLastPathComponent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)object;
            File nodeFile = (File)node.getUserObject();
            if (nodeFile == parentFile) {
                return treePath;
            }
        }
        // not found!
        return null;
    }

    public void setFileDetail(File file) {
        // TODO: Bug
//        date.setText(String.valueOf(file.lastModified()));
//        size.setText(String.valueOf(size(file.toPath())));
//        if (file.isDirectory()) {
//            isDir.setSelected(true);
//            isFile.setSelected(false);
//        } else {
//            isDir.setSelected(false);
//            isFile.setSelected(true);
//        }
//        fileIcon.setIcon(FileSystemView.getFileSystemView().getSystemIcon(file));
//        fileIcon.setText(file.getName());
//        path.setText(file.getAbsolutePath());
        currentFile = fileManager.getCurrentFile();
        currentFile = file;
        Icon icon = fileSystemView.getSystemIcon(file);
        fileIcon.setIcon(icon);
        fileIcon.setText(fileSystemView.getSystemDisplayName(file));
        path.setText(file.getPath());
        date.setText(new Date(file.lastModified()).toString());
        size.setText(file.length() + " bytes");
        readCheckBox.setSelected(file.canRead());
        writeCheckBox.setSelected(file.canWrite());
        executeCheckBox.setSelected(file.canExecute());
        isDir.setSelected(file.isDirectory());
        isFile.setSelected(file.isFile());

        JFrame f = (JFrame)fileManager.getGui().getTopLevelAncestor();
        if (f!=null) {
            f.setTitle(
                    "File Manager" +
                            " :: " +
                            fileSystemView.getSystemDisplayName(file) );
        }
        fileManager.getGui().repaint();
    }

    /**
     * Attempts to calculate the size of a file or directory.
     * @see <a href="https://stackoverflow.com/questions/2149785/get-size-of-folder-or-file/19877372#19877372">solution form stack overflow</a>
     * Since the operation is non-atomic, the returned value may be inaccurate.
     * However, this method is quick and does its best.
     */
    public static long size(Path path) {

        final AtomicLong size = new AtomicLong(0);

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {

                    System.out.println("skipped: " + file + " (" + exc + ")");
                    // Skip folders that can't be traversed
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

                    if (exc != null)
                        System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
                    // Ignore errors traversing a folder
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
        }

        return size.get();
    }

    void setTableData(List<File> files) {
        // TODO: Bug
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (fileTableModel==null) {
                    fileTableModel = new FileTableModel();
                    table.setModel(fileTableModel);
                }
                table.getSelectionModel().removeListSelectionListener(listSelectionListener);
                fileTableModel.setFiles(files);
                table.getSelectionModel().addListSelectionListener(listSelectionListener);
                if (!cellSizesSet) {
                    Icon icon = fileSystemView.getSystemIcon(files.get(0));

                    // size adjustment to better account for icons
                    table.setRowHeight( icon.getIconHeight()+rowIconPadding );

                    setColumnWidth(0,-1);
                    setColumnWidth(3,60);
                    table.getColumnModel().getColumn(3).setMaxWidth(120);
                    setColumnWidth(4,-1);
                    setColumnWidth(5,-1);
                    setColumnWidth(6,-1);
                    setColumnWidth(7,-1);
                    setColumnWidth(8,-1);
                    setColumnWidth(9,-1);

                    cellSizesSet = true;
                }
            }
        });
    }

    private void setColumnWidth(int column, int width) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        if (width<0) {
            // use the preferred width of the header..
            JLabel label = new JLabel( (String)tableColumn.getHeaderValue() );
            Dimension preferred = label.getPreferredSize();
            // altered 10->14 as per camickr comment.
            width = (int)preferred.getWidth()+14;
        }
        tableColumn.setPreferredWidth(width);
        tableColumn.setMaxWidth(width);
        tableColumn.setMinWidth(width);
    }

    class FileTableModel extends AbstractTableModel {
        private List<File> files;
        private String[] columnNames = {
                "Icon",
                "File",
                "Path/name",
                "Size",
                "Last Modified",
                "R",
                "W",
                "E",
                "D",
                "F",
        };

        FileTableModel() {
            files = new ArrayList<>();
        }

        FileTableModel(List<File> files) {
            this.files = files;
        }

        void setFiles(List<File> files) {
            this.files = files;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return files.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            File file = files.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return fileSystemView.getSystemIcon(file);
                case 1:
                    return fileSystemView.getSystemDisplayName(file);
                case 2:
                    return file.getPath();
                case 3:
                    return file.length();
                case 4:
                    return file.lastModified();
                case 5:
                    return file.canRead();
                case 6:
                    return file.canWrite();
                case 7:
                    return file.canExecute();
                case 8:
                    return file.isDirectory();
                case 9:
                    return file.isFile();
                default:
                    System.err.println("Logic Error");
            }
            return "";
        }

        public File getFile(int row) {
            return files.get(row);
        }

        public List<File> getFiles() {
            return files;
        }
    }


}
