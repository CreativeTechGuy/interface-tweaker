package com.creativetechguy;

import lombok.Getter;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.PluginPanel;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class InterfaceTweakerPanel extends PluginPanel {
    private InterfaceTweakerConfig config;

    @Getter
    private boolean isActive = false;

    private InterfaceOverrideConfigManager interfaceOverrideConfigManager;
    private final JPanel widgetList;

    private final Collection<Widget> visibleWidgets;
    private final InterfaceHighlightOverlay interfaceHighlightOverlay;

    private int updateId = 0;

    public InterfaceTweakerPanel(InterfaceOverrideConfigManager interfaceOverrideConfigManager, Collection<Widget> visibleWidgets, InterfaceHighlightOverlay interfaceHighlightOverlay, InterfaceTweakerConfig config) {
        this.interfaceOverrideConfigManager = interfaceOverrideConfigManager;
        this.visibleWidgets = visibleWidgets;
        this.interfaceHighlightOverlay = interfaceHighlightOverlay;
        this.config = config;

        setLayout(new BorderLayout());

        widgetList = new JPanel();
        widgetList.setLayout(new BoxLayout(widgetList, BoxLayout.Y_AXIS));
        widgetList.setBorder(new EmptyBorder(10, 5, 5, 5));

        add(widgetList, BorderLayout.CENTER);
    }

    @Override
    public void onActivate() {
        isActive = true;
        updateList();
    }

    @Override
    public void onDeactivate() {
        isActive = false;
    }

    public void updateList() {
        updateId++;
        int currentUpdateId = updateId;
        ArrayList<Widget> visibleWidgetsCopy = new ArrayList<>(visibleWidgets);
        visibleWidgetsCopy.sort(Comparator.comparing(Widget::getId));
        ArrayList<InterfaceOverride> overridesCopy = new ArrayList<>(interfaceOverrideConfigManager.getAllOverrides());
        overridesCopy.sort(Comparator.comparing(InterfaceOverride::getId));
        SwingUtilities.invokeLater(() -> {
            if (updateId != currentUpdateId) {
                return;
            }
            widgetList.removeAll();
            for (InterfaceOverride o : overridesCopy) {
                addRow(null, o.getId());
            }
            for (Widget w : visibleWidgetsCopy) {
                if (!interfaceOverrideConfigManager.hasOverride(w.getId())) {
                    addRow(w, w.getId());
                }
            }
            revalidate();
            repaint();
        });
    }

    private boolean hasFriendlyName(int id) {
        if (interfaceOverrideConfigManager.hasLabel(id)) {
            return true;
        } else if (NamedInterfaces.hasName(id) && !config.disablePluginInterfaceNames()) {
            return true;
        }
        return false;
    }

    private String getName(int id) {
        if (interfaceOverrideConfigManager.hasLabel(id)) {
            return interfaceOverrideConfigManager.getLabel(id) + " (" + id + ")";
        } else if (NamedInterfaces.hasName(id) && !config.disablePluginInterfaceNames()) {
            return NamedInterfaces.getName(id) + " (" + id + ")";
        }
        return Integer.toString(id);
    }

    private void addRow(@Nullable Widget w, int id) {
        if (config.interfacesShown() == InterfaceCategories.ONLY_NAMED && !hasFriendlyName(id) && !interfaceOverrideConfigManager.hasOverride(
                id)) {
            return;
        }
        JLabel data = new JLabel();
        data.setText(getName(id));
        if (interfaceOverrideConfigManager.hasOverride(id)) {
            data.setToolTipText((interfaceOverrideConfigManager.getOverride(id)
                    .isHidden() ? "Hidden" : interfaceOverrideConfigManager.getOverride(id)
                    .getOpacityPercent()) + ": " + getName(id));
        } else {
            data.setToolTipText(getName(id));
        }

        if (w != null) {
            data.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    interfaceHighlightOverlay.setSelectedWidget(w);
                    data.setForeground(Color.ORANGE);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    interfaceHighlightOverlay.setSelectedWidget(null);
                    if (interfaceOverrideConfigManager.hasOverride(id)) {
                        data.setForeground(Color.CYAN);
                    } else {
                        data.setForeground(null);
                    }
                }
            });
        }

        JPopupMenu menu = new JPopupMenu();
        if (interfaceOverrideConfigManager.hasOverride(id)) {
            data.setForeground(Color.CYAN);
            JMenuItem resetMenuItem = new JMenuItem("Restore Widget");
            resetMenuItem.addActionListener((l) -> {
                interfaceOverrideConfigManager.removeOverride(id);
                updateList();
            });
            menu.add(resetMenuItem);
        }
        if (interfaceOverrideConfigManager.hasLabel(id)) {
            JMenuItem removeLabelMenuItem = new JMenuItem("Remove Custom Label");
            removeLabelMenuItem.addActionListener((l) -> {
                interfaceOverrideConfigManager.removeLabel(id);
                updateList();
            });
            menu.add(removeLabelMenuItem);
        }
        JMenuItem labelMenuItem = new JMenuItem(interfaceOverrideConfigManager.hasLabel(id) ? "Change Custom label" : "Set Custom Label");
        labelMenuItem.addActionListener((l) -> {
            String newLabel = JOptionPane.showInputDialog("Interface Label:");
            if (!newLabel.trim().isEmpty()) {
                interfaceOverrideConfigManager.setLabel(id, newLabel);
            } else {
                interfaceOverrideConfigManager.removeLabel(id);
            }
            updateList();
        });
        menu.add(labelMenuItem);
        if (!(interfaceOverrideConfigManager.hasOverride(id) && interfaceOverrideConfigManager.getOverride(id)
                .isHidden())) {
            JMenuItem hideMenuItem = new JMenuItem("Hide Widget");
            hideMenuItem.addActionListener((l) -> {
                interfaceOverrideConfigManager.addOverride(new InterfaceOverride(id, 255));
                updateList();
            });
            menu.add(hideMenuItem);
        }
        for (int i = 25; i <= 75; i += 25) {
            JMenuItem setAlphaMenuItem = new JMenuItem("Set " + i + "% Visible");
            final int opacity = 255 - (int) Math.round((i / 100.0) * 255);
            if (interfaceOverrideConfigManager.hasOverride(id) && interfaceOverrideConfigManager.getOverride(id)
                    .getOpacity() == opacity) {
                continue;
            }
            setAlphaMenuItem.addActionListener((l) -> {
                interfaceOverrideConfigManager.addOverride(new InterfaceOverride(id, opacity));
                updateList();
            });
            menu.add(setAlphaMenuItem);
        }

        menu.setBorder(new EmptyBorder(2, 2, 2, 2));
        data.setComponentPopupMenu(menu);
        widgetList.add(data);
    }
}
