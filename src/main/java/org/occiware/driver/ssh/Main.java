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
 */
public class Main {

    public static void main(String[] args) {

        System.out.println("SSH test.");

        String knownHosts = System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "known_hosts";
        String hostname = "localhost";
        int port = 2222;

        SshClient sshClient = new SshClient("occiware", "Occiware1234", null, knownHosts, hostname, port);
        sshClient.setTimeout(5000);
        try {
            // sshClient.connect();
            System.out.println("Connected to : localhost");
            // Launch the script...
            File file = new File(System.getProperty("user.home") + File.separator + "scripts" + File.separator + "marsmon.sh");
            InputStream in = new FileInputStream(file);
            SSHShellThread sshShellThread = new SSHShellThread(sshClient);
            sshShellThread.setScriptInputStream(in);
            Thread thread = new Thread(sshShellThread);
            thread.start();

            while (sshShellThread.getShellStream() == null) {
                System.out.println("no shell stream...");
            }

            try {
                Thread.currentThread().sleep(3000);
            } catch (InterruptedException ex) {
            }
            String line = sshShellThread.getLastLine();
            System.out.println("One line : " + line);
            try {
                Thread.currentThread().sleep(2000);
            } catch (InterruptedException ex) {
            }
            line = sshShellThread.getLastLine();
            System.out.println("Other line : " + line);

            sshShellThread.stop();

        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            throw new RuntimeException(ex);
        }


    }

}
