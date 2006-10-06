#!/usr/bin/env perl
#
# Copyright 2006 IONA Technologies
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

use strict;
use lib "$ENV{VOB_ROOT}/buildtools/lib";
use IO::Select;
use Win32;
use Win32::Job;

# Unfortunately some of our systems do not have an up-to-date
# Socket module. This does a selective import based on version.
#
my $procinfo = undef;
BEGIN {
    require Socket;
    if ($Socket::VERSION > '1.7') {
        import Socket qw(:DEFAULT IPPROTO_TCP TCP_NODELAY);
    } else {
        import Socket ();
    }
}

(my $me = $0) =~ s:^.*[\\\/]::;

my $debug = $ENV{ASYNC_EXEC_DEBUG} || undef;
if ($debug) {
    open(DEBUG, ">>$debug");
    my $s = select(DEBUG);
    $| = 1;
    select($s);
}

my $usage = "usage: $me <argument file>\n";
unless (@ARGV == 1) {
    if ($debug) {
        print DEBUG "$$: usage error: $usage";
        close DEBUG;
    }
    die $usage;
}

(my $argfile = $ARGV[0]) =~ s:\\:/:g;
print DEBUG "$$: using arguments from $argfile\n" if $debug;

END {
    print DEBUG "$$: removing $argfile\n" if $debug;
    unlink $argfile;
    close DEBUG if $debug;
}

open(IN, "$argfile") || die "$me: cannot open $argfile: $!\n";
my @args = <IN>;
close IN;
chomp @args;
my ($default_timeout, $port, $exec_cmd, @exec_args) = @args;
if ($default_timeout !~ /^\d+/) {
    print STDERR "$me: $argfile: illegal default timeout: \"$default_timeout\"\n";
    print STDERR "$me: $argfile contents were as follows:\n";
    print STDERR '   ', join("\n   ", @args), "\n";
    die "$me: number expected for default timeout\n";
}
if ($port !~ /^\d+/) {
    print STDERR "$me: $argfile: illegal port: \"$port\"\n";
    print STDERR "$me: $argfile contents were as follows:\n";
    print STDERR '   ', join("\n   ", @args), "\n";
    die "$me: number expected for port\n";
}
die "$me: $argfile: no launch command provided\n" unless $exec_cmd;
unless (-e $exec_cmd) {
    my @exts = split(/;/, $ENV{PATHEXT});
    foreach my $ext (@exts) {
	if (-e "${exec_cmd}$ext") {
	    $exec_cmd = "${exec_cmd}$ext";
	    last;
	}
    }
}
$exec_cmd = Win32::GetShortPathName($exec_cmd)
  if ($exec_cmd =~ /\s/ && -e $exec_cmd);
foreach (@exec_args) {
    chomp;
    if (/\s/) {
	if (-e $_) {
	    $_ = Win32::GetShortPathName($_);
	} else {
	    $_ = "\"$_\"";
	}
    }
}
if ($debug) {
    print DEBUG "$$: command line: $exec_cmd ", join(' ', @exec_args), "\n";
    print DEBUG "$$: default timeout set to $default_timeout seconds\n";
}

my $job = Win32::Job->new();
die "$me: creating win32 job failed: $^E\n" unless $job;

local *S;
my $tcp = getprotobyname('tcp');
socket(S, PF_INET, SOCK_STREAM, $tcp) || die "$me: socket failed: $!\n";
my $sin = sockaddr_in($port, INADDR_LOOPBACK);
connect(S, $sin) || die "$me: connect to port $port failed: $!\n";
if ($Socket::VERSION > '1.7') {
    no strict 'subs';
    setsockopt(S, $tcp, TCP_NODELAY, pack('l', 1));
}
my $selsave = select(S); $| = 1; select($selsave);
print DEBUG "$$: connected to port $port\n" if $debug;

my $cmd = "$exec_cmd " . join(' ', @exec_args);
my $child_pid = $job->spawn($exec_cmd, $cmd);
die "$me: process creation failed: $^E\n" unless $child_pid;

my $rset = IO::Select->new();
$rset->add(*S);

my $interval = 5;
my $total = 0;
my $ack_needed = 0;

# Returning true (1) from this function makes Win32::Job::watch kill
# the child processes. Returning false (0) means just keep monitoring.
#
sub manage_socket_comms {
    my @fh = IO::Select->select($rset, undef, undef, $interval);
    if (@fh == 0) {
        # Increment the total by twice the interval, since the
        # Win32::Job::watch call (which calls this function)
        # takes up an interval, and select above takes up an interval.
        #
        $total += 2 * $interval;
        if ($total >= $default_timeout) {
	    print DEBUG "$$: timed out waiting for connection\n" if $debug;
            return 1;
        }
    } else {
	print DEBUG "$$: attempting to read from connection\n" if $debug;
	my $cmd = <S>;
	if ($debug) {
	    $cmd =~ s/\r*\n$//;
	    print DEBUG "$$: received \"$cmd\", now killing children\n";
	}
	$ack_needed = 1;
        return 1;
    }
    return 0;
}

my $exit_status = 0;
my $ok = $job->watch(\&manage_socket_comms, $interval);
if ($ok) {
    print DEBUG "$$: child exited normally\n" if $debug;
    my $ret = $job->status();
    $exit_status = $ret->{$child_pid}->{exitcode};
}

if ($ack_needed) {
    print DEBUG "$$: sending acknowledgement\n" if $debug;
    print S "ok\n";
}
print DEBUG "$$: closing socket\n" if $debug;
close S;
print DEBUG "$$: exiting with status $exit_status\n" if $debug;
exit $exit_status;
