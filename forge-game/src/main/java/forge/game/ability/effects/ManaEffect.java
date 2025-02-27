package forge.game.ability.effects;

import static forge.util.TextUtil.toManaString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;
import io.sentry.Breadcrumb;
import io.sentry.Sentry;

public class ManaEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        AbilityManaPart abMana = sa.getManaPart();
        final List<Player> tgtPlayers = getDefinedPlayersOrTargeted(sa);

        // Spells are not undoable
        sa.setUndoable(sa.isAbility() && sa.isUndoable() && tgtPlayers.size() < 2);

        final boolean optional = sa.hasParam("Optional");
        final Game game = sa.getActivatingPlayer().getGame();

        if (optional && !sa.getActivatingPlayer().getController().confirmAction(sa, null, Localizer.getInstance().getMessage("lblDoYouWantAddMana"))) {
            return;
        }

        for (Player p : tgtPlayers) {
            if (sa.usesTargeting() && !p.canBeTargetedBy(sa)) {
                // Illegal target. Skip.
                continue;
            }

            if (abMana.isComboMana()) {
                int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(card, sa.getParam("Amount"), sa) : 1;

                String express = abMana.getExpressChoice();
                String colorsProducedStr = abMana.getComboColors(sa);
                String[] colorsProduced = colorsProducedStr.split(" ");
                String usedToPayMana = (sa.getUsedToPayMana() == null ? null : sa.getUsedToPayMana().toString());

                final StringBuilder choiceString = new StringBuilder();
                ColorSet colorOptions = ColorSet.fromNames(colorsProduced);
                String[] colorsNeeded = express.isEmpty() ? null : express.split(" ");
                boolean differentChoice = abMana.getOrigProduced().contains("Different");
                ColorSet fullOptions = colorOptions;
                // Use specifyManaCombo if possible
                if (colorsNeeded == null && amount > 1 && !sa.hasParam("TwoEach")) {
                    Map<Byte, Integer> choices = p.getController().specifyManaCombo(sa, colorOptions, amount, differentChoice);
                    for (Map.Entry<Byte, Integer> e : choices.entrySet()) {
                        Byte chosenColor = e.getKey();
                        String choice = MagicColor.toShortString(chosenColor);
                        Integer count = e.getValue();
                        while (count > 0) {
                            if (choiceString.length() > 0) {
                                choiceString.append(" ");
                            }
                            choiceString.append(choice);
                            --count;
                        }
                    }
                } else {
                    for (int nMana = 0; nMana < amount; nMana++) {
                        String choice = "";
                        if (colorsNeeded != null && colorsNeeded.length > nMana) { // select from express choices if possible
                            colorOptions = ColorSet
                                    .fromMask(fullOptions.getColor() & ManaAtom.fromName(colorsNeeded[nMana]));
                        }
                        if (colorsProduced.length > 0 && !colorsProduced[0].isEmpty() && sa.getNeedChooseMana()) {
                            choice = MagicColor.toShortString(sa.getActivatingPlayer().getController().chooseColor("Select Mana to Produce", sa, fullOptions));
                        } else if (colorOptions.isColorless() && colorsProduced.length > 0) {
                            // If we just need generic mana, no reason to ask the controller for a choice,
                            // just use the first possible color.
                            choice = colorsProduced[differentChoice ? nMana : 0];
                        } else {
                            byte chosenColor = 0;
                            if(usedToPayMana != null) {
                                ArrayList<String> new_colors = new ArrayList<>();
                                
                                if((colorOptions.hasWhite() || colorsProducedStr.contains("W")) && (usedToPayMana.contains("{W") || usedToPayMana.contains("W}"))) {
                                    new_colors.add("white");
                                }
                                if((colorOptions.hasBlue() || colorsProducedStr.contains("U")) && (usedToPayMana.contains("{U") || usedToPayMana.contains("U}"))) {
                                    new_colors.add("blue");
                                }
                                if((colorOptions.hasBlack() || colorsProducedStr.contains("B")) && (usedToPayMana.contains("{B") || usedToPayMana.contains("B}"))) {
                                    new_colors.add("black");
                                }
                                if((colorOptions.hasRed() || colorsProducedStr.contains("R")) && (usedToPayMana.contains("{R") || usedToPayMana.contains("R}"))) {
                                    new_colors.add("red");
                                }
                                if((colorOptions.hasGreen() || colorsProducedStr.contains("G")) && (usedToPayMana.contains("{G") || usedToPayMana.contains("G}"))) {
                                    new_colors.add("green");
                                }
                                
                                if(new_colors.size() == 1) {
                                    chosenColor = MagicColor.fromName(new_colors.iterator().next());
                                } else if(new_colors.size() > 1) {
                                    chosenColor = sa.getActivatingPlayer().getController().chooseColor("Select Mana to Produce", sa, ColorSet.fromNames(new_colors));
                                } else {
                                    chosenColor = MagicColor.fromName(MagicColor.toLongString(colorOptions.iterator().next()));
                                }

                                if(amount > 1) {
                                    switch(chosenColor) {
                                    case MagicColor.WHITE:
                                        usedToPayMana = usedToPayMana.replaceFirst("\\{[^{}]*W[^{}]*\\}", "");
                                        break;
                                    case MagicColor.BLUE:
                                        usedToPayMana = usedToPayMana.replaceFirst("\\{[^{}]*U[^{}]*\\}", "");
                                        break;
                                    case MagicColor.BLACK:
                                        usedToPayMana = usedToPayMana.replaceFirst("\\{[^{}]*B[^{}]*\\}", "");
                                        break;
                                    case MagicColor.RED:
                                        usedToPayMana = usedToPayMana.replaceFirst("\\{[^{}]*R[^{}]*\\}", "");
                                        break;
                                    case MagicColor.GREEN:
                                        usedToPayMana = usedToPayMana.replaceFirst("\\{[^{}]*G[^{}]*\\}", "");
                                        break;
                                    }
                                }
                            }
                            else {
                                chosenColor = sa.getActivatingPlayer().getController().chooseColor("Select Mana to Produce", sa,
                                        differentChoice ? fullOptions : colorOptions);
                            }
                            if (chosenColor == 0)
                                throw new RuntimeException("ManaEffect::resolve() /*combo mana*/ - " + p + " color mana choice is empty for " + card.getName());

                            if (differentChoice) {
                                fullOptions = ColorSet.fromMask(fullOptions.getColor() - chosenColor);
                            }
                            choice = MagicColor.toShortString(chosenColor);
                        }

                        if (nMana > 0) {
                            choiceString.append(" ");
                        }
                        choiceString.append(choice);
                        if (sa.hasParam("TwoEach")) {
                            choiceString.append(" ").append(choice);
                        }
                    }
                }

                if (choiceString.toString().isEmpty() && "Combo ColorIdentity".equals(abMana.getOrigProduced())) {
                    // No mana could be produced here (non-EDH match?), so cut short
                    return;
                }

                game.getAction().notifyOfValue(sa, card, Localizer.getInstance().getMessage("lblPlayerPickedChosen", p.getName(), choiceString), p);
                abMana.setExpressChoice(choiceString.toString());
            }
            else if (abMana.isAnyMana()) {
                // AI color choice is set in ComputerUtils so only human players need to make a choice
                boolean auto = false;
                if(sa.getUsedToPayMana() != null && sa.isUndoable() && !sa.getNeedChooseMana()) {
                    String usedToPayMana = sa.getUsedToPayMana().toString();
                    if (!usedToPayMana.contains("W") && !usedToPayMana.contains("U") && !usedToPayMana.contains("B") && !usedToPayMana.contains("R") && !usedToPayMana.contains("G")) {
                        auto = true;
                    }
                }

                String colorsNeeded = abMana.getExpressChoice();
                String choice = "";

                if(auto) {
                    choice = "W";
                } else {
                    ColorSet colorMenu = null;
                    byte mask = 0;
                    //loop through colors to make menu
                    for (int nChar = 0; nChar < colorsNeeded.length(); nChar++) {
                        mask |= MagicColor.fromName(colorsNeeded.charAt(nChar));
                    }
                    colorMenu = mask == 0 ? ColorSet.ALL_COLORS : ColorSet.fromMask(mask);
                    byte val = p.getController().chooseColor(Localizer.getInstance().getMessage("lblSelectManaProduce"), sa, colorMenu);
                    if (0 == val) {
                        throw new RuntimeException("ManaEffect::resolve() /*any mana*/ - " + p + " color mana choice is empty for " + card.getName());
                    }
                    choice = MagicColor.toShortString(val);
                }

                game.getAction().notifyOfValue(sa, card, Localizer.getInstance().getMessage("lblPlayerPickedChosen", p.getName(), choice), p);
                abMana.setExpressChoice(choice);
            }
            else if (abMana.isSpecialMana()) {
                String type = abMana.getOrigProduced().split("Special ")[1];

                if (type.equals("EnchantedManaCost")) {
                    Card enchanted = card.getEnchantingCard();
                    if (enchanted == null)
                        continue;

                    StringBuilder sb = new StringBuilder();
                    int generic = enchanted.getManaCost().getGenericCost();

                    for (ManaCostShard s : enchanted.getManaCost()) {
                        ColorSet cs = ColorSet.fromMask(s.getColorMask());
                        byte chosenColor;
                        if (cs.isColorless())
                            continue;
                        if (s.isOr2Generic()) { // CR 106.8
                            chosenColor = p.getController().chooseColorAllowColorless(Localizer.getInstance().getMessage("lblChooseSingleColorFromTarget", s.toString()), card, cs);
                            if (chosenColor == MagicColor.COLORLESS) {
                                generic += 2;
                                continue;
                            }
                        }
                        else if (cs.isMonoColor())
                            chosenColor = s.getColorMask();
                        else /* (cs.isMulticolor()) */ {
                            chosenColor = p.getController().chooseColor(Localizer.getInstance().getMessage("lblChooseSingleColorFromTarget", s.toString()), sa, cs);
                        }
                        sb.append(MagicColor.toShortString(chosenColor));
                        sb.append(' ');
                    }
                    if (generic > 0) {
                        sb.append(generic);
                    }

                    abMana.setExpressChoice(sb.toString().trim());
                } else if (type.equals("LastNotedType")) {
                    final StringBuilder sb = new StringBuilder();
                    int nMana = 0;
                    for (Object o : card.getRemembered()) {
                        if (o instanceof String) {
                            sb.append(o.toString());
                            nMana++;
                        }
                    }
                    if (nMana == 0) {
                        return;
                    }
                    abMana.setExpressChoice(sb.toString());
                } else if (type.startsWith("EachColorAmong")) {
                    final String res = type.split("_")[1];
                    final ZoneType zone = type.startsWith("EachColorAmong_") ? ZoneType.Battlefield : ZoneType.smartValueOf(type.split("_")[0].substring(14));
                    final CardCollection list = CardLists.getValidCards(card.getGame().getCardsIn(zone),
                            res, sa.getActivatingPlayer(), card, sa);
                    byte colors = 0;
                    for (Card c : list) {
                        colors |= c.getColor().getColor();
                    }
                    if (colors == 0) return;
                    abMana.setExpressChoice(ColorSet.fromMask(colors));
                } else if (type.startsWith("EachColoredManaSymbol")) {
                    final String res = type.split("_")[1];
                    StringBuilder sb = new StringBuilder();
                    for (Card c : AbilityUtils.getDefinedCards(card, res, sa)) {
                        for (ManaCostShard s : c.getManaCost()) {
                            ColorSet cs = ColorSet.fromMask(s.getColorMask());
                            if (cs.isColorless())
                                continue;
                            sb.append(' ');
                            if (cs.isMonoColor())
                                sb.append(MagicColor.toShortString(s.getColorMask()));
                            else /* (cs.isMulticolor()) */ {
                                byte chosenColor = p.getController().chooseColor(Localizer.getInstance().getMessage("lblChooseSingleColorFromTarget", s.toString()), sa, cs);
                                sb.append(MagicColor.toShortString(chosenColor));
                            }
                        }
                    }
                    abMana.setExpressChoice(sb.toString().trim());
                } else if (type.startsWith("DoubleManaInPool")) {
                    StringBuilder sb = new StringBuilder();
                    for (byte color : ManaAtom.MANATYPES) {
                        sb.append(StringUtils.repeat(MagicColor.toShortString(color), " ", p.getManaPool().getAmountOfColor(color))).append(" ");
                    }
                    abMana.setExpressChoice(sb.toString().trim());
                }
            }

            String mana = GameActionUtil.generatedMana(sa);

            // this can happen when mana is based on criteria that didn't match
            if (mana.isEmpty()) {
                String msg = "AbilityFactoryMana::manaResolve() - special mana effect is empty for";

                Breadcrumb bread = new Breadcrumb(msg);
                bread.setData("Card", card.getName());
                bread.setData("SA", sa.toString());
                Sentry.addBreadcrumb(bread, sa);

                continue;
            }

            abMana.produceMana(mana, p, sa);
        }

        // Only clear express choice after mana has been produced
        abMana.clearExpressChoice();
    }

    /**
     * <p>
     * manaStackDescription.
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param abMana
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param af
     *            a {@link forge.game.ability.AbilityFactory} object.
     *
     * @return a {@link java.lang.String} object.
     */

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        String mana = !sa.hasParam("Amount") || StringUtils.isNumeric(sa.getParam("Amount"))
                ? GameActionUtil.generatedMana(sa) : "mana";
        sb.append("Add ").append(toManaString(mana)).append(".");
        if (sa.hasParam("RestrictValid")) {
            sb.append(" ");
            final String desc = sa.getDescription();
            if (desc.contains("Spend this") && desc.contains(".")) {
                int i = desc.indexOf("Spend this");
                sb.append(desc, i, desc.indexOf(".", i) + 1);
            } else if (desc.contains("This mana can't") && desc.contains(".")) { //for negative restrictions (Jegantha)
                int i = desc.indexOf("This mana can't");
                sb.append(desc, i, desc.indexOf(".", i) + 1);
            } else {
                sb.append("[failed to add RestrictValid to StackDesc]");
            }
        }
        return sb.toString();
    }
}
