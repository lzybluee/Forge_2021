package forge.screens.match.menus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import com.google.common.primitives.Ints;

import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.menus.MenuUtil;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.screens.match.VAutoYields;
import forge.screens.match.controllers.CDock.ArcState;
import forge.toolbox.FSkin.SkinIcon;
import forge.toolbox.FSkin.SkinnedCheckBoxMenuItem;
import forge.toolbox.FSkin.SkinnedMenu;
import forge.toolbox.FSkin.SkinnedMenuItem;
import forge.toolbox.FSkin.SkinnedRadioButtonMenuItem;
import forge.util.Localizer;

/**
 * Returns a JMenu containing options associated with current game.
 * <p>
 * Replicates options available in Dock tab.
 */
public final class GameMenu {
    private final CMatchUI matchUI;
    public GameMenu(final CMatchUI matchUI) {
        this.matchUI = matchUI;
    }

    private static ForgePreferences prefs = FModel.getPreferences();
    private static boolean showIcons;

    public JMenu getMenu() {
        final Localizer localizer = Localizer.getInstance();
        final JMenu menu = new JMenu(localizer.getMessage("lblGame"));
        menu.setMnemonic(KeyEvent.VK_G);
        menu.add(getMenuItem_Undo());
        menu.add(getMenuItem_Concede());
        menu.add(getMenuItem_EndTurn());
        menu.add(getMenuItem_AlphaStrike());
        menu.addSeparator();
        menu.add(getMenuItem_TargetingArcs());
        menu.add(new CardOverlaysMenu(matchUI).getMenu());
        menu.add(getMenuItem_AutoYields());
        menu.add(getMenuItem_SkipAutoPay());
        menu.add(getMenuItem_SkipRestoreDeck());
        menu.add(getMenuItem_MTGAShuffle());
        menu.add(getMenuItem_StartPlayer());
        menu.addSeparator();
        menu.add(getMenuItem_ViewDeckList());
        menu.add(getMenuItem_ViewOpponentDeckList());
        menu.addSeparator();
        menu.add(getMenuItem_GameSoundEffects());
        return menu;
    }

    private static SkinnedCheckBoxMenuItem getMenuItem_GameSoundEffects() {
        final Localizer localizer = Localizer.getInstance();
        SkinnedCheckBoxMenuItem menuItem = new SkinnedCheckBoxMenuItem(localizer.getMessage("lblSoundEffects"));
        menuItem.setState(prefs.getPrefBoolean(FPref.UI_ENABLE_SOUNDS));
        menuItem.addActionListener(getSoundEffectsAction());
        return menuItem;
    }
    private static ActionListener getSoundEffectsAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                toggleGameSoundEffects();
            }
        };
    }
    private static void toggleGameSoundEffects() {
        final boolean isSoundEffectsEnabled = !prefs.getPrefBoolean(FPref.UI_ENABLE_SOUNDS);
        prefs.setPref(FPref.UI_ENABLE_SOUNDS, isSoundEffectsEnabled);
        prefs.save();
    }

    private SkinnedMenuItem getMenuItem_Undo() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblUndo"));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_Z));
        menuItem.addActionListener(getUndoAction());
        return menuItem;
    }

    private ActionListener getUndoAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                matchUI.getGameController().undoLastAction();
            }
        };
    }

    private SkinnedMenuItem getMenuItem_Concede() {
        SkinnedMenuItem menuItem = new SkinnedMenuItem(matchUI.getConcedeCaption());
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_CONCEDE) : null));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_Q));
        menuItem.addActionListener(getConcedeAction());
        return menuItem;
    }

    private ActionListener getConcedeAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                matchUI.concede();
            }
        };
    }

    private SkinnedMenuItem getMenuItem_AlphaStrike() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblAlphaStrike"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_ALPHASTRIKE) : null));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_A));
        menuItem.addActionListener(getAlphaStrikeAction());
        return menuItem;
    }

    private ActionListener getAlphaStrikeAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                matchUI.getGameController().alphaStrike();
            }
        };
    }

    private SkinnedMenuItem getMenuItem_EndTurn() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblEndTurn"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_ENDTURN) : null));
        menuItem.setAccelerator(MenuUtil.getAcceleratorKey(KeyEvent.VK_E));
        menuItem.addActionListener(getEndTurnAction());
        return menuItem;
    }

    private ActionListener getEndTurnAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                matchUI.getGameController().passPriorityUntilEndOfTurn();
            }
        };
    }

    private SkinnedMenu getMenuItem_TargetingArcs() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenu menu = new SkinnedMenu(localizer.getMessage("lblTargetingArcs"));
        final ButtonGroup group = new ButtonGroup();

        SkinIcon menuIcon = MenuUtil.getMenuIcon(FSkinProp.ICO_ARCSOFF);

        if (matchUI.getCDock().getArcState() == null) {
            final String arcStateStr = FModel.getPreferences().getPref(FPref.UI_TARGETING_OVERLAY);
            final Integer arcState = Ints.tryParse(arcStateStr);
            matchUI.getCDock().setArcState(ArcState.values()[arcState == null ? 0 : arcState]);
        }

        SkinnedRadioButtonMenuItem menuItem;
        menuItem = getTargetingArcRadioButton(localizer.getMessage("lblOff"), FSkinProp.ICO_ARCSOFF, ArcState.OFF);
        if (menuItem.isSelected()) { menuIcon = MenuUtil.getMenuIcon(FSkinProp.ICO_ARCSOFF); }
        group.add(menuItem);
        menu.add(menuItem);
        menuItem = getTargetingArcRadioButton(localizer.getMessage("lblCardMouseOver"), FSkinProp.ICO_ARCSHOVER, ArcState.MOUSEOVER);
        if (menuItem.isSelected()) { menuIcon = MenuUtil.getMenuIcon(FSkinProp.ICO_ARCSHOVER); }
        group.add(menuItem);
        menu.add(menuItem);
        menuItem = getTargetingArcRadioButton(localizer.getMessage("lblAlwaysOn"), FSkinProp.ICO_ARCSON, ArcState.ON);
        if (menuItem.isSelected()) { menuIcon = MenuUtil.getMenuIcon(FSkinProp.ICO_ARCSON); }
        group.add(menuItem);

        menu.setIcon((showIcons ? menuIcon : null));
        menu.add(menuItem);

        return menu;
    }

    private SkinnedRadioButtonMenuItem getTargetingArcRadioButton(final String caption, final FSkinProp icon, final ArcState arcState) {
        final SkinnedRadioButtonMenuItem menuItem = new SkinnedRadioButtonMenuItem(caption);
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(icon) : null));
        menuItem.setSelected(arcState == matchUI.getCDock().getArcState());
        menuItem.addActionListener(getTargetingRadioButtonAction(arcState));
        return menuItem;
    }

    private ActionListener getTargetingRadioButtonAction(final ArcState arcState) {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                prefs.setPref(FPref.UI_TARGETING_OVERLAY, String.valueOf(arcState.ordinal()));
                prefs.save();
                matchUI.getCDock().setArcState(arcState);
                setTargetingArcMenuIcon((SkinnedRadioButtonMenuItem)e.getSource());
            }
        };
    }

    private static void setTargetingArcMenuIcon(SkinnedRadioButtonMenuItem item) {
        final JPopupMenu pop = (JPopupMenu)item.getParent();
        final JMenu menu = (JMenu)pop.getInvoker();
        menu.setIcon(item.getIcon());
    }

    private SkinnedMenuItem getMenuItem_AutoYields() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblAutoYields"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_WARNING) : null));
        menuItem.addActionListener(getAutoYieldsAction());
        return menuItem;
    }

    private ActionListener getAutoYieldsAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final VAutoYields autoYields = new VAutoYields(matchUI);
                autoYields.showAutoYields();
            }
        };
    }

    private SkinnedMenuItem getMenuItem_ViewDeckList() {
        final Localizer localizer = Localizer.getInstance();
        final SkinnedMenuItem menuItem = new SkinnedMenuItem(localizer.getMessage("lblDeckList"));
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_DECKLIST) : null));
        menuItem.addActionListener(getViewDeckListAction());
        return menuItem;
    }

    private SkinnedMenuItem getMenuItem_ViewOpponentDeckList() {
        final SkinnedMenuItem menuItem = new SkinnedMenuItem("Opponent Deck List");
        menuItem.setIcon((showIcons ? MenuUtil.getMenuIcon(FSkinProp.ICO_DECKLIST) : null));
        menuItem.addActionListener(getViewOpponentDeckListAction());
        return menuItem;
    }

    private ActionListener getViewDeckListAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                matchUI.viewDeckList();
            }
        };
    }

    private ActionListener getViewOpponentDeckListAction() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                matchUI.viewOpponentDeckList();
            }
        };
    }
    
    private static SkinnedCheckBoxMenuItem getMenuItem_SkipRestoreDeck() {
        SkinnedCheckBoxMenuItem menuItem = new SkinnedCheckBoxMenuItem("Keep Sideboard");
        menuItem.setState(prefs.getPrefBoolean(FPref.UI_SKIP_RESTORE_DECK));
        menuItem.addActionListener(getSkipRestoreDeck());
        return menuItem;
    }
    private static ActionListener getSkipRestoreDeck() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                toggleSkipRestoreDeck();
            }
        };
    }
    private static void toggleSkipRestoreDeck() {
        final boolean skipRestoreDeck = !prefs.getPrefBoolean(FPref.UI_SKIP_RESTORE_DECK);
        prefs.setPref(FPref.UI_SKIP_RESTORE_DECK, skipRestoreDeck);
        prefs.save();
    }

    private static SkinnedCheckBoxMenuItem getMenuItem_MTGAShuffle() {
        SkinnedCheckBoxMenuItem menuItem = new SkinnedCheckBoxMenuItem("MTGA Shuffle");
        menuItem.setState(prefs.getPrefBoolean(FPref.FILTERED_HANDS));
        menuItem.addActionListener(getMTGAShuffle());
        return menuItem;
    }
    private static ActionListener getMTGAShuffle() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                toggleMTGAShuffle();
            }
        };
    }
    private static void toggleMTGAShuffle() {
        final boolean MTGAShuffle = !prefs.getPrefBoolean(FPref.FILTERED_HANDS);
        prefs.setPref(FPref.FILTERED_HANDS, MTGAShuffle);
        prefs.save();
    }

    private SkinnedMenu getMenuItem_StartPlayer() {
        final SkinnedMenu menu = new SkinnedMenu("Start Player");
        final ButtonGroup group = new ButtonGroup();

        SkinnedRadioButtonMenuItem menuItem;
        menuItem = getStartPlayerRadioButton("Random");
        group.add(menuItem);
        menu.add(menuItem);
        menuItem = getStartPlayerRadioButton("Human");
        group.add(menuItem);
        menu.add(menuItem);
        menuItem = getStartPlayerRadioButton("AI");
        group.add(menuItem);
        menu.add(menuItem);

        return menu;
    }

    private SkinnedRadioButtonMenuItem getStartPlayerRadioButton(final String caption) {
        final SkinnedRadioButtonMenuItem menuItem = new SkinnedRadioButtonMenuItem(caption);
        menuItem.setSelected(caption.equals(prefs.getPref(FPref.UI_START_PLAYER)));
        menuItem.addActionListener(getStartPlayerButtonAction(caption));
        return menuItem;
    }
    
    private ActionListener getStartPlayerButtonAction(final String starter) {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                prefs.setPref(FPref.UI_START_PLAYER, starter);
                prefs.save();
            }
        };
    }
    
    private static SkinnedCheckBoxMenuItem getMenuItem_SkipAutoPay() {
        SkinnedCheckBoxMenuItem menuItem = new SkinnedCheckBoxMenuItem("No Auto Pay/Confirm");
        menuItem.setState(prefs.getPrefBoolean(FPref.UI_SKIP_AUTO_PAY));
        menuItem.addActionListener(getSkipAutoPay());
        return menuItem;
    }
    private static ActionListener getSkipAutoPay() {
        return new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                toggleSkipAutoPay();
            }
        };
    }
    private static void toggleSkipAutoPay() {
        final boolean skipAutoPay = !prefs.getPrefBoolean(FPref.UI_SKIP_AUTO_PAY);
        prefs.setPref(FPref.UI_SKIP_AUTO_PAY, skipAutoPay);
        prefs.save();
    }
}
