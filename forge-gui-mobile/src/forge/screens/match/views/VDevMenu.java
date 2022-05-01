package forge.screens.match.views;

import forge.Forge;
import forge.menu.FCheckBoxMenuItem;
import forge.menu.FDropDownMenu;
import forge.menu.FMenuItem;
import forge.screens.match.MatchController;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.ThreadUtil;

public class VDevMenu extends FDropDownMenu {
    @Override
    protected void buildMenu() {
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblGenerateMana"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() { //must invoke all these in game thread since they may require synchronous user input
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().generateMana(false);
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblTutor"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().tutorForCard(false);
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblRollbackPhase"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() { //must invoke all these in game thread since they may require synchronous user input
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().rollbackPhase();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblCastSpellOrPlayLand"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().castASpell(false);
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblCardToHand"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().addCardToHand(false);
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblCardToBattlefield"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().addCardToBattlefield(false);
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblCardToLibrary"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().addCardToLibrary(false);
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblCardToGraveyard"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().addCardToGraveyard(false);
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblCardToExile"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().addCardToExile(false);
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblRepeatAddCard"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().repeatLastAddition();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblExileFromHand"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().exileCardsFromHand();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblExileFromPlay"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().exileCardsFromBattlefield();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblRemoveFromGame"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().removeCardsFromGame();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblSetLife"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().setPlayerLife(false);
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblWinGame"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().winGame(false);
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblSetupGame"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().setupGameState(false);
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblDumpGame"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().dumpGameState(false);
                    }
                });
            }
        }));

        final boolean unlimited = MatchController.instance.getGameController().canPlayUnlimited();
        addItem(new FCheckBoxMenuItem("Play Unlimited", unlimited,
                new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                MatchController.instance.getGameController().cheat().setCanPlayUnlimited(!unlimited);
            }
        }));
        final boolean viewAll = MatchController.instance.getGameController().mayLookAtAllCards();
        addItem(new FCheckBoxMenuItem(Forge.getLocalizer().getMessage("lblViewAll"), viewAll,
                new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                MatchController.instance.getGameController().cheat().setViewAllCards(!viewAll);
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblAddCounterPermanent"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().addCountersToPermanent(false);
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblSubCounterPermanent"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().removeCountersFromPermanent(false);
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblTapPermanent"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().tapPermanents(false);
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblUntapPermanent"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().untapPermanents(false);
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblRiggedRoll"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().riggedPlanarRoll();
                    }
                });
            }
        }));
        addItem(new FMenuItem(Forge.getLocalizer().getMessage("lblWalkTo"), new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                ThreadUtil.invokeInGameThread(new Runnable() {
                    @Override
                    public void run() {
                        MatchController.instance.getGameController().cheat().planeswalkTo();
                    }
                });
            }
        }));
    }
}
