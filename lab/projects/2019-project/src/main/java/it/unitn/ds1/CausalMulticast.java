package it.unitn.ds1;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import it.unitn.ds1.Chatter.JoinGroupMsg;
import it.unitn.ds1.Chatter.StartChatMsg;
import it.unitn.ds1.Chatter.PrintHistoryMsg;

public class CausalMulticast {
    final private static int N_LISTENERS = 10; // number of listening actors
    private static List<ActorRef> group = new ArrayList<>();
    final private static ActorSystem system = ActorSystem.create("helloakka");
    protected static FileOutputStream outputStream;
    protected static OutputStreamWriter outputStreamWriter;
    protected static BufferedWriter bufferedWriter;

    public static void addToGroup (ActorRef actorRef){
        group.add(actorRef);
    }
    public static void main(String[] args) throws InterruptedException, IOException {

        int id = 0;

        group.add(system.actorOf(Chatter.props(id), "Manager"));
        ActorRef a = system.actorOf(Chatter.props(-1), "Participants1");
        ActorRef b = system.actorOf(Chatter.props(-2), "Participants2");
        ActorRef c = system.actorOf(Chatter.props(-3), "Participants3");
        ActorRef d = system.actorOf(Chatter.props(-4), "Participants4");

        try {
            System.out.println(">>> Press ENTER to start the program <<<");
            System.in.read();

            // send the group member list to everyone in the group
            JoinGroupMsg join = new JoinGroupMsg(0, new Chatter.Groups(0, new ArrayList<Integer>() {{
                add(0);
            }}, group));
            for (ActorRef peer : group) {
                peer.tell(join, null);
            }

            group.get(0).tell(new Chatter.RequestJoin(), a);
            Thread.sleep(10000);
            /*group.get(0).tell(new Chatter.RequestJoin(), a);
            Thread.sleep(10000);*/
            group.get(0).tell(new Chatter.RequestJoin(), b);
            Thread.sleep(18000);
            group.get(2).tell(new  Chatter.Crash(false), b);
            Thread.sleep(15000);
            group.get(0).tell(new Chatter.RequestJoin(), c);
            Thread.sleep(15000);
            group.get(1).tell(new Chatter.Crash(true), a);
            Thread.sleep(20000);
            group.get(0).tell(new Chatter.RequestJoin(), d);

            System.in.read();

            outputStream = new FileOutputStream("output.txt");
            outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");
            bufferedWriter = new BufferedWriter(outputStreamWriter);

            PrintHistoryMsg msg = new PrintHistoryMsg();
            for (ActorRef peer : group) {
                peer.tell(msg, null);
            }

            System.out.println(">>> Press ENTER to exit <<<");
            System.in.read();
            Thread.sleep(2000);
            bufferedWriter.close();
        } catch (IOException ioe) {
        }
        system.terminate();
    }
}
