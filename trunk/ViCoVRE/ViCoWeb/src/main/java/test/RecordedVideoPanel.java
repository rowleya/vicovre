/*
 * Copyright (c) 2008, University of Manchester All rights reserved.
 * See LICENCE in root directory of source code for details of the license.
 */

package test;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Vector;

import javax.media.CannotRealizeException;
import javax.media.Codec;
import javax.media.Effect;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.NoDataSourceException;
import javax.media.NoPlayerException;
import javax.media.PackageManager;
import javax.media.Player;
import javax.media.PlugInManager;
import javax.media.protocol.DataSource;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.googlecode.vicovre.codecs.colourspace.YUV420RGB32Converter;
import com.googlecode.vicovre.codecs.h261.H261ASDecoder;
import com.googlecode.vicovre.codecs.h261.H261Decoder;
import com.googlecode.vicovre.media.Misc;
import com.googlecode.vicovre.media.renderer.RGBRenderer;

/**
 * A Panel for playing recorded video
 * @author Andrew G D Rowley
 * @version 1.0
 */
public class RecordedVideoPanel extends JPanel {

    private RGBRenderer renderer = new RGBRenderer(new Effect[]{});

    private Player mediaPlayer = null;

    private Component component = null;

    private DataSource dataSource = null;

    /**
     * Creates a new Recorded Video Panel
     * @param filename The name of the file to play
     * @throws NoDataSourceException
     * @throws IOException
     * @throws CannotRealizeException
     * @throws NoPlayerException
     */
    public RecordedVideoPanel(String filename)
            throws NoDataSourceException, IOException, NoPlayerException,
            CannotRealizeException {
        this(filename, 0, 1.0);
    }

    /**
     * Creates a new Recorded Video Panel
     * @param filename The name of the file to play
     * @param seek The position to start from in milliseconds
     * @throws NoDataSourceException
     * @throws IOException
     * @throws CannotRealizeException
     * @throws NoPlayerException
     */
    public RecordedVideoPanel(String filename, long seek)
            throws NoDataSourceException, IOException, NoPlayerException,
            CannotRealizeException {
        this(filename, seek, 1.0);
    }

    /**
     * Creates a new Recorded Video Panel
     * @param filename The name of the file to play
     * @param seek The position to start from in milliseconds
     * @param scale The speed at which to play (1.0 = normal)
     * @throws NoDataSourceException
     * @throws IOException
     * @throws CannotRealizeException
     * @throws NoPlayerException
     */
    public RecordedVideoPanel(String filename, long seek, double scale)
            throws NoDataSourceException, IOException, NoPlayerException,
            CannotRealizeException {
        MediaLocator locator = new MediaLocator("memetic://" + filename);
        dataSource = Manager.createDataSource(locator);

        mediaPlayer = Manager.createRealizedPlayer(dataSource);
        mediaPlayer.prefetch();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        component = mediaPlayer.getVisualComponent();
        add(component);
        add(mediaPlayer.getControlPanelComponent());
    }

    /**
     *
     * @see java.awt.Component#setSize(java.awt.Dimension)
     */
    public void setSize(int width, int height) {
        super.setSize(width, height);
        component.setPreferredSize(new Dimension(width, height));
        component.setSize(width, height);
        component.setBounds(0, 0, width, height);
    }

    /**
     *
     * @see java.awt.Component#setBounds(int, int, int, int)
     */
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        component.setPreferredSize(new Dimension(width, height));
        component.setSize(width, height);
        component.setBounds(0, 0, width, height);
    }

    /**
     * Starts the panel
     */
    public void play() {
        renderer.start();
    }

    /**
     * Stops the panel
     */
    public void stop() {
        renderer.stop();
    }

    /**
     * The main method
     * @param args None
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Vector<String> prefixes = PackageManager.getProtocolPrefixList();
        prefixes.add("com.googlecode.vicovre");

        PlugInManager.setPlugInList(new Vector<Codec>(), PlugInManager.CODEC);
        Misc.addCodec(H261ASDecoder.class);
        Misc.addCodec(H261Decoder.class);
        Misc.addCodec(YUV420RGB32Converter.class);
        RecordedVideoPanel panel = new RecordedVideoPanel(args[0]);
        JFrame frame = new JFrame("RecordedVideoPanel");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setLayout(new BoxLayout(frame.getContentPane(),
                BoxLayout.X_AXIS));
        frame.getContentPane().add(panel);
        panel.setSize(new Dimension(352, 288));
        frame.pack();
        frame.setVisible(true);
    }
}
