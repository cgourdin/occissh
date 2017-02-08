/**
 * Copyright (c) 2015-2017 Inria
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Contributors:
 * - Christophe Gourdin <christophe.gourdin@inria.fr>
 */
package org.occiware.driver.ssh;

import java.io.*;

/**
 * Created by cgourdin on 07/02/2017.
 * Utility class for monitoring values in output (encapsulated in a thread).
 * The lastline property give the current line read on output console.
 */
public class SSHShellThread implements Runnable {

    private SshClient client;

    private InputStream scriptInputStream;

    private String lastline = "";

    public SSHShellThread(SshClient client) {
        this.client = client;
    }

    private PipedOutputStream shellStream;

    private boolean stopThread = false;


    @Override
    public void run() {
        try {
            // Launch connection.
            client.connect();

            shellStream = client.executeShellScript(scriptInputStream);

            PipedInputStream pout = new PipedInputStream(shellStream);

            BufferedReader consoleOutput = new BufferedReader(new InputStreamReader(pout));


            boolean end = false;
            while(!end && !stopThread)
            {
                consoleOutput.mark(32);
                if (consoleOutput.read()==0x03) {
                    end = true;
                }else {
                    consoleOutput.reset();
                    lastline = consoleOutput.readLine();
                    System.out.println("line: " + lastline);
                    end = false;
                }
            }


        } catch (SshException | IOException ex) {
            System.err.println("Error thrown : " + ex.getClass().getName() + " --> " + ex.getMessage());
        } finally {
            cleanup();
        }


    }

    public String getLastLine() {

        return lastline;
    }


    public SshClient getClient() {
        return client;
    }

    public void setClient(SshClient client) {
        this.client = client;
    }

    public InputStream getScriptInputStream() {
        return scriptInputStream;
    }

    public void setScriptInputStream(InputStream scriptInputStream) {
        this.scriptInputStream = scriptInputStream;
    }

    public PipedOutputStream getShellStream() {
        return shellStream;
    }

    public void setShellStream(PipedOutputStream shellStream) {
        this.shellStream = shellStream;
    }

    public void cleanup() {

            if (client.getCurrentShellChannel() != null) {
                client.getCurrentShellChannel().disconnect();
            }
            try {
                client.disconnect();
                System.out.println("Disconnected !!!");
            } catch (SshException ex) {
            }
            if (scriptInputStream != null) {
                Utils.closeQuietly(scriptInputStream);
            }
            if (shellStream != null) {
                Utils.closeQuietly(shellStream);
            }
    }

    public void stop() {
        stopThread = true;
    }

}
