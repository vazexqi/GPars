// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.util;

import groovy.util.GroovyTestCase;
import groovyx.gpars.DataflowMessagingRunnable;
import groovyx.gpars.dataflow.DataflowChannel;
import groovyx.gpars.dataflow.DataflowQueue;
import groovyx.gpars.dataflow.operator.DataflowProcessor;
import groovyx.gpars.group.DefaultPGroup;
import groovyx.gpars.scheduler.ResizeablePool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// implementation
//create operators
//join them with graphs
//create graph object
//pass in list of operators
//pump in data
public class GraphTest extends GroovyTestCase {
    final DefaultPGroup defaultPGroup = new DefaultPGroup(new ResizeablePool(true, 1));
    DataflowChannel channel1 = new DataflowQueue();
    DataflowChannel channel2 = new DataflowQueue();
    DataflowChannel channel3 = new DataflowQueue();

    DataflowChannel stream1 = new DataflowQueue();
    DataflowChannel stream2 = new DataflowQueue();
    DataflowChannel stream3 = new DataflowQueue();

    public void testSimpleGraph() {
        System.out.println("testSimpleGraph");
        final List<DataflowProcessor> operators = new ArrayList<DataflowProcessor>();
        final List<Integer> outputs = new ArrayList<Integer>();
        final List synchro_outputs = Collections.synchronizedList(outputs);

        final DataflowProcessor operator1 = defaultPGroup.operator(false, Arrays.asList(channel1), Arrays.asList(channel2), new DataflowMessagingRunnable(1) {
            @Override
            protected void doRun(final Object[] arguments) {
                int input = (Integer) arguments[0];
                getOwningProcessor().bindOutput(input * input);
            }
        });

        final DataflowProcessor operator2 = defaultPGroup.operator(false, Arrays.asList(channel2), Arrays.asList(channel3), new DataflowMessagingRunnable(1) {
            @Override
            protected void doRun(final Object[] arguments) {
                int input = (Integer) arguments[0];
                getOwningProcessor().bindOutput(input - 1);
            }
        });
        final DataflowProcessor operator3 = defaultPGroup.operator(false, Arrays.asList(channel3), new ArrayList(), new DataflowMessagingRunnable(1) {
            @Override
            protected void doRun(final Object[] arguments) {
                int input = (Integer) arguments[0];
                outputs.add(input);
            }
        });
        operators.add(operator1);
        operators.add(operator2);
        operators.add(operator3);
        Graph g = new Graph(operators);
        channel1.bind(1);
        channel2.bind(2);
        channel2.bind(3);
        channel3.bind(4);
        g.waitForAll();
        assertEquals("Output size does not match", 4, outputs.size());
    }

    public void testBranchingGraph() {
        System.out.println("testBranchingGraph");
        final List<DataflowProcessor> operators = new ArrayList<DataflowProcessor>();
        final List<Integer> outputs = new ArrayList<Integer>();
        final List synchro_outputs = Collections.synchronizedList(outputs);

        final DataflowProcessor operator1 = defaultPGroup.operator(false, Arrays.asList(channel1), Arrays.asList(channel2), new DataflowMessagingRunnable(1) {
            @Override
            protected void doRun(final Object[] arguments) {
                int input = (Integer) arguments[0];
                getOwningProcessor().bindOutput(input * input);
            }
        });
        final List branchStreams = Arrays.asList(stream1, stream2);

        final DataflowProcessor operator2 = defaultPGroup.operator(false, Arrays.asList(channel2), branchStreams, new DataflowMessagingRunnable(1) {
            @Override
            protected void doRun(final Object[] arguments) {
                int input = (Integer) arguments[0];
                getOwningProcessor().bindAllOutputsAtomically(input);
            }
        });
        final DataflowProcessor branch1 = defaultPGroup.operator(false, Arrays.asList(stream1), new ArrayList(), new DataflowMessagingRunnable(1) {
            @Override
            protected void doRun(final Object[] arguments) {
                int input = (Integer) arguments[0];
                synchro_outputs.add(input * 2);
            }
        });
        final DataflowProcessor branch2 = defaultPGroup.operator(false, Arrays.asList(stream2), new ArrayList(), new DataflowMessagingRunnable(1) {
            @Override
            protected void doRun(final Object[] arguments) {
                int input = (Integer) arguments[0];
                synchro_outputs.add(input / 2);
            }
        });
        operators.add(operator1);
        operators.add(operator2);
        operators.add(branch1);
        operators.add(branch2);
        Graph g = new Graph(operators);
        channel1.bind(1);
        channel1.bind(2);
        channel1.bind(3);
        channel1.bind(4);
        g.waitForAll();
        assertEquals("Output size does not match", 8, synchro_outputs.size());
    }

    /*public void testCycleGraph() {
        final DataflowProcessor operator1 = defaultPGroup.operator(Arrays.asList(channel1), Arrays.asList(channel2), new DataflowMessagingRunnable(1) {
            @Override
            protected void doRun(final Object[] arguments) {
                int input = (Integer) arguments[0];
                getOwningProcessor().bindOutput(input * input);
            }
        });
        final DataflowProcessor operator2 = defaultPGroup.operator(Arrays.asList(channel2), Arrays.asList(channel1), new DataflowMessagingRunnable(1) {
            @Override
            protected void doRun(final Object[] arguments) {
                int input = (Integer) arguments[0];
                if(input<10)getOwningProcessor().bindOutput(input+1);
                final List<Integer> outputs = new ArrayList<Integer>();

            }
        });

        assertEquals("Output size does not match", 8, synchro_outputs.size());

    }         */


}
