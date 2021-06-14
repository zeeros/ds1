package it.unitn.ds1;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import it.unitn.ds1.VirtualSynchActor.JoinGroupMsg;
import it.unitn.ds1.VirtualSynchActor.CrashMsg;
import it.unitn.ds1.VirtualSynchActor.CrashType;
import it.unitn.ds1.VirtualSynchActor.RecoveryMsg;

public class VirtualSynch {
  final static int N_NODES = 2;

  public static void main(String[] args) {

    // Create the actor system
    final ActorSystem system = ActorSystem.create("vssystem");

    // Create a "virtual synchrony manager"
    ActorRef manager = system.actorOf(VirtualSynchManager.props(), "vsmanager");

    // Create nodes and put them to a list
    List<ActorRef> group = new ArrayList<>();
    for (int i=0; i<N_NODES; i++) {
      group.add(system.actorOf(VirtualSynchActor.props(manager, false), "vsnodeG" + i));
    }

    // Send join messages to the manager and the nodes to inform them of the whole group
    JoinGroupMsg start = new JoinGroupMsg(group);
    manager.tell(start, ActorRef.noSender());
    for (ActorRef peer: group) {
      peer.tell(start, ActorRef.noSender());
    }

    inputContinue();

    // Create new nodes and make them join the existing group
    ActorRef joiningFirst = system.actorOf(VirtualSynchActor.props(manager, true), "vsnodeJ0");
    ActorRef joiningSecond = system.actorOf(VirtualSynchActor.props(manager, true), "vsnodeJ1");

    inputContinue();

    // Make one of the new nodes crash while sending stabilization,
    // and the other while sending the flush (which will occur due to the first crash);
    // nextCrashAfter in CrashMsg controls how many messages are correctly sent before crashing
    joiningFirst.tell(new CrashMsg(CrashType.StableChatMsg, 2), ActorRef.noSender());
    joiningSecond.tell(new CrashMsg(CrashType.ViewFlushMsg, 0), ActorRef.noSender());

    inputContinue();

    // Restart nodes (they will join again through the manager)
    joiningFirst.tell(new RecoveryMsg(), ActorRef.noSender());
    joiningSecond.tell(new RecoveryMsg(), ActorRef.noSender());

    inputContinue();

    // Make one of the new nodes crash while sending stabilization,
    // and the other while sending the flush (which will occur due to the first crash);
    // nextCrashAfter in CrashMsg controls how many messages are correctly sent before crashing
    joiningSecond.tell(new CrashMsg(CrashType.ViewFlushMsg, 1), ActorRef.noSender());
    joiningFirst.tell(new CrashMsg(CrashType.ChatMsg, 1), ActorRef.noSender());

    inputContinue();

    // system shutdown
    system.terminate();
  }

  public static void inputContinue() {
    try {
      System.out.println(">>> Press ENTER to continue <<<");
      System.in.read();
    }
    catch (IOException ignored) {}
  }
}
