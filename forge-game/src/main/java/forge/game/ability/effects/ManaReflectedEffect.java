package forge.game.ability.effects;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

public class ManaReflectedEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        // Spells are not undoable
        AbilityManaPart ma = sa.getManaPart();
        sa.setUndoable(sa.isAbility() && sa.isUndoable() && sa.getSubAbility() == null);

        final Collection<String> colors = CardUtil.getReflectableManaColors(sa);

        for (final Player player : getTargetPlayers(sa)) {
            final String generated = generatedReflectedMana(sa, colors, player);
            ma.produceMana(generated, player, sa);
        }
    }


    // *************** Utility Functions **********************

    /**
     * <p>
     * generatedReflectedMana.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param colors
     *            a {@link java.util.ArrayList} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a {@link java.lang.String} object.
     */
    private static String generatedReflectedMana(final SpellAbility sa, final Collection<String> colors, final Player player) {
        if (colors.isEmpty()) {
            return "0";
        }
        // Calculate generated mana here for stack description and resolving
        final int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa) : 1;

        String baseMana;

        // TODO: This effect explicitly obeys express color choice as set by auto payment and AI routines in order
        // to avoid misplays and auto mana payment selection errors. Perhaps a better solution is possible?
        String expressChoiceColors = sa.getManaPart().getExpressChoice();
        ColorSet colorMenu = null;
        byte mask = 0;
        // loop through colors to make menu
        for (int nChar = 0; nChar < expressChoiceColors.length(); nChar++) {
            mask |= MagicColor.fromName(expressChoiceColors.charAt(nChar));
        }

        if (mask == 0 && !expressChoiceColors.isEmpty() && colors.contains("colorless")) {
            baseMana = MagicColor.toShortString(player.getController().chooseColorAllowColorless(Localizer.getInstance().getMessage("lblSelectManaProduce"), sa.getHostCard(), ColorSet.fromMask(mask)));
        } else {
            // Nothing set previously so ask player if needed
            if (mask == 0) {
                if (sa.getNeedChooseMana()) {
                    if(colors.contains("colorless")) {
                        baseMana = MagicColor.toShortString(player.getController().chooseColorAllowColorless("Select Mana to Produce", sa.getHostCard(), ColorSet.fromNames(colors)));
                    } else {
                        baseMana = MagicColor.toShortString(player.getController().chooseColor("Select Mana to Produce", sa, ColorSet.fromNames(colors)));
                    }
                } else if (colors.size() == 1) {
                    baseMana = MagicColor.toShortString(colors.iterator().next());
                } else {
                    if (sa.getUsedToPayMana() == null) {
                        if(colors.contains("colorless")) {
                            baseMana = MagicColor.toShortString(player.getController().chooseColorAllowColorless("Select Mana to Produce", sa.getHostCard(), ColorSet.fromNames(colors)));
                        } else {
                            baseMana = MagicColor.toShortString(player.getController().chooseColor("Select Mana to Produce", sa, ColorSet.fromNames(colors)));
                        }
                    } else {
                        String usedToPayMana = sa.getUsedToPayMana().toString();
                        ArrayList<String> new_colors = new ArrayList<>();
                        
                        for(String color : colors) {
                            if(color.equals("white") && (usedToPayMana.contains("{W") || usedToPayMana.contains("W}"))) {
                                new_colors.add("white");
                            }
                            if(color.equals("blue") && (usedToPayMana.contains("{U") || usedToPayMana.contains("U}"))) {
                                new_colors.add("blue");
                            }
                            if(color.equals("black") && (usedToPayMana.contains("{B") || usedToPayMana.contains("B}"))) {
                                new_colors.add("black");
                            }
                            if(color.equals("red") && (usedToPayMana.contains("{R") || usedToPayMana.contains("R}"))) {
                                new_colors.add("red");
                            }
                            if(color.equals("green") && (usedToPayMana.contains("{G") || usedToPayMana.contains("G}"))) {
                                new_colors.add("green");
                            }
                            if(color.equals("colorless") && (usedToPayMana.contains("{C") || usedToPayMana.contains("C}"))) {
                                new_colors.add("colorless");
                            }
                        }

                        if(new_colors.size() == 1) {
                            baseMana = MagicColor.toShortString(new_colors.iterator().next());
                        } else if(new_colors.size() > 1) {
                            if(new_colors.contains("colorless")) {
                                baseMana = MagicColor.toShortString(player.getController().chooseColorAllowColorless("Select Mana to Produce", sa.getHostCard(), ColorSet.fromNames(new_colors)));
                            } else {
                                baseMana = MagicColor.toShortString(player.getController().chooseColor("Select Mana to Produce", sa, ColorSet.fromNames(new_colors)));
                            }
                        } else {
                            baseMana = MagicColor.toShortString(colors.iterator().next());
                        }
                    }
                }
            } else {
                colorMenu = ColorSet.fromMask(mask);
                byte color = sa.getActivatingPlayer().getController().chooseColor(Localizer.getInstance().getMessage("lblSelectManaProduce"), sa, colorMenu);
                if (color == 0) {
                    System.err.println("Unexpected behavior in ManaReflectedEffect: " + sa.getActivatingPlayer() + " - color mana choice is empty for " + sa.getHostCard().getName());
                }
                baseMana = MagicColor.toShortString(color);
            }
        }

        final StringBuilder sb = new StringBuilder();
        if (amount == 0) {
            sb.append("0");
        } else {
            if (StringUtils.isNumeric(baseMana)) {
                // if baseMana is an integer(colorless), just multiply amount
                // and baseMana
                final int base = Integer.parseInt(baseMana);
                sb.append(base * amount);
            } else {
                for (int i = 0; i < amount; i++) {
                    if (i != 0) {
                        sb.append(" ");
                    }
                    sb.append(baseMana);
                }
            }
        }
        return sb.toString();
    }
}
