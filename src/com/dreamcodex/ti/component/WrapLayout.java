package com.dreamcodex.ti.component;

import java.awt.*;

/**
 * A FlowLayout that wraps its components onto more rows when the container is too narrow,
 * and reports a preferred height that includes those rows. (FlowLayout itself always reports
 * the size of a single row, so a container with a BorderLayout cuts the wrapped rows off.)
 */
public class WrapLayout extends FlowLayout {

    public WrapLayout(int align) {
        super(align);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            // Use the width the container has, or the nearest ancestor's before the first layout
            Container container = target;
            while (container.getSize().width == 0 && container.getParent() != null) {
                container = container.getParent();
            }
            int targetWidth = container.getSize().width;
            if (targetWidth == 0) {
                targetWidth = Integer.MAX_VALUE;
            }
            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
            int maxWidth = targetWidth - horizontalInsetsAndGap;

            Dimension size = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;
            for (int i = 0; i < target.getComponentCount(); i++) {
                Component component = target.getComponent(i);
                if (component.isVisible()) {
                    Dimension d = preferred ? component.getPreferredSize() : component.getMinimumSize();
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        addRow(size, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }
                    if (rowWidth != 0) {
                        rowWidth += hgap;
                    }
                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }
            addRow(size, rowWidth, rowHeight);
            size.width += horizontalInsetsAndGap;
            size.height += insets.top + insets.bottom + vgap * 2;
            return size;
        }
    }

    private void addRow(Dimension size, int rowWidth, int rowHeight) {
        size.width = Math.max(size.width, rowWidth);
        if (size.height > 0) {
            size.height += getVgap();
        }
        size.height += rowHeight;
    }
}
