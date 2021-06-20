SELF_EXCLUDED = 1 # set to 1 if the multicasts are not sent to self, 0 otherwise


recv = read.table("recv.log", h=T)
send = read.table("send.log", h=T)


n_epoch_send = setNames(aggregate(src ~ epoch, send, FUN=function(x) length(unique(x))), c("epoch", "n_send_tot"))
n_epoch_recv = setNames(aggregate(dst ~ epoch, recv, FUN=function(x) length(unique(x))), c("epoch", "n_recv_tot"))

message("Number of senders/receivers per epoch")
merge(n_epoch_send, n_epoch_recv, by=c("epoch"), all=T)

message("Senders per epoch")
sndr_epoch = unique(send[c("epoch", "src")])
for (e in unique(sndr_epoch$epoch)) {
  message("Epoch ", e)
  print(sort(sndr_epoch[sndr_epoch$epoch == e, "src"]))
}

sendrecv = merge(send, recv, by=c("src", "seqn"), suffixes=c(".send", ".recv"), all=T)

write.table(sendrecv, "sendrecv.log")

message("Not delivered messages")
sendrecv[is.na(sendrecv$epoch.recv),]
message("Not sent but delivered messages")
sendrecv[is.na(sendrecv$epoch.send),]

message("Messages with epoch mismatch")
sendrecv[!is.na(sendrecv$epoch.recv) & !is.na(sendrecv$epoch.send) & sendrecv$epoch.send != sendrecv$epoch.recv,]

message("Duplicates")
dups = duplicated(recv[c("src", "seqn", "dst")])
sum(dups)

# number of receivers per message
n_msg_recv = setNames(
                aggregate(dst ~ seqn + src + epoch, recv, FUN=function(x) length(unique(x))),
                c("seqn", "src", "epoch", "n_recv"))

message("Messages delivered not to all the participants")
t = merge(n_msg_recv, n_epoch_recv, by=c("epoch"))
missed = t[t$n_recv != t$n_recv_tot - SELF_EXCLUDED,]

missed_with_dst = merge(recv, missed, all.x=F, all.y=T)

missed_with_dst_list = merge(
      missed, 
      aggregate(dst ~ src + seqn, missed_with_dst, FUN=function(x) sort(x)),
      by=c("src", "seqn"))

missed_with_dst_list[with(missed_with_dst_list, order(epoch)),]
