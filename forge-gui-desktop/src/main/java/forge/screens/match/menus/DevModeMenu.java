package forge.screens.match.menus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import forge.menus.MenuUtil;
import forge.screens.match.controllers.CDev;
import forge.screens.match.views.IDevListener;
import forge.util.Localizer;

/**
 * Gets a menu that replicates all the DevMode options.
 * <p>
 * Simply calls the associated method in CDev.
 */
public class DevModeMenu implements ActionListener, IDevListener {

    private CDev controller;
    private JCheckBoxMenuItem playUnlimited = null, viewAll = null;
    public DevModeMenu(final CDev controller) {
        this.controller = controller;
        controller.addListener(this);
    }

    // Using an enum to avoid having to create multiple
    // ActionListeners each calling a single method.
    private enum DevMenuItem {
        GENERATE_MANA(Localizer.getInstance().getMessage("lblGenerateMana")),
        TUTOR_FOR_CARD(Localizer.getInstance().getMessage("lblTutor")),
        ADD_CARD_TO_HAND(Localizer.getInstance().getMessage("lblCardToHand")),
        ADD_CARD_TO_PLAY(Localizer.getInstance().getMessage("lblCastSpellOrPlayLand")),
        EXILE_FROM_HAND(Localizer.getInstance().getMessage("lblExileFromHand")),
        EXILE_FROM_PLAY(Localizer.getInstance().getMessage("lblExileFromPlay")),
        SET_PLAYER_LIFE(Localizer.getInstance().getMessage("lblSetLife")),
        WIN_GAME(Localizer.getInstance().getMessage("lblWinGame")),
        SETUP_GAME_STATE(Localizer.getInstance().getMessage("lblSetupGame")),
        DUMP_GAME_STATE(Localizer.getInstance().getMessage("lblDumpGame")),
        PLAY_UNLIMITED_LANDS("Play Unlimited"),
        VIEW_ALL(Localizer.getInstance().getMessage("lblViewAll")),
        ADD_COUNTER(Localizer.getInstance().getMessage("lblAddCounterPermanent")),
        TAP_PERMANENT(Localizer.getInstance().getMessage("lblTapPermanent")),
        UNTAP_PERMANENT(Localizer.getInstance().getMessage("lblUntapPermanent")),
        RIGGED_PLANAR_ROLL(Localizer.getInstance().getMessage("lblRiggedRoll")),
        PLANESWALK_TO(Localizer.getInstance().getMessage("lblWalkTo")),
        DEV_CORNER(Localizer.getInstance().getMessage("lblDeveloperCorner"));

        protected String caption;
        DevMenuItem(final String value) {
            this.caption = value;
        }
        protected static DevMenuItem getValue(final String s) {
            for (final DevMenuItem t : DevMenuItem.values()) {
                if (t.caption == s) {
                    return t;
                }
            }
            return null;
        }
    }

    @Override
    public void update(final boolean playUnlimited, final boolean mayViewAllCards) {
        if (this.playUnlimited != null && this.viewAll != null) {
            this.playUnlimited.setSelected(playUnlimited);
            this.viewAll.setSelected(mayViewAllCards);
        }
    }

    public JMenu getMenu() {
        final JMenu menu = new JMenu(Localizer.getInstance().getMessage("lblDev"));
        menu.setMnemonic(KeyEvent.VK_D);
        menu.add(getMenuItem(DevMenuItem.GENERATE_MANA));
        menu.add(getMenuItem(DevMenuItem.TUTOR_FOR_CARD));
        menu.addSeparator();
        menu.add(getMenuItem(DevMenuItem.ADD_CARD_TO_HAND));
        menu.add(getMenuItem(DevMenuItem.ADD_CARD_TO_PLAY));
        menu.add(getMenuItem(DevMenuItem.EXILE_FROM_HAND));
        menu.add(getMenuItem(DevMenuItem.EXILE_FROM_PLAY));
        menu.addSeparator();
        menu.add(getMenuItem(DevMenuItem.SET_PLAYER_LIFE));
        menu.add(getMenuItem(DevMenuItem.WIN_GAME));
        menu.addSeparator();
        menu.add(getMenuItem(DevMenuItem.SETUP_GAME_STATE));
        menu.add(getMenuItem(DevMenuItem.DUMP_GAME_STATE));
        menu.addSeparator();
        menu.add(playUnlimited = getCheckboxMenuItem(DevMenuItem.PLAY_UNLIMITED_LANDS));
        menu.add(viewAll = getCheckboxMenuItem(DevMenuItem.VIEW_ALL));
        menu.add(getMenuItem(DevMenuItem.ADD_COUNTER));
        menu.addSeparator();
        menu.add(getMenuItem(DevMenuItem.TAP_PERMANENT));
        menu.add(getMenuItem(DevMenuItem.UNTAP_PERMANENT));
        menu.addSeparator();
        menu.add(getMenuItem(DevMenuItem.RIGGED_PLANAR_ROLL));
        menu.add(getMenuItem(DevMenuItem.PLANESWALK_TO));
        menu.addSeparator();
        menu.add(getMenuItem(DevMenuItem.DEV_CORNER));
        return menu;
    }

    private JMenuItem getMenuItem(final DevMenuItem m) {
        final JMenuItem menuItem = new JMenuItem(m.caption);
        menuItem.addActionListener(this);
        return menuItem;
    }

    private JCheckBoxMenuItem getCheckboxMenuItem(final DevMenuItem m) {
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(m.caption);
        menuItem.addActionListener(this);
        return menuItem;
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        switch (DevMenuItem.getValue(e.getActionCommand())) {
        case GENERATE_MANA:        { controller.generateMana(false); break; }
        case TUTOR_FOR_CARD:       { controller.tutorForCard(false); break; }
        case ADD_CARD_TO_HAND:     { controller.addCardToHand(false); break; }
        case ADD_CARD_TO_PLAY:     { controller.addCardToBattlefield(false); break; }
        case EXILE_FROM_PLAY:      { controller.exileCardsFromPlay(); break; }
        case EXILE_FROM_HAND:      { controller.exileCardsFromHand(); break; }
        case SET_PLAYER_LIFE:      { controller.setPlayerLife(false); break; }
        case WIN_GAME:             { controller.winGame(false); break; }
        case SETUP_GAME_STATE:     { controller.setupGameState(false); break; }
        case DUMP_GAME_STATE:      { controller.dumpGameState(false); break; }
        case PLAY_UNLIMITED_LANDS: { controller.togglePlayUnlimited(); break; }
        case VIEW_ALL:             { controller.toggleViewAllCards(); break; }
        case ADD_COUNTER:          { controller.addCounterToPermanent(false); break; }
        case TAP_PERMANENT:        { controller.tapPermanent(false); break; }
        case UNTAP_PERMANENT:      { controller.untapPermanent(false); break; }
        case RIGGED_PLANAR_ROLL:   { controller.riggedPlanerRoll(); break; }
        case PLANESWALK_TO:        { controller.planeswalkTo(); break; }
        case DEV_CORNER:           { openDevForumInBrowser(); break; }
        default:
            break;
        }
    }

    private static void openDevForumInBrowser() {
        MenuUtil.openUrlInBrowser("http://www.slightlymagic.net/forum/viewforum.php?f=52");
    }

}
