package com.sciaps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;


public final class Main {


    public static void main(final String[] args) {

        String usrProfile = System.getProperty("user.home");
        String defaultFileLocation = usrProfile;
        if (usrProfile != null && usrProfile.isEmpty() == false) {
            defaultFileLocation = usrProfile + File.separator + "sciaps" + File.separator + "DB-Converter";
            System.setProperty("loggerFilename", defaultFileLocation + File.separator + "DBConvertLog");
        } else {
            System.setProperty("loggerFilename", "DBConvertLog");
        }

        final Logger logger = LoggerFactory.getLogger(Main.class);
        logger.info("Application Starting");

        final String defaultFilePath = defaultFileLocation;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame.setDefaultLookAndFeelDecorated(true);
                try {
                    UIManager.setLookAndFeel("org.pushingpixels.substance.api.skin.SubstanceOfficeSilver2007LookAndFeel");
                } catch (Exception e) {
                    logger.error("Unable to set custom Look and Feel, default is used: " + e.getMessage());
                }

                MainFrame mainFrame = new MainFrame();
                mainFrame.mFilePath = defaultFilePath;

                if (args.length == 1) {
                    logger.info("DB file: " + args[0]);
                    mainFrame.setDBFile(args[0]);
                }

                mainFrame.setSize(700, 200);
                mainFrame.setLocationRelativeTo(null);
                mainFrame.setVisible(true);
            }
        });
    }
}