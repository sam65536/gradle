/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.process.internal.streams;

import org.gradle.internal.Stoppable;
import org.gradle.internal.concurrent.DefaultExecutorFactory;
import org.gradle.internal.concurrent.StoppableExecutor;
import org.gradle.util.DisconnectableInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * by Szczepan Faber, created at: 4/17/12
 */
public class StreamsForwarder implements Stoppable, StreamsHandler {

    private final OutputStream standardOutput;
    private final OutputStream errorOutput;
    private final InputStream input;
    private final boolean readErrorStream;
    private StoppableExecutor executor;

    public StreamsForwarder(OutputStream standardOutput, OutputStream errorOutput, InputStream input,
                            boolean readErrorStream, String displayName) {
        this.standardOutput = standardOutput;
        this.errorOutput = errorOutput;
        this.input = input;
        this.readErrorStream = readErrorStream;
        this.executor = new DefaultExecutorFactory().create(displayName);
    }

    public StreamsForwarder(OutputStream standardOutput) {
        this(standardOutput, SafeStreams.systemErr(), SafeStreams.emptyInput(), true, "Forward streams with process.");
    }

    public StreamsForwarder() {
        this(SafeStreams.systemOut(), SafeStreams.systemErr(), SafeStreams.emptyInput(), true, "Forward streams with process.");
    }

    public void start() {
        executor.execute(standardInputRunner);
        if (readErrorStream) {
            executor.execute(errorOutputRunner);
        }
        executor.execute(standardOutputRunner);
    }

    public void stop() {
        try {
            standardInputRunner.closeInput();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        executor.stop();
    }

    private ExecOutputHandleRunner standardOutputRunner;
    private ExecOutputHandleRunner errorOutputRunner;
    private ExecOutputHandleRunner standardInputRunner;

    public void connectStreams(Process process, String processName) {
        InputStream instr = new DisconnectableInputStream(input);

        standardOutputRunner = new ExecOutputHandleRunner("read process standard output",
                process.getInputStream(), standardOutput);
        errorOutputRunner = new ExecOutputHandleRunner("read process error output", process.getErrorStream(),
                errorOutput);
        standardInputRunner = new ExecOutputHandleRunner("write process standard input",
                instr, process.getOutputStream());
    }
}
