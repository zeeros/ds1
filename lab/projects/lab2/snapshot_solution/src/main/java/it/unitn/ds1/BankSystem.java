package it.unitn.ds1;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import it.unitn.ds1.Bank.JoinGroupMsg;

public class BankSystem {
  final static int N_BRANCHES = 10;

  public static void main(String[] args) {
    // Create the actor system
    final ActorSystem system = ActorSystem.create("banksystem");

    // Create bank branches and put them to a list
    List<ActorRef> group = new ArrayList<>();
    for (int i=0; i<N_BRANCHES; i++) {
      group.add(system.actorOf(Bank.props(i, i == 0), "bank" + i));
    }

    // Send join messages to the banks to inform them of the whole group
    JoinGroupMsg start = new JoinGroupMsg(group);
    for (ActorRef peer: group) {
      peer.tell(start, ActorRef.noSender());
    }

    // Alternative scheduling for snapshots initiated by bank 0, once every second;
    // currently implemented in preStart() based on the snapshotInitiator parameter.
//    system.scheduler().scheduleWithFixedDelay(
//            Duration.create(1, TimeUnit.SECONDS),
//            Duration.create(1, TimeUnit.SECONDS),
//            group.get(0), new StartSnapshot(), system.dispatcher(), ActorRef.noSender());

    try {
      System.out.println(">>> Press ENTER to exit <<<");
      System.in.read();
    } 
    catch (IOException ioe) {}
    system.terminate();
  }
}
