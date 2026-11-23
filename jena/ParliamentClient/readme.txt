ParliamentClient testing notes:

Normally, one would expect to see unit tests for the RemoteModel class
here.  However, this class is difficult to test because it requires a
Parliament server process to be running.  Since the server is built by
the "server" project, whose build script runs after the build of
ParliamentClient, testing of RemoteModel is performed there, in
the class ParliamentServerTestCase.

Also, the stress test (package com.bbn.parliament.stresstest)
should be moved into the server project and re-factored to
derive from ParliamentServerBase for similar reasons.
