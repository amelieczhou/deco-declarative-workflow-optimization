#!/usr/bin/env python
import sys
import os

# Import the Python DAX library
os.sys.path.insert(0, "/home/zhouchi/pegasus-source-4.3.2/lib/pegasus/python")
from Pegasus.DAX3 import *


# The name of the DAX file is the first argument
if len(sys.argv) != 2:
	sys.stderr.write("Usage: %s DAXFILE\n" % (sys.argv[0]))
	sys.exit(1)
daxfile = sys.argv[1]


# Create a abstract dag
print "Creating ADAG..."
diamond = ADAG("diamond")


# Add a preprocess job
print "Adding preprocess job..."
preprocess = Job(name="preprocess")
a = File("f.a")
b1 = File("f.b1")
b2 = File("f.b2")
preprocess.addArguments("-i",a,"-o",b1,"-o",b2)
preprocess.uses(a, link=Link.INPUT)
preprocess.uses(b1, link=Link.OUTPUT, transfer=False, register=False)
preprocess.uses(b2, link=Link.OUTPUT, transfer=False, register=False)
diamond.addJob(preprocess)


# Add left Findrange job
print "Adding left Findrange job..."
frl = Job(name="findrange")
c1 = File("f.c1")
frl.addArguments("-i",b1,"-o",c1)
frl.uses(b1, link=Link.INPUT)
frl.uses(c1, link=Link.OUTPUT, transfer=False, register=False)
diamond.addJob(frl)


# Add right Findrange job
print "Adding right Findrange job..."
frr = Job(name="findrange")
c2 = File("f.c2")
frr.addArguments("-i",b2,"-o",c2)
frr.uses(b2, link=Link.INPUT)
frr.uses(c2, link=Link.OUTPUT, transfer=False, register=False)
diamond.addJob(frr)


# Add Analyze job
print "Adding Analyze job..."
analyze = Job(name="analyze")
d = File("f.d")
analyze.addArguments("-i",c1,"-i",c2,"-o",d)
analyze.uses(c1, link=Link.INPUT)
analyze.uses(c2, link=Link.INPUT)
analyze.uses(d, link=Link.OUTPUT, transfer=True, register=False)
diamond.addJob(analyze)


# Add control-flow dependencies
print "Adding control flow dependencies..."
diamond.addDependency(Dependency(parent=preprocess, child=frl))
diamond.addDependency(Dependency(parent=preprocess, child=frr))
diamond.addDependency(Dependency(parent=frl, child=analyze))
diamond.addDependency(Dependency(parent=frr, child=analyze))


# Write the DAX to stdout
print "Writing %s" % daxfile
f = open(daxfile, "w")
diamond.writeXML(f)
f.close()

