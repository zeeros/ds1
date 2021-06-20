import sys
import re

log = sys.argv[1]

S = re.compile("(?P<src>\d+) send multicast (?P<seqn>[\w-]+) within (?P<epoch>\d+)")
R = re.compile("(?P<dst>\d+) deliver multicast (?P<seqn>[\w-]+) from (?P<src>\d+) within (?P<epoch>\d+)")
V = re.compile("(?P<id>\d+) install view (?P<epoch>\d+) (?P<group>.+)")

sent = {}
recv = {}

with open(log,"r") as f, open("recv.log", "w") as r, open("send.log", "w") as s, open("view.log", "w") as v:
    s.write("epoch\tsrc\tseqn\n")
    r.write("epoch\tsrc\tseqn\tdst\n")
    for l in f:
        m = S.search(l)
        if m:
            g = m.groupdict()
            s.write("{}\t{}\t{}\n".format(g["epoch"], g["src"], g["seqn"]))
            continue
        m = R.search(l)
        if m:
            g = m.groupdict()
            r.write("{}\t{}\t{}\t{}\n".format(g["epoch"], g["src"], g["seqn"], g["dst"]))
            continue
        m = V.search(l)
        if m:
            g = m.groupdict()
            group = sorted([int(x.strip(" \t.")) for x in g["group"].split(",")])
            v.write("{} {} {}\n".format(g["epoch"], g["id"], group))
            continue
            
        
