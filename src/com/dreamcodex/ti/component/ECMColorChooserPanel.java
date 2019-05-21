package com.dreamcodex.ti.component;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Created with IntelliJ IDEA.
 * User: Rasmus
 * Date: 29-11-13
 * Time: 16:25
 */
public class ECMColorChooserPanel extends AbstractColorChooserPanel implements ChangeListener {

    protected JSlider redSlider;
    protected JSlider greenSlider;
    protected JSlider blueSlider;
    protected JSpinner redField;
    protected JSpinner blueField;
    protected JSpinner greenField;
    protected ColorGridPanel colorGridPanel;

    private final int minValue = 0;
    private final int maxValue = 255;

    private boolean isAdjusting = false; // indicates the fields are being set internally

    public ECMColorChooserPanel() {
        super();
        setInheritsPopupMenu(true);
    }

    /**
     * Sets the values of the controls to reflect the color
     */
    private void setColor( Color newColor ) {
        int red = newColor.getRed();
        int blue = newColor.getBlue();
        int green = newColor.getGreen();

        if (redSlider.getValue() != red) {
            redSlider.setValue(red);
        }
        if (greenSlider.getValue() != green) {
            greenSlider.setValue(green);
        }
        if (blueSlider.getValue() != blue) {
            blueSlider.setValue(blue);
        }

        if (((Integer)redField.getValue()).intValue() != red)
            redField.setValue(new Integer(red));
        if (((Integer)greenField.getValue()).intValue() != green)
            greenField.setValue(new Integer(green));
        if (((Integer)blueField.getValue()).intValue() != blue )
            blueField.setValue(new Integer(blue));

        colorGridPanel.repaint();
    }

    public String getDisplayName() {
        return "ECM Safe";
    }

    /**
     * Provides a hint to the look and feel as to the
     * <code>KeyEvent.VK</code> constant that can be used as a mnemonic to
     * access the panel. A return value <= 0 indicates there is no mnemonic.
     * <p>
     * The return value here is a hint, it is ultimately up to the look
     * and feel to honor the return value in some meaningful way.
     * <p>
     * This implementation looks up the value from the default
     * <code>ColorChooser.rgbMnemonic</code>, or if it
     * isn't available (or not an <code>Integer</code>) returns -1.
     * The lookup for the default is done through the <code>UIManager</code>:
     * <code>UIManager.get("ColorChooser.rgbMnemonic");</code>.
     *
     * @return KeyEvent.VK constant identifying the mnemonic; <= 0 for no
     *         mnemonic
     * @see #getDisplayedMnemonicIndex
     * @since 1.4
     */
    public int getMnemonic() {
        return getInt("ColorChooser.rgbMnemonic", -1);
    }

    /**
     * Provides a hint to the look and feel as to the index of the character in
     * <code>getDisplayName</code> that should be visually identified as the
     * mnemonic. The look and feel should only use this if
     * <code>getMnemonic</code> returns a value > 0.
     * <p>
     * The return value here is a hint, it is ultimately up to the look
     * and feel to honor the return value in some meaningful way. For example,
     * a look and feel may wish to render each
     * <code>AbstractColorChooserPanel</code> in a <code>JTabbedPane</code>,
     * and further use this return value to underline a character in
     * the <code>getDisplayName</code>.
     * <p>
     * This implementation looks up the value from the default
     * <code>ColorChooser.rgbDisplayedMnemonicIndex</code>, or if it
     * isn't available (or not an <code>Integer</code>) returns -1.
     * The lookup for the default is done through the <code>UIManager</code>:
     * <code>UIManager.get("ColorChooser.rgbDisplayedMnemonicIndex");</code>.
     *
     * @return Character index to render mnemonic for; -1 to provide no
     *                   visual identifier for this panel.
     * @see #getMnemonic
     * @since 1.4
     */
    public int getDisplayedMnemonicIndex() {
        return getInt("ColorChooser.rgbDisplayedMnemonicIndex", -1);
    }

    public Icon getSmallDisplayIcon() {
        return null;
    }

    public Icon getLargeDisplayIcon() {
        return null;
    }

    /**
     * The background color, foreground color, and font are already set to the
     * defaults from the defaults table before this method is called.
     */
    public void installChooserPanel(JColorChooser enclosingChooser) {
        super.installChooserPanel(enclosingChooser);
    }

    protected void buildChooser() {

        String redString = UIManager.getString("ColorChooser.rgbRedText");
        String greenString = UIManager.getString("ColorChooser.rgbGreenText");
        String blueString = UIManager.getString("ColorChooser.rgbBlueText");

        setLayout( new BorderLayout() );
        setBorder(new EmptyBorder(8, 8, 8, 8));
        Color color = getColorFromModel();

        JPanel enclosure = new JPanel();
        enclosure.setLayout( new SmartGridLayout( 3, 3 ) );
        enclosure.setInheritsPopupMenu(true);

        // The panel that holds the sliders

        add( enclosure, BorderLayout.CENTER );

        // The row for the red value
        JLabel l = new JLabel(redString);
        l.setDisplayedMnemonic(getInt("ColorChooser.rgbRedMnemonic", -1));
        enclosure.add(l);
        redSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, color.getRed());
        redSlider.setMajorTickSpacing( 85 );
        redSlider.setMinorTickSpacing( 17 );
        redSlider.setSnapToTicks(true);
        redSlider.setPaintTicks( true );
        redSlider.setPaintLabels( true );
        redSlider.setInheritsPopupMenu(true);
        enclosure.add( redSlider );
        redField = new JSpinner(new SpinnerNumberModel(color.getRed(), minValue, maxValue, 17));
        l.setLabelFor(redSlider);
        redField.setInheritsPopupMenu(true);
        JPanel redFieldHolder = new JPanel(new CenterLayout());
        redFieldHolder.setInheritsPopupMenu(true);
        redField.addChangeListener(this);
        redFieldHolder.add(redField);
        enclosure.add(redFieldHolder);

        // The row for the green value
        l = new JLabel(greenString);
        l.setDisplayedMnemonic(getInt("ColorChooser.rgbGreenMnemonic", -1));
        enclosure.add(l);
        greenSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, color.getGreen());
        greenSlider.setMajorTickSpacing( 85 );
        greenSlider.setMinorTickSpacing( 17 );
        greenSlider.setSnapToTicks(true);
        greenSlider.setPaintTicks( true );
        greenSlider.setPaintLabels( true );
        greenSlider.setInheritsPopupMenu(true);
        enclosure.add(greenSlider);
        greenField = new JSpinner(new SpinnerNumberModel(color.getGreen(), minValue, maxValue, 17));
        l.setLabelFor(greenSlider);
        greenField.setInheritsPopupMenu(true);
        JPanel greenFieldHolder = new JPanel(new CenterLayout());
        greenFieldHolder.add(greenField);
        greenFieldHolder.setInheritsPopupMenu(true);
        greenField.addChangeListener(this);
        enclosure.add(greenFieldHolder);

        // The slider for the blue value
        l = new JLabel(blueString);
        l.setDisplayedMnemonic(getInt("ColorChooser.rgbBlueMnemonic", -1));
        enclosure.add(l);
        blueSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, color.getBlue());
        blueSlider.setMajorTickSpacing( 85 );
        blueSlider.setMinorTickSpacing( 17 );
        blueSlider.setSnapToTicks(true);
        blueSlider.setPaintTicks( true );
        blueSlider.setPaintLabels( true );
        blueSlider.setInheritsPopupMenu(true);
        enclosure.add(blueSlider);
        blueField = new JSpinner(new SpinnerNumberModel(color.getBlue(), minValue, maxValue, 17));
        l.setLabelFor(blueSlider);
        blueField.setInheritsPopupMenu(true);
        JPanel blueFieldHolder = new JPanel(new CenterLayout());
        blueFieldHolder.add(blueField);
        blueField.addChangeListener(this);
        blueFieldHolder.setInheritsPopupMenu(true);
        enclosure.add(blueFieldHolder);

        redSlider.addChangeListener( this );
        greenSlider.addChangeListener( this );
        blueSlider.addChangeListener( this );

        redSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        greenSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        blueSlider.putClientProperty("JSlider.isFilled", Boolean.TRUE);

        JPanel colorGridContainer = new JPanel(new BorderLayout());
        Border outsideBorder = BorderFactory.createEmptyBorder(8, 8, 8, 8);
        Border insideBorder = BorderFactory.createEtchedBorder();
        Border border = BorderFactory.createCompoundBorder(outsideBorder, insideBorder);
        colorGridContainer.setBorder(border);
        colorGridPanel = new ColorGridPanel();
        colorGridContainer.add(colorGridPanel, BorderLayout.CENTER);
        add(colorGridContainer, BorderLayout.WEST);
    }

    public void uninstallChooserPanel(JColorChooser enclosingChooser) {
        super.uninstallChooserPanel(enclosingChooser);
        removeAll();
    }

    public void updateChooser() {
        if (!isAdjusting) {
            isAdjusting = true;

            setColor(getColorFromModel());

            isAdjusting = false;
        }
    }

    public void stateChanged( ChangeEvent e ) {
        if ( e.getSource() instanceof JSlider && !isAdjusting) {

            int red = redSlider.getValue();
            int green = greenSlider.getValue();
            int blue = blueSlider.getValue() ;
            Color color = new Color (red, green, blue);

            getColorSelectionModel().setSelectedColor(color);
        } else if (e.getSource() instanceof JSpinner && !isAdjusting) {

            int red = ((Integer)redField.getValue()).intValue();
            int green = ((Integer)greenField.getValue()).intValue();
            int blue = ((Integer)blueField.getValue()).intValue();
            Color color = new Color (red, green, blue);

            getColorSelectionModel().setSelectedColor(color);
        }
    }

    /**
     * Returns an integer from the defaults table. If <code>key</code> does
     * not map to a valid <code>Integer</code>, <code>default</code> is
     * returned.
     *
     * @param key  an <code>Object</code> specifying the int
     * @param defaultValue Returned value if <code>key</code> is not available,
     *                     or is not an Integer
     * @return the int
     */
    static int getInt(Object key, int defaultValue) {
        Object value = UIManager.get(key);

        if (value instanceof Integer) {
            return ((Integer)value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String)value);
            } catch (NumberFormatException nfe) {}
        }
        return defaultValue;
    }

    private class ColorGridPanel extends JPanel implements MouseListener, ChangeListener {

        private int squareSize = 12;
        private Dimension dimension = new Dimension(squareSize * 16 + 2, squareSize * 16 + 2);
        private Color color = null;

        public ColorGridPanel() {
            addMouseListener(this);
            getColorSelectionModel().addChangeListener(this);
            color = getColorSelectionModel().getSelectedColor();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    Color squareColor = new Color(color.getRed(), x * 17, y * 17, 255);
                    g.setColor(squareColor);
                    g.fillRect(x * squareSize, y * squareSize, squareSize, squareSize);
                    g.setColor(Color.WHITE);
                    g.drawRect(x * squareSize, y * squareSize, squareSize, squareSize);
                }
            }
            g.setColor(Color.BLACK);
            g.drawRect((color.getGreen() / 17) * squareSize, (color.getBlue() / 17) * squareSize, squareSize, squareSize);
        }

        public Dimension getPreferredSize() {
            return dimension;
        }

        public Dimension getMinimumSize() {
            return dimension;
        }

        public void stateChanged(ChangeEvent e) {
           color = getColorSelectionModel().getSelectedColor();
        }

        public void mouseClicked(MouseEvent e) {
            Point point = e.getPoint();
            getColorSelectionModel().setSelectedColor(new Color(color.getRed(), (point.x / squareSize) * 17, (point.y / squareSize) * 17, 255));
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }
    }
}
