package edu.usc.ict.iago.agent;

import edu.usc.ict.iago.utils.BehaviorPolicy;
import edu.usc.ict.iago.utils.GameSpec;
import edu.usc.ict.iago.utils.History;
import edu.usc.ict.iago.utils.Offer;

import java.util.Map;

public class Biu5Behavior extends IAGOCoreBehavior implements BehaviorPolicy {

    private AgentUtilsExtension utils;
    private GameSpec game;
    private Map<String, Integer> payoff;
    private Offer allocated;
    private Offer concession;

    @Override
    protected void updateAllocated(Offer update) {

    }

    @Override
    protected Offer getAllocated() {
        return null;
    }

    @Override
    protected Offer getFinalOffer(History history) {
        return null;
    }

    @Override
    protected void setUtils(AgentUtilsExtension utils) {
        this.utils = utils;
        this.game = this.utils.getSpec();
//        this.payoff = this.game.getSimpleUserPoints();
    }

    @Override
    protected Offer getTimingOffer(History history) {
        return null;
    }

    @Override
    protected Offer getAcceptOfferFollowup(History history) {
        return null;
    }

    @Override
    protected Offer getFirstOffer(History history) {
        return null;
    }

    @Override
    protected int getAcceptMargin() {
        return 0;
    }

    @Override
    protected Offer getRejectOfferFollowup(History history) {
        return null;
    }

    @Override
    protected Offer getConceded() {
        return null;
    }

    @Override
    protected void updateAdverseEvents(int change) {

    }

    @Override
    public Offer getNextOffer(History history) {
        return null;
    }
}
