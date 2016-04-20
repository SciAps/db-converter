package com.sciaps;

import com.devsmart.StringUtils;
import com.google.common.io.Files;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public final class MainFrame extends javax.swing.JFrame implements DBConverter.DBConverterStatusInterface {

    static Logger logger = LoggerFactory.getLogger(MainFrame.class);

    public static String mFilePath = ".";
    private JTextField mDBFileTextField;
    private JButton mFileChooseButton;
    private JButton mConvertButton;
    private JPanel mProgressPanel;
    private JLabel mStatusLabel;
    private JLabel mResultLabel;
    private JProgressBar mProgressBar;

    public MainFrame() {
        setTitle("LIBZ DB Conversion");
        setLayout(new MigLayout("fill"));

        mResultLabel = new JLabel("No result yet");
        mResultLabel.setHorizontalAlignment(JLabel.CENTER);
        mResultLabel.setOpaque(true);

        mProgressPanel = createProgressPanel();

        add(createContenPanel(), "push, grow, wrap");
        add(mResultLabel, "hidemode 3, pushx, growx");
        add(mProgressPanel, "hidemode 3, pushx, growx");

        setIconImage(getIcon());

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                System.exit(0);
            }
        });

        mProgressPanel.setVisible(false);
        mResultLabel.setVisible(false);
    }

    private JPanel createProgressPanel() {
        mStatusLabel = new JLabel();
        mProgressBar = new JProgressBar(0, 0);
        mProgressBar.setIndeterminate(true);

        JPanel panel = new JPanel(new MigLayout("fill"));
        panel.add(mStatusLabel, "split");
        panel.add(mProgressBar, "growx, pushx");

        return panel;
    }

    private JPanel createContenPanel() {

        JLabel label = new JLabel("Enter Database Location: ");
        mDBFileTextField = new JTextField();

        mFileChooseButton = new JButton("Select DB File");
        mFileChooseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doSelectDBFile();
            }
        });

        mConvertButton = new JButton("Convert DB");
        mConvertButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doDBConversion();
            }
        });

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                doClose();
            }
        });

        JPanel panel = new JPanel(new MigLayout("fill"));
        panel.add(label, "cell 0 0 3");
        panel.add(mDBFileTextField, "pushx, growx, cell 0 1 3");
        panel.add(mFileChooseButton, "cell 0 2, pushx, growx");
        panel.add(mConvertButton, "cell 1 2, pushx, growx");
        panel.add(closeButton, "cell 2 2, pushx, growx");

        return panel;
    }

    private Image getIcon() {
        try {
            URL url = ClassLoader.getSystemResource("sciaps_icon.png");
            return ImageIO.read(url);

        } catch (IOException ex) {
            logger.error("", ex);
        }

        return null;
    }

    private void doSelectDBFile() {
        JFileChooser fileChooser = new JFileChooser(MainFrame.mFilePath);
        fileChooser.setDialogTitle("Select Database");
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileFilter(new FileNameExtensionFilter("files", "sdb"));

        int retval = fileChooser.showDialog(this, "Select");
        if (retval != JFileChooser.APPROVE_OPTION) {
            return;
        }

        final File db = fileChooser.getSelectedFile();

        setDBFile(db.getAbsolutePath());
    }

    private void doDBConversion() {

        String dbFilePath = mDBFileTextField.getText();

        if (StringUtils.isEmptyString(dbFilePath)) {
            JOptionPane.showMessageDialog(this, "Enter or select a DB file then try again.", "No File Found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final File dbFile = new File(dbFilePath);
        if (!dbFile.exists()) {
            JOptionPane.showMessageDialog(this, "No file found: " + dbFilePath, "No File Found", JOptionPane.ERROR_MESSAGE);
            return;
        } else if (Files.getFileExtension(dbFile.getAbsolutePath()).compareTo("sdb") != 0) {
            JOptionPane.showMessageDialog(this, "Invalid DB File, must end with .sdb", "Invalid DB File", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setDBProcessingInProgree(true);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mResultLabel.setVisible(false);
                String resultMsg = "DB Converted Successfully.";
                final DBConverter dbConverter = new DBConverter(MainFrame.this);

                try {
                    dbConverter.doDBConvert(dbFile);

                    logger.info(resultMsg);
                    mResultLabel.setBackground(Color.GREEN);
                } catch (Exception e) {
                    resultMsg = "DB Convert Failed (see log file for detail).";
                    logger.error("DB Convert Failed: ", e);
                    mResultLabel.setBackground(Color.RED);
                } finally {
                    setDBProcessingInProgree(false);

                    mResultLabel.setText(resultMsg);
                    mResultLabel.setVisible(true);
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void setDBProcessingInProgree(boolean val) {
        mConvertButton.setEnabled(!val);
        mFileChooseButton.setEnabled(!val);
        mProgressPanel.setVisible(val);
    }

    private void doClose() {
        System.exit(0);
    }

    public void setDBFile(String filename) {
        mDBFileTextField.setText(filename);
    }

    @Override
    public void processingState(String msg) {
        mStatusLabel.setText(msg);
    }
}