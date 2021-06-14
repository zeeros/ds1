package it.unitn.ds1;
import akka.actor.*;
import scala.concurrent.duration.Duration;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

import it.unitn.ds1.VirtualSynchManager.CrashReportMsg;
import it.unitn.ds1.VirtualSynchManager.JoinNodeMsg;

public class VirtualSynchActor extends AbstractActor {

  // message sequence number for identification
  private int seqno;

  // group manager
  private final ActorRef manager;

  // whether the node should join through the manager
  private boolean joining;

  // participants (initial group, current and proposed views)
  private final Set<ActorRef> group;
  private final Set<ActorRef> currentView;
  private final Map<Integer, Set<ActorRef>> proposedView;
  private int viewId;

  // last sequence number for each node message (to avoid delivering duplicates)
  private final Map<ActorRef, Integer> membersSeqno;

  // unstable messages
  private final Set<ChatMsg> unstableMsgSet;

  // deferred messages (of a future view)
  private final Set<ChatMsg> deferredMsgSet;

  // group view flushes
  //private final Map<Integer, Set<ActorRef>> flushes;

  private final Random rnd;

  // type of the next simulated crash
  enum CrashType {
    NONE,
    ChatMsg,
    StableChatMsg,
    ViewFlushMsg
  }
  private CrashType nextCrash;

  // number of transmissions before crashing
  private int nextCrashAfter;

  /*-- Actor constructors --------------------------------------------------- */
  public VirtualSynchActor(ActorRef manager, boolean joining) {
    this.manager = manager;
    this.seqno = 1;
    this.joining = joining;
    this.viewId = 0;
    this.group = new HashSet<>();
    this.currentView = new HashSet<>();
    this.proposedView = new HashMap<>();
    this.membersSeqno = new HashMap<>();
    this.unstableMsgSet = new HashSet<>();
    this.deferredMsgSet = new HashSet<>();
    //this.flushes = new HashMap<>();
    this.rnd = new Random();
    this.nextCrash = CrashType.NONE;
    this.nextCrashAfter = 0;
  }

  static public Props props(ActorRef manager, boolean joining) {
    return Props.create(VirtualSynchActor.class, () -> new VirtualSynchActor(manager, joining));
  }

  /*-- Message classes ------------------------------------------------------ */

  // Start message that informs every participant about its peers
  public static class JoinGroupMsg implements Serializable {
    public final List<ActorRef> group;   // an array of group members
    public JoinGroupMsg(List<ActorRef> group) {
      this.group = Collections.unmodifiableList(new ArrayList<>(group));
    }
  }

  public static class SendChatMsg implements Serializable {}

  public static class ChatMsg implements Serializable {
    public final Integer viewId;
    public final ActorRef sender;
    public final Integer seqno;
    public final String content;
    public ChatMsg(int viewId, ActorRef sender, int seqno, String content) {
      this.viewId = viewId;
      this.sender = sender;
      this.seqno = seqno;
      this.content = content;
    }
  }

  public static class StableChatMsg implements Serializable {
    public final ChatMsg stableMsg;
    public StableChatMsg(ChatMsg stableMsg) {
      this.stableMsg = stableMsg;
    }
  }

  public static class StableTimeoutMsg implements Serializable {
    public final ChatMsg unstableMsg;
    public final ActorRef sender;
    public StableTimeoutMsg(ChatMsg unstableMsg, ActorRef sender) {
      this.unstableMsg = unstableMsg;
      this.sender = sender;
    }
  }

  public static class ViewChangeMsg implements Serializable {
    public final Integer viewId;
    public final Set<ActorRef> proposedView;
    public ViewChangeMsg(int viewId, Set<ActorRef> proposedView) {
      this.viewId = viewId;
      this.proposedView = Collections.unmodifiableSet(new HashSet<>(proposedView));
    }
  }

  // TODO use the suggested flush messages (below) for view change

  /*public static class ViewFlushMsg implements Serializable {
    public final Integer viewId;
    public ViewFlushMsg(int viewId) {
      this.viewId = viewId;
    }
  }

  public static class FlushTimeoutMsg implements Serializable {
    public final Integer viewId;
    public FlushTimeoutMsg(int viewId) {
      this.viewId = viewId;
    }
  }*/

  public static class CrashMsg implements Serializable {
    public final CrashType nextCrash;
    public final Integer nextCrashAfter;
    public CrashMsg(CrashType nextCrash, int nextCrashAfter) {
      this.nextCrash = nextCrash;
      this.nextCrashAfter = nextCrashAfter;
    }
  }

  public static class RecoveryMsg implements Serializable {}

  /*-- Actor start logic ---------------------------------------------------------- */

  @Override
  public void preStart() {

    // joining nodes contact the manager
    if(joining) {
      //System.out.println(getSelf().path().name() + " joining through manager");
      manager.tell(new JoinNodeMsg(), getSelf());
    }

    // schedule first ChatMsg
    getContext().system().scheduler().scheduleOnce(
            Duration.create(2, TimeUnit.SECONDS),        // when to send the message
            getSelf(),                                          // destination actor reference
            new SendChatMsg(),                                  // the message to send
            getContext().system().dispatcher(),                 // system dispatcher
            getSelf()                                           // source of the message (myself)
    );
  }

  /*-- Helper methods ---------------------------------------------------------- */

  private int multicast(Serializable m, Set<ActorRef> multicastGroup) {
    int i = 0;
    for (ActorRef r: multicastGroup) {

      // check if the node should crash
      if(m.getClass().getSimpleName().equals(nextCrash.name())) {
        if (i >= nextCrashAfter) {
          //System.out.println(getSelf().path().name() + " CRASH after " + i + " " + nextCrash.name());
          break;
        }
      }

      // send m to r (except to self)
      if (!r.equals(getSelf())) {

        // model a random network/processing delay
        try { Thread.sleep(rnd.nextInt(10)); }
        catch (InterruptedException e) { e.printStackTrace(); }

        r.tell(m, getSelf());
        i++;
      }
    }

    return i;
  }

  // TODO create void putInFlushes and boolean isViewFlushed methods
  // TODO (HINT) a view is flushed if all flush messages associated to it have been received

  private boolean isViewChanging() {

    // the view change is instantaneous in the incomplete implementation,
    // thus this method always returns false
    return false;

    // TODO implement effective view change status check
  }
  
  private void deliver(ChatMsg m, boolean deferred) {
    if(membersSeqno.getOrDefault(m.sender, 0) < m.seqno) {
      membersSeqno.put(m.sender, m.seqno);
      System.out.println(
              getSelf().path().name() + " delivers " + m.seqno
              + " from " + m.sender.path().name() + " in view " + (deferred ? m.viewId : this.viewId)
              + (deferred ? " (deferred)" : "")
      );
    }
  }

  private boolean canDeliver(int viewId) {
    return this.viewId == viewId;
  }

  private void deferredDeliver(int prevViewId, int nextViewId) {

    // a joining node delivers only those messages related to the first view
    if(joining) {
      for (ChatMsg m : deferredMsgSet) {
        if (m.viewId == nextViewId) {
          deliver(m, true);
        }
      }
      return;
    }

    // due to multiple crashes, some views may not have been installed;
    // make sure you deliver all pending messages between views
    for(int i = prevViewId; i <= nextViewId; i++) {
      for (ChatMsg m : deferredMsgSet) {
        if (m.viewId == i) {
          deliver(m, true);
        }
      }
    }
  }

  private void installView(int viewId) {

    // check if there are messages waiting to be delivered in the new view
    deferredDeliver(this.viewId, viewId);

    // update view ID
    this.viewId = viewId;

    // remove flushes, unstable and deferred messages of the old views
    unstableMsgSet.removeIf(unstableMsg -> unstableMsg.viewId < this.viewId);
    deferredMsgSet.removeIf(deferredMsg -> deferredMsg.viewId <= this.viewId);

    // TODO also remove old view flush messages!
    //flushes.entrySet().removeIf(entry -> entry.getKey() < this.viewId);

    // update current view
    currentView.clear();
    currentView.addAll(proposedView.get(this.viewId));

    // remove proposed view entry as it is not needed anymore
    proposedView.entrySet().removeIf(entry -> entry.getKey() <= this.viewId);

    System.out.println(
            getSelf().path().name() + " installs view " + this.viewId + " " + currentView
            + " with updated proposedView " + proposedView
    );

    // a joining node is a full member now
    joining = false;
  }

  /*-- Actor message handlers ---------------------------------------------------------- */

  private void onJoinGroupMsg(JoinGroupMsg msg) {

    // initialize group
    group.addAll(msg.group);

    // at the beginning, the view includes all nodes in the group
    currentView.addAll(group);
  }

  private void onSendChatMsg(SendChatMsg msg) {

    // schedule next ChatMsg
    getContext().system().scheduler().scheduleOnce(
            Duration.create(rnd.nextInt(1000) + 300, TimeUnit.MILLISECONDS),
            getSelf(),                                          // destination actor reference
            new SendChatMsg(),                                  // the message to send
            getContext().system().dispatcher(),                 // system dispatcher
            getSelf()                                           // source of the message (myself)
    );

    // avoid transmitting while joining and during view changes
    if(joining || isViewChanging()) return;

    // prepare chat message and add it to the unstable set
    String content = "Message " + seqno + " in view " + currentView;
    ChatMsg m = new ChatMsg(viewId, getSelf(), seqno, content);
    unstableMsgSet.add(m);

    // send message to the group
    int numSent = multicast(m, currentView);
    System.out.println(
            getSelf().path().name() + " multicasts " + m.seqno
                    + " in view " + this.viewId + " to " + (currentView.size() - 1) + " nodes"
                    + " (" + numSent + ", " + currentView + ")"
    );

    // increase local sequence number (for packet identification)
    seqno++;

    // check if the node should crash
    if(nextCrash.name().equals(CrashType.ChatMsg.name())) {
      getContext().become(crashed());
      return;
    }

    // after the message has been sent, it is stable for the sender
    unstableMsgSet.remove(m);

    // broadcast stabilization message
    multicast(new StableChatMsg(m), currentView);

    // check if the node should crash
    if(nextCrash.name().equals(CrashType.StableChatMsg.name())) {
      getContext().become(crashed());
    }
  }

  private void onChatMsg(ChatMsg msg) {

    // joining nodes ignore incoming messages,
    // unless those that may belong to the first view that will be installed eventually
    if(joining) {
      if(isViewChanging() && !getSelf().equals(msg.sender)) {
        System.out.println(getSelf().path().name() + " deferred (joining) " + msg.seqno + " from " + msg.sender.path().name());
        deferredMsgSet.add(msg);
      }
      return;
    }

    // ignore own messages (may be sent during flush protocol)
    if(getSelf().equals(msg.sender)) return;

    // the node will deliver the message, but it will also be kept in the unstable set;
    // if the initiator crashes, we can retransmit the unstable message
    unstableMsgSet.add(msg);

    // deliver immediately or add to deferred to deliver in a future view
    if(canDeliver(msg.viewId)) {
      deliver(msg, false);
    }
    else {
      System.out.println(getSelf().path().name() + " deferred " + msg.seqno + " from " + msg.sender.path().name());
      deferredMsgSet.add(msg);
    }

    // send message to self in order to timeout while waiting stabilization;
    // schedule timeout only if the message was sent by the original initiator,
    // this way we prevent setting a timeout during flush protocol
    if(getSender().equals(msg.sender)) {
      getContext().system().scheduler().scheduleOnce(
              Duration.create(300, TimeUnit.MILLISECONDS),  // how frequently generate them
              getSelf(),                                          // destination actor reference
              new StableTimeoutMsg(msg, getSender()),             // the message to send
              getContext().system().dispatcher(),                 // system dispatcher
              getSelf()                                           // source of the message (myself)
      );
    }
  }

  private void onStableChatMsg(StableChatMsg msg) {
    unstableMsgSet.remove(msg.stableMsg);
    //System.out.println(getSelf().path().name() + " stabilized " + msg.stableMsg.seqno + " from " + msg.stableMsg.sender.path().name());
  }

  private void onStableTimeoutMsg(StableTimeoutMsg msg) {

    // check if the message is still unstable
    if(!unstableMsgSet.contains(msg.unstableMsg)) return;

    // alert the manager about the crashed node
    Set<ActorRef> crashed = new HashSet<>();
    crashed.add(msg.unstableMsg.sender);
    manager.tell(new CrashReportMsg(crashed), getSelf());
  }

  private void onCrashedChatMsg(ChatMsg msg) {

    // deliver immediately or ignore the message;
    // used to debug virtual synchrony correctness
    if(msg.viewId >= this.viewId) {
      if(membersSeqno.getOrDefault(msg.sender, 0) < msg.seqno) {
        membersSeqno.put(msg.sender, msg.seqno);
        System.out.println(
                getSelf().path().name() + " delivers " + msg.seqno
                        + " from " + msg.sender.path().name() + " in view " + msg.viewId
                        + " (crashed)"
        );
      }
    }
  }

  private void onViewChangeMsg(ViewChangeMsg msg) {

    // check whether the node is in the view;
    // the message may have been caused by another node joining
    // and this one may not be part of the view yet
    if(!msg.proposedView.contains(getSelf())) return;

    // store the proposed view to begin transition
    proposedView.put(msg.viewId, new HashSet<>(msg.proposedView));

    // install
    installView(msg.viewId);

    // TODO implement view flushing (do not install the view right away)...
    // TODO (HINT) first, send all unstable messages
    // TODO (HINT) then, send flush messages for the PREVIOUS view
    // TODO (HINT) if you simulate a crash during a flush multicast, change the actor behavior
    /*if(nextCrash.name().equals(CrashType.ViewFlushMsg.name())) {
      getContext().become(crashed());
    }*/
    // TODO (HINT) prepare to timeout if flushes are not received in time
    // TODO (HINT) ...but keep in mind that this node may have already received all of them!
  }

  /*private void onViewFlushMsg(ViewFlushMsg msg) {

    // TODO add sender to flush map at the corresponding view ID
    //putInFlushes(msg.viewId, getSender());

    // TODO check if all flushed were received; in that case install view
    if(isViewFlushed(msg.viewId)) {
      installView(msg.viewId + 1);
    }
  }*/

  /*private void onFlushTimeoutMsg(FlushTimeoutMsg msg) {

    // TODO check if there still are missing flushes
    if(!flushes.containsKey(msg.viewId)) return;

    // TODO find all nodes whose flush has not been received and report them as crashed
    Set<ActorRef> crashed = new HashSet<>(proposedView.get(msg.viewId + 1));
    crashed.removeAll(flushes.get(msg.viewId));
    manager.tell(new CrashReportMsg(crashed), getSelf());
  }*/

  private void onCrashMsg(CrashMsg msg) {
    nextCrash = msg.nextCrash;
    nextCrashAfter = msg.nextCrashAfter;
    //flushes.clear(); // TODO clear flushes
    proposedView.clear();
    deferredMsgSet.clear();
  }

  private void onRecoveryMsg(RecoveryMsg msg) {
    joining = true;
    nextCrash = CrashType.NONE;
    nextCrashAfter = 0;
    getContext().become(createReceive());
    manager.tell(new JoinNodeMsg(), getSelf());

    // schedule first ChatMsg
    getContext().system().scheduler().scheduleOnce(
            Duration.create(1, TimeUnit.SECONDS),         // when to send the message
            getSelf(),                                          // destination actor reference
            new SendChatMsg(),                                  // the message to send
            getContext().system().dispatcher(),                 // system dispatcher
            getSelf()                                           // source of the message (myself)
    );
  }

  // Here we define the mapping between the received message types
  // and our actor methods
  @Override
  public Receive createReceive() {
    return receiveBuilder()
            .match(JoinGroupMsg.class,  this::onJoinGroupMsg)
            .match(SendChatMsg.class,  this::onSendChatMsg)
            .match(ChatMsg.class,  this::onChatMsg)
            .match(StableChatMsg.class,  this::onStableChatMsg)
            .match(StableTimeoutMsg.class,  this::onStableTimeoutMsg)
            .match(ViewChangeMsg.class,  this::onViewChangeMsg)
            .match(CrashMsg.class, this::onCrashMsg)
            .match(RecoveryMsg.class, msg -> System.out.println(getSelf().path().name() + " ignoring RecoveryMsg"))
            .build();

    // TODO match flush messages
  }

  final AbstractActor.Receive crashed() {
    return receiveBuilder()
            .match(RecoveryMsg.class, this::onRecoveryMsg)
            .match(ChatMsg.class, this::onCrashedChatMsg)
            .matchAny(msg -> {})
            //.matchAny(msg -> System.out.println(getSelf().path().name() + " ignoring " + msg.getClass().getSimpleName() + " (crashed)"))
            .build();
  }
}
