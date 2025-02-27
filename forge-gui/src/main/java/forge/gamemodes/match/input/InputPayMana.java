package forge.gamemodes.match.input;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import forge.ai.ComputerUtilMana;
import forge.ai.PlayerControllerAi;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.card.Card;
import forge.game.cost.CostPart;
import forge.game.cost.CostSacrifice;
import forge.game.cost.CostTap;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityView;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.HumanPlay;
import forge.player.PlayerControllerHuman;
import forge.util.Evaluator;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import forge.util.TextUtil;

public abstract class InputPayMana extends InputSyncronizedBase {
    private static final long serialVersionUID = 718128600948280315L;

    protected int phyLifeToLose = 0;

    protected final Player player;
    protected final Game game;
    protected ManaCostBeingPaid manaCost;
    protected final SpellAbility saPaidFor;
    protected boolean effect;
    protected boolean mandatory = false;
    private final boolean wasFloatingMana;
    private final Queue<Card> delaySelectCards = new LinkedList<>();

    private boolean bPaid = false;
    protected Boolean canPayManaCost = null;

    private boolean locked = false;

    protected InputPayMana(final PlayerControllerHuman controller, final SpellAbility saPaidFor0, final Player player0, final boolean effect) {
        super(controller);
        player = player0;
        game = player.getGame();
        saPaidFor = saPaidFor0;
        this.effect = effect;

        //if player is floating mana, show mana pool to make it easier to use that mana
        wasFloatingMana = !player.getManaPool().isEmpty();
        if (wasFloatingMana) {
            getController().getGui().showManaPool(PlayerView.get(player));
        }
    }

    @Override
    protected void onStop() {
        if (!isFinished()) {
            // Clear current Mana cost being paid for SA
            saPaidFor.setManaCostBeingPaid(null);
            player.popPaidForSA();

            if (wasFloatingMana) { //hide mana pool if it was shown due to floating mana
                getController().getGui().hideManaPool(PlayerView.get(player));
            }
        }
    }

    @Override
    protected boolean onCardSelected(final Card card, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
        if (otherCardsToSelect != null) {
            for (Card c : otherCardsToSelect) {
                for (SpellAbility sa : getAllManaAbilities(c)) {
                    if (sa.canPlay()) {
                        delaySelectCards.add(c);
                        break;
                    }
                }
            }
        }
        if (!getAllManaAbilities(card).isEmpty() && activateManaAbility(card, null, triggerEvent)) {
            return true;
        }
        return activateDelayedCard();
    }

    protected List<SpellAbility> getAllManaAbilities(Card card) {
        List<SpellAbility> result = Lists.newArrayList();
        for (SpellAbility sa : card.getManaAbilities()) {
            result.add(sa);
            result.addAll(GameActionUtil.getAlternativeCosts(sa, player));
        }
        final Collection<SpellAbility> toRemove = Lists.newArrayListWithCapacity(result.size());
        for (final SpellAbility sa : result) {
            sa.setActivatingPlayer(player);
            // fix things like retrace
            // check only if SA can't be cast normally
            if (sa.canPlay(true)) {
                continue;
            }
            toRemove.add(sa);
        }
        result.removeAll(toRemove);
        return result;
    }

    @Override
    public String getActivateAction(Card card) {
        for (SpellAbility sa : getAllManaAbilities(card)) {
            if (sa.canPlay()) {
                return "pay mana with card";
            }
        }
        return null;
    }

    private boolean activateDelayedCard() {
        if (delaySelectCards.isEmpty()) {
            return false;
        }
        if (manaCost.isPaid()) {
            delaySelectCards.clear(); //clear delayed cards if mana cost already paid
            return false;
        }
        if (activateManaAbility(delaySelectCards.poll())) {
            return true;
        }
        return activateDelayedCard();
    }

    @Override
    public boolean selectAbility(final SpellAbility ab) {
        if (ab != null && ab.isManaAbility()) {
            return activateManaAbility(ab.getHostCard(), ab);
        }
        return false;
    }

    @Deprecated
    public List<SpellAbility> getUsefulManaAbilities(Card card) {
        List<SpellAbility> abilities = new ArrayList<>();

        if (card.getController() != player) {
            return abilities;
        }

        byte colorCanUse = 0;
        for (final byte color : ManaAtom.MANATYPES) {
            if (manaCost.isAnyPartPayableWith(color, player.getManaPool())) {
                colorCanUse |= color;
            }
        }
        if (manaCost.isAnyPartPayableWith((byte) ManaAtom.GENERIC, player.getManaPool())) {
            colorCanUse |= ManaAtom.GENERIC;
        }
        if (colorCanUse == 0) { // no mana cost or something
            return abilities;
        }

        final String typeRes = manaCost.getSourceRestriction();
        if (StringUtils.isNotBlank(typeRes) && !card.getType().hasStringType(typeRes)) {
            return abilities;
        }

        for (SpellAbility ma : getAllManaAbilities(card)) {
            ma.setActivatingPlayer(player);
            if (ma.isManaAbilityFor(saPaidFor, colorCanUse))
                abilities.add(ma);
        }
        return abilities;
    }

    public void useManaFromPool(byte colorCode) {
        // find the matching mana in pool.
        if (player.getManaPool().tryPayCostWithColor(colorCode, saPaidFor, manaCost)) {
            onManaAbilityPaid();
            showMessage();
        }
    }

    protected boolean activateManaAbility(final Card card) {
        return activateManaAbility(card, null, null);
    }
    protected boolean activateManaAbility(final Card card, SpellAbility chosenAbility) {
        return activateManaAbility(card, chosenAbility, null);
    }
    protected boolean activateManaAbility(final Card card, SpellAbility chosenAbility, final ITriggerEvent triggerEvent) {
        if (locked) {
            System.err.println("Should wait till previous call to playAbility finishes.");
            return false;
        }

        // make sure computer's lands aren't selected

        byte colorCanUse = 0;
        byte colorNeeded = 0;

        for (final byte color : ManaAtom.MANATYPES) {
            if (manaCost.isAnyPartPayableWith(color, player.getManaPool())) { colorCanUse |= color; }
            if (manaCost.needsColor(color, player.getManaPool()))           { colorNeeded |= color; }
        }
        if (manaCost.isAnyPartPayableWith((byte) ManaAtom.GENERIC, player.getManaPool())) {
            colorCanUse |= ManaAtom.GENERIC;
        }

        if (colorCanUse == 0) { // no mana cost or something
            return false;
        }

        final SpellAbility chosen;
        if (chosenAbility == null) {
            HashMap<SpellAbilityView, SpellAbility> abilitiesMap = new LinkedHashMap<>();
            // you can't remove unneeded abilities inside a for (am:abilities) loop :(

            final String typeRes = manaCost.getSourceRestriction();
            if (StringUtils.isNotBlank(typeRes) && !card.getType().hasStringType(typeRes)) {
                return false;
            }

            boolean guessAbilityWithRequiredColors = true;
            SpellAbility priorAbility = null;
            int amountOfMana = -1;
            boolean hasSpecialEffect = false;
            boolean canProduceMoreMana = false;

            for (SpellAbility ma : getAllManaAbilities(card)) {
                ma.setActivatingPlayer(player);

                if (!ma.isManaAbilityFor(saPaidFor, colorCanUse)) { continue; }

                if(saPaidFor.getHostCard() != null && !saPaidFor.getHostCard().getConvoked().isEmpty()) {
                    boolean isConvoked = false;
                    for(Card c : saPaidFor.getHostCard().getConvoked()) {
                        if(c.getId() == card.getId() && ma.getPayCosts() != null && ma.getPayCosts().getCostParts() != null) {
                            for(CostPart part : ma.getPayCosts().getCostParts()) {
                                if(part instanceof CostSacrifice) {
                                    if(part.payCostFromSource()) {
                                        isConvoked = true;
                                        break;
                                    }
                                }
                            }
                            if(isConvoked) {
                                break;
                            }
                        }
                    }
                    if(!isConvoked && ma.getPayCosts() != null && ma.getPayCosts().getCostParts() != null) {
                        for(CostPart part : ma.getPayCosts().getCostParts()) {
                            if(part instanceof CostSacrifice) {
                                if(!part.payCostFromSource()) {
                                    isConvoked = true;
                                    break;
                                }
                            }
                        }
                    }
                    if(isConvoked) {
                        continue;
                    }
                }

                // If Mana Abilities produce differing amounts of mana, let the player choose
                int maAmount = ma.totalAmountOfManaGenerated(saPaidFor, true);
                if (amountOfMana == -1) {
                    amountOfMana = maAmount;
                } else {
                    if (amountOfMana != maAmount) {
                        guessAbilityWithRequiredColors = false;
                    }
                }

                if(ma.getManaPart() != null && ma.getManaPart().getManaRestrictions() != null && !ma.getManaPart().getManaRestrictions().isEmpty()
                        && (ma.getPayCosts() == null || ma.getPayCosts().hasOnlySpecificCostType(CostTap.class))) {
                    priorAbility = ma;
                }

                abilitiesMap.put(ma.getView(), ma);

                if(ma.hasParam("Amount") && ma.hasParam("RestrictValid") && !(ma.getPayCosts() != null && ma.getPayCosts().hasManaCost())) {
                    canProduceMoreMana = true;
                }

                if(ma.hasParam("AddsNoCounter")) {
                    hasSpecialEffect = true;
                }

                // skip express mana if the ability is not undoable or reusable
                if (!ma.isUndoable() || !ma.getPayCosts().isRenewableResource() || ma.getSubAbility() != null
                        || ma.isManaCannotCounter(saPaidFor)) {
                    guessAbilityWithRequiredColors = false;
                }
            }

            if (abilitiesMap.isEmpty() || (chosenAbility != null && !abilitiesMap.containsValue(chosenAbility))) {
                return false;
            }

            // Store some information about color costs to help with any mana choices
            if (colorNeeded == 0) { // only colorless left
                if (saPaidFor.getHostCard() != null && saPaidFor.getHostCard().hasSVar("ManaNeededToAvoidNegativeEffect")) {
                    String[] negEffects = saPaidFor.getHostCard().getSVar("ManaNeededToAvoidNegativeEffect").split(",");
                    for (String negColor : negEffects) {
                        byte col = ManaAtom.fromName(negColor);
                        colorCanUse |= col;
                    }
                }
            }

            // If the card has any ability that tracks mana spent, skip express Mana choice
            if (saPaidFor.tracksManaSpent()) {
                colorCanUse = ColorSet.ALL_COLORS.getColor();
                guessAbilityWithRequiredColors = false;
            }

            boolean choice = true;
            boolean isPayingGeneric = false;
            if (guessAbilityWithRequiredColors) {
                // express Mana Choice
                if (colorNeeded == 0) {
                    choice = false;
                    //avoid unnecessary prompt by pretending we need White
                    //for the sake of "Add one mana of any color" effects
                    colorNeeded = MagicColor.WHITE;
                    isPayingGeneric = true; // for further processing
                }
                else {
                    final HashMap<SpellAbilityView, SpellAbility> colorMatches = new HashMap<>();
                    for (SpellAbility sa : abilitiesMap.values()) {
                        if (sa.isManaAbilityFor(saPaidFor, colorNeeded)) {
                            colorMatches.put(sa.getView(), sa);
                        }
                    }

                    if (colorMatches.isEmpty()) {
                        // can only match colorless just grab the first and move on.
                        // This is wrong. Sometimes all abilities aren't created equal
                        choice = false;
                    }
                    else if (colorMatches.size() < abilitiesMap.size()) {
                        // leave behind only color matches
                        abilitiesMap = colorMatches;
                    }
                }
            }

            if (hasSpecialEffect && isPayingGeneric) {
                choice = true;
            }

            if (canProduceMoreMana && priorAbility != null) {
                choice = false;
            }

            List<SpellAbility> choices = new ArrayList<>(abilitiesMap.values());
            if(choices.size() > 1) {
                if(choice) {
                    chosen = getController().getAbilityToPlay(card, choices, triggerEvent);
                    if(chosen == null) {
                        return true;
                    }
                } else {
                    if(abilitiesMap.containsValue(priorAbility)) {
                        chosen = priorAbility;
                    } else {
                        chosen = choices.get(0);
                    }
                }
            } else {
                chosen = choices.get(0);
            }
        } else {
            chosen = chosenAbility;
        }

        ColorSet colors = ColorSet.fromMask(0 == colorNeeded ? colorCanUse : colorNeeded);

        chosen.setNeedChooseMana(saPaidFor.tracksManaSpent());

        // Filter the colors for the express choice so that only actually producible colors can be chosen
        int producedColorMask = 0;
        for (final byte color : ManaAtom.MANATYPES) {
            if (chosen.canProduce(MagicColor.toShortString(color)) && colors.hasAnyColor(color)) {
                producedColorMask |= color;
            }
        }
        ColorSet producedAndNeededColors = ColorSet.fromMask(producedColorMask);

        chosen.setManaExpressChoice(producedAndNeededColors);

        // System.out.println("Chosen sa=" + chosen + " of " + chosen.getHostCard() + " to pay mana");

        locked = true;
        game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                chosen.setUsedToPayMana(InputPayMana.this.manaCost);
                boolean paid = HumanPlay.playSpellAbility(getController(), chosen.getActivatingPlayer(), chosen);
                chosen.setUsedToPayMana(null);
                chosen.setNeedChooseMana(false);
                if (paid) {
                    final List<AbilityManaPart> manaAbilities = chosen.getAllManaParts();
                    boolean restrictionsMet = true;

                    for (AbilityManaPart sa : manaAbilities) {
                        if (!sa.meetsManaRestrictions(saPaidFor)) {
                            restrictionsMet = false;
                            break;
                        }
                    }

                    if (restrictionsMet) {
                        player.getManaPool().payManaFromAbility(saPaidFor, InputPayMana.this.manaCost, chosen, !FModel.getPreferences().getPrefBoolean(FPref.UI_SKIP_AUTO_PAY));
                    }
                    onManaAbilityPaid();
                }
                // Need to call this to unlock
                onStateChanged();
            }
        });

        return true;
    }

    protected boolean isAlreadyPaid() {
        if (manaCost.isPaid()) {
            bPaid = true;
        }
        return bPaid;
    }

    protected boolean supportAutoPay() {
        return true;
    }

    protected void runAsAi(Runnable proc) {
        player.runWithController(proc, new PlayerControllerAi(game, player, player.getOriginalLobbyPlayer()));
    }

    @Override
    protected void onOk() {
        if (supportAutoPay() && !locked) { //prevent AI taking over from double-clicking Auto
            locked = true;
            //use AI utility to automatically pay mana cost if possible
            final Runnable proc = new Runnable() {
                @Override
                public void run() {
                    ComputerUtilMana.payManaCost(manaCost, saPaidFor, player, effect);
                }
            };
            //must run in game thread as certain payment actions can only be automated there
            game.getAction().invoke(new Runnable() {
                @Override
                public void run() {
                    runAsAi(proc);
                    onStateChanged();
                }
            });
        }
    }

    protected void updateButtons() {
        if (supportAutoPay()) {
            getController().getGui().updateButtons(getOwner(), Localizer.getInstance().getMessage("lblAuto"), Localizer.getInstance().getMessage("lblCancel"), false, !mandatory, false);
        } else {
            getController().getGui().updateButtons(getOwner(), "", Localizer.getInstance().getMessage("lblCancel"), false, !mandatory, false);
        }
    }

    protected final void updateMessage() {
        locked = false;
        if (activateDelayedCard()) {
            return;
        }
        if (supportAutoPay()) {
            if (canPayManaCost == null) {
                //use AI utility to determine if mana cost can be paid if that hasn't been determined yet
                Evaluator<Boolean> proc = new Evaluator<Boolean>() {
                    @Override
                    public Boolean evaluate() {
                        return ComputerUtilMana.canPayManaCost(manaCost, saPaidFor, player, effect);
                    }
                };
                runAsAi(proc);
                canPayManaCost = proc.getResult();
            }
            if (canPayManaCost) { //enabled Auto button if mana cost can be paid
                getController().getGui().updateButtons(getOwner(), Localizer.getInstance().getMessage("lblAuto"), Localizer.getInstance().getMessage("lblCancel"), true, !mandatory, true);
            }
        }
        showMessage(getMessage(), saPaidFor.getView());
    }

    @Override
    public void showMessage() {
        if (isFinished()) { return; }
        updateButtons();
        onStateChanged();
    }

    protected void onStateChanged() {
        if (isAlreadyPaid()) {
            done();
            stop();
        } else {
            FThreads.invokeInEdtNowOrLater(new Runnable() {
                @Override
                public void run() {
                    updateMessage();
                }
            });
        }
    }

    protected void onManaAbilityPaid() {} // some inputs overload it
    protected abstract void done();
    protected abstract String getMessage();

    @Override
    public String toString() {
        return TextUtil.concatNoSpace("PayManaBase ", manaCost.toString(), " left");
    }

    public boolean isPaid() { return bPaid; }

    protected String messagePrefix;
    public void setMessagePrefix(String prompt) {
        // TODO Auto-generated method stub
        messagePrefix = prompt;
    }
}
