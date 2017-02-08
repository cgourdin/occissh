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


import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Created by cgourdin on 07/02/2017.
 * This class is a ssh client, this manage ssh connection, ssh session and can execute remote server commands.
 */
public class SshClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SshClient.class);

    private JSch jsch;

    /**
     * Username.
     */
    private String userName = null;
    /**
     * User password.
     */
    private String password = null;
    /**
     * Passphrase if any.
     */
    private String userPassphrase;

    /**
     * Known hosts filename.
     */
    private String knownHostsFilename = null;
    /**
     * Private key content.
     */
    private String privateKey = null;

    /**
     * Ip address or compute hostname.
     */
    private String hostname = null;

    /**
     * Port number, default is 22.
     */
    private Integer port = 22;
    /**
     * Timeout ssh session, default is 0 milliseconds (this means no timeout session).
     */
    private Integer timeout = 0;

    private Session sshSession = null;

    private Channel currentShellChannel = null;

    /**
     * Build ssh client with username and password.
     * @param userName
     * @param password
     * @param userPassphrase
     * @param knownHostsFilename
     * @param hostname
     * @param port
     */
    public SshClient(String userName, String password, String userPassphrase, String knownHostsFilename, String hostname, Integer port) {
        this.userName = userName;
        this.password = password;
        this.userPassphrase = userPassphrase;
        this.knownHostsFilename = knownHostsFilename;
        this.hostname = hostname;
        this.port = port;
        this.jsch = new JSch();
    }

    /**
     * Build ssh client with a private key.
     * @param privateKey
     * @param userPassphrase
     * @param hostname
     * @param port
     */
    public SshClient(String privateKey, String userPassphrase,  String hostname, String knownHostsFilename, Integer port) {
        this.userPassphrase = userPassphrase;
        this.privateKey = privateKey;
        this.hostname = hostname;
        this.knownHostsFilename = knownHostsFilename;
        this.port = port;
        this.jsch = new JSch();

    }

    /**
     * Connect with an ssh session.
     * @throws SshException
     */
    public void connect() throws SshException {
        if (jsch == null) {
            throw new SshException("Ssh client has not been initialized properly.");
        }

        try {
            jsch.setKnownHosts(knownHostsFilename);
            if (privateKey != null) {
                jsch.addIdentity(privateKey);
            }
            sshSession = jsch.getSession(userName, hostname, port);
            sshSession.setPassword(password);

            // For test purpose :
            sshSession.setConfig("StrictHostKeyChecking", "no");

            sshSession.connect(timeout);
            LOGGER.info("Connected !");
        } catch (JSchException ex) {
            String message = "Exception thrown, trying to connect with ssh failed : " + ex.getMessage();
            LOGGER.error(message, ex);
            throw new SshException(message, ex);
        }
    }

    /**
     * Disconnect from remote.
     * @throws SshException
     */
    public void disconnect() throws SshException {
        if (sshSession != null) {
            sshSession.disconnect();
        }
    }

    /**
     * Is ssh session is connected.
     * @return a boolean, true for session connected, false if not connected (or no session).
     */
    public boolean isConnected() {
        boolean result;
        if (sshSession == null) {
            result = false;
        } else {
            result = sshSession.isConnected();
        }
        return result;
    }

    /**
     * Execute an ssh command remotely and return its output.
     * @param command
     * @return String
     */
    public String execute(String command) throws SshException {

        if (jsch == null) {
            throw new SshException("Ssh client has not been initialized properly.");
        }
        if (!isConnected()) {
            this.connect();
        }

        StringBuilder outputBuffer = new StringBuilder();

        try {
            Channel channel = sshSession.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);
            InputStream cmdOutput = channel.getInputStream();
            channel.connect();

            int readByte = cmdOutput.read();

            while(readByte != 0xffffffff) {
                outputBuffer.append((char)readByte);
                readByte = cmdOutput.read();
            }

            channel.disconnect();
        } catch(IOException ex) {
            LOGGER.error("IOException thrown : " + ex.getMessage(), ex);
            throw new SshException("IOException thrown : " + ex.getMessage(), ex);

        } catch(JSchException ex) {
            LOGGER.error("Ssh Exception thrown : " + ex.getMessage(), ex);
            throw new SshException("Ssh Exception thrown : " + ex.getMessage(), ex);
        }
        return outputBuffer.toString();
    }


    public PipedOutputStream executeShellScript(InputStream scriptIn) throws SshException {

        if (jsch == null) {
            throw new SshException("Ssh client has not been initialized properly.");
        }

//        ByteArrayOutputStream os = new ByteArrayOutputStream();
//        String scriptContent;
//        try {
//
//            scriptContent = Utils.copyStream(scriptIn, os);
//            LOGGER.info("Script content : " + scriptContent);
//            if (scriptContent == null || scriptContent.trim().isEmpty()) {
//                throw new SshException("The script is empty, cant execute it.");
//            }
//
//        } catch (IOException ex) {
//            throw new SshException("IOException thrown when trying to read script content : " + ex.getMessage(), ex);
//        }

        // Execute the script in a shell.
//        InputStream in;
//        PipedOutputStream shellStream;
//        try {
//            in = new PipedInputStream();
//            shellStream = new PipedOutputStream((PipedInputStream) in);
//
//        } catch (IOException ex) {
//            throw new SshException("IOException thrown when trying to read script content : " + ex.getMessage(), ex);
//        }

        if (!isConnected()) {
            this.connect();
        }
        try {
            PipedOutputStream shellStream = new PipedOutputStream();
            if (currentShellChannel != null) {
                currentShellChannel.disconnect();
            }
            currentShellChannel = sshSession.openChannel("shell");
            currentShellChannel.setOutputStream(shellStream);
            currentShellChannel.setInputStream(scriptIn);

            currentShellChannel.connect();
            return shellStream;
        } catch (JSchException ex) {
            LOGGER.error("Ssh exception thrown : " + ex.getMessage(), ex);
            throw new SshException("Ssh exception thrown : " + ex.getMessage(), ex);
        }

    }




    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUserPassphrase(String userPassphrase) {
        this.userPassphrase = userPassphrase;
    }

    public String getKnownHostsFilename() {
        return knownHostsFilename;
    }

    public void setKnownHostsFilename(String knownHostsFilename) {
        this.knownHostsFilename = knownHostsFilename;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Channel getCurrentShellChannel() {
        return currentShellChannel;
    }

    public void setCurrentShellChannel(Channel currentShellChannel) {
        this.currentShellChannel = currentShellChannel;
    }



}
