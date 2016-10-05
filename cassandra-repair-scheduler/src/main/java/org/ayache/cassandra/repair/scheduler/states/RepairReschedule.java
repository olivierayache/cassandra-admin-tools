/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ayache.cassandra.repair.scheduler.states;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ayache.automaton.api.IStateRetriever;
import org.ayache.automaton.api.OutGoingTransitions;
import org.ayache.automaton.api.State;
import org.ayache.cassandra.repair.scheduler.NodeReparator;
import org.ayache.cassandra.repair.scheduler.RepairTransition;

/**
 *
 * @author Ayache
 */
@OutGoingTransitions(transitionType = RepairTransition.class, transitions = {"WAKE_UP", "CANCEL"}, init = true)
public class RepairReschedule extends State<RepairContext, Void, RepairRescheduleInner> {

    private static final Calendar CALENDAR = Calendar.getInstance();
    private static final Calendar CALENDAR_ = Calendar.getInstance();

    public RepairReschedule(boolean[] accessor) {
        super(accessor);
    }

    public RepairReschedule(String name, Enum[] outGoingTransitions, boolean[] accessor) {
        super(name, outGoingTransitions, accessor);
    }

    @Override
    public void registerNextStates(IStateRetriever retriever) {
        addNextState(retriever.state(Init.class)).whenWAKE_UP().end();
        addNextState(retriever.state(Cancelled.class)).whenCANCEL().end();
    }

    @Override
    public Void execute(RepairContext context) {
        if (context.cancel) {
            context.addMessage("Waiting for wake up aborted").activate(RepairTransition.CANCEL);
            return null;
        }
        final ReentrantLock reentrantLock = new ReentrantLock();
        reentrantLock.lock();
        Condition newCondition = reentrantLock.newCondition();
        context.setWaitingCondition(reentrantLock, newCondition);
        context.checkAndApplyChanges();
        try {
            boolean shouldShift = false;
            CALENDAR.setTimeInMillis(System.currentTimeMillis());
            CALENDAR_.setTimeInMillis(System.currentTimeMillis());
            CALENDAR_.set(Calendar.HOUR_OF_DAY, context.lastHourToBegin);
            CALENDAR_.set(Calendar.MINUTE, context.lastMinutesToBegin);
            CALENDAR.set(Calendar.HOUR_OF_DAY, context.hourToBegin);
            CALENDAR.set(Calendar.MINUTE, context.minutesToBegin);
            long lastTimeInMillis = CALENDAR_.getTimeInMillis();
            long startTimeInMillis = CALENDAR.getTimeInMillis();
            long time = System.currentTimeMillis();
            if (CALENDAR_.before(CALENDAR)) {
                if (System.currentTimeMillis() < lastTimeInMillis) {
                    shouldShift = true;
                }
                lastTimeInMillis += 24 * 3600 * 1000;
            }else{
                if (time > lastTimeInMillis){
                    startTimeInMillis +=  24 * 3600 * 1000;
                }
            }
            time += 24 * 3600 * 1000 * (shouldShift ? 1 : 0);
            if (time < lastTimeInMillis && time > startTimeInMillis) {
            } else {
                while(time < startTimeInMillis && !context.cancel) { //prevent spurious wakeup
                    newCondition.awaitUntil(new Date(startTimeInMillis));
                    time = System.currentTimeMillis();
                }
            }
            if (context.cancel) {
                context.addMessage("Waiting for wake up cancelled").activate(RepairTransition.CANCEL);
            } else {
                context.addStatus(NodeReparator.Status.STARTED).activate(RepairTransition.WAKE_UP);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(RepairReschedule.class.getName()).log(Level.SEVERE, null, ex);
            context.addMessage("Waiting for wake up cancelled").activate(RepairTransition.CANCEL);
        } finally {
            reentrantLock.unlock();
        }
        return null;
    }

}
