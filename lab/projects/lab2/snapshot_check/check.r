args = commandArgs(trailingOnly=TRUE)
t = read.table(args[1], h=F)
aggregate(V6 ~ t$V4, t, FUN=sum)
